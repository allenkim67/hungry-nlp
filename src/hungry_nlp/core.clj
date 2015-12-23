(ns hungry-nlp.core
  (:require [hungry-nlp.entities :as entities]
            [hungry-nlp.classifier :as cl])
  (:use [clojure.tools.trace]))

(defn extract-intent [annotations]
  (let [intent (cl/classify (:var-sentence annotations))]
    (assoc annotations :intent intent)))

(defn analyze [id message]
  (let [msg (clojure.string/lower-case message)]
    (->> msg
         (entities/extract-entities id)
         extract-intent)))