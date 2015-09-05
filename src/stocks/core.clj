(ns stocks.core
  (:require [korma.core :as sql]
            [stocks.db :as db]
            [incanter.charts :as charts]
            [incanter.core :as incanter]
            [incanter.stats]
            [stocks.charts :refer [candle-stick-plot]]
            [stocks.utils :refer [flat-seeker fat-partition]]
            [clojure.core.matrix.stats :refer [mean] :as stats]
            [clojure.set :refer [rename-keys]]
            [clj-time.coerce :refer [to-date]]))

(defn fetch-data
  ([ticker-symbol] (fetch-data ticker-symbol 100))
  ([ticker-symbol lim]
  (->> (sql/select db/price (sql/limit lim) (sql/where {:symbol ticker-symbol}))
       (mapv #(rename-keys % {:open_p :open
                              :close_p :close
                              :volume :volume
                              :date_ex :date
                              :high_p :high
                              :low_p :low }))
       (mapv #(assoc % :date (.getTime (:date %))))  
       (mapv #(select-keys % [:open :close :high
                              :low :volume :date])))))

(def ^:const DJIA-list
  ["MMM" "AXP" "AAPL" "BA" "CAT" "CVX" "CSCO" "KO" "DIS" "DD"
   "XOM" "GE" "GS" "HD" "IBM" "INTC" "JNJ" "JPM" "MCD" "MRK"
   "MSFT" "NKE" "PFE" "PG" "TRV" "UTX" "UNH" "VZ" "V" "WMT"])

(defn generate-window-fnc [f]
  (fn [v window-size]
    (->> v
         (fat-partition window-size 1)
         (map f))))

(def window-rolling-sd (generate-window-fnc stats/sd))
(def window-rolling-average (generate-window-fnc mean))

(defn exp-rolling-average 
  "p is the rolling average percentage"
  ([v] (exp-rolling-average 0.25 v))
  ([p v]
   (if (empty? v)
     []
     (reduce (fn [memo v]
               (let [tail (peek memo)
                     new-value (+ (* (- 1 p) tail) (* p v))]
                 (conj memo new-value)))
             [(first v)]
             (next v)))))

(defmacro plot-features
  [data & features]
    (assert (even? (count features)))
    `(let [~'timestamps (mapv :date ~data)]
      (-> (candle-stick-plot :data ~data
                             :title "Candle Sticks"
                             :time-label "Time"
                             :value-label "Price")
         ~@(mapv (fn [[label v]]
                   `(charts/add-lines ~'timestamps ~v
                                      :series-label ~label))
                 (partition 2 features)))))

(defn draw-flat-histogram 
  ([sym] (draw-flat-histogram sym 100))
  ([sym lim]
  (let [data (fetch-data sym lim)
        timestamps (->> data (map :date))
        closing-prices (->> data (map :close) (map double))
        flat-seq (flat-seeker 0.25 timestamps closing-prices)]
    (println "Total of " (count flat-seq))
    flat-seq)))

(defn plot-candle-stick 
  ([sym] (plot-candle-stick sym 100))
  ([sym lim]
  (let [data (fetch-data sym lim)
        closing-prices (mapv :close data)
        timestamps (mapv :date data)]
    (let [chart (plot-features data
                               "0.25 exponential rolling average"
                               (exp-rolling-average 0.25 closing-prices))
          flat-seqs (flat-seeker 0.15 timestamps closing-prices)]
      (incanter/view 
        (reduce (fn [chart flat-seq]
                  (charts/add-lines chart
                                    (map first flat-seq)
                                    (map second flat-seq)))
                chart flat-seqs))))))

