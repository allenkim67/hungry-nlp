(ns hungry-nlp.util
  (:require [clojure.java.io :as io])
  (:use [clojure.tools.trace]))

(defn fcomp [& fs]
  (apply comp (reverse fs)))

(defn kv-pairs [kvs]
  (->> (if (map? kvs) (into [] kvs) kvs)
       (mapcat (fn [[k vs]] (map #(vector k %) vs)))))

(defn make-spit [path content]
  (do (io/make-parents path)
      (spit path content)))

(defn splice
  ([string start end] (splice string start end ""))
  ([string start end replacement] (str (subs string 0 start) replacement (subs string end))))

(defn repeat-s [n s]
  (apply str (repeat n s)))

(defn to-map [coll default]
  (apply merge (map #(hash-map (keyword %) default) coll)))