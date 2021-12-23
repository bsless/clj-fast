(ns clj-fast.core
  (:require
   [clj-fast.util :refer [as]]
   [clojure.core.protocols :as p])
  (:import
   (io.github.bsless.clj_fast Util)
   (clojure.lang Box RT)))

(set! *warn-on-reflection* true)

(defn entry-at
  "Returns the map-entry mapped to key or nil if key not present."
  {:inline
   (fn entry-at [m k]
     (if (symbol? m)
       `(.entryAt ~(with-meta m {:tag 'clojure.lang.IPersistentMap}) ~k)
       `(let [m# ~m] (entry-at m# ~k))))}
  [^clojure.lang.IPersistentMap m k]
  (.entryAt m k))

(defn val-at
  "Returns the value mapped to key or nil if key not present."
  {:inline
   (fn val-at [m k & nf]
     (if (symbol? m)
       (let [m (as clojure.lang.IPersistentMap m)]
         `(.valAt ~m ~k ~@nf))
       `(let [m# ~m] (val-at m# ~k ~@nf))))
   :inline-arities #{2 3}}
  ([^clojure.lang.IPersistentMap m k]
   (.valAt m k))
  ([^clojure.lang.IPersistentMap m k nf]
   (.valAt m k nf)))

;;; Credit Metosin
;;; https://github.com/metosin/reitit/blob/0bcfda755f139d14cf4eff37e2b294f573215213/modules/reitit-core/src/reitit/impl.cljc#L136
(definline fast-assoc
  "Like assoc but only takes one kv pair. Slightly faster."
  [a k v]
  (if (symbol? a)
    (let [a (as clojure.lang.Associative a)]
      `(.assoc ~a ~k ~v))
    `(let [a# ~a] (fast-assoc a# ~k ~v))))

(defn kvreduce
  {:inline
   (fn kvreduce [f init amap]
     (if (symbol? amap)
       (let [amap (as clojure.lang.IKVReduce amap)]
         `(.kvreduce ~amap ~f ~init))
       `(let [amap# ~amap] (kvreduce ~f ~init amap#))))}
  [f init ^clojure.lang.IKVReduce amap]
  (.kvreduce amap f init))

;;; Credit Metosin
;;; https://github.com/metosin/compojure-api/blob/master/src/compojure/api/common.clj#L46
(definline fast-map-merge
  "Returns a map that consists of the second of the maps assoc-ed onto
  the first. If a key occurs in more than one map, the mapping from
  te latter (left-to-right) will be the mapping in the result."
  [x y]
  `(kvreduce fast-assoc ~x ~y))

(definline fast-count
  "like [[clojure.core/count]] but works only for clojure.lang.Counted
  collections."
  [coll]
  (if (symbol? coll)
    (let [coll (as clojure.lang.Counted coll)]
      `(.count ~coll))
    `(let [coll# ~coll] (fast-count coll#))))

(definline short-circuiting-merge
  "Return a function which will merge two maps and short circuit if any of
  them are empty or nil."
  [count-fn merge-fn]
  `(fn [x# y#]
     (if (zero? (~count-fn x#))
       y#
       (if (zero? (~count-fn y#))
         x#
         (~merge-fn x# y#)))))

(defmacro def-short-circuiting-merge
  "Define a short-circuiting merge function. Will use the provided
  `count-fn` and `merge-fn`.
  Example:
  (def-short-circuiting-merge sc-merge count merge)
  Can take advantage of `inline` implementations as well."
  [name count-fn merge-fn]
  (let [ifn (:inline (meta #'short-circuiting-merge))
        inline (ifn count-fn merge-fn)]
    `(do
       (def ~name (short-circuiting-merge ~count-fn ~merge-fn))
       (alter-meta! (var ~name) assoc :inline ~inline)
       (var ~name))))

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

(defmacro definline+
  [name & decls]
  (let [[pre-args decls] (split-with (comp not list?) decls)
        argvs (map first decls)
        body' (eval (list* `fn (symbol (str "apply-inline-" name)) decls))
        decls' (map (fn build-decls [argv] (list argv (apply body' argv))) argvs)
        counts (into #{} (map count) argvs)]
    `(do
       (defn ~name ~@pre-args ~@decls')
       (alter-meta! (var ~name) assoc :inline (fn ~name ~@decls) :inline-arities ~counts)
       (var ~name))))

(defn pack
  {:inline (fn pack [& args] (println 'inlining!) `(Util/pack ~@args))
   :inline-arities (fn [n] (println 'yo n) (< n 21))}
  ([] (Util/pack))
  ([a] (Util/pack a))
  ([a b] (Util/pack a b))
  ([a b c] (Util/pack a b c))
  ([a b c d] (Util/pack a b c d))
  ([a b c d e] (Util/pack a b c d e))
  ([a b c d e f] (Util/pack a b c d e f))
  ([a b c d e f g] (Util/pack a b c d e f g))
  ([a b c d e f g h] (Util/pack a b c d e f g h))
  ([a b c d e f g h i] (Util/pack a b c d e f g h i))
  ([a b c d e f g h i j] (Util/pack a b c d e f g h i j))
  ([a b c d e f g h i j k] (Util/pack a b c d e f g h i j k))
  ([a b c d e f g h i j k l] (Util/pack a b c d e f g h i j k l))
  ([a b c d e f g h i j k l m] (Util/pack a b c d e f g h i j k l m))
  ([a b c d e f g h i j k l m n] (Util/pack a b c d e f g h i j k l m n))
  ([a b c d e f g h i j k l m n o] (Util/pack a b c d e f g h i j k l m n o))
  ([a b c d e f g h i j k l m n o p]
   (Util/pack a b c d e f g h i j k l m n o p))
  ([a b c d e f g h i j k l m n o p q]
   (Util/pack a b c d e f g h i j k l m n o p q))
  ([a b c d e f g h i j k l m n o p q r]
   (Util/pack a b c d e f g h i j k l m n o p q r))
  ([a b c d e f g h i j k l m n o p q r s]
   (Util/pack a b c d e f g h i j k l m n o p q r s))
  ([a b c d e f g h i j k l m n o p q r s t]
   (Util/pack a b c d e f g h i j k l m n o p q r s t)))

(import 'clojure.lang.TransformerIterator$MultiIterator)

(defn multi-iterator
  [a b]
  (new TransformerIterator$MultiIterator (pack a b)))


(defn transformer-iterator
  ([xform a b]
   (or (clojure.lang.RT/chunkIteratorSeq
        (new
         clojure.lang.TransformerIterator
         xform
         (pack a b)
         true))
       ())))
