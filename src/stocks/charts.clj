(ns stocks.charts
  (:require [incanter.charts :as charts]
            [incanter.core :as incanter]
            [clj-time.coerce :refer [to-date]]))

(defn candle-stick-plot 
  "Returns a JFree chart object, refer the key values format from code" 
  [& options]
  (let [{:keys [title time-label value-label data]
             :or {title "Candle Stick Title"
                  time-label "Time label"
                  value-label "Value label"}} options
        ohlc-data
         (org.jfree.data.xy.DefaultHighLowDataset.
           "Series label"
           (into-array java.util.Date (map to-date (map :date data)))
           (into-array Double/TYPE (mapv double (mapv :high data)))
           (into-array Double/TYPE (mapv double (mapv :low data)))
           (into-array Double/TYPE (mapv double (mapv :open data)))
           (into-array Double/TYPE (mapv double (mapv :close data)))
           (into-array Double/TYPE (mapv double (mapv :volume data))))
        chart (org.jfree.chart.ChartFactory/createCandlestickChart 
                title time-label value-label
                ohlc-data false) ]
      (-> chart .getPlot .getRangeAxis (.setAutoRangeIncludesZero false))
      chart))


(defmethod charts/add-lines* org.jfree.data.xy.DefaultHighLowDataset
  [chart x y & optargs]
  (let [options (apply assoc {} optargs)]
    (let [data-set (org.jfree.data.xy.XYSeries. (:series-label options) true)
          data-plot (.getPlot chart)
          n (.getDatasetCount data-plot)
          line-renderer (org.jfree.chart.renderer.xy.XYLineAndShapeRenderer. true (true? (:points options)))
          data-set-coll (org.jfree.data.xy.XYSeriesCollection.)]
      (doseq [[x y] (map vector x y)]
        (if (and (not (nil? x)) (not (nil? y)))
          (.add data-set (double x) (double y))))
      (.addSeries data-set-coll data-set)
      (doto data-plot
        (.setSeriesRenderingOrder org.jfree.chart.plot.SeriesRenderingOrder/FORWARD)
        (.setDatasetRenderingOrder org.jfree.chart.plot.DatasetRenderingOrder/FORWARD)
        (.setDataset n data-set-coll)
        (.setRenderer n line-renderer))
      chart)))
