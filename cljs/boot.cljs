(ns boot
  (:require [goog.events]
            [goog.net.XhrIo]
            [goog.Uri.QueryData]))

(defn ^:export ping [end]
  (let [request (goog.net.XhrIo.)]
    (.send request (str "/" end) "GET")))

(def ^:export scale-factor 0.859375)

(def ^:export reload-image-time 50)

(def images (js/Array.))

(defn load-image []
  (when (<= 3 (.-length images))
    (let [img (js/Image.)]
      (set! (.-onload img)
            (fn []
              (.push images img)))
      (set! (.-src img)
            (str "/snap.jpg?" (.random js/Math))))))

(defn draw-image [can]
  (loop [images (seq images)
         i 0]
    (when images
      (let [img (first images)]
        (js/setTimeout
         (fn []
           (.drawImage (.getContext can "2d") img 0 0))
         i))
      (recur (rest images) (+ i 200)))))

;; (defn ^:export main []
;;   (let [can (.getElementById js/document "snap")
;;         context (.getContext can "2d")
;;         img (js/Image.)
;;         f (fn []
;;             (js/setTimeout
;;              (fn []
;;                (set! (.-src img)
;;                      (str "/snap.jpg?"
;;                           (.random js/Math))))
;;              reload-image-time))]
;;     (.scale context scale-factor scale-factor)
;;     (set! (.-onload img)
;;           (fn []
;;             (.drawImage context img 0 0)
;;             (f)))
;;     (js/setInterval f 10000)
;;     (f)))

(defn ^:export main []
  (let [can (.getElementById js/document "snap")
        context (.getContext can "2d")]
    (.scale context scale-factor scale-factor)
    (js/setInterval load-image 500)
    (js/setInterval (partial draw-image can) 1000)))


(defn ^:export led-flip []
  (ping "pin/gpio2_9"))
