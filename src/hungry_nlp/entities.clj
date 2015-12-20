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
                                                  :name %)
                                       names)))
       (sort-by (comp - count :name))))

(defn locate-entities [entities sentence]
  (let [threshold {"number" 0.1}
        reducer (fn [result entity]
                  (let [accSentence (or (-> result last :positions meta :sentence) sentence)]
                    (->> (fuzzy/match accSentence (:name entity) (get threshold (:type entity)))
                         (merge entity)
                         (conj result))))]
    (->> (reduce reducer [] entities)
         (filter (comp not-empty :positions))
         (mapcat (fn [entity] (map #(-> (assoc entity :position %)
                                        (dissoc :positions))
                                   (:positions entity))))
         (sort-by :position))))

(defn add-entity [groups entity]
  (let [type (keyword (:type entity))
        name (:canonical entity)]
    (if (or (empty? groups) (contains? (last groups) :food))
      (conj groups {type name})
      (util/update-last-in groups #(assoc % type name)))))

(defn group-entities
  ([entities] (group-entities entities []))
  ([entities groups]
   (let [entity (first entities)]
     (if (empty? entities)
       groups
       (recur (rest entities) (add-entity groups entity))))))