(ns hungry-nlp.core
  (:require [clojurewerkz.serialism.core :as s]
            [hungry-nlp.fuzzy :as fuzzy]
            [hungry-nlp.util :as util]
            [hungry-nlp.s3 :as s3])
  (:use [clojure.tools.trace]))

(defn get-entities [id]
  (let [shared-entities (s/deserialize (slurp "resources/shared_entities.json") :json)
        user-entities (s/deserialize (slurp (s3/read id)) :json)]
    (merge shared-entities user-entities)))

(defn parse-entities [entities]
  (->> entities
       util/kv-pairs
       (mapcat (fn [[type names]] (map #(hash-map :type (name type)
                                                  :canonical (first names)
                                                  :name %)
                                       names)))
       (sort-by (comp - count :name))
       (sort-by :type)))

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

(defn add-order-entity [entity orders]
  (let [type (keyword (:type entity))
        name (:canonical entity)]
    (if (= (:type entity) "number")
      (conj orders (hash-map type name))
      (util/update-last-in orders #(assoc % type name)))))

(defn group-entities
  ([entities] (group-entities entities []))
  ([entities groups]
   (if (empty? entities)
     groups
     (let [entity (first entities)]
       (if (= (:type entity) "intent")
         (recur (rest entities) (conj groups (hash-map :intent (:canonical entity) :entities [])))
         (recur (rest entities) (util/update-last-in groups [:entities] (partial add-order-entity entity))))))))

(defn analyze [id message]
  (let [message (clojure.string/lower-case message)] 
    (-> (get-entities id)
        parse-entities
        (locate-entities message)
        group-entities)))