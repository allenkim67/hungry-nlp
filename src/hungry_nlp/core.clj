(ns hungry-nlp.core
  (:require [hungry-nlp.entities :as entities]
            [hungry-nlp.classifier :as cl]
            [hungry-nlp.relations :as relations])
  (:use [clojure.tools.trace]))

(defn analyze [id sentence]
  (let [s (clojure.string/lower-case sentence)
        entities (entities/extract-entities id s)
        var-sentence (entities/var-sentence entities s)
        intent (cl/classify var-sentence)
        relations (relations/extract-relations entities var-sentence)]
    {:entities entities
     :var-sentence var-sentence
     :intent intent
     :relations relations}))