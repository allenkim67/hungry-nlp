(ns hungry-nlp.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
            [hungry-nlp.core :as nlp]
            [clojurewerkz.serialism.core :as s]
            [hungry-nlp.s3 :as s3])
  (:use [clojure.tools.trace]))

(defn json-response [data & [status]]
  {:status  (or status 200)
   :headers {"Content-Type" "application/json"}
   :body    data})

(defroutes app-routes
  (GET "/" [] "Hello World")
  (GET "/query/:id" req
    (let [message (get-in req [:params :message])
          response (nlp/analyze (get-in req [:params :id]) message)]
      (println (str "ANALYSIS: " response "\n"))
      (json-response (merge response {:message message}))))
  (POST "/userEntities/:id" req
    (let [id (get-in req [:params :id])
          entities-json (s/serialize (:body req) :json)]
      (s3/upload (str "user_entities/" id) entities-json)
      {:status 200}))
  (route/not-found "Not Found"))

(def app
  (-> app-routes
      (wrap-defaults (assoc-in site-defaults [:security :anti-forgery] false))
      wrap-json-response
      (wrap-json-body {:keywords? true})))