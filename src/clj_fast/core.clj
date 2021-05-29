(ns clj-fast.core
  (:require
   [clj-fast.util :refer [as]]
   [clojure.core.protocols :as p])
  (:import
   (clojure.lang Box)))

(defn entry-at
  "Returns the map-entry mapped to key or nil if key not present."
  {:inline
   (fn [m k]
     `(.entryAt ~(with-meta m {:tag 'clojure.lang.IPersistentMap}) ~k))}
  [^clojure.lang.IPersistentMap m k]
  (.entryAt m k))

(defn val-at
  "Returns the value mapped to key or nil if key not present."
  {:inline
   (fn [m k & nf]
     `(.valAt ~(with-meta m {:tag 'clojure.lang.IPersistentMap}) ~k ~@nf))
   :inline-arities #{2 3}}
  ([^clojure.lang.IPersistentMap m k]
   (.valAt m k))
  ([^clojure.lang.IPersistentMap m k nf]
   (.valAt m k nf)))

;;; Credit Metosin
;;; https://github.com/metosin/reitit/blob/0bcfda755f139d14cf4eff37e2b294f573215213/modules/reitit-core/src/reitit/impl.cljc#L136
(defn fast-assoc
  "Like assoc but only takes one kv pair. Slightly faster."
  {:inline
   (fn [a k v]
     `(.assoc ~(with-meta a {:tag 'clojure.lang.Associative}) ~k ~v))}
  [^clojure.lang.Associative a k v]
  (.assoc a k v))

(definline kvreduce
  [f init amap]
  `(.kvreduce ~(as clojure.lang.IKVReduce amap) ~f ~init))

;;; Credit Metosin
;;; https://github.com/metosin/compojure-api/blob/master/src/compojure/api/common.clj#L46
(defn fast-map-merge
  "Returns a map that consists of the second of the maps assoc-ed onto
  the first. If a key occurs in more than one map, the mapping from
  te latter (left-to-right) will be the mapping in the result."
  [x y]
  (reduce-kv fast-assoc x y))

(definline fast-count
  "like [[clojure.core/count]] but works only for clojure.lang.Counted
  collections."
  [coll]
  `(.count ~(as clojure.lang.Counted coll)))

;;; Credit github.com/joinr: github.com/bsless/clj-fast/issues/1
(defn rmerge!
  "Returns a transient map that consists of the second of the maps assoc-ed
  onto the first. If a key occurs in more than one map, the mapping from
  te latter (left-to-right) will be the mapping in the result."
  [l  r]
  (let [rf (fn [^clojure.lang.ITransientAssociative acc k v]
             (if-not (acc k)
               (.assoc acc k v)
               acc))]
    (if (instance? clojure.lang.IKVReduce l)
      (.kvreduce ^clojure.lang.IKVReduce l rf r)
      (p/kv-reduce l rf r))))

(defn fast-update-in
  ([m ks f]
   (let [up (fn up [m ks f]
              (let [[k & ks] ks]
                (if ks
                  (assoc m k (up (get m k) ks f))
                  (assoc m k (f (get m k))))))]
     (up m ks f)))
  ([m ks f a]
   (let [up (fn up [m ks f a]
              (let [[k & ks] ks]
                (if ks
                  (assoc m k (up (get m k) ks f a))
                  (assoc m k (f (get m k) a)))))]
     (up m ks f a)))
  ([m ks f a b]
   (let [up (fn up [m ks f a b]
              (let [[k & ks] ks]
                (if ks
                  (assoc m k (up (get m k) ks f a b))
                  (assoc m k (f (get m k) a b)))))]
     (up m ks f a b)))
  ([m ks f a b c]
   (let [up (fn up [m ks f a b c]
              (let [[k & ks] ks]
                (if ks
                  (assoc m k (up (get m k) ks f a b c))
                  (assoc m k (f (get m k) a b c)))))]
     (up m ks f a b c)))
  ([m ks f a b c & args]
   (let [up (fn up [m ks f a b c args]
              (let [[k & ks] ks]
                (if ks
                  (assoc m k (up (get m k) ks f a b c args))
                  (assoc m k (apply f (get m k) a b c args)))))]
     (up m ks f a b c args))))

(definline box!
  "Returns `v` inside a mutable box."
  [v]
  `(Box. ~v))

(definline unbox!
  "Get `v` out of mutable box."
  [^Box b]
  `(.-val ~(as Box b)))

(definline bset!
  "Sets the value of Box to `v` without regard to its current value.
  Returns `v`."
  [^Box b v]
  `(set! (. ~(as Box b) val) ~v))

(definline bset-vals!
  "Sets the value of `b` to `v`. Returns `[old new]`, the values in the
  box before and after the reset."
  [^Box b v]
  `(let [old# (unbox! ~b)
         new# (bset! ~b ~v)]
     [old# new#]))

(defn bswap!
  "Unsafely swaps the value of the box to be:
  (apply f current-value-of-box args).
  Stateful and messy."
  {:inline-arities #{2 3 4 5 6}
   :inline (fn [b f & args] `(bset! ~b (~f (unbox! ~b) ~@args)))}
  ([^Box b f] (bset! b (f (unbox! b))))
  ([^Box b f x] (bset! b (f (unbox! b) x)))
  ([^Box b f x y] (bset! b (f (unbox! b) x y)))
  ([^Box b f x y z] (bset! b (f (unbox! b) x y z)))
  ([^Box b f x y z & args] (bset! b (apply f (unbox! b) x y z args))))

(defn bswap-vals!
  "Unsafely swaps the value of the box to be:
  (apply f current-value-of-box args).
  Returns `[old new]`
  Stateful and messy."
  {:inline-arities #{2 3 4 5 6}
   :inline
   (fn [b f & args]
     `(let [old# (unbox! ~b)]
        [old# (bset! ~b (~f (unbox! ~b) ~@args))]))}
  ([^Box b f]
   (let [old (unbox! b)]
     [old (bset! b (f old))]))
  ([^Box b f x]
   (let [old (unbox! b)]
     [old (bset! b (f old x))]))
  ([^Box b f x y]
   (let [old (unbox! b)]
     [old (bset! b (f old x y))]))
  ([^Box b f x y z]
   (let [old (unbox! b)]
     [old (bset! b (f old x y z))]))
  ([^Box b f x y z & args]
   (let [old (unbox! b)]
     [old (bset! b (apply f old x y z args))])))
