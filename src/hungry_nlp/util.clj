(ns hungry-nlp.util)

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

(def compact-map
  (partial into {} (remove (fcomp second nil?))))