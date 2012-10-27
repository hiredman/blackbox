(ns boot
  (:require [goog.events]
            [goog.net.XhrIo]
            [goog.Uri.QueryData]))

(defn ^:export ping [end]
  (let [request (goog.net.XhrIo.)]
    (.send request (str "/" end) "GET")))

(defn ^:export main []
  )
