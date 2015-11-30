(ns hungry-nlp.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
            [clojurewerkz.serialism.core :as s]
            [hungry-nlp.core :as nlp]
            [hungry-nlp.trainer :as trainer]))

(defn json-response [data & [status]]
  {:status  (or status 200)
   :headers {"Content-Type" "application/json"}
   :body    data})

(defn write-entities-json [id json]
  (let [shared-entities (s/deserialize (slurp "resources/json/entities.json") :json)
        entities-json (merge shared-entities json)]
    (do
      (spit (str "resources/json/user/" id "-entities.json") (s/serialize entities-json :json))
      (trainer/train-entities id))))

(defroutes app-routes
  (GET "/" [] "Hello World")
  (GET "/query/:id" req (json-response (nlp/analyze (get-in req [:params :id]) (get-in req [:params :message]))))
  (POST "/userEntities/:id" req (write-entities-json (get-in req [:params :id]) (:body req)) {:status 200})
  (route/not-found "Not Found"))

(def app
  (-> app-routes
      (wrap-defaults (assoc-in site-defaults [:security :anti-forgery] false))
      wrap-json-response
      (wrap-json-body {:keywords? true})))