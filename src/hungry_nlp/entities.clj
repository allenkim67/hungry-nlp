(ns hungry-nlp.entities
  (:require [clojurewerkz.serialism.core :as s]
            [hungry-nlp.fuzzy :as fuzzy]
            [hungry-nlp.util :as util]
            [hungry-nlp.s3 :as s3])
  (:use [clojure.tools.trace]))

(def entity-threshold {"number" 0.1})

(defn get-entities [id]
  (let [shared-entities (s/deserialize (slurp "resources/shared_entities.json") :json)
        user-entities (s/deserialize (slurp (s3/download (str "user_entities/" id))) :json)]
    (merge shared-entities user-entities)))

(defn parse-entities [entities]
  (->> entities
       util/kv-pairs
       (mapcat (fn [[type names]] (map #(hash-map :type (name type)
                                                  :canonical (first names)
                                                  :name (clojure.string/lower-case %))
                                       names)))
       (sort-by (comp - count :name))))

(defn best-fit [entities sentence]
  entities)

(defn extract-entities [id sentence]
  (let [entities (parse-entities (get-entities id))
        reducer (fn [acc-matches entity]
                  (let [name (clojure.string/lower-case (:name entity))
                        threshold (get entity-threshold (:type entity))
                        positions (fuzzy/find-all sentence name threshold)]
                    (concat acc-matches (map #(assoc entity :position %) positions))))
        matched-entities (->> (reduce reducer [] entities) (sort-by :position))]
    (best-fit matched-entities sentence)))

(defn var-sentence [entities sentence]
  (let [reducer (fn [acc-sentence entity]
                  (let [{name :name type :type} entity
                        offset (- (count acc-sentence) (count sentence))
                        position (+ (:position entity) offset)]
                    (util/splice acc-sentence
                                 position
                                 (+ position (count name))
                                 (str "<" type ">"))))]
    (reduce reducer sentence entities)))