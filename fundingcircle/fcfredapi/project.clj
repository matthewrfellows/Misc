(defproject fcfredapi "0.1.0-SNAPSHOT"
  :description "Funding Circle FRED API Code Challenge app"
  :url ""
  :license {:name ""
            :url ""}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/java.jdbc "0.5.8"]
                 [mysql/mysql-connector-java "5.1.38"] ; mysql
                ;; [org.postgresql/postgresql "9.4.1208.jre7"] ; POSTGRES, example.
                 [clj-http "2.1.0"]
                 [cheshire "5.6.1"]
                 [com.taoensso/timbre "4.3.1"]]
  :main ^:skip-aot fcfredapi.core
  :source-paths ["src/clojure"]
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
