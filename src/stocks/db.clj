(ns stocks.db
  (:use korma.db)
  (:use korma.core))

(defdb db (mysql {:db "stocks"
                  :user "stockuser"
                  :password "stockpass"}))

(defentity price)
(defentity sentiment)
(defentity symbol)
