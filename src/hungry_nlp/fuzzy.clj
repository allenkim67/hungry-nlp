(ns hungry-nlp.fuzzy
  (:require [hungry-nlp.util :as util])
  (:import [lib.java diff_match_patch])
  (:use [clojure.tools.trace]))

(def dmp (diff_match_patch.))

; set from 0.0 - 1.0 where 0 is strict and 1 matches anything
(set! (.Match_Threshold dmp) 0.3)

(defn match
  ([sentence words] (match sentence words (util/to-map words [])))
  ([sentence words matches]
   (if (empty? words)
     matches
     (let [words (sort-by (comp - count) words)
           word (first words)
           match-start (.match_main dmp sentence word 0)
           word-size (count word)]
       (if (= match-start -1)
         (recur sentence
                (rest words) 
                matches)
         (recur (util/splice sentence match-start (+ match-start word-size) (util/repeat-s word-size "|"))
                words
                (update-in matches [(keyword word)] #(conj % match-start))))))))