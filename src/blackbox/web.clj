(ns blackbox.web
  (:require [compojure.route :as route]
            [compojure.core :refer :all]
            [hiccup.core :refer [html]]
            [ring.middleware.resource :refer [wrap-resource]]
            [blackbox.camera :as cam]))

(defn view-port []
  @cam/picture-loop
  (while (nil? @cam/picture-file))
  @cam/picture-file)

(defn mjpeg-handler [_]
  {:status 200
   :headers {"content-type" "multipart/x-mixed-replace; boundary=--spionisto"
             "Cache-Control" "no-cache"}
   :body (cam/mjpeg-stream)})

(defn root-handler [_]
  {:status 404
   :body
   (html
    [:html
     [:head]
     [:body
      [:div {:style "float:left;"}
       [:div {:style "width:1em;height:1em;background-color:red;margin:1px;"}]
       [:div {:style "width:1em;height:1em;background-color:black;margin:1px;"}]
       [:div {:style "width:1em;height:1em;background-color:black;margin:1px;"}]
       [:div {:style "width:1em;height:1em;background-color:black;margin:1px;"}]
       ]
      [:center
       [:img {:src "/snap.jpg"}]
       [:br]
       ]]])})

(defroutes handler*
  (GET "/" [] root-handler)
  (GET "/snap.jpg" []
       {:headers {"content-type" "image/jpeg"}
        :body (view-port)})
  (GET "/video.ogg" []
       {:status 200
        :headers {"content-type" "video/ogg"
                  "Cache-Control" "no-cache"}
        :body (cam/video-stream)})
  (GET "/mjpeg" [] mjpeg-handler)
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
       {:status 200})
  (route/not-found "<h1>Page not found</h1>"))

(def handler (-> handler*
                 (wrap-resource "")))

(comment
  (require 'blackbox.gpio)
  (blackbox.gpio/flip! blackbox.gpio/gpio2_9)
  (blackbox.gpio/mux! blackbox.gpio/gpio2_9 :out -1)
  )
