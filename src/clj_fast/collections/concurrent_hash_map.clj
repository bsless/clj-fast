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
  {:inline
   (fn [m k v]
     `(do (.putIfAbsent ~(with-meta m t) ~k ~v)
          ~m))}
  [^java.util.concurrent.ConcurrentHashMap m k v]
  (.putIfAbsent ^java.util.concurrent.ConcurrentHashMap m k v) m)

(defn concurrent-hash-map?
  {:inline
   (fn [m] `(instance? ConcurrentHashMap ~m))}
  [chm]
  (instance? ConcurrentHashMap chm))

(defn get
  [m k]
  {:inline
   (fn [m k]
     `(.get ~(with-meta m t) ~k)
     m)}
  [^java.util.concurrent.ConcurrentHashMap m k]
  (.get ^java.util.concurrent.ConcurrentHashMap m k))

(defn get?
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
  [m ks]
  (lens/get-some
   (fn [m k] `(get? ~m ~k))
   m ks))

(defmacro put-in!
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
