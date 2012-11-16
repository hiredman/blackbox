(ns blackbox.drive
  (:require [blackbox.gpio :refer :all]))

(defprotocol SPTT
  (throw1 [_])
  (throw2 [_])
  (throw3 [_]))

(deftype Track [forward-pin reverse-pin]
  clojure.lang.IDeref
  (deref [_]
    {:forward-pin @forward-pin
     :reverse-pin @reverse-pin})
  SPTT
  (throw1 [_]
    (low! reverse-pin)
    (high! forward-pin))
  (throw2 [_]
    (low! forward-pin)
    (low! reverse-pin))
  (throw3 [_]
    (low! forward-pin)
    (high! reverse-pin))
  IFlipFlop
  (high! [_]
    (low! reverse-pin)
    (high! forward-pin))
  (low! [_]
    (low! forward-pin)
    (high! reverse-pin))
  (flip! [_]
    (flip! forward-pin)
    (flip! reverse-pin)))

(mux! GPIO1_15 :out -1)
(mux! GPIO1_14 :out -1)
(mux! GPIO0_27 :out -1)
(mux! GPIO2_1 :out -1)

(def right-side
  (Track. GPIO2_1 GPIO0_27))

(def left-side
  (Track. GPIO1_14 GPIO1_15))

(defn forward [n]
  (throw1 left-side)
  (throw1 right-side)
  (Thread/sleep n)
  (throw2 right-side)
  (throw2 left-side))

(defn left [n]
  (throw1 right-side)
  (Thread/sleep n)
  (throw2 right-side))

(defn right [n]
  (throw1 left-side)
  (Thread/sleep n)
  (throw2 left-side))

(defn backward [n]
  (throw3 right-side)
  (throw3 left-side)
  (Thread/sleep n)
  (throw2 left-side)
  (throw2 right-side))
