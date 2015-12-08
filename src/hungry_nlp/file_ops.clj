(ns hungry-nlp.file-ops
  (:require [clojurewerkz.serialism.core :as s]
            [hungry-nlp.util :as util])
  (:use [clojure.tools.trace]))

(defn read-json [path] (s/deserialize (slurp path) :json))

(defn get-shared-entities [] (read-json "resources/json/shared/entities.json"))
(defn get-shared-intents [] (read-json "resources/json/shared/intents.json"))

(defn get-user-entities [id] (read-json (str "resources/json/user/" id "_entities.json")))
(defn write-user-entities [id content] (util/make-spit (str "resources/json/user/" id "_entities.json") (s/serialize content :json)))

(defn compound-entities-training-filepath [id] (str "resources/training/" id "_compound_entities.train"))
(defn base-entities-training-filepath [id] (str "resources/training/" id "_base_entities.train"))
(defn intents-training-filepath [id] (str "resources/training/" id "_intents.train"))

(defn write-compound-entities-training [id content] (util/make-spit (str "resources/training/" id "_compound_entities.train") content))
(defn write-base-entities-training [id content] (util/make-spit (str "resources/training/" id "_base_entities.train") content))
(defn write-intents-training [id content] (util/make-spit (str "resources/training/" id "_intents.train") content))

(defn get-merged-entities [id]
  (let [shared-entities (get-shared-entities)]
    (assoc shared-entities :baseEntities (merge (:baseEntities shared-entities)
                                                 (get-user-entities id)))))
(def merge-synonyms
  (partial util/map-vals-mapcat #(if (map? %) (:synonyms %) [%])))