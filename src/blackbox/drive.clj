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

(mux! gpio1_2 :out -1)
(mux! gpio1_7 :out -1)
(mux! gpio1_13 :out -1)
(mux! gpio1_12 :out -1)

(def right-side
  (Track. gpio1_7 gpio1_13))

(def left-side
  (Track. gpio1_12 gpio1_2))

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
