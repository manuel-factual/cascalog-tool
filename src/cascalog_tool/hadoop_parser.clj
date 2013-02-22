(ns cascalog-tool.hadoop-parser
  (:require [clj-http.client :as client]))

(def HADOOP-JOB-URL-BASE
  "http://n103.la.prod.factual.com:50030/jobdetails.jsp?jobid=")

(def HDFS-FILE-BROWSER-BASE
  "http://d101.la.prod.factual.com:50075/browseDirectory.jsp?dir=")

(defn parse-status-from-job-url [job_url]
  (try
    (let [response (:body (client/get job_url))
          status-matcher (re-matcher #"<b>Status:</b>\s+([^<]+)<br>" response)]
      (if (re-find status-matcher)
        (second (re-groups status-matcher))
        nil))
    (catch Exception e
      nil)))

(defn add-hadoop-step
  [output curr-step job-id]
  (let [job-url (str HADOOP-JOB-URL-BASE job-id)]
    (update-in output [:steps]
      #((fnil conj [] nil) %
        {:job_id job-id
         :step   curr-step
         :job_url job-url
         :job_status (parse-status-from-job-url job-url)}))))

(defn add-output-file
  [output file-path]
  (update-in output [:output]
    #((fnil conj [] nil) %
      {:file_path file-path
       :file_url (str "http://d101.la.prod.factual.com:50075/browseDirectory.jsp?dir=" file-path)})))

(defn parse-hadoop-job-status [output-lines]
  (loop [curr-line (first output-lines)
         remaining-lines (rest output-lines)
         curr-step nil
         output {}]
    (if (nil? curr-line)
      output
      (let [step-matches (re-matches #".*starting step: \((\d+/\d+)\).*" curr-line)
            job-matches (re-matches #".*submitted hadoop job: (.*)" curr-line)
            output-matches (re-matches #".*sink:.*\[\"(.*)\"\]\"\]" curr-line)]
        (cond
          step-matches
            (recur (first remaining-lines)
                   (rest remaining-lines)
                   (second step-matches)
                   output)
          job-matches
            (recur (first remaining-lines)
                   (rest remaining-lines)
                   curr-step
                   (add-hadoop-step output curr-step (second job-matches)))
          output-matches
            (recur (first remaining-lines)
                   (rest remaining-lines)
                   curr-step
                   (add-output-file output (second output-matches)))

          :else
            (recur (first remaining-lines)
                   (rest remaining-lines)
                   curr-step
                   output))))))
