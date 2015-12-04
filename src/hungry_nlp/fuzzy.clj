(ns hungry-nlp.fuzzy
  (:import [main.java diff_match_patch]))

; set from 0.0 - 1.0 where 0 is strict and 1 matches anything
(def match-threshold 0.4)

(defn match [sentence word]
  (let [dmp (diff_match_patch.)]
    (set! (.Match_Threshold dmp) match-threshold)
    (.match_main dmp sentence word 0)))