(ns hungry-nlp.relations
  (:require [hungry-nlp.util :as util]
            [traversy.lens :as t])
  (:use clojure.tools.trace))

(def relation-rules {:number-of [{:start #"<number>" :between #"[^<]*" :end #"<food>"}]
                     :with      [{:start #"<food>" :between #"[^<]*(with|w/)[^<]*(<include>[^<]*)*" :end #"<include>"}]})

(defn lookup-entity [sentence entities index]
  (let [var-pos (keys (util/re-pos #"<.*?>" sentence))]
    (->> index
         (.indexOf var-pos)
         (nth entities))))

(defn match-relation [sentence entities relation]
  (let [start-matches (util/re-spans (:start relation) sentence)
        end-matches (util/re-spans (:end relation) sentence)]
    (for [[i1 i2] start-matches
          [i3 _] end-matches
          :when (and (< i2 i3)
                     (re-matches (:between relation) (subs sentence i2 i3)))]
      (map (partial lookup-entity sentence entities) [i1 i3]))))

(defn extract-relation [sentence entities]
  (t/update relation-rules
            t/all-values
            (partial mapcat (partial match-relation sentence entities))))