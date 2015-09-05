(ns stocks.nn
  (:require [clojure.core.matrix :as m]
            [clojure.core.matrix.operators :refer :all]
            [clojure.core.matrix.stats :as stats]
            [flambo.conf :as conf]
            [flambo.api :as f]
            [stocks.utils :as utils :refer [fat-partition]])
  (:import (org.apache.spark.mllib.linalg Vector Vectors)
           (org.apache.spark.mllib.regression.LabeledPoint)
           ))

(defn shooting-star-degree
  [h]
  (if (> (:open h) (:close h))
    ; A greeen bar 
    (- (:high h) (:close h))
    ; A red bar
    (- (:low h) (:close h))))

(defn feature-scaling
  [m]
  (m/div (m - (stats/mean m) (stats/sd m))))

(defn feature-engineering [v-in]
  "Given the array of hash tables, returns a matrix that describes a current asset."
  (m/array :vectorz
   (loop [v v-in
          v-window (fat-partition 1 5 v-in)
          v-rolling-exp (first v-in)
          res []]
    (if (empty? v)
      res
      (let [head (first v)
            head-window (first v-window)
            updated-rolling-exp (utils/update-exp 0.25 v-rolling-exp
                                                  (:close (first v)))]
        [(next v)
         (next v-window)
         updated-rolling-exp
         (conj res
               (flatten
                 ; Exponential rolling mean
                 updated-rolling-exp
                 ; The window of the last 5 values
                 (mapv :close head-window)
                 ; The window of the last 5 opening prices
                 (mapv :open head-window)
                 ; The shooting start degree for the last 5 prices
                 (mapv shooting-star-degree head-window)
                 ; Volumes from all the ones in the header
                 (mapv :volume head-window)
                 ; TODO: Features to describe how bullish/bearish was the market
                 ; TODO: Features to measure correlation with other factors
                 ))])))))

