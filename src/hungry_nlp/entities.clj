(ns hungry-nlp.entities
  (:require [clojurewerkz.serialism.core :as s]
            [hungry-nlp.fuzzy :as fuzzy]
            [hungry-nlp.util :as util]
            [hungry-nlp.s3 :as s3])
  (:use [clojure.tools.trace]))

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

(def entity-threshold {"number" 0.1})

(defn locate-entities-reducer [result entity]
  (let [threshold (get entity-threshold (:type entity))
        match (fuzzy/match result entity threshold)]
    (-> result
        (assoc :_marked-sentence (:_marked-sentence match))
        (assoc :var-sentence (:var-sentence match))
        (update-in [:entities] #(conj % (:entity match))))))

(defn format-entities [entities]
  (->> entities
       (filter (comp not-empty :positions))
       (mapcat (fn [entity] (map #(-> (assoc entity :position %)
                                      (dissoc :positions))
                                 (:positions entity))))
       (sort-by :position)))

(defn extract-entities [id sentence]
  (let [initial {:entities         []
                 :sentence         sentence
                 :var-sentence     sentence
                 :_marked-sentence sentence}
        entities (parse-entities (get-entities id))]
    (update-in (reduce locate-entities-reducer initial entities)
               [:entities]
               format-entities)))