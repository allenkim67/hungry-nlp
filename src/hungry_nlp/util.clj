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

(defn kv-pair-groups [m]
  "{:a [1 2 3] :b [4 5 6]} -> [[[:a 1] [:a 2] [:a 3]] [[:b 4] [:b 5] [:b 6]]] "
  (->> m
       (into [])
       (map (fn [[k vs]] (map #(vector k %) vs)))))

(defn map-vals-map [f m]
  (map-vals #(map f %) m))

(defn map-vals-mapcat [f m]
  (map-vals #(mapcat f %) m))

(defn make-spit [path content]
  (do (io/make-parents path)
      (spit path content)))

(defn zipmap-concat [ks vs]
  (let [rf (fn [m [k v]] (if (k m)
                           (assoc m k (conj (k m) v))
                           (assoc m k [v])))]
    (->> (map vector ks vs)
         (reduce rf {}))))

(def merge-vals
  (fcomp vals (partial apply merge)))