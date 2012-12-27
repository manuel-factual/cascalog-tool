(ns cascalog-tool.handler
  (:use compojure.core
        [hiccup core page]
        [ring.middleware.params :only [wrap-params]]
        cascalog.api
        [ring.adapter.jetty :only [run-jetty]])
  (:require [compojure.handler :as handler]
            [compojure.route :as route])
  (:import clojure.lang.Compiler
           java.io.StringReader)
  (:gen-class))

(defn index-page []
  (html
    [:head
      [:title "This is a title..."]
      [:link {:href "//netdna.bootstrapcdn.com/twitter-bootstrap/2.2.2/css/bootstrap-combined.min.css"
              :rel "stylesheet"}]
      [:script {:src "//ajax.googleapis.com/ajax/libs/jquery/1.8.3/jquery.min.js"}]
      [:script {:src "//netdna.bootstrapcdn.com/twitter-bootstrap/2.2.2/js/bootstrap.min.js"}]
      [:script {:src "http://d1n0x3qji82z53.cloudfront.net/src-min-noconflict/ace.js"}]
      [:script {:src "/javascript/index.js"}]
      [:style {:type "text/css"
               :media "screen"}
        "#editor {
          width:100%;
          height:50%;
          position:relative;
        }"]]
    [:body
      [:div {:id "editor"}
            "EDIT ME!!!!"]
      [:button#submit_text.btn
        "Submit"]]))

(defmacro wrap-hadoop-conf [& body]
  `(with-job-conf
    {"fs.default.name" "hdfs://n101:9000"
     "mapred.job.tracker" "n103:9001"}
    ~@body))

(defn run [text]
  (println "Got request")
  (wrap-hadoop-conf
    (load-string text))
  nil)

(defroutes app-routes
  (GET "/" [] (index-page))
  (POST "/run" [text] (run text))
  (route/resources "/")
  (route/not-found "Not Found"))

(def app
  (handler/site (wrap-params app-routes)))

(defn -main []
  (run-jetty app {:port 3000}))
