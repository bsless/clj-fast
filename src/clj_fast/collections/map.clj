(ns clj-fast.collections.map
  (:refer-clojure :exclude [get find get-in map? memoize])
  (:import
   (java.util HashMap Map))
  (:require
   [clj-fast.lens :as lens]
   [clj-fast.util :as u]))

(def ^:private t {:tag 'java.util.Map})

(definline map?
  [m]
  `(instance? java.util.Map ~m))

;;; Credit Metosin
;;; https://github.com/metosin/reitit/blob/0bcfda755f139d14cf4eff37e2b294f573215213/modules/reitit-core/src/reitit/impl.cljc#L136
(defn ->hashmap
  ([] (HashMap.))
  ([m]
   (let [m (or m {})]
     (HashMap. ^Map m))))

;;; Credit Metosin
(defn get
  {:inline
   (fn ([m k]
       `(.get ~(with-meta m t) ~k))
     ([m k o]
      `(.getOrDefault ~(with-meta m t) ~k ~o)))
   :inline-arities #{2 3}}
  ([^Map m k]
   (.get m k))
  ([^Map m k o]
   (.getOrDefault m k o)))

(defn get?
  {:inline
   (fn ([m k]
       (let [m (with-meta m t)]
         `(if (map? ~m)
            (.get ~m ~k))))
     ([m k o]
      (let [m (with-meta m t)]
        `(if (map? ~m)
           (.getOrDefault ~m ~k ~o)))))
   :inline-arities #{2 3}}
  ([^Map m k]
   (when (map? m)
     (.get m k)))
  ([^Map m k o]
   (when (map? m)
     (.getOrDefault m k o))))

(defmacro get-in
  [m ks]
  (lens/get-some (fn [sym k] `(get ~sym ~k)) m ks))

(defn put
  {:inline
   (fn [m k v]
     (let [m* (gensym "m__")]
       `(let [~m* ~m]
          (.put ~(with-meta m* {:tag 'java.util.Map}) ~k ~v)
          ~m*)))}
  [^Map m k v]
  (.put m k v) m)

(defmacro put-in
  "Like core/assoc-in but for nested ConcurrentMaps."
  [m ks v]
  (lens/put
   (fn [m k v] `(put ~m ~k ~v))
   (fn [m k] `(or (get ~m ~k) (->hashmap)))
   m (u/simple-seq ks) v))

(defn memoize
  [f]
  (let [mem (atom (->hashmap))
        sentinel (new Object)]
    (fn [& args]
      (if-let [e (get @mem args)]
        (if (u/eq? sentinel e) nil e)
        (let [ret (apply f args)
              ret (if (nil? ret) sentinel ret)]
          (swap! mem put args ret)
          ret)))))

(defmacro memoize*
  [n f]
  (if (zero? n)
    `(u/memoize0 ~f)
    (let [args (repeatedly n #(gensym))]
      `(let [mem# (atom (->hashmap))
             sentinel# (new Object)]
         (fn [~@args]
           (if-let [e# (get-in @mem# ~args)]
             (if (u/eq? sentinel# e#) nil e#)
             (let [ret# (~f ~@args)
                   ret# (if (nil? ret#) sentinel# ret#)]
               (swap! mem# (fn [m# v#] (put-in m# [~@args] v#)) ret#)
               ret#)))))))
