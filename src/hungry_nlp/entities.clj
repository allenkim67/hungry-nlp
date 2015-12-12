(ns hungry-nlp.entities
  (:require [clojurewerkz.serialism.core :as s]
            [traversy.lens :as t :refer [*>]]
            [hungry-nlp.fuzzy :as fuzzy]
            [hungry-nlp.util :as util])
  (:use [clojure.tools.trace]))

(defn get-entities [id]
  (let [shared-entities (s/deserialize (slurp "resources/shared_entities.json") :json)
        user-entities (s/deserialize (slurp (str "resources/user_entities/" id "_entities.json")) :json)]
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
  (let [reducer (fn [result entity]
                  (let [threshold {"number" 0.1, "food" 0.3}
                        s (or (-> result last :positions meta :sentence) sentence)]
                    (conj result
                          (merge entity
                                 (fuzzy/match s
                                              (:name entity)
                                              (get threshold (:type entity)))))))]
    (->> (reduce reducer [] entities)
         (filter (comp not-empty :positions))
         (mapcat (fn [entity] (map #(-> (assoc entity :position %)
                                        (dissoc :positions))
                                   (:positions entity))))
         (sort-by :position))))

(defn group-orders
  ([entities] (group-orders entities []))
  ([entities orders]
    (if (empty? entities)
      (if (empty? orders)
        {}
        {:orders (reverse orders)})
      (let [order (last orders)
            entity (first entities)
            type (keyword (:type entity))
            name (:canonical entity)]
        (recur (rest entities)
               (if (and order (not (contains? order :food)))
                 (util/update-last-in orders #(assoc % type name))
                 (conj orders (hash-map type name))))))))