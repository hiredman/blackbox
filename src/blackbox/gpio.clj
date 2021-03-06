;; https://github.com/alexanderhiam/PyBBIO
(ns blackbox.gpio
  (:require [clojure.java.io :as io])
  (:import (java.util.concurrent LinkedBlockingQueue)))

;; https://github.com/hiredman/beaglebone-jni-utils
(let [f (java.io.File/createTempFile "libB" ".so")]
  (.deleteOnExit f)
  (io/copy (io/input-stream (io/resource "libB.so")) f)
  (a.B/load (.getAbsolutePath f)))

(def b (delay (a.B.)))

(def mmap-offset 0x44c00000)

(def mmap-size (- 0x48ffffff mmap-offset))

;; N is the base address of nnmap'ed /dev/mem
(defonce N (delay (.mem @b mmap-offset mmap-size)))
(defonce queue (LinkedBlockingQueue.))

(def conf-rx-active (bit-shift-left 1 5))
(def conf-pullup (bit-shift-left 1 4))
(def conf-pulldown 0)
(def conf-pull-disable (bit-shift-left 1 3))

(def conf-gpio-mode 0x07)
(def conf-gpio-output conf-gpio-mode)
(def conf-gpio-input (+ conf-gpio-mode conf-rx-active))

;; gpio chips
(def gpio0 (- 0x44e07000 mmap-offset))
(def gpio1 (- 0x4804c000 mmap-offset))
(def gpio2 (- 0x481ac000 mmap-offset))
(def gpio3 (- 0x481ae000 mmap-offset))

(def gpio-oe 0x134)
(def gpio-data-in 0x138)
(def gpio-clear-data-out 0x190)
(def gpio-set-data-out 0x194)

(def gpio
  {"USR0" [gpio1, (bit-shift-left 1 21), "gpmc_a5"],
   "USR1" [gpio1, (bit-shift-left 1 22), "gpmc_a6"],
   "USR2" [gpio1, (bit-shift-left 1 23), "gpmc_a7"],
   "USR3" [gpio1, (bit-shift-left 1 24), "gpmc_a8"],
   "GPIO0_7" [gpio0,  (bit-shift-left 1 7), "ecap0_in_pwm0_out"],
   "GPIO0_26" [gpio0, (bit-shift-left 1 26), "gpmc_ad10"],
   "GPIO0_27" [gpio0, (bit-shift-left 1 27), "gpmc_ad11"],
   "GPIO1_0" [gpio1, 1, "gpmc_ad0"],
   "GPIO1_1" [gpio1, (bit-shift-left 1 1), "gpmc_ad1"],
   "GPIO1_2" [gpio1,  (bit-shift-left 1 2), "gpmc_ad2"],
   "GPIO1_3" [gpio1,  (bit-shift-left 1 3), "gpmc_ad3"],
   "GPIO1_4" [gpio1,  (bit-shift-left 1 4), "gpmc_ad4"],
   "GPIO1_5" [gpio1,  (bit-shift-left 1 5), "gpmc_ad5"],
   "GPIO1_6" [gpio1,  (bit-shift-left 1 6), "gpmc_ad6"],
   "GPIO1_7" [gpio1,  (bit-shift-left 1 7), "gpmc_ad7"],
   "GPIO1_12" [gpio1, (bit-shift-left 1 12), "gpmc_ad12"],
   "GPIO1_13" [gpio1, (bit-shift-left 1 13), "gpmc_ad13"],
   "GPIO1_14" [gpio1, (bit-shift-left 1 14), "gpmc_ad14"],
   "GPIO1_15" [gpio1, (bit-shift-left 1 15), "gpmc_ad15"],
   "GPIO1_16" [gpio1, (bit-shift-left 1 16), "gpmc_a0"],
   "GPIO1_17" [gpio1, (bit-shift-left 1 17), "gpmc_a1"],
   "GPIO1_28" [gpio1, (bit-shift-left 1 28), "gpmc_ben1"],
   "GPIO1_29" [gpio1, (bit-shift-left 1 29), "gpmc_csn0"],
   "GPIO1_30" [gpio1, (bit-shift-left 1 30), "gpmc_csn1"],
   "GPIO1_31" [gpio1, (bit-shift-left 1 31), "gpmc_csn2"],
   "GPIO2_1" [gpio2, (bit-shift-left 1 1), "gpmc_clk"],
   "GPIO2_6" [gpio2, (bit-shift-left 1 6), "lcd_data0"],
   "GPIO2_7" [gpio2, (bit-shift-left 1 7), "lcd_data1"],
   "GPIO2_8" [gpio2, (bit-shift-left 1 8), "lcd_data2"],
   "GPIO2_9" [gpio2, (bit-shift-left 1 9), "lcd_data3"],
   "GPIO2_10" [gpio2, (bit-shift-left 1 10), "lcd_data4"],
   "GPIO2_11" [gpio2, (bit-shift-left 1 11), "lcd_data5"],
   "GPIO2_12" [gpio2, (bit-shift-left 1 12), "lcd_data6"],
   "GPIO2_13" [gpio2, (bit-shift-left 1 13), "lcd_data7"],
   "GPIO2_22" [gpio2, (bit-shift-left 1 22), "lcd_vsync"],
   "GPIO2_23" [gpio2, (bit-shift-left 1 23), "lcd_hsync"],
   "GPIO2_24" [gpio2, (bit-shift-left 1 24), "lcd_pclk"],
   "GPIO2_25" [gpio2, (bit-shift-left 1 25), "lcd_ac_bias_en"],
   "GPIO3_19" [gpio3, (bit-shift-left 1 19), "mcasp0_fsr"],
   "GPIO3_21" [gpio3, (bit-shift-left 1 21), "mcasp0_ahclkx"]})

(defn gpio-chip [pin]
  (nth (get gpio pin) 0))

(defn gpio-address [pin]
  (nth (get gpio pin) 1))

(def us
  (.get (doto (.getDeclaredField sun.misc.Unsafe "theUnsafe")
          (.setAccessible true))
        nil))

;; make unsigned?
(defn get-reg
  ([address]
     (get-reg address 32))
  ([address length]
     (case (long length)
       32 (.getInt ^sun.misc.Unsafe us (long (+ @N address)))
       16 (.getShort ^sun.misc.Unsafe us (long (+ @N address))))))

;; make unsigned?
(defn set-reg
  ([address value] (set-reg address value 32))
  ([address value length]
     (case (long length)
       32 (.putInt ^sun.misc.Unsafe us (long (+ @N address)) (int value))
       16 (.putInt ^sun.misc.Unsafe us (long (+ @N address)) (short value)))))

(defn or-reg
  ([address mask] (or-reg address mask 32))
  ([address mask length]
     (set-reg address
              (bit-or (get-reg address length)
                      mask)
              length)))

(defn and-reg
  ([address mask] (or-reg address mask 32))
  ([address mask length]
     (set-reg address
              (bit-and (get-reg address length)
                       mask)
              length)))

(defn clear-reg
  ([address mask] (clear-reg address mask 32))
  ([address mask length]
     (and-reg address
              (bit-xor mask (int 0xfffffff))
              length)))

(defn digital-write [pin state]
  (if state
    (set-reg (+ (gpio-chip pin)
                gpio-set-data-out)
             (gpio-address pin))
    (set-reg (+ (gpio-chip pin)
                gpio-clear-data-out)
             (gpio-address pin))))

(defn digital-read [pin]
  (not (zero?
        (bit-and (long (bit-and
                        (get-reg (+ (gpio-chip pin)
                                    gpio-data-in))
                        (gpio-address pin)))
                 (int 0xfffffff)))))

(def pin-mux-path "/sys/kernel/debug/omap_mux/")

(defn pin-mux [fn mode]
  (with-open [w (io/output-stream (str pin-mux-path fn))]
    (.write w (.getBytes (Long/toHexString mode)))))

(defn pin-mode [pin direction & [pull]]
  (if (= direction :in)
    (let [pull (or pull 0)
          pull (cond
                (> pull 0) conf-pullup
                (zero? pull) conf-pull-disable
                :else conf-pulldown)]
      (pin-mux (get (get gpio pin) 2)
               (bit-or conf-gpio-input pull))
      (or-reg (+ (gpio-chip pin)
                 gpio-oe)
              (gpio-address pin)))
    (do
      (pin-mux (get (get gpio pin) 2)
               conf-gpio-output)
      (clear-reg (+ (gpio-chip pin)
                    gpio-oe)
                 (gpio-address pin)))))

(defprotocol IMux
  (mux! [_ direction pull])
  (direction [_]))

(defprotocol IFlipFlop
  (high! [_])
  (low! [_])
  (flip! [_]))

(deftype Pin [pin
              ^:volatile-mutable dir
              ^:volatile-mutable state
              ^:volatile-mutable pull]
  clojure.lang.IDeref
  (deref [_]
    (if (= :in dir)
      (digital-read pin)
      state))
  IMux
  (direction [_]
    dir)
  (mux! [_ d p]
    (set! dir d)
    (set! pull p)
    (pin-mode pin dir pull))
  IFlipFlop
  (high! [_]
    (assert (= dir :out))
    (set! state true)
    (digital-write pin state))
  (low! [_]
    (assert (= dir :out))
    (set! state false)
    (digital-write pin state))
  (flip! [_]
    (assert (= dir :out))
    (set! state (not state))
    (digital-write pin state))
  Object
  (toString [this]
    (format "#<%s %s>"
            pin
            (pr-str {:direction dir
                     :state state
                     :pull pull}))))

(doseq [[pin & _] gpio]
  (let [p (->Pin pin :out false nil)]
    (intern *ns* (symbol (.toLowerCase pin)) p)
    (intern *ns* (symbol pin) p)))

(defmethod print-method Pin [p writer]
  (.write writer (.toString p)))

;; use an executor?
(def event-loop (future
                  (while true
                    (try
                      (let [f (.take queue)
                            r (f)]
                        (when-not (nil? r)
                          (.put queue r)))
                      (catch Exception _)))))

(comment

  (mux! gpio2_7 :out -1)
  (mux! gpio2_6 :in -1)
  (def f
    (future
      (while true
        (try
          (if @gpio2_6
            (high! gpio2_7)
            (low! gpio2_7))
          (catch Exception _)))))

  )

;;opkg install rxtx
;;librxtx-java
;;jdk1.7.0_06/bin/java -Djava.library.path=/usr/lib/jni  -Dgnu.io.rxtx.SerialPorts=/dev/ttyO1 -classpath /usr/share/java/ext/RXTXcomm.jar:clojure-1.5.0-master-SNAPSHOT.jar clojure.main
(comment

  (import '(gnu.io CommPort
                   CommPortIdentifier
                   SerialPort)
          '(java.io Closeable
                    BufferedReader
                    InputStreamReader))

  (defprotocol IReadLines
    (read-a-line [r]))

  (defn port [port-name & {:keys [baud] :or {baud 115200}}]
    (let [port-id (CommPortIdentifier/getPortIdentifier port-name)
          port (.open port-id (pr-str #'port) 2000)]
      (.setSerialPortParams port baud SerialPort/DATABITS_8 SerialPort/STOPBITS_1 SerialPort/PARITY_NONE)
      (let [ins (-> port .getInputStream InputStreamReader. BufferedReader.)
            outs (.getOutputStream port)]
        (reify
          IReadLines
          (read-a-line [_]
            (.readLine ins))
          Closeable
          (close [_]
            (try
              (.close ins)
              (finally
                (try
                  (.close outs)
                  (finally
                    (.close port))))))))))

  )
