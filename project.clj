(defproject cascalog-tool "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [compojure "1.1.1"]
                 [ring/ring-jetty-adapter "1.1.4"]
                 [hiccup "1.0.2"]
                 [cascalog "2.0.0-SNAPSHOT"]
                 [org.apache.hadoop/hadoop-core "0.20.2-cdh3u4"]
                 [org.slf4j/slf4j-api "1.7.2"]
                 [org.slf4j/slf4j-log4j12 "1.7.2"]]
  :plugins [[lein-ring "0.7.1"]]
  :ring {:handler cascalog-tool.handler/app}
  :dev-dependencies [[ring-mock "0.1.2"]
                     [lein-hadoop "1.0.0"]]
  :aot [cascalog-tool.handler])
