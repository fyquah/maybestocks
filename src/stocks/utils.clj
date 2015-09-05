(ns stocks.utils)

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
        (filter #(> (count %) 3) res))
      (let [head-diff (double (first diff-v))
            head (double (first v))
            current-seq (peek res)]
        (recur (next x)
               (next v)
               (next diff-v)
               (update-exp sensitivity abs-std head-diff)
               (update-exp sensitivity polar-std (abs head-diff))
               (update-exp sensitivity rolling-mean head)
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
                       ; check if it is still in the current box
                       (every? #(< (abs (/ (double (- (first v) %)) %))
                                   (/ sensitivity 2))
                               (map second current-seq)) 
                       ; check if it the latest change is trying to 
                       ; leave the box, with a tolerance of sensitivity
                       (let [prices (map second current-seq)
                             floor (apply min (map second current-seq))
                             ceiling (apply max (map second current-seq))]
                         (or (< (- head ceiling)
                                 (* sensitivity (- ceiling rolling-mean)))
                             (< (- floor head)
                                 (* sensitivity (- rolling-mean floor)))))))
                 (peek-update res #(conj % [(first x) (first v)]))
                 (if (empty? (peek res))
                   res
                   ; Terminate, and start a new one if it is empty
                   (conj res []))))))))

