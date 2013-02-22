(ns cascalog-tool.handler
  (:use compojure.core
        [hiccup core page]
        [ring.middleware.params :only [wrap-params]]
        [ring.adapter.jetty :only [run-jetty]])
  (:require [cascalog-tool.runner :as runner]
            [cascalog-tool.hadoop-parser :as hadoop-parser]
            [cascalog-tool.smart-taps :as st]
            [cascalog-tool.subqueries :as subqueries]
            [cheshire.core :as json]
            [clojure.string :as s]
            [compojure.handler :as handler]
            [compojure.route :as route])
  (:import clojure.lang.Compiler
           java.io.StringReader)
  (:gen-class))

(def QUERY_FILE_TEMPLATE "query_template.clj")

(def navbar
  [:div.navbar.navbar-fixed-top.navbar-inverse
    [:div.navbar-inner
      [:div.container-fluid
        [:div.nav-collapse
          [:ul.nav
            [:li
              [:a#submit_link {:href "#"}
                "Submit"]]]]]]])

(def editor-pane
  [:div {:id "editor"}
       (slurp QUERY_FILE_TEMPLATE)])

(def runner-pane
  [:div
    [:a#submit_link.btn.btn-success "Submit!"]
    [:div#runner_output]])

(defn get-form-input-id [form_id input_name]
  (str form_id "_" input_name))

(defn generate-text-input-tuple
  [label name subquery_form_id]
  (let [input_id (get-form-input-id subquery_form_id name)]
    [label name input_id
       [:input.subquery_input {:name name
                               :id input_id}]]))

(defn generate-select-input-tuple
  [label name subquery_form_id label-values]
  (let [input_id (get-form-input-id subquery_form_id name)]
    [label name input_id
       [:select.subquery_input {:name name
                                :id input_id}
        (map
          (fn [[label val]]
            [:option {:value val} label])
          label-values)]]))

(def subqueries-form-inputs
  {"input" [(generate-text-input-tuple "HDFS Path to read from" "file-path" "subquery_form_input")]

   "output" [(generate-text-input-tuple "HDFS Path to output to" "output_path" "subquery_form_output")
             (generate-text-input-tuple "Subquery to output" "subquery_to_output" "subquery_form_output")
             (generate-select-input-tuple "Output Format" "output_type" "subquery_form_output"
              [["Plain Text" "plain_text"]
               ["Sequence File" "seqfile"]])]

   "group_by_count" []
   "group_by_sum" []
   "filter" []
   "simple_join" []})

(def subqueries-output-areas
  {"input" [:div.output
            [:h3 "Input Subquery Example"]
            [:pre.input_template]
            [:h3 "File Preview"]
            [:pre.file_preview]]})

(def generic-subquery-output-area
  [:div.output
    [:h3 "Subquery Example"]
    [:pre.query_template]])

(def generate-subquery-forms
  (for [[id form-inputs] subqueries-form-inputs]
    (let [form_id (str "subquery_form_" id)]
      [:div.subquery_form {:id form_id
                           :subquery_type id
                           :style "display:none"}
        (for [[label field_name id input] form-inputs]
          [:div.control-group
            [:label.control-label {:for id}
              label]
            [:div.controls
              input]])
        (if-let [custom-output-area (subqueries-output-areas id)]
          custom-output-area
          generic-subquery-output-area)])))

(def subquery-pane
  [:div
    [:select#subquery_select
      [:option {:value "input"} "Input Subquery"]
      [:option {:value "output"} "Output Execution"]
      [:option {:value "group_by_count"} "Group by -> Row Count"]
      [:option {:value "group_by_sum"} "Group by -> Sum"]
      [:option {:value "filter"} "Filter by matching values"]
      [:option {:value "simple_join"} "Join 2 inputs"]]
    generate-subquery-forms])

(def tool-pane
  [:div
    [:ul.nav.nav-tabs
      [:li
        [:a {:href "#runner-pane" :data-toggle "tab"}
          "Runner"]]
      [:li
        [:a {:href "#subqueries-pane" :data-toggle "tab"}
          "Subqueries"]]]

    [:div.tab-content
      [:div#runner-pane.tab-pane.active
        runner-pane]
      [:div#subqueries-pane.tab-pane
        subquery-pane]]])

(defn index-page []
  (html
    [:head
      [:title "Cascalog Webtool"]
      [:link {:href "//netdna.bootstrapcdn.com/twitter-bootstrap/2.2.2/css/bootstrap-combined.min.css"
              :rel "stylesheet"}]
      [:script {:src "//ajax.googleapis.com/ajax/libs/jquery/1.8.3/jquery.min.js"}]
      [:script {:src "//netdna.bootstrapcdn.com/twitter-bootstrap/2.2.2/js/bootstrap.min.js"}]
      [:script {:src "http://d1n0x3qji82z53.cloudfront.net/src-min-noconflict/ace.js"}]
      [:script {:src "/javascript/runner.js"}]
      [:script {:src "/javascript/subqueries.js"}]
      [:script {:src "/javascript/index.js"}]
      [:style {:type "text/css"
               :media "screen"}
        "#editor {
          width:100%;
          height:100%;
          position:relative;
        }"]]
    [:body
      [:div {:style "float:right;width:50%"}
        tool-pane]
      [:div {:style "float:right;width:50%"}
        editor-pane]
      [:div#collapsible-template {:style "display:none"}
        [:ul.nav.nav-stacked.nav-tabs
          [:li
            [:a.collapse_link]]]
        [:div.well.content {:style "display:none"}]]]))

(defn get-lines-page []
  (json/encode
    (assoc @runner/runner-output :hadoop-job-status (hadoop-parser/parse-hadoop-job-status (:call-errors @runner/runner-output)))))

(defn run-query-func [text]
  (println "Got request")
  (runner/run-query-func-async text)
  (json/encode {:status "OK"}))

(defn preview-file
  [file]
  (let [file (s/trim file)]
    {:status 200
     :headers {"Content-Type" "text/plain"}
     :body (st/check-file file 10)}))

(defn get-tap-template
  [file]
  (let [file (s/trim file)]
    (if-let [template (st/tap-template file)]
      {:status 200
       :headers {"Content-Type" "text/plain"}
       :body template}
      {:status 404
       :headers {"Content-Type" "text/plain"}
       :body "File not found in hdfs."})))

(defroutes app-routes
  (GET "/" [] (index-page))
  (GET "/get-runner-output" [] (get-lines-page))
  (GET "/preview-file" {{file-path :file-path} :params} (preview-file file-path))
  (GET "/input-template" {{file-path :file-path} :params} (get-tap-template file-path))
  (GET "/get-subquery-template" [subquery_type & arg-map] (subqueries/get-subquery-template subquery_type arg-map))
  (POST "/run" [text] (run-query-func text))
  (route/resources "/")
  (route/not-found "Not Found"))

(def app
  (handler/site (wrap-params app-routes)))

(defn -main []
  (run-jetty app {:port 3000}))
