(defproject query-sandbox "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [org.apache.hadoop/hadoop-core "0.20.2-cdh3u4"]
                 [cascalog "1.9.0"]
                 [cheshire "4.0.0"]
                 [org.slf4j/slf4j-api "1.7.2"]
                 [org.slf4j/slf4j-log4j12 "1.7.2"]]
  :aot [query-sandbox.query])
