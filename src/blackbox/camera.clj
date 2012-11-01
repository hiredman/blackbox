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

(def video
  "gst-launch v4l2src ! image/jpeg,width=640,height=480 ! jpegdec ! videorate ! video/x-raw-yuv,framerate=2/1 ! theoraenc bitrate=50 ! oggmux ! filesink location=/dev/stdout")

#_(def video
    "gst-launch  --eos-on-shutdown v4l2src ! image/jpeg,width=640,height=480 ! jpegdec ! videorate ! 'video/x-raw-yuv, framerate=15/1' ! queue max-size-bytes=100000000 max-size-time=0 ! theoraenc bitrate=150 ! oggmux filesink location=/dev/stdout")

(defn video-stream []
  (.getInputStream (.exec (Runtime/getRuntime) video)))


(def mjpeg-video
  "gst-launch v4l2src ! image/jpeg,width=640,height=480 ! jpegdec ! jpegenc ! multipartmux boundary=spionisto ! filesink location=/dev/stdout")


(defonce exec (java.util.concurrent.Executors/newSingleThreadExecutor))


(defn mjpeg-stream []
  (let [i (java.io.PipedInputStream.)
        o (java.io.PipedOutputStream. i)]
    (future
      (println "future" (Thread/currentThread))
      (try
        (while true
          (with-open [s (java.net.Socket. "localhost" 5000)
                      i (.getInputStream s)]
            #_(io/copy i o :buffer-size bs)
            (let [buf (byte-array 512)]
              (loop []
                (let [size (.read i buf)]
                  (when (pos? size)
                    (.write o buf 0 size)
                    (recur)))))
            (println "done copying")))
        (catch Exception e
          (println (Thread/currentThread))
          (st/print-stack-trace e)
          (flush)))
      (println "here" (Thread/currentThread)))
    i))

(defn mjpeg-stream []
  (.getInputStream (java.net.Socket. "localhost" 5000)))

;; (defn stream-copy [state outputstream]
;;   (try
;;     (send-off *agent* #'stream-copy outputstream)
;;     (if (nil? state)
;;       (let [s (java.net.Socket. "localhost" 5000)
;;             i (.getInputStream s)
;;             buf (byte-array 512)]
;;         {:s s :i i :buf buf})
;;       (let [{:keys [i s buf]} state
;;             size (.read i buf)]
;;         (loop [n 0]
;;           (let [size (.read i buf)]
;;             (when (pos? size)
;;               (.write outputstream buf 0 size)
;;               (if (> n 100)
;;                 state
;;                 (recur (inc n))))))))
;;     (catch Exception e
;;       (st/print-stack-trace e)
;;       (throw e))))

;; (defn mjpeg-stream []
;;   (let [i (java.io.PipedInputStream.)
;;         o (java.io.PipedOutputStream. i)
;;         a (agent nil)]
;;     (send-off a stream-copy o)
;;     i))


;; (defn mjpeg-stream []
;;   (let [s (java.net.Socket. "localhost" 5000)]
;;     (.getInputStream s)))


;; gst-launch v4l2src ! image/jpeg,width=640,height=480,framerate=5/1
;; ! jpegdec !  video/x-raw, framerate=5/1 !  jpegenc ! multipartmux
;; boundary=spionisto ! tcpserversink host=localhost port=5000

(def test-video
  "gst-launch videotestsrc ! jpegenc ! multipartmux boundary=spionisto ! filesink location=/dev/stdout")

(defn test-stream []
  (.getInputStream (.exec (Runtime/getRuntime) test-video)))

;; gst-launch v4l2src ! image/jpeg,width=640,height=480 ! jpegdec !
;; jpegenc ! multipartmux boundary=spionisto ! tcpserversink
;; host=localhost port=5000
