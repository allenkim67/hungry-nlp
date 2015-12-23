(ns hungry-nlp.fuzzy
  (:require [hungry-nlp.util :as util])
  (:import [lib.java diff_match_patch])
  (:use [clojure.tools.trace]))

; set from 0.0 - 1.0 where 0 is strict and 1 matches anything
(def default-threshold 0.2)

(defn splice-marker [sentence entity match-start]
  (let [{name :name} entity]
    (util/splice sentence
                 match-start
                 (+ match-start (count name))
                 (util/repeat-s (count name) "|"))))

(defn splice-var [sentence entity match-start]
  (let [{name :name type :type} entity]
    (util/splice sentence
                 match-start
                 (+ match-start (count name))
                 (str "<" type ">"))))

(defn match-iter [sentences entity matcher]
  (let [entity (if (:positions entity) entity (assoc entity :positions []))
        {marked-sentence :_marked-sentence var-sentence :var-sentence} sentences
        match-start (.match_main matcher marked-sentence (:name entity) 0)
        var-match-start (.match_main matcher var-sentence (:name entity) 0)]
    (if (= match-start -1)
      {:_marked-sentence (:_marked-sentence sentences)
       :var-sentence     (:var-sentence sentences)
       :entity           entity}
      (let [marked-sentence (splice-marker marked-sentence entity match-start)
            var-sentence (splice-var var-sentence entity var-match-start)]
        (recur {:_marked-sentence marked-sentence :var-sentence var-sentence}
               (update-in entity [:positions] #(conj % match-start))
               matcher)))))

(defn match
  ([sentences entity] (match sentences entity default-threshold))
  ([sentences entity match-threshold]
    (let [threshold (or match-threshold default-threshold)
          matcher (diff_match_patch.)]
      (set! (.-Match_Threshold matcher) threshold)
      (match-iter sentences entity matcher))))