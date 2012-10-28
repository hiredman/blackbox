(ns blackbox.web
  (:require [clojure.java.io :as io]
            [compojure.route :as route]
            [compojure.core :refer :all]
            [hiccup.core :refer [html]]
            [ring.middleware.resource :refer [wrap-resource]]))

(def take-picture
  "gst-launch v4l2src num-buffers=1 ! image/jpeg,width=640,height=480 ! jpegdec ! jpegenc ! filesink location=/tmp/blah.jpg")

(defn view-port []
  (locking #'view-port
    (let [p (.exec (Runtime/getRuntime) take-picture)]
      (.waitFor p))
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
       (println "snap")
       {:headers {"content-type" "image/jpeg"}
        :body (view-port)})
  (GET "/pin/:pin" [pin]
       (require 'blackbox.gpio)
       ((resolve 'blackbox.gpio/flip!)
        @(ns-resolve 'blackbox.gpio
                     (symbol pin)))
       {:status 200})
  (GET "/read/:pin" [pin]
       (require 'blackbox.gpio)
       {:status 200
        :body (str @@(ns-resolve 'blackbox.gpio
                                 (symbol pin)))})
  (GET "/move/:direction" [direction]
       (println direction)
       (require 'blackbox.drive)
       ((ns-resolve 'blackbox.drive (symbol direction))
        200)
       {:status 200}))



(def handler (-> handler*
                 (wrap-resource "")))


(comment
  (require 'blackbox.gpio)
  (blackbox.gpio/flip! blackbox.gpio/gpio2_9)
  (blackbox.gpio/mux! blackbox.gpio/gpio2_9 :out -1)
  )


