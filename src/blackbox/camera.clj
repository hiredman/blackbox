(ns blackbox.camera
  (:require [clojure.stacktrace :as st]
            [clojure.java.io :as io]))

(def take-picture
  "gst-launch v4l2src num-buffers=1 ! image/jpeg,width=640,height=480 ! jpegdec ! jpegenc ! filesink location=/tmp/%s.jpg")

(def picture-file (ref nil))

(def old-picture-files (ref []))

(def picture-loop
  (delay
   (future
     (while true
       (try
         (let [now (System/currentTimeMillis)
               p (.exec (Runtime/getRuntime)
                        (format take-picture now))
               f (io/file (str "/tmp/" now ".jpg"))]
           (.waitFor p)
           (dosync
            (alter old-picture-files conj @picture-file)
            (ref-set picture-file f))
           (future
             (doseq [f (dosync
                        (let [old-files (filter #(> (- now (.lastModified %))
                                                    (* 60 1000))
                                                @old-picture-files)]
                          (alter old-files (partial remove (set old-files)))
                          old-files))]
               (.delete f))))
         (catch Exception e
           (st/print-stack-trace e)))))))
