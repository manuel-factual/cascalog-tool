(ns cascalog-tool.smart-taps
  (:require [cascalog.conf :as cc]
            [cheshire.core :as json]
            [hadoop-util.core :as hadoop])
  (:use [clojure.java.io :only (reader)])
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
  [path]
  (with-open [stream (path->input-stream path)]
    (-> stream
        line-seq
        vec)))

(defn hdfs-path->line-seq
  "Get a line-seq of a given path. If a path is a directory, a concatenated sequence of lines
   of all non-hidden files from the directory is returned"
  [path]
  (mapcat hdfs-file->line-seq (get-hdfs-files-in-dir path)))

(defn check-file
  "Look at the first line of an hdfs path."
  ([path] (check-file path 1))
  ([path n]
     (apply str (interpose "\n"  (take n (hdfs-path->line-seq path))))))

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
       (.substring first-line 0 first-bang ))

     (not= -1 (.indexOf first-line "\t"))
     (str "tsv:" (inc (count (filter (partial = \tab) first-line))))

     (and (not= -1 (.indexOf first-line "{"))
          (not= -1 (.indexOf first-line "\"")))
     "json"

     :else
     (str "unknown:" (.substring first-line 0 20)))))
