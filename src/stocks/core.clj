(ns stocks.core
  (:require [korma.core :as sql]
            [stocks.db :as db]
            [incanter.charts :as charts]
            [incanter.core :as incanter]
            [clojure.set :refer [rename-keys]]
            [clj-time.coerce :refer [to-date]]))

(defn data []
  (->> (sql/select db/price (sql/limit 100) (sql/where {:symbol "A"}))
       (mapv #(rename-keys % {:open_p :open
                              :close_p :close
                              :volume :volume
                              :date_ex :date
                              :high_p :high
                              :low_p :low }))
       (mapv #(assoc % :date (.getTime (:date %))))  
       (mapv #(select-keys % [:open :close :high
                              :low :volume :date]))))

(defn plot []
  (let [da (data)]
    (println (class (:date (first da))))
    (let [ohlc-data
           (org.jfree.data.xy.DefaultHighLowDataset.
             "Series label"
             (into-array java.util.Date (map to-date (map :date da)))
             (into-array Double/TYPE (mapv double (mapv :high da)))
             (into-array Double/TYPE (mapv double (mapv :low da)))
             (into-array Double/TYPE (mapv double (mapv :open da)))
             (into-array Double/TYPE (mapv double (mapv :close da)))
             (into-array Double/TYPE (mapv double (mapv :volume da))))
          chart (org.jfree.chart.ChartFactory/createCandlestickChart 
                       "title" "time label" "value label"
                       ohlc-data true)   ]
      (-> chart .getPlot .getRangeAxis (.setAutoRangeIncludesZero false))
      (incanter/view chart))))

