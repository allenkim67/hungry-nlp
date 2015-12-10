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

(defn locate-entities [entities message]
  (-> entities
      (t/update 
        (*> t/all-values t/each) 
        (fn [names] (-> (fuzzy/match message names)
                        (t/view t/all-values)
                        flatten
                        (->> (vector (first names))))))
      (t/update 
        t/all-values 
        (util/fcomp (partial filter (comp not-empty second))
                    util/kv-pairs))
      util/kv-pairs
      (->> (map flatten)
           (sort-by last))))

(defn group-orders
  ([entities] (group-orders entities []))
  ([entities orders]
    (if (empty? entities)
      (if (empty? orders) {} {:orders (reverse orders)})
      (let [order (first orders)
            entity (first entities)]
        (recur (rest entities)
               (if (and order (not (contains? order :food)))
                 (update-in orders [0] #(assoc % :food (second entity)))
                 (into [(hash-map (keyword (first entity)) (second entity))] orders)))))))