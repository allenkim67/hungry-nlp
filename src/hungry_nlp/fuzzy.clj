(ns hungry-nlp.fuzzy
  (:import [lib.java diff_match_patch])
  (:use [clojure.tools.trace]))

; set from 0.0 - 1.0 where 0 is strict and 1 matches anything
(def default-threshold 0.2)

(defn find-iter ([sentence word matcher] (find-iter sentence word matcher []))
  ([sentence word matcher positions]
   (let [match-index (.match_main matcher sentence word 0)]
     (if (= match-index -1)
       positions
       (let [end-index (+ match-index (count word))
             offset (if (empty? positions) 0 (+ (last positions) (count word)))]
         (recur (subs sentence end-index)
                word
                matcher
                (conj positions (+ match-index offset))))))))

(defn find-all [sentence word threshold]
  (let [matcher (diff_match_patch.)]
    (set! (.-Match_Threshold matcher) (or threshold default-threshold))
    (find-iter sentence word matcher)))