(ns hungry-nlp.core
  (:require [clojure.java.io :as io]
            [opennlp.nlp :as nlp]
            [opennlp.tools.train :as train]
            [clojurewerkz.serialism.core :as s]
            [stencil.core :as stencil]
            [clojure.math.combinatorics :as combo]
            [hungry-nlp.fuzzy :as fuzzy]
            [hungry-nlp.util :as util])
  (:gen-class))

(defn kv-pairs [m]
  (->> m
       (into [])
       (map (fn [[k vs ]] (map #(vector k %) vs)))))

(defn merge-synonyms [entities]
  (let [flattener (fn [entity] (if (map? entity) (conj (:synonyms entity) (:id entity)) [entity]))]
    (util/map-vals (partial mapcat flattener) entities)))

(def stringify-intents
  (partial reduce
           (fn [string intent-pair] (str string
                                         (name (first intent-pair))
                                         " "
                                         (second intent-pair)
                                         "\n"))
           ""))

(def get-template-vars
  (comp
    (partial map second)
    (partial re-seq #"\{\{\{(.*?)\}\}\}")))

(defn get-entities-combos [entities, entity-vars]
  (let [entities (merge-synonyms entities)]
    (->> entity-vars
         (map keyword)
         (select-keys entities)
         kv-pairs
         (apply combo/cartesian-product)
         (map flatten)
         (map (partial apply hash-map)))))

(defn interpolate-intent [entities, intent]
  (let [entities (->> intent get-template-vars ((partial get-entities-combos entities)))]
    (map (partial stencil/render-string intent) entities)))

(defn get-intents-json []
  (s/deserialize (slurp "resources/json/shared/intents.json") :json))

(defn get-entities-json [id]
  (let [filepath-default "resources/json/shared/entities.json"
        filepath-with-id (str "resources/json/user/" id "-entities.json")
        user-entities (s/deserialize (slurp filepath-with-id) :json)
        shared-entities (s/deserialize (slurp filepath-default) :json)]
    (merge user-entities shared-entities)))

(defn interpolate-intents [entities, intents]
  (util/map-vals (partial mapcat
                          (partial interpolate-intent entities))
                 intents))

(def format-intents
  (util/fcomp
    (util/fcomp kv-pairs util/flatten-one)
    stringify-intents))

(defn write-intents-file [id content]
  (let [filepath (str "resources/training/" id "-intents.train")]
    (do (io/make-parents filepath)
        (spit filepath content))))

(defn train-intents [id]
  (->> (get-intents-json)
       (interpolate-intents (get-entities-json id))
       format-intents
       (write-intents-file id)))

(defn analyze-intent [id message]
  (let [intent-model (train/train-document-categorization (str "resources/training/" id "-intents.train"))
        categorizer (nlp/make-document-categorizer intent-model)]
    (:best-category (categorizer message))))

(defn canonical-name [entity-groups type match]
  (let [entities (get entity-groups type)
        is-match (fn [entity] (if (map? entity)
                                (or (= (:id entity) match)
                                    (some? (some #{match} (:synonyms entity))))
                                (= match entity)))]
    (->> entities
         (util/find-first is-match)
         (#(if (map? %) (:id %) %)))))

(defn analyze-entities [id message]
  (let [entities (get-entities-json id)
        is-match (util/fcomp (partial fuzzy/match message) pos?)]
    (->> (merge-synonyms entities)
         (util/map-vals (partial util/find-first is-match))
         util/compact-map
         (util/map-kv (partial canonical-name entities)))))

(defn analyze [id message]
  (let [intent (analyze-intent id message)
        entities (analyze-entities id message)
        response {:intent intent, :entities entities}]
    (do (println response)
        response)))