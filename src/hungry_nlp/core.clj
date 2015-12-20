(ns hungry-nlp.core
  (:require [hungry-nlp.classifier :as cl]
            [hungry-nlp.entities :as entities])
  (:use [clojure.tools.trace]))

(defn analyze-entities [id message]
  (let [message (clojure.string/lower-case message)] 
    (-> (entities/get-entities id)
        entities/parse-entities
        (entities/locate-entities message)
        entities/group-entities)))

(defn analyze [id message]
  (let [lowercase-message (clojure.string/lower-case message)
        entities (analyze-entities id lowercase-message)
        intent (cl/classify (if (empty? entities) :general :order) lowercase-message)]
    {:intent intent :entities entities}))