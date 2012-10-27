(ns blackbox.init
  (:require [clojure.tools.nrepl.server :refer [start-server]]
            [clojure.java.io :as io]))

(def server (delay (start-server :port 7888)))

(defn init []
  @server
  (with-open [o (io/output-stream "/sys/class/leds/beaglebone::usr3/brightness")]
    (.write o (.getBytes "1\n"))))
