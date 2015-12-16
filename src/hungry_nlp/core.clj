(ns hungry-nlp.core
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

(defn add-order [groups type name]
  (if (empty? groups)
    (conj groups {:intent "order" :orders [{type name}] })
    (if (contains? (first groups) :orders)
      (if (contains? (get-in groups [0 :orders 0]) :food)
        (update-in groups [0 :orders] #(util/prepend % {type name}))
        (update-in groups [0 :orders 0] #(assoc % type name)))
      (update-in groups [0] #(assoc % :orders [{type name}])))))

(defn group-entities
  ([entities] (group-entities entities []))
  ([entities groups]
   (if (empty? entities)
     (reverse groups)
     (let [entity (first entities)
           name (:canonical entity)
           type (keyword (:type entity))]
       (if (= type :intent)
         (recur (rest entities) (util/prepend groups {:intent name}))
         (recur (rest entities) (add-order groups type name)))))))

(defn analyze [id message]
  (let [message (clojure.string/lower-case message)] 
    (-> (get-entities id)
        parse-entities
        (locate-entities message)
        group-entities
        (#(if (empty? %) [{:intent nil}] %)))))