(ns fcfredapi.fred
  (:require [clj-http.client :as http]
            [clojure.java.io :as jio]
            [clojure.set :as cset]
            [cheshire.core :as json]
            [fcfredapi.sql :as sql]
            [taoensso.timbre :refer [debug warn info error debugf warnf infof errorf]]))

;; --------------------------------------

(def series-ids ["GDPC1" "UMCSENT" "UNRATE"])

(def save-revision-history? true)

(def apikey-file-path "resources/fredapikey.key")

(def obs-url "https://api.stlouisfed.org/fred/series/observations")
(def max-chunk-size 100000)

(def dat->table-key-map {:realtime_start :rt_start :realtime_end :rt_end})

(def api-key
  (if (.exists (jio/as-file apikey-file-path))
    (clojure.string/trim (slurp apikey-file-path))
    (if-let [k (System/getenv "FRED_API_KEY")]
      k
      (throw (Exception. "Unable to determine FRED API key")))))

;; ---------------------------------------

(defn- fix-value-str
  [v]
  (if (number? v)
    v
    (try 
      (Float. v)
      (catch NumberFormatException e
        nil))))

(defn- api-get
  [url params & {:keys [header opts]}]
  (let [resp (http/get url (merge {:query-params (assoc params :api_key api-key) :throw-exceptions false} 
                                  (when header {:header header})
                                  opts))
        status (:status resp)]
    (if (= 200 status)
      (:body resp)
      (errorf "status %s for request to '%s', with offset %d" (str status) url (:offset params 0)))))

(defn- get-series-chunk-json
  [series-id offset & {chunk-size :chunk-size}]
  {:pre [(string? series-id) (integer? offset) (not (neg? offset))]}
  (let [chunk-size (or chunk-size max-chunk-size)
        params {:series_id series-id :offset offset :limit chunk-size :file_type "json"}]
    (when-let [resp (not-empty (api-get obs-url params))]
      (json/decode resp true))))

(defn- get-series
  [series-id]
  (when-let [data (get-series-chunk-json series-id 0 :chunk-size max-chunk-size)]
    (if (> (:count data) max-chunk-size)
      ;; currently it looks like there will never be more than 100k records for the series we want. But, just in case,
      ;; log that it happened so we can change the code to accomodate.
      (errorf "FRED data for series-id %s exceeds max-chunk-size!" series-id)
      data)))

(defn- save-series-revision
  [series-id recs]
  (try
    (let [resp (sql/insert! :obs_revisions recs)
          n-recs (count recs)
          n-resp (count resp)]
      (when-not (= n-recs n-resp)
        (errorf "Saved only %d out of %d records for obs revisions, series '%s'" n-resp n-recs series-id)))
    (catch Exception e
      (error (.getMessage e)))))

(defn retrieve-and-save-series
  [series-id]
  (try
    (when-let [dat (get-series series-id)]
      (when-let [obs (not-empty (:observations dat))]
        (let [recs (->> obs
                        (map #(assoc % :series series-id))
                        (map #(update-in % [:value] fix-value-str)))]
          (when save-revision-history?
            (save-series-revision series-id (map #(cset/rename-keys % dat->table-key-map) recs)))
          ;; If you wanted to run this regularly to update the values, say daily, you'd need to change
          ;; the following to handle inserting OR updating. In mysql this is typically done with
          ;; INSERT ... ON DUPLICATE ...
          (let [resp (sql/insert! :observations (map #(dissoc % :realtime_start :realtime_end) recs))
                n-recs (count recs)
                n-resp (count resp)]
            (if (= n-recs n-resp)
              n-recs
              (errorf "Saved only %d out of %d records for series '%s'" n-resp n-recs series-id))))))
    (catch Exception e
      (error (str "(" series-id ") " (.getMessage e))))))
  
(defn retrieve-all-series
  []
  (dorun (pmap retrieve-and-save-series series-ids)))
  
  


