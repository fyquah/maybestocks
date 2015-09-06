(ns stocks.core
  (:require [korma.core :as sql]
            [stocks.db :as db]
            [incanter.charts :as charts]
            [incanter.core :as incanter]
            [incanter.stats]
            [stocks.charts :refer [candle-stick-plot]]
            [stocks.utils :refer [flat-seeker fat-partition]]
            [clojure.core.matrix.stats :as stats]
            [clojure.set :refer [rename-keys]]
            [clj-time.coerce :refer [to-date]]))

(defn parse-price-data [data]
  (->> data
      (mapv #(rename-keys % {:open_p :open
                             :close_p :close
                             :volume :volume
                             :date_ex :date
                             :high_p :high
                             :low_p :low }))
      (mapv #(assoc % :date (.getTime (:date %))))
      (mapv #(select-keys % [:open :close :high
                             :low :volume :date]))))

(defn fetch-data
  ([ticker-symbol] (fetch-data ticker-symbol 10000))
  ([ticker-symbol lim]
  (->> (sql/select db/price (sql/limit lim) (sql/where (merge {:symbol ticker-symbol})))
       parse-price-data)))

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
(def window-rolling-average (generate-window-fnc stats/mean))

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

(def STOP-LOSS 0.02)
(def TAKE-PROFIT 0.15)

; Not used, left for reference
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

(def UP 1)
(def DOWN 0)

(defn get-decision [a b c]
  (if (> a c) DOWN UP))

(defn peek-x [x v]
  (if (= 0 x)
    []
    (conj (peek-x (dec x) (pop v))
          (peek v))))

(defn remove-until
  [f coll]
  (if (or (empty? coll) (f (first coll)))
    coll
    (recur f (next coll))))

(defn mean [v]
  (if (empty? v)
    0 (stats/mean v)))

(defn find-closing-price*
  [profit-fnc open-p prices]
  (loop [prices prices
         best open-p]
    (if (empty? prices)
      nil
      (let [head (first prices)]
        (cond
          (< (profit-fnc head) (- (* STOP-LOSS head)))
          ; settle for a stop loss
          head
          (and (> (profit-fnc head) (* TAKE-PROFIT open-p))
               (< (+ head (* STOP-LOSS best)) best))
          ; surpass the take-profit mark
          head
          :else
          (recur (next prices)
                 (max head best)))))))

(defn find-closing-price
  [decision open-p prices]
  (find-closing-price* (if (= "SELL" decision)
                         #(- open-p %)
                         #(- % open-p))
                       open-p
                       prices))

(defn simulate
  [data]
  (->> (loop [timestamps (map :date data)
             closing (->> data (map :close) (map double))
             flat-seq (flat-seeker 0.15 timestamps closing)
             res []]
        (cond (or (empty? flat-seq)
                (empty? timestamps))
              res
              (not= (first timestamps)
                    (-> flat-seq first first first))
              (recur (next timestamps)
                     (next closing)
                     flat-seq
                     res)
              :else
              (let [current-seq (first flat-seq)
                    updated-timestamps (remove-until #(= % (-> flat-seq second
                                                               first first))
                                                     timestamps)
                    updated-closing (drop (- (count timestamps)
                                             (count updated-timestamps))
                                          closing)]
                (recur updated-timestamps
                       updated-closing
                       (next flat-seq)
                       (let [last-3-of-seq (->> current-seq (mapv second) (peek-x 3))
                             decision (if (= UP (apply get-decision last-3-of-seq))
                                        "BUY" "SELL")
                             open-p (last last-3-of-seq)
                             close-p (find-closing-price decision open-p
                                                         updated-closing)]
                         ; we need to determine when to let go.
                         ; these are determined by stop loss and profit exit
                         ; strategy
                         (if (nil? close-p)
                           res
                           (conj res {:open_p     open-p
                                      :close_p    close-p 
                                      :date_ex    (first (last current-seq))
                                      :action     decision})))))))
      (filter (fn [{:keys [close_p open_p]}]
                ; if the difference is more than 50%, discard the result
                ; those are probably stock splits
                (<= (stocks.utils/abs (/ (- open_p close_p)
                                         (min close_p open_p)))
                    0.5)))))

(defn run-and-cache-simulation
  [sym]
  (->> (fetch-data sym)
       (simulate)
       (map (fn [m]
              (assoc m :date_ex
                     (new java.sql.Date (:date_ex m))
                     :symbol sym)))
       (sql/values)
       (sql/insert :simulation))
  (sql/select :simulation (sql/where (= :symbol sym))))

(defn calculate-profit
  [orders]
  (reduce (fn [m order]
            (+ m (if (= (:action order) "BUY")
                   (- (:close_p order) (:open_p order))
                   (- (:open_p order) (:close_p order)))))
          0 orders))

(defn fetch-simulation-results
  "Runs one run of simulation for the particular symbol, DOES NOT 
  persist the result "
  [sym]
  (let [results (sql/select :simulation (sql/where (= :symbol sym)))]
    (if (> (count results) 0)
      results
      (run-and-cache-simulation sym))))

(defn DJIA-summary
  []
  (->> DJIA-list
       (map (fn [sym] 
             (let [orders (fetch-simulation-results sym)]
               [sym {:profit (calculate-profit orders) 
                     :orders orders}])))
       (into {})))

; Not used. left for reference
(defn accuracy [sym]
  (let [data (fetch-data sym)]
    (loop [timestamps (map :date data)
           closing (->> data (map :close) (map double))
           flat-seq (flat-seeker 0.15 timestamps closing)
           res 0.0]
      (cond (or (empty? flat-seq)
              (empty? timestamps))
            res
            (not= (first timestamps)
                  (-> flat-seq first first first))
            (recur (next timestamps)
                   (next closing)
                   flat-seq
                   res)
            :else
            (let [current-seq (first flat-seq)
                  updated-timestamps (remove-until #(= % (-> flat-seq second
                                                             first first))
                                                timestamps)
                  updated-closing (drop (- (count timestamps)
                                           (count updated-timestamps))
                                        closing)]
              (recur updated-timestamps
                     updated-closing
                     (next flat-seq)
                     (if (->> current-seq
                              (mapv second)
                              (peek-x 3)
                              (apply get-decision)
                              (= (if (< (mean (take-last 4 (map second
                                                                      current-seq)))
                                        (mean (take 3 updated-closing)))
                                   UP DOWN)))
                       (inc res)
                       (dec res))))))))
