(ns cascalog-tool.handler
  (:use compojure.core
        [hiccup core page]
        [ring.middleware.params :only [wrap-params]]
        [ring.adapter.jetty :only [run-jetty]])
  (:require [cascalog-tool.runner :as runner]
            [cascalog-tool.hadoop-parser :as hadoop-parser]
            [cascalog-tool.smart-taps :as st]
            [cheshire.core :as json]
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

(def subqueries-form-labels
  {"input" [["file-path" "HDFS Path to read from"]]
   "output" [["file-path" "HDFS Path to output to"]]})

(def subqueries-output-areas
  {"input" [:div.output
            [:h3 "Input Subquery Example"]
            [:pre.input_template]
            [:h3 "File Preview"]
            [:pre.file_preview]]})

(def generate-subquery-forms
  (for [[id form-labels] subqueries-form-labels]
    (let [form_id (str "subquery_form_" id)]
      [:div.subquery_form {:id form_id
                           :subquery_type id
                           :style "display:none"}
        (for [[field_name label] form-labels]
          (let [fieldname_id (str form_id "_" field_name)]
            [:div.control-group
              [:label.control-label {:for fieldname_id}
                label]
              [:div.controls
                [:input.subquery_input
                        {:type "text"
                         :id fieldname_id
                         :name field_name}]]]))
        (subqueries-output-areas id)])))

(def subquery-pane
  [:div
    [:select#subquery_select
      [:option {:value "input"} "Input Subquery"]
      [:option {:value "output"} "Output Execution"]]
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
  {:status 200
   :headers {"Content-Type" "text/plain"}
   :body (st/check-file file 10)})

(defn get-tap-template
  [file]
  (if-let [template (st/tap-template file)]
    {:status 200
     :headers {"Content-Type" "text/plain"}
     :body template}
    {:status 404
     :headers {"Content-Type" "text/plain"}
     :body "File not found in hdfs."}))

(defroutes app-routes
  (GET "/" [] (index-page))
  (GET "/get-runner-output" [] (get-lines-page))
  (GET "/preview-file" {{file-path :file-path} :params} (preview-file file-path))
  (GET "/input-template" {{file-path :file-path} :params} (get-tap-template file-path))
  (POST "/run" [text] (run-query-func text))
  (route/resources "/")
  (route/not-found "Not Found"))

(def app
  (handler/site (wrap-params app-routes)))

(defn -main []
  (run-jetty app {:port 3000}))
