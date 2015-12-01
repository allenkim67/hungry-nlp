(ns hungry-nlp.trainer
  (:require [clojure.java.io :as io]
            [clojurewerkz.serialism.core :as s]
            [stencil.core :as stencil]
            [clojure.math.combinatorics :as combinatorics]
            [hungry-nlp.util :as util]))

(def pairs
  (partial reduce-kv
           (fn [a k v] (conj a (map #(vector k %) v)))
           []))

(defn get-entity-combos [entity-groups names]
  (->> names
       (map keyword)
       (select-keys entity-groups)
       pairs
       (apply combinatorics/cartesian-product)
       (map flatten)
       (map #(apply hash-map %))))

(defn get-template-vars [template-string]
  (->> template-string
       (re-seq #"\{\{\{(.*?)\}\}\}")
       (map last)))

(defn interpolate-intent [entities intent]
  (->> intent
       get-template-vars
       (get-entity-combos entities)
       (map #(stencil/render-string intent %))))

(defn interpolate-intents [entities intents]
  (util/map-values (fn [v k] (mapcat (partial interpolate-intent entities) v)) intents))

(defn stringify-intents [intents]
  (reduce (fn [s [k v]] (str s (name k) " " v "\n"))
          ""
          (apply concat (pairs intents))))

(defn train-intents []
  (let [intents-json (s/deserialize (slurp "resources/json/shared/intents.json") :json)
        entities-json (s/deserialize (slurp "resources/json/shared/entities.json") :json)
        intents (interpolate-intents entities-json intents-json)
        intents-train-filepath "resources/training/shared/intents.train"]
    (do (io/make-parents intents-train-filepath)
        (spit intents-train-filepath (stringify-intents intents)))))

(defn wrap-spans [entities-json]
  (util/map-values (fn [v k] (map #(str "<START:" (name k) "> " % " <END>") v)) entities-json))

(defn train-entities
  ([] (train-entities nil))
  ([id] (let [entities-json-filepath (if id (str "resources/json/user/" id "-entities.json") "resources/json/shared/entities.json")
              entities-train-filepath (if id (str "resources/train/user/" id "-entities.train") "resources/training/shared/entities.train")
              intents-json (s/deserialize (slurp "resources/json/shared/intents.json") :json)
              entities-json (s/deserialize (slurp entities-json-filepath) :json)
              wrapped-entities (wrap-spans entities-json)
              intents (interpolate-intents wrapped-entities intents-json)]
          (do (io/make-parents entities-train-filepath)
              (spit entities-train-filepath (->> intents
                                                 vals
                                                 flatten
                                                 (clojure.string/join "\n")))))))