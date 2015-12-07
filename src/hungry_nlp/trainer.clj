(ns hungry-nlp.trainer
  (:require [stencil.core :as stencil]
            [clojure.math.combinatorics :as combinatorics]
            [hungry-nlp.util :as util]
            [hungry-nlp.file-ops :as fop])
  (:use [clojure.tools.trace]))

(def stringify-intents
  (let [join-intents (fn [s [category intent]] (str s (name category) " " intent "\n"))]
    (partial reduce join-intents "")))

(def format-intents
  (util/fcomp
    (util/fcomp util/kv-pairs util/flatten-one)
    stringify-intents))

(defn get-entity-combos [entities entity-names]
  (->> entity-names
       (map keyword)
       (select-keys entities)
       util/kv-pairs
       (apply combinatorics/cartesian-product)
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
    (util/map-vals-mapcat interpolate intents)))

(defn wrap-spans [entities-json]
  (util/map-kv (fn [k v] (map #(str "<START:" (name k) "> " % " <END>") v)) entities-json))

(defn train-intents [id]
  (->> (fop/get-shared-intents)
       (interpolate-intents (->> (fop/get-merged-entities id)
                                 (util/map-vals-mapcat :synonyms)))
       format-intents
       (fop/write-intents-training id)))

(defn train-entities [id]
  (->> (fop/get-shared-intents)
       (interpolate-intents (->> (fop/get-merged-entities id)
                                 (util/map-vals-mapcat :synonyms)
                                 wrap-spans))
       ((util/fcomp vals flatten (partial clojure.string/join "\n")))
       (fop/write-entities-training id)))

(defn update-user-data [id entities]
  (do (fop/write-entities-training id entities)
      (train-entities id)
      (train-intents id)))