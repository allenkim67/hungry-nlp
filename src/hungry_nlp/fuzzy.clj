(ns hungry-nlp.fuzzy
  (:import [main.java diff_match_patch]))

(defn match [sentence word]
  (let [dmp (diff_match_patch.)]
    (set! (.Match_Threshold dmp) 0.4)
    (.match_main dmp sentence word 0)))