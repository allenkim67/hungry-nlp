(ns hungry-nlp.fuzzy
  (:require [hungry-nlp.util :as util])
  (:import [lib.java diff_match_patch])
  (:use [clojure.tools.trace]))

; set from 0.0 - 1.0 where 0 is strict and 1 matches anything
(def default-threshold 0.2)

(defn match-helper
  ([sentence word matcher] (match-helper sentence word matcher {:positions []}))
  ([sentence word matcher matches]
   (let [match-start (.match_main matcher sentence word 0)
         word-size (count word)]
     (if (= match-start -1)
       (update-in matches [:positions] #(with-meta % {:sentence sentence}))
       (let [marked-sentence (util/splice sentence
                                          match-start
                                          (+ match-start word-size)
                                          (util/repeat-s word-size "|"))]
         (recur marked-sentence
                word
                matcher
                (update-in matches [:positions] #(conj % match-start))))))))

(defn match
  ([sentence words] (match sentence words default-threshold))
  ([sentence words match-threshold]
   (let [matcher (diff_match_patch.)]
     (set! (.Match_Threshold matcher) (or match-threshold default-threshold))
     (match-helper sentence words matcher))))