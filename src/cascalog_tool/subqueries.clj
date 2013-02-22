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
       "    " (:subquery_to_output arg-map) "))")

    "group_by_count"
      (str
       "(def group-count-query\n"
       "  (<- [<group by vars> ?count]\n"
       "    (input-tap <input-vars>)\n"
       "    (c/count ?count)))\n")

    "group_by_sum"
      (str
       "(def group-sum-query\n"
       "  (<- [<group by vars> ?sum]\n"
       "    (input-tap <input-vars>)\n"
       "    (c/sum <sum-var> :> ?sum)))\n")

    "filter"
      (str
       "(deffilterop filter-op [val]\n"
       "  (#{ <values to allow> } val))\n"
       "\n"
       "(def filter-query\n"
       "  (<- [<output vars>]\n"
       "    (input-tap <input-vars>)\n"
       "    (filter-op <test-var>)))\n")

    "simple_join"
      (str
       "(def join-query\n"
       "  (<- [<output vars>]\n"
       "    (input-tap1 <input vars 1>)\n"
       "    (input-tap2 <input vars 2>)))\n")

  ))
