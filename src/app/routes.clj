(ns app.routes
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [hiccup.middleware :refer  [wrap-base-url]]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [ring.util.response :as response]
            [clojure.data.json :as json]
            [clojure.string :as str]
            [stocks.core :as core]
            [stocks.utils]
            [hiccup.page :as page]
            [korma.core :as sql]))

(defn get-prices
  ([{{:keys [fromMonth fromYear toMonth toYear symbol]} :params}]
   (response/content-type
    (let [fromDateString (str fromYear "/" fromMonth "/01")
          toDateString (str toYear "/" toMonth "/31")
          data (core/parse-price-data
                 (sql/select :price
                             (sql/where
                               (and (= :symbol symbol)
                                    (>= :date_ex fromDateString)
                                    (<= :date_ex toDateString)))))
          timestamp (map :date data)
          close (map :close data)
          flat-seq (stocks.utils/flat-seeker 0.15 timestamp close)]
      {:body (json/write-str {:data data
                              :flat flat-seq})})
     "application/json")))

(defn get-company
  ([{{sym :symbol} :params}]
   (response/content-type
    {:body (-> (sql/select :symbol (sql/where (= :symbol sym)))
               first
               json/write-str)}
    "application/json")))

(defn get-simulation
  ([{{sym :symbol} :params}]
   (response/content-type
     {:body (->> (core/fetch-simulation-results sym)
                 (map (fn [m]
                        (assoc m :date_ex
                               (.getTime (:date_ex m)))))
                 (json/write-str))}
     "application/json")))

(defn get-companies
  ([m]
   (response/content-type
     {:body (json/write-str (sql/select :symbol))}
     "application/json")))

(defn get-popular-companies
  ([m]
    (response/content-type
     {:body (json/write-str (sql/select :symbol
                                        (sql/where {:symbol [in core/DJIA-list]})))}
      "application/json")))

(defn get-DJIA-summary
  ([m]
   (let [summary (stocks.core/DJIA-summary)]
     (page/html5
       [:head
        (page/include-css "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.5/css/bootstrap.min.css")]
       [:body
        [:div.container
         [:div.page-header
          [:h2 "DJIA Summary"]]
         [:p "Total profits : " 
          [:b (reduce + 0 (map (fn [[k v]] (:profit v)) summary))]]
         [:p "Stop loss : "
          [:b stocks.core/STOP-LOSS]]
         [:p "Take Profit : "
          [:b stocks.core/TAKE-PROFIT]]
         [:table.table
          [:thead
           [:th "Symbol"]
           [:th "Profit"]
           [:th "Number of Orders"] 
           [:th "Profit per Order"]]
          [:tbody
           (map (fn [[sym {:keys [orders profit]}]]
                  [:tr
                   [:td sym]
                   [:td profit]
                   [:td (count orders)]
                   [:td (format "%.2f" (/ (double profit) (count orders)))]])
                summary)]]]]))))

(defroutes app-routes
  (GET "/" [] "hello world")
  (GET "/prices" [] get-prices)
  (GET "/popular_companies" [] get-popular-companies)
  (GET "/company" [] get-company)
  (GET "/simulation" [] get-simulation)
  (GET "/DJIA-summary" [] get-DJIA-summary)
  (route/resources "/")
  (route/not-found "Not found"))

(defn init [] (println "Initializing server"))
(defn destroy [] (println "Destroying server"))
(def handler
  (-> (routes app-routes)
      (handler/site)
      (wrap-base-url)))
