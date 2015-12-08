(ns hungry-nlp.core
  (:require [opennlp.nlp :as nlp]
            [opennlp.tools.train :as train]
            [clj-fuzzy.metrics :as fuzzy]
            [hungry-nlp.util :as util]
            [hungry-nlp.file-ops :as fop])
  (:use [clojure.tools.trace])
  (:gen-class))

(def tokenize (nlp/make-tokenizer "resources/en-token.bin"))

(defn analyze-intent [id message]
  (let [intent-model (train/train-document-categorization (fop/intents-training-filepath id))
        categorizer (nlp/make-document-categorizer intent-model)]
    (:best-category (categorizer message))))

(defn spellcheck [entities entity-type match]
  (->> (get entities entity-type)
       (sort-by (partial fuzzy/jaro-winkler match))
       last))

(defn canonical-name [entities type match]
  (let [entities (get entities type)
        is-match #(some? (some #{match} (:synonyms %)))]
    (->> entities (util/find-first is-match) :id)))

(defn analyze-compound-entities [id message]
  (let [name-finder-model (train/train-name-finder (fop/compound-entities-training-filepath id))
        name-finder (nlp/make-name-finder name-finder-model)
        matched-entities (->> message tokenize name-finder)
        matched-types (->> matched-entities meta :spans (map (util/fcomp :type keyword)))]
    (->> (util/zipmap-concat matched-types matched-entities))))

(defn analyze-base-entities [id message]
  (let [name-finder-model (train/train-name-finder (fop/base-entities-training-filepath id))
        name-finder (nlp/make-name-finder name-finder-model)
        matched-entities (->> message tokenize name-finder)
        matched-types (->> matched-entities meta :spans (map (util/fcomp :type keyword)))
        entities (fop/get-merged-entities id)]
    (->> (zipmap matched-types matched-entities)
         (util/map-kv (partial spellcheck (->> entities util/merge-vals fop/merge-synonyms)))
         (util/map-kv (partial canonical-name (:baseEntities entities))))))

(defn analyze-entities [id message]
  (let [compound-entities (analyze-compound-entities id message)]
    (->> compound-entities
         (util/map-vals-map (partial analyze-base-entities id)))))

(defn analyze [id message]
  (let [intent (analyze-intent id message)
        entities (analyze-entities id message)
        response (if intent {:intent intent, :entities entities} {:error "no match"})]
    (do (println response)
        response)))