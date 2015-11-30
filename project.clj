(defproject hungry-nlp "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [compojure "1.4.0"]
                 [ring/ring-defaults "0.1.5"]
                 [clojure-opennlp "0.3.3"]
                 [clojurewerkz/serialism "1.3.0"]
                 [stencil "0.5.0"]
                 [org.clojure/math.combinatorics "0.1.1"]
                 [clj-fuzzy "0.3.1"]
                 [instaparse "1.4.0"]
                 [ring/ring-json "0.4.0"]]
  :plugins [[lein-ring "0.9.7"]]
  :ring {:handler hungry-nlp.handler/app}
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring/ring-mock "0.3.0"]]}})