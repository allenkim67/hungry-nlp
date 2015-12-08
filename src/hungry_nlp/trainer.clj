(ns hungry-nlp.trainer
  (:require [stencil.core :as stencil]
            [clojure.math.combinatorics :as combo]
            [hungry-nlp.util :as util]
            [hungry-nlp.file-ops :as fop])
  (:use [clojure.tools.trace]))

(def stringify-intents
  (let [join-intents (fn [s [category intent]] (str s (name category) " " intent "\n"))]
    (partial reduce join-intents "")))

(def format-intents
  (util/fcomp
    (util/fcomp util/kv-pair-groups util/flatten-one)
    stringify-intents))

(defn get-entity-combos [entities entity-names]
  (->> entity-names
       (map keyword)
       (select-keys entities)
       util/kv-pair-groups
       (apply combo/cartesian-product)
       (map flatten)
       (map #(apply hash-map %))))

(def get-template-vars
  (util/fcomp (partial re-seq #"\{\{\{(.*?)\}\}\}")
              (partial map last)))

(defn interpolate-intents [entities intents]
  (let [interpolate (fn [intent] (->> intent
                                      get-template-vars
                                      (get-entity-combos entities)
                                      (map #(stencil/render-string intent %))))]
    (if (every? (util/fcomp get-template-vars empty?) (->> intents vals flatten))
      intents
      (recur entities (util/map-vals-mapcat interpolate intents)))))

(defn wrap-spans [entity-type entities-json]
  (let [wrap (fn [k v] (map #(str "<START:" (name k) "> " % " <END>") v))]
    (update-in entities-json [entity-type] (partial util/map-kv wrap))))

(defn train-intents [id]
  (->> (fop/get-shared-intents)
       (interpolate-intents (->> (fop/get-merged-entities id)
                                 util/merge-vals
                                 fop/merge-synonyms))
       format-intents
       (fop/write-intents-training id)))

(defn train-base-entities [id]
  (let [entities (fop/get-merged-entities id)
        compound-intents (->> entities :compoundEntities)]
    (->> compound-intents
         (interpolate-intents (->> entities
                                   (util/map-vals fop/merge-synonyms)
                                   (wrap-spans :baseEntities)
                                   util/merge-vals))
         ((util/fcomp vals flatten (partial clojure.string/join "\n")))
         (fop/write-base-entities-training id))))

(defn train-compound-entities [id]
  (->> (fop/get-shared-intents)
       (interpolate-intents (->> (fop/get-merged-entities id)
                                 (wrap-spans :compoundEntities)
                                 util/merge-vals
                                 fop/merge-synonyms))
       ((util/fcomp vals flatten (partial clojure.string/join "\n")))
       (fop/write-compound-entities-training id)))

(defn train-entities [id]
  (do
    (train-compound-entities id)
    (train-base-entities id)))

(defn update-user-data [id entities]
  (do (fop/write-user-entities id entities)
      (train-entities id)
      (train-intents id)))