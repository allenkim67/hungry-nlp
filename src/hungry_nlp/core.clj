(ns hungry-nlp.core
  (:require [hungry-nlp.entities :as entities]
            [hungry-nlp.classifier :as cl])
  (:use [clojure.tools.trace]))

(defn analyze-intent [type message]
  (cl/classify type message))

(defn analyze [id message]
  (let [entities (-> (entities/get-entities id)
                     (entities/locate-entities message)
                     entities/group-orders)
        intent (analyze-intent (if (empty? (:orders entities)) :general :order) message)]
    {:intent intent :entities entities}))