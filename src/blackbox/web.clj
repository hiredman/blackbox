(ns blackbox.web
  (:require [clojure.java.io :as io]
            [compojure.route :as route]
            [compojure.core :refer :all]
            [hiccup.core :refer [html]]
            [ring.middleware.resource :refer [wrap-resource]]
            [blackbox.gpio]))

(def take-picture
  "gst-launch v4l2src num-buffers=1 ! image/jpeg,width=640,height=480 ! jpegdec ! jpegenc ! filesink location=/tmp/blah.jpg")

(defn view-port []
  (locking #'view-port
    (.waitFor (.exec (Runtime/getRuntime) take-picture))
    (io/file "/tmp/blah.jpg")))

(defroutes handler*
  (GET "/" []
       {:status 404
        :body (html
               [:html
                [:head]
                [:body
                 [:div {:style "float:left;"}
                   [:div {:style "width:1em;height:1em;background-color:red;margin:1px;"}]
                   [:div {:style "width:1em;height:1em;background-color:black;margin:1px;"}]
                   [:div {:style "width:1em;height:1em;background-color:black;margin:1px;"}]
                   [:div {:style "width:1em;height:1em;background-color:black;margin:1px;"}]]
                 [:center
                  [:img {:src "/snap.jpg"}]
                  [:br]
                  ]]])})
  (GET "/snap.jpg" []
       {:headers {"content-type" "application/json"}
        :body (view-port)})
  (GET "/pin/:pin" [pin]
       (blackbox.gpio/flip!
        @(ns-resolve 'blackbox.gpio
                     (symbol pin)))
       {:status 200}))



(def handler (-> handler*
                 (wrap-resource "")))


;; (blackbox.gpio/flip! blackbox.gpio/gpio2_9)
