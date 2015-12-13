(ns hungry-nlp.core
  (:require [hungry-nlp.entities :as entities]
            [hungry-nlp.classifier :as cl])
  (:use [clojure.tools.trace]))

(defn analyze-intent [type message]
  (cl/classify type message))

(defn analyze [id message]
  (let [message (clojure.string/lower-case message)
        entities (-> (entities/get-entities id)
                     entities/parse-entities
                     (entities/locate-entities message)
                     entities/group-orders)
        intent (analyze-intent (if (empty? (:orders entities)) :general :order) message)]
    {:intent intent :entities entities}))