(ns hungry-nlp.util)

(defn map-values [f old-map]
  (reduce-kv (fn [new-map k v] (assoc new-map k (f v k))) {} old-map))