(ns stocks.core
  (:require [korma.core :as sql]
            [stocks.db :as db]
            [incanter.charts :as charts]
            [incanter.core :as incanter]
            [stocks.charts :refer [candle-stick-plot]]
            [stocks.utils :refer [flat-seeker]]
            [clojure.core.matrix.stats :refer [mean] :as stats]
            [clojure.set :refer [rename-keys]]
            [clj-time.coerce :refer [to-date]]))

(defn fetch-data [ticker-symbol]
  (->> (sql/select db/price (sql/limit 100) (sql/where {:symbol ticker-symbol}))
       (mapv #(rename-keys % {:open_p :open
                              :close_p :close
                              :volume :volume
                              :date_ex :date
                              :high_p :high
                              :low_p :low }))
       (mapv #(assoc % :date (.getTime (:date %))))  
       (mapv #(select-keys % [:open :close :high
                              :low :volume :date]))))

(defn fat-partition
  [window-size step v]
   (partition 
     window-size step
     (concat (repeat (dec window-size) (first v))
             v)))

(defn generate-window-fnc [f]
  (fn [v window-size]
    (->> v
         (fat-partition window-size 1)
         (map f))))

(def window-rolling-sd (generate-window-fnc stats/sd))
(def window-rolling-average (generate-window-fnc mean))

(comment (defn window-rolling-average
  "v is the vector, window-size, is, well , duh. It is assumed that
  window-size <= len(v)"
  ([v window-size]
    (->> v
         (fat-partition window-size 1)
         (map mean)))))

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
                             :legend true
                             :value-label "Price")
         ~@(mapv (fn [[label v]]
                   `(charts/add-lines ~'timestamps ~v
                                      :series-label ~label))
                 (partition 2 features)))))

(defn plot []
  (let [data (fetch-data "A")
        closing-prices (mapv :close data)
        timestamps (mapv :date data)
        mean-volume (mean (mapv :volume data))]
    ; Candlestick

                       "Machine detected artifical floor / ceilings"
                       (flat-seeker 0.25 timestamps closing-prices)     
    (let [chart (plot-features data
                               "0.25 exponential rolling average"
                               (exp-rolling-average 0.25 closing-prices))
          flat-seqs (flat-seeker 0.25 timestamps closing-prices)]
      (incanter/view 
        (reduce (fn [chart flat-seq]
                  (charts/add-lines chart
                                    (map first flat-seq)
                                    (map second flat-seq)))
                chart flat-seqs)))))



(comment "0.25 exponential rolling average"
(exp-rolling-average 0.25 closing-prices)
"5 day rolling average"
(window-rolling-average closing-prices 5)
"Magnitude of shooting star"  
(map #(let [top (max (:open %) (:close %))
            bottom (max (:open %) (:close %))]
        (max (- (:high %) top)
             (- (:low %) bottom)))
     data)
"Volume of trades"
(map #(* (:volume %) (/ 30 mean-volume)) data)
"Exponential rolling mean, for volume of trades"
(exp-rolling-average 
  0.25
  (map #(* (:volume %) (/ 30 mean-volume)) data)))
