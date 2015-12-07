(ns hungry-nlp.util
  (:require [clojure.java.io :as io]))

(defn fcomp [& fs]
  (apply comp (reverse fs)))

(def flatten-one
  (partial apply concat))

(defn map-vals [f old-map]
  (reduce-kv (fn [m k v] (assoc m k (f v))) {} old-map))

(defn map-kv [f old-map]
  (reduce-kv (fn [m k v] (assoc m k (f k v))) {} old-map))

(defn find-first [f coll]
  (first (filter f coll)))

(defn kv-pairs [m]
  (->> m
       (into [])
       (map (fn [[k vs]] (map #(vector k %) vs)))))

(defn map-vals-mapcat [f m]
  (map-vals #(mapcat f %) m))

(defn make-spit [path content]
  (do (io/make-parents path)
      (spit path content)))