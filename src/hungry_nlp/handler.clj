(ns hungry-nlp.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
            [clojure-watch.core :refer [start-watch]]
            [hungry-nlp.core :as nlp]))

(defn json-response [data & [status]]
  {:status  (or status 200)
   :headers {"Content-Type" "application/json"}
   :body    data})

(defroutes app-routes
  (GET "/" [] "Hello World")
  (GET "/query/:id" req (json-response (nlp/analyze (get-in req [:params :id]) (get-in req [:params :message]))))
  (POST "/userEntities/:id" req (nlp/update-user-data (get-in req [:params :id]) (:body req)) {:status 200})
  (route/not-found "Not Found"))

(def app
  (-> app-routes
      (wrap-defaults (assoc-in site-defaults [:security :anti-forgery] false))
      wrap-json-response
      (wrap-json-body {:keywords? true})))