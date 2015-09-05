(defproject flambo-tutorial "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :plugins [[lein-ring "0.9.6"]]
  :dependencies [[yieldbot/flambo "0.6.0"]
                 [korma "0.4.2"]
                 [mysql/mysql-connector-java "5.1.36"]
                 [org.clojure/clojure "1.7.0"]
                 [net.mikera/core.matrix "0.40.0"]
                 [net.mikera/vectorz-clj "0.34.0"]
                 [net.mikera/core.matrix.stats "0.7.0"]
                 [incanter "1.5.6" :exclusions [clj-time]]
                 ; The webserver stuff
                 [compojure "1.4.0"]
                 [org.clojure/data.json "0.2.6"]
                 [hiccup "1.0.5"]
                 [ring-server "0.3.1"]]
  :ring {:handler app.routes/handler
         :init app.routes/init
         :destroy app.routes/destroy}
  :profiles {:dev
             {:aot  [flambo.function]}
             :provided
             {:dependencies
               [[org.apache.spark/spark-core_2.10 "1.3.0"]
                [org.apache.spark/spark-mllib_2.10 "1.4.1"]
                [org.apache.spark/spark-streaming_2.10 "1.3.0"]
                [org.apache.spark/spark-streaming-kafka_2.10 "1.3.0"]
                [org.apache.spark/spark-streaming-flume_2.10 "1.3.0"]
                [org.apache.spark/spark-sql_2.10 "1.3.0"]]}
             :uberjar {:aot :all}})
