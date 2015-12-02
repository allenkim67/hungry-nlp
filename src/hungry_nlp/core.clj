(ns hungry-nlp.core
  (:require [clojure.java.io :as io]
            [opennlp.nlp :as nlp]
            [opennlp.tools.train :as train]
            [clojurewerkz.serialism.core :as s]
            [clj-fuzzy.metrics :as fuzzy])
  (:gen-class))

(def tokenize (nlp/make-tokenizer "resources/en-token.bin"))

(defn analyze-intent [message]
  (let [intent-model (train/train-document-categorization "resources/training/shared/intents.train")
        categorizer (nlp/make-document-categorizer intent-model)]
    (:best-category (categorizer message))))

(defn spellcheck [id entity-type match]
  (let [entities-filepath-with-id (str "resources/json/user/" id "-entities.json")
        entities-json-filepath (if (.exists (io/file entities-filepath-with-id))
                                 entities-filepath-with-id
                                 "resources/json/shared/entities.json")
        entities-json (s/deserialize (slurp entities-json-filepath) :json)]
    (->> (entity-type entities-json)
         (sort-by (partial fuzzy/jaro-winkler match))
         last)))

(defn analyze-entities
  ([message] (analyze-entities nil message))
  ([id message]
    (let [entities-path-from-id (str "resources/training/user/" id "-entities.train")
          entites-train-filepath (if (and (.exists (io/file entities-path-from-id)) id)
                                   entities-path-from-id
                                   "resources/training/shared/entities.train")
          name-finder-model (train/train-name-finder entites-train-filepath)
          name-finder (nlp/make-name-finder name-finder-model)
          found-entities (name-finder (tokenize message))
          entity-types (->> found-entities meta :spans (map (comp keyword :type)))]
      (into {} (map vector entity-types (map (partial spellcheck id) entity-types found-entities))))))

(defn analyze [id message]
  (let [intent (analyze-intent message)
        entities (analyze-entities id message)
        response {:intent intent, :entities entities}]
    (do (println response)
        response)))