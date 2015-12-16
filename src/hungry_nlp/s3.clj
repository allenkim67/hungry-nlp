(ns hungry-nlp.s3
  (:require [aws.sdk.s3 :as s3]))

(def cred {:access-key (System/getenv "S3_ACCESS_KEY"), :secret-key (System/getenv "S3_SECRET_KEY")})

(defn download [k]
  (:content (s3/get-object cred "hungrybot" k)))

(defn upload [k v]
  (s3/put-object cred "hungrybot" k v))