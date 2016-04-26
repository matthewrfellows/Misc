(ns fcfredapi.sql
  (:require [clojure.java.jdbc :as sql]
            [clojure.java.io :as jio]))

;; --------------------------------------

(def sql-password-file-path "resources/sql-password.key")

(def sql-password
  (if (.exists (jio/as-file sql-password-file-path))
    (clojure.string/trim (slurp sql-password-file-path))
    (if-let [k (System/getenv "FRED_SQL_PASS")]
      k
      (throw (Exception. "Unable to determine sql password")))))

(def sql-db
  {:classname "com.mysql.jdbc.Driver"
   :subprotocol "mysql"
   :subname "//localhost:3306/fcfreddata"
   :user "clojure"
   :password sql-password
   :serverTimezone "UTC"
   :useLegacyDatetimeCode false})

;; --------------------------------------


(defn insert!
  [table dat]
  (if (sequential? dat)
    (sql/insert-multi! sql-db table dat)
    (sql/insert! sql-db table dat)))
