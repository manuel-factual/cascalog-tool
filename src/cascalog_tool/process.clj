(ns cascalog-tool.process
  (:import java.io.File))

(defn exec-shell [cmd working-dir]
  (.exec (Runtime/getRuntime) cmd nil (new File working-dir)))

(defn get-stdout-lineseq [process]
  (line-seq (clojure.java.io/reader (.getInputStream process))))

(defn get-stderr-lineseq [process]
  (line-seq (clojure.java.io/reader (.getErrorStream process))))
