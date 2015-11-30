(ns hungry-nlp.core
  (:require [opennlp.nlp :as nlp]
            [opennlp.tools.train :as train]
            [clojurewerkz.serialism.core :as s]
            [clj-fuzzy.metrics :as fuzzy])
  (:gen-class))

(def tokenize (nlp/make-tokenizer "resources/en-token.bin"))

(defn analyze-intent [message]
  (let [intent-model (train/train-document-categorization "resources/training/intents.train")
        categorizer (nlp/make-document-categorizer intent-model)]
    (:best-category (categorizer message))))

(defn spellcheck [entity-type match]
  (let [entities-json (s/deserialize (slurp "resources/json/entities.json") :json)]
    (->> (entity-type entities-json)
         (sort-by (partial fuzzy/jaro-winkler match))
         last)))

(defn analyze-entities [message]
  (let [name-finder-model (train/train-name-finder "resources/training/entities.train")
        name-finder (nlp/make-name-finder name-finder-model)
        found-entities (name-finder (tokenize message))
        entity-types (->> found-entities meta :spans (map (comp keyword :type)))]
    (into {} (map vector entity-types (map spellcheck entity-types found-entities)))))

(defn analyze [message]
  (let [intent (analyze-intent message)
        entities (analyze-entities message)]
    {:intent intent, :entities entities}))