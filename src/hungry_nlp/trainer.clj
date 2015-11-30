(ns hungry-nlp.trainer
  (:require [clojurewerkz.serialism.core :as s]
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
        intents (interpolate-intents entities-json intents-json)]
    (spit "resources/training/shared/intents.train" (stringify-intents intents))))

(defn wrap-spans [entities-json]
  (util/map-values (fn [v k] (map #(str "<START:" (name k) "> " % " <END>") v)) entities-json))

(defn train-entities [id]
  (let [intents-json (s/deserialize (slurp "resources/json/shared/intents.json") :json)
        entities-json (s/deserialize (slurp (str "resources/json/user/" id "-entities.json")) :json)
        wrapped-entities (wrap-spans entities-json)
        intents (interpolate-intents wrapped-entities intents-json)]
    (spit (str "resources/training/user/" id "-entities.train") (->> intents
                                                                vals
                                                                flatten
                                                                (clojure.string/join "\n")))))