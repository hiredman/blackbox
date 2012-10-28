(ns blackbox.init
  (:require [clojure.tools.nrepl.server :refer [start-server]]
            [clojure.java.io :as io]
            [clojure.java.shell :as s]))

(def server (delay (start-server :port 7888)))

(defn restart-connman []
  (.waitFor (.exec (Runtime/getRuntime) "ifup wlan0"))
  #_(.waitFor (.exec (Runtime/getRuntime) "systemctl restart avahi-daemon.service")))

(defn init []
  @server
  (require 'blackbox.gpio)
  ((resolve 'blackbox.gpio/mux!)
   @(resolve 'blackbox.gpio/gpio2_9) :out -1)
  ((resolve 'blackbox.gpio/mux!)
   @(resolve 'blackbox.gpio/gpio2_7) :in -1)
  (with-open [o (io/output-stream "/sys/class/leds/beaglebone::usr3/brightness")]
    (.write o (.getBytes "1\n")))
  #_(future
    (try
      (while true
        (with-open [i (.getInputStream
                       (.exec (Runtime/getRuntime) "ifconfig wlan0"))]
          (let [x (slurp i)]
            (when-not (.contains x "UP")
              (restart-connman))))
        (Thread/sleep 1000))
      (catch Exception e
        (println e)))))
