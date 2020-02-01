(ns clj-fast.collections.hash-map
  (:refer-clojure :exclude [get])
  (:import
   (java.util HashMap Map)))

;;; Credit Metosin
;;; https://github.com/metosin/reitit/blob/0bcfda755f139d14cf4eff37e2b294f573215213/modules/reitit-core/src/reitit/impl.cljc#L136
(defn ->hashmap [m]
  (let [m (or m {})]
    (HashMap. ^Map m)))

;;; Credit Metosin
(defn get
  [^HashMap m k]
  (.get m k))
