(ns fcfredapi.core
  (:require [fcfredapi.fred :as fred])
  (:gen-class))
;; ----------------------------------

(defn -main
  "Run me only once."
  [& args]
  (when-let [a (first args)]
    (when (= a "fred")
      (fred/retrieve-all-series))))
