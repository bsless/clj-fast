(ns clj-fast.collections.concurrent-map
  (:refer-clojure :exclude [get memoize])
  (:require
   [clj-fast
    [util :as u]
    [lens :as lens]])
  (:import
   [java.util Map]
   [java.util.concurrent
    ConcurrentMap ;; interface
    ConcurrentHashMap
    ConcurrentSkipListMap]))

(def ^:const t {:tag 'java.util.concurrent.ConcurrentMap})

(defn ->concurrent-hash-map
  ([] (ConcurrentHashMap.))
  ([m] (new ConcurrentHashMap ^Map m)))

(defn ->concurrent-skip-list-map
  ([] (ConcurrentSkipListMap.))
  ([m] (new ConcurrentSkipListMap ^Map m)))

(defn put!?
  "Puts v in k if k is absent from m."
  {:inline
   (fn [m k v]
     `(do (.putIfAbsent ~(with-meta m t) ~k ~v)
          ~m))}
  [^java.util.concurrent.ConcurrentMap m k v]
  (.putIfAbsent ^java.util.concurrent.ConcurrentMap m k v) m)

(defn concurrent-hash-map?
  "Checks if m is an instance of a ConcurrentMap"
  {:inline
   (fn [m] `(instance? ConcurrentMap ~m))}
  [chm]
  (instance? ConcurrentMap chm))

(defn get
  "Returns the value mapped to key or nil if key not present."
  [m k]
  {:inline
   (fn [m k]
     `(.get ~(with-meta m t) ~k))}
  [^java.util.concurrent.ConcurrentMap m k]
  (.get ^java.util.concurrent.ConcurrentMap m k))

(defn get?
  "Returns the value mapped to key or nil if key not present if m is a
  ConcurrentMap, otherwise returns m."
  [m k]
  {:inline
   (fn [m k]
     `(when (concurrent-hash-map? ~m)
        (.get ~(with-meta m t) ~k))
     m)}
  [m k]
  (when (concurrent-hash-map? m)
    (.get ^java.util.concurrent.ConcurrentMap m k)))

(defmacro get-in?
  "Like core/get-in but for nested ConcurrentMaps."
  [m ks]
  (lens/get-some
   (fn [m k] `(get? ~m ~k))
   m ks))

(defmacro put-in!
  "Like core/assoc-in but for nested ConcurrentMaps."
  [m ks v]
  (lens/put
   (fn [m k v] `(put!? ~m ~k ~v))
   (fn [m k] `(or (get? ~m ~k) (->concurrent-hash-map)))
   m (u/simple-seq ks) v))

(defn memoize
  [f]
  (let [mem (->concurrent-hash-map)
        sentinel (new Object)]
    (fn [& args]
      (if-let [e (get mem args)]
        (if (= e sentinel) nil e)
        (let [ret (apply f args)
              ret (if (nil? ret) sentinel ret)]
          (put!? mem args ret)
          ret)))))

(defmacro memoize*
  [n f]
  (if (zero? n)
    `(u/memoize0 ~f)
    (let [args (repeatedly n #(gensym))]
      `(let [mem# (->concurrent-hash-map)
             sentinel# (new Object)]
         (fn [~@args]
           (if-let [e# (get-in? mem# ~args)]
             (if (= e# sentinel#) nil e#)
             (let [ret# (~f ~@args)
                   ret# (if (nil? ret#) sentinel# ret#)]
               (put-in! mem# [~@args] ret#)
               ret#)))))))
