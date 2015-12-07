(ns hungry-nlp.file-ops
  (:require [clojurewerkz.serialism.core :as s]
            [hungry-nlp.util :as util])
  (:use [clojure.tools.trace]))

(defn read-json [path] (s/deserialize (slurp path) :json))

(defn get-shared-entities [] (read-json "resources/json/shared/entities.json"))
(defn get-shared-intents [] (read-json "resources/json/shared/intents.json"))

(defn get-user-entities [id] (read-json (str "resources/json/user/" id "-entities.json")))
(defn write-user-entities [id content] (util/make-spit (str "resources/json/user/" id "-entities.json") (s/serialize content :json)))

(defn entities-training-filepath [id] (str "resources/training/" id "-entities.train"))
(defn intents-training-filepath [id] (str "resources/training/" id "-intents.train"))

(defn write-entities-training [id content] (util/make-spit (str "resources/training/" id "-entities.train") content))
(defn write-intents-training [id content] (util/make-spit (str "resources/training/" id "-intents.train") content))

(defn get-merged-entities [id]
  (merge (get-shared-entities)
         (get-user-entities id)))