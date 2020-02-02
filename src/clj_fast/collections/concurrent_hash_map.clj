(ns clj-fast.collections.concurrent-hash-map
  (:refer-clojure :exclude [get])
  (:require
   [clj-fast
    [util :as u]])
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
  (let [ks (u/simple-seq ks)
        sym (gensym "m__")
        steps
        (map (fn [step] `(if (nil? ~sym) nil (get? ~sym ~step)))
             ks)]
    `(let [~sym ~m
           ~@(interleave (repeat sym) steps)]
       ~sym)))

(defmacro put-in!
  [m ks v]
  (let [me {:tag 'ConcurrentHashMap}
        g (with-meta (gensym "m__") me)
        gs (repeatedly (count ks) #(with-meta (gensym) me))
        gs+ (list* g gs)
        bs
        (into
         [g m]
         (mapcat (fn [g- g k]
                   [g- `(or (get? ~g ~k) (->concurrent-hash-map))])
                 (butlast gs)
                 gs+
                 ks))
        iter
        (fn iter
          [[sym & syms] [k & ks] v]
          (if ks
            `(put!? ~sym ~k ~(iter syms ks v))
            `(put!? ~sym ~k ~v)))]
    `(let ~bs
       ~(iter gs+ ks v))))

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
