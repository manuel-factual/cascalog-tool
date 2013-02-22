(ns cascalog-tool.runner
  (:require [cascalog-tool.process :as process]))

(def runner-thread (atom nil))
(def runner-output (atom {}))

(def QUERY_SANDBOX_BASE "query-sandbox")
(def QUERY_JAR "target/query-sandbox-0.1.0-SNAPSHOT-standalone.jar")
(def QUERY_FILE (str QUERY_SANDBOX_BASE "/src/query_sandbox/query.clj"))
(def QUERY_CLASS "query_sandbox.query")

(defn write-to-file [code]
  (spit QUERY_FILE code))

(defn reset-output!
  []
  (reset! runner-output {
    :compile-output []
    :compile-errors []
    :call-output []
    :call-errors []
    :status :initialize
    :running true
  }))

(defn append-to-key [keyname line]
  (fn [m]
    (update-in m [keyname] #(conj % line))))

(defn set-status! [status]
  (swap! runner-output #(assoc % :status status)))

(defn set-running! [is-running]
  (swap! runner-output #(assoc % :running is-running)))

(defn make-uberjar [code]
  (let [_ (write-to-file code)
        compile-process (process/exec-shell "lein uberjar" QUERY_SANDBOX_BASE)]
    (doseq [line (process/get-stdout-lineseq compile-process)]
      (swap! runner-output (append-to-key :compile-output line)))
    (doseq [line (process/get-stderr-lineseq compile-process)]
      (swap! runner-output (append-to-key :compile-errors line)))
    (.waitFor compile-process)))

(defn call-main []
  (let [call-process (process/exec-shell (str "hadoop jar " QUERY_JAR " " QUERY_CLASS) QUERY_SANDBOX_BASE)]
    (doseq [line (process/get-stdout-lineseq call-process)]
      (swap! runner-output (append-to-key :call-output line)))
    (doseq [line (process/get-stderr-lineseq call-process)]
      (swap! runner-output (append-to-key :call-errors line)))
    (.waitFor call-process)))

(defn run-query-func [code]
  (reset-output!)
  (set-status! :compiling)
  (let [compile-ret-code (make-uberjar code)]
    (if (= compile-ret-code 0)
      (do
        (set-status! :calling)
        (let [call-ret-code (call-main)]
          (if (= call-ret-code 0)
            (do
              (set-status! :completed))
            (do
              (set-status! :call-fail)))
          (set-running! false)
          call-ret-code))
      (do
        (set-status! :compile-fail)
        (set-running! false)
        compile-ret-code))))

(defn run-query-func-async [code]
  (reset! runner-thread (future (run-query-func code))))
