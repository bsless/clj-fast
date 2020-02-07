(ns clj-fast.collections.concurrent-hash-map
  (:refer-clojure :exclude [get])
  (:require
   [clj-fast
    [util :as u]
    [lens :as lens]])
  (:import
   [java.util.concurrent ConcurrentHashMap]))

(def ^:const t {:tag 'java.util.concurrent.ConcurrentHashMap})

(defn ->concurrent-hash-map
  ([] (ConcurrentHashMap.)))

(defn put!?
  "Puts v in k if k is absent from m."
  {:inline
   (fn [m k v]
     `(do (.putIfAbsent ~(with-meta m t) ~k ~v)
          ~m))}
  [^java.util.concurrent.ConcurrentHashMap m k v]
  (.putIfAbsent ^java.util.concurrent.ConcurrentHashMap m k v) m)

(defn concurrent-hash-map?
  "Checks if m is an instance of a ConcurrentHashMap"
  {:inline
   (fn [m] `(instance? ConcurrentHashMap ~m))}
  [chm]
  (instance? ConcurrentHashMap chm))

(defn get
  "Returns the value mapped to key or nil if key not present."
  [m k]
  {:inline
   (fn [m k]
     `(.get ~(with-meta m t) ~k)
     m)}
  [^java.util.concurrent.ConcurrentHashMap m k]
  (.get ^java.util.concurrent.ConcurrentHashMap m k))

(defn get?
  "Returns the value mapped to key or nil if key not present if m is a
  ConcurrentHashMap, otherwise returns m."
  [m k]
  {:inline
   (fn [m k]
     `(when (concurrent-hash-map? ~m)
        (.get ~(with-meta m t) ~k))
     m)}
  [m k]
  (when (concurrent-hash-map? m)
    (.get ^java.util.concurrent.ConcurrentHashMap m k)))

(defmacro get-in?
  "Like core/get-in but for nested ConcurrentHashMaps."
  [m ks]
  (lens/get-some
   (fn [m k] `(get? ~m ~k))
   m ks))

(defmacro put-in!
  "Like core/assoc-in but for nested ConcurrentHashMaps."
  [m ks v]
  (lens/put
   (fn [m k v] `(put!? ~m ~k ~v))
   (fn [m k] `(or (get? ~m ~k) (->concurrent-hash-map)))
   m (u/simple-seq ks) v))

(defmacro memoize-c
  [n f]
  (let [args (repeatedly n #(gensym))]
    `(let [mem# (->concurrent-hash-map)]
       (fn [~@args]
         (if-let [e# (get-in? mem# ~args)]
           e#
           (let [ret# (~f ~@args)]
             (put-in! mem# [~@args] ret#)
             ret#))))))
