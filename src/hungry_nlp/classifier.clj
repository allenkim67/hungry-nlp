(ns hungry-nlp.classifier
  (:import (edu.stanford.nlp.classify ColumnDataClassifier))
  (:gen-class))

(def threshold 0.0)

(defn classify 
  ([s] (classify :general s))
  ([type s] 
   (let [cdc (ColumnDataClassifier. (str "resources/intents.prop"))
         cl (.makeClassifier cdc (.readTrainingExamples cdc (str "resources/" (name type) "_intents.train")))
         datum (.makeDatumFromStrings cdc (into-array String ["" s]))
         probability (->> (.scoresOf cl datum) .values seq (apply max))]
     (println (str "\n" "CLASSIFICATION PROBABILITY: " probability))
     (if (< probability threshold) nil (.classOf cl datum)))))