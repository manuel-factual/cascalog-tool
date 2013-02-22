(ns cascalog-tool.subqueries
  (:use compojure.core
        [hiccup core page]))

(defn get-subquery-template [subquery-type arg-map]
  (condp = subquery-type

    "output"
      (str
       "(defn output []\n"
       "  (?- " (condp = (:output_type arg-map)
                  "plain_text" (str "(hfs-textline \"" (:output_path arg-map) "\" :sinkmode :replace)\n")
                  "seqfile"    (str "(hfs-seqfile \"" (:output_path arg-map) "\"  :sinkmode :replace)\n"))
       "    " (:subquery_to_output arg-map) "))")))
