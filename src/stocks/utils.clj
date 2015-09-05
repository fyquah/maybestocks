(ns stocks.utils
  (:require [clojure.core.matrix :as m]
            [clojure.core.matrix.stats :as stats]))

(defn abs [x]
  (if (< x 0) (- x) x))

(defn portion-diff [v]
  (mapv (fn [new-val old-val]
          (/ (double (- new-val old-val)) old-val))
        (next v) v))

(defn update-exp
  [p old-val new-val]
  (+ (* new-val p)
     (* (- 1 p) old-val)))

(defn fat-partition
  [window-size step v]
   (partition 
     window-size step
     (concat (repeat (dec window-size) (first v))
             v)))

(defn peek-update
  [v f]
  (conj (pop v) (f (peek v))))

(defn flat-seeker
  "Define flat ;
  Limitation, cannot easily conclude a 2 point ceilling / floor (then
  again, those are not conclusive anyway ...)

  Factors to consider:
  1. Check for significant change in the standard deviation distribution (either via
  polarity or value)
  2. Check if price leaves a certain range, governed by the standard deviation.
  3. When a point tries to leave its 'containing box', it is given a tollerance of
  (ceiling/floor - rollingmean) * sensitivity
  
  Should not be too hard to change this to a trend seeker (i.e, detect points which
  follow trends rather than floors / ceilings)"
  [sensitivity x-in v-in]
  (loop [x (next x-in)
         v (next v-in)
         diff-v (portion-diff v-in)
         abs-std 0.5
         polar-std 0
         rolling-mean (first v-in)
         res [[]]]
    (if (empty? v)
      (do 
        (filter #(>= (count %) 4) res))
      (let [head-diff (double (first diff-v))
            head (double (first v))
            current-seq (peek res)]
        (recur (next x)
               (next v)
               (next diff-v)
               (update-exp sensitivity abs-std head-diff)
               (update-exp sensitivity polar-std (abs head-diff))
               (update-exp 0.35 rolling-mean head)
               (if (or 
                     (and 
                       (empty? current-seq)
                       (< (abs (/ (- head rolling-mean)
                                   rolling-mean))
                           abs-std)
                       (< (abs polar-std) sensitivity))
                     (and 
                       ; for non empty current sequences
                       (not (empty? current-seq))
                       ; Check if the downward slope has been confirmed by 
                       ; two transactions
                       ; Skip test if the sequence has less than 5 items
                       (or (< (count current-seq) 3)
                           ; Check if the last two confirmation is implying
                           ; somethig about a downwards breakthrough
                           ; return false if a breakthrough is present
                           ; return true otherwise
                           (let [data (map #(double (second %))
                                           (pop current-seq))
                                 m (stats/mean data)
                                 s (stats/sd data)
                                 candidates [head
                                             (second (peek current-seq))]]
                             (not (every? #(> (abs (- m %)) (* 2 s))
                                         candidates))))
                       ; check if it the latest change is trying to 
                       ; leave the box, with a tolerance of sensitivity
                       (let [prices (map second current-seq)
                             floor (apply min (map second current-seq))
                             ceiling (apply max (map second current-seq))]
                         (or (and
                               (<= head ceiling)
                               (>= head floor)) 
                             (and
                               (> rolling-mean floor)
                               (< (- head ceiling)
                                 (* sensitivity (- ceiling rolling-mean))))
                             (and
                               (< rolling-mean ceiling)
                               (< (- floor head)
                                 (* sensitivity (- rolling-mean floor))))))))
                 (peek-update res #(conj % [(first x) (first v)]))
                 (if (empty? (peek res))
                   res
                   ; Terminate, and start a new one if it is empty
                   (conj res []))))))))

