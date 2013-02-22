(ns cascalog-tool.smart-taps
  (:require [cascalog.conf :as cc]
            [cheshire.core :as json]
            [clj-http.client :as client]
            [clojure.java.io :refer (reader)]
            [clojure.string :as s]
            [hadoop-util.core :as hadoop])
  (:import [org.apache.hadoop.fs PathFilter]))

(def ^:const FACTUAL-DEFAULTS
  {
   "fs.default.name" "hdfs://n101.la.prod.factual.com:9000"
   "hadoop.tmp.dir" "/tmp/hadoop-${user.name}"
   "fs.checkpoint.dir" "/var/hadoop/dfs/checkpoint"
   "io.compression.codecs" "org.apache.hadoop.io.compress.GzipCodec,org.apache.hadoop.io.compress.DefaultCodec,com.hadoop.compression.lzo.LzoCodec,com.hadoop.compression.lzo.LzopCodec,org.apache.hadoop.io.compress.BZip2Codec"
   "io.compression.codec.lzo.class" "com.hadoop.compression.lzo.LzoCodec"
   })

(defn get-filesystem
  "Get the current Hadoop file system."
  [& conf-maps]
  (hadoop/filesystem (apply merge FACTUAL-DEFAULTS (cc/project-conf) conf-maps)))

(defn path->input-stream
  "Get an input stream from HDFS for the given path."
  [path]
  (let [path (hadoop/path path)]
    (reader (.open (get-filesystem) path))))

(defn get-hdfs-files-in-dir
  [path]
  "Returns a sequence of file paths in the given directory path.
   If the path itself is a file, this returns a sequence with
   just the given path."
  (let [statuses (.listStatus
                    (get-filesystem)
                    (hadoop/path path)
                    (reify
                      PathFilter
                      (accept [this f]
                       ; Only accept non-hidden files
                       (not (re-find #"^[._]" (.getName f))))))]
    (keep
      (fn [status]
        (when-not (.isDir status)
          (.toString (.getPath status))))
      statuses)))

(defn hdfs-file->line-seq
  "Get a line-seq of the given file."
  ([path]
     (hdfs-file->line-seq path :all))
  ([path number-of-lines]
      (with-open [stream (path->input-stream path)]
        (if (= number-of-lines :all)
          (-> stream line-seq vec)
          (->> stream line-seq (take number-of-lines) vec)))))

(defn check-file
  "Look at the first line of an hdfs path."
  ([path] (check-file path 1))
  ([path n]
     (let [response
           (apply str
                  (interpose "\n"
                             (hdfs-file->line-seq
                              (first (get-hdfs-files-in-dir path))
                              n)))]
       (if (= -1 (.indexOf response "com.factual.common.thrift."))
         response
         ;get deserialized thrift
         (:body
          (client/get
           (str
            "http://dev101.la.prod.factual.com:5678/ds?path="
            (let [full-path
                  (first (get-hdfs-files-in-dir path))
                  first-index (.indexOf full-path "/")]
              (.substring full-path first-index))
            "&rows=10&type=JSON")))))))

(defn guess-type
  "Guess the type of file at the path.
   Possible types:
   seq file (check first couple characters for the class)
   plain text (tsv, json)"
  [path]
  (let [first-line (check-file path)]
    (cond
     (.startsWith first-line "SEQ")
     (let [first-bang (.indexOf first-line "!")]
       [(.substring first-line 0 first-bang )])

     (and (not= -1 (.indexOf first-line "{"))
          (not= -1 (.indexOf first-line "\""))
          (not= -1 (.indexOf first-line "}")))
     ["json"]

     (not= -1 (.indexOf first-line "\t"))
     ["tsv" (inc (count (filter (partial = \tab) first-line)))]

     (= 0 (.length first-line))
     ["not-found"]

     :else
     ["unknown"])))

(defn tap-template
  "Generate a basic input tap template for the given file."
  [file]

  (let [[guess & extra] (guess-type file)]
    (condp #(.startsWith %2 %1) guess
      "SEQ"
      (if (not= -1 (.indexOf guess "com.factual.common.thrift."))
        (str
         "(def query\n"
         "  (<-\n"
         "    [?input]\n"
         "    ((thrift-dirtree-to-map-tap \"" file "\") ?input)))")
        (str
         "(def query\n"
         "  (<-\n"
         "    [<fill-in-vars>]"
         "    ((hfs-seqfile \"" file "\") <fill-in-vars>)))"))

      "tsv"
      (let [out-vars (s/join " " (map str (repeat "?var")
                                  (range 1 (inc (first extra)))))]
        (str
         "(def query\n"
         "  (<-\n"
         "    [" out-vars "]\n"
         "    ((hfs-textline \"" file "\") ?input-line)\n"
         "    (s/split ?input-line #\"\\t\" -1 :> " out-vars ")))"))

      "json"
      (str
       "(def query\n"
       "  (<-\n"
       "    [?input]\n"
       "    ((hfs-textline \"" file "\") ?input-json)\n"
       "    (json/decode ?input-json true :> ?input)))")

      "unknown"
      (str
       "(def query\n"
       "  (<-\n"
       "    [?input]\n"
       "    ((hfs-textline \"" file "\") ?input)))")

      "not-found"
      nil)))
