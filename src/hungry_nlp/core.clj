(ns hungry-nlp.core
  (:require [hungry-nlp.entities :as entities]
            [hungry-nlp.classifier :as cl])
  (:use [clojure.tools.trace]))

(defn extract-intent [annotation]
  (let [intent (cl/classify (:var-sentence annotation))]
    (assoc annotation :intent intent)))

(defn analyze [id message]
  (let [msg (clojure.string/lower-case message)]
    (->> msg
         (entities/extract-entities id)
         extract-intent)))