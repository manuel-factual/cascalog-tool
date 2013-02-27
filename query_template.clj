;; Dummy template for query
(ns query-sandbox.query
  (:require [cascalog.api :refer :all]
            [cascalog.ops :as c]
            [cascalog-toolbox.core :refer (thrift-dirtree-to-map-tap)]
            [cheshire.core :as json]
            [clojure.string :as s]
            [datastore-cascalog.taps.keyval :as kv])
  (:gen-class))

(def query
  (<-
    [?a ?b ?count]
    (input-tap ?a ?b)
    (c/count ?count)))

(defn output []
  (?- output-tap
    query))

(defn -main []
  (output))
