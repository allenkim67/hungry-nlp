(ns hungry-nlp.relations
  (:require [hungry-nlp.util :as util]
            [traversy.lens :as t])
  (:use clojure.tools.trace))

(def relation-rules {:number-of     [{:start #"<number>" :between #"[^<]*(<intentAugment>)?[^<]*" :end #"<food>"}]
                     :with          [{:start #"<food>" :between #"[^<]*(with|w/)[^<]*(<include>[^<]*)*" :end #"<include>"}]
                     :augmentIntent [{:start #"<intentAugment>" :between #"[^<]*" :end #"<food>"}]})

(defn lookup-entity [entities sentence index]
  (let [var-pos (keys (util/re-pos #"<.*?>" sentence))]
    (->> index
         (.indexOf var-pos)
         (nth entities))))

(defn match-relation [entities sentence relation]
  (let [start-matches (util/re-spans (:start relation) sentence)
        end-matches (util/re-spans (:end relation) sentence)]
    (for [[i1 i2] start-matches
          [i3 _] end-matches
          :when (and (< i2 i3)
                     (re-matches (:between relation) (subs sentence i2 i3)))]
      (map (partial lookup-entity entities sentence) [i1 i3]))))

(defn extract-relations [entities sentence]
  (t/update relation-rules
            t/all-values
            (partial mapcat (partial match-relation entities sentence))))

(defn find-related-entities [entity relations]
  (let [equals-first-or-last #(or (= (first %) entity) (= (last %) entity))
        relation-pairs (apply concat (vals relations))
        matched-relations (filter equals-first-or-last relation-pairs)]
    (map #(if (= (first %) entity) (last %) (first %)) matched-relations)))