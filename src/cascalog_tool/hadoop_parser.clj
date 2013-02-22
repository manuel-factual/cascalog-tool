(ns cascalog-tool.hadoop-parser)

(def HADOOP-JOB-URL-BASE
  "http://n103.la.prod.factual.com:50030/jobdetails.jsp?jobid=")

(defn add-hadoop-step
  [output curr-step job-id]
  (conj output
    {:job_id job-id
     :step   curr-step
     :job_url (str HADOOP-JOB-URL-BASE job-id)}))

(defn parse-hadoop-job-status [output-lines]
  (loop [curr-line (first output-lines)
         remaining-lines (rest output-lines)
         curr-step nil
         output []]
    (if (nil? curr-line)
      output
      (let [step-matches (re-matches #".*starting step: \((\d+/\d+)\).*" curr-line)
            job-matches (re-matches #".*submitted hadoop job: (.*)" curr-line)]
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
          :else
            (recur (first remaining-lines)
                   (rest remaining-lines)
                   curr-step
                   output))))))
