(ns hungry-nlp.classifier
  (:import (edu.stanford.nlp.classify ColumnDataClassifier))
  (:gen-class))

(defn classify 
  ([s] classify :general s)
  ([type s] 
   (let [cdc (ColumnDataClassifier. (str "resources/intents.prop")) 
         cl (.makeClassifier cdc (.readTrainingExamples cdc (str "resources/" (name type) "_intents.train"))) 
         datum (.makeDatumFromStrings cdc (into-array String ["" s]))] 
     (.classOf cl datum))))