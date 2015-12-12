(ns hungry-nlp.util
  (:use [clojure.tools.trace]))

(defn fcomp [& fs]
  (apply comp (reverse fs)))

(defn kv-pairs [kvs]
  (->> (if (map? kvs) (into [] kvs) kvs)
       (mapcat (fn [[k vs]] (map #(vector k %) vs)))))

(defn splice
  ([string start end] (splice string start end ""))
  ([string start end replacement] (str (subs string 0 start) replacement (subs string end))))

(defn repeat-s [n s]
  (apply str (repeat n s)))

(defn update-last-in [coll f]
  (update-in coll [(dec (count coll))] f))