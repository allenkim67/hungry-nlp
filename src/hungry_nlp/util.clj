(ns hungry-nlp.util
  (:use [clojure.tools.trace]))

(defn fcomp [& fs]
  (apply comp (reverse fs)))

(defn kv-pairs [kvs]
  (->> (if (map? kvs) (into [] kvs) kvs)
       (mapcat (fn [[k vs]] (map #(vector k %) vs)))))

(defn splice
  ([string start end] (splice string start end ""))
  ([string start end replacement]
   (let [flex-end (min end (count string))]
     (str (subs string 0 start) replacement (subs string flex-end)))))

(defn repeat-s [n s]
  (apply str (repeat n s)))

(defn update-last-in
  ([coll f] (update-last-in coll [] f))
  ([coll ks f]
   (let [last-index (if (> (dec (count coll)) -1) (dec (count coll)) 0)]
     (update-in coll (concat [last-index] ks) f))))

(defn prepend [coll x]
  (into [x] coll))

(defn re-pos [re s]
  (loop [m (re-matcher re s)
         res {}]
    (if (.find m)
      (recur m (assoc res (.start m) (.group m)))
      res)))

(defn re-spans [re s]
  (loop [m (re-matcher re s)
         res []]
    (if (.find m)
      (recur m (conj res [(.start m) (.end m)]))
      res)))