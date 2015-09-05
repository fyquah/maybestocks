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
            [korma.core :as sql]))

(defn get-prices
  ([{{:keys [fromMonth fromYear toMonth toYear symbol]} :params}]
   (println fromMonth fromYear)
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

(defroutes app-routes
  (GET "/" [] "hello world")
  (GET "/prices" [] get-prices)
  (route/resources "/")
  (route/not-found "Not found"))

(defn init [] (println "Initializing server"))
(defn destroy [] (println "Destroying server"))
(def handler
  (-> (routes app-routes)
      (handler/site)
      (wrap-base-url)))
