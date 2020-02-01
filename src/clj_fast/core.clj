(ns clj-fast.core
  (:import
   (java.util HashMap Map)
   [java.util.concurrent ConcurrentHashMap]))

;;; Credit Metosin
;;; https://github.com/metosin/reitit/blob/0bcfda755f139d14cf4eff37e2b294f573215213/modules/reitit-core/src/reitit/impl.cljc#L136
(defn fast-assoc
  [^clojure.lang.Associative a k v]
  (.assoc a k v))

(defmacro fast-assoc*
  [m & kvs]
  {:pre [(even? (count kvs))]}
  (let [chain#
        (map (fn [[k v]] `(fast-assoc ~k ~v)) (partition 2 kvs))]
    `(-> ~m ~@chain#)))

;;; Credit Metosin
;;; https://github.com/metosin/reitit/blob/0bcfda755f139d14cf4eff37e2b294f573215213/modules/reitit-core/src/reitit/impl.cljc#L136
(defn fast-map [m]
  (let [m (or m {})]
    (HashMap. ^Map m)))

;;; Credit Metosin
(defn fast-get
  [^HashMap m k]
  (.get m k))


;;; Credit Metosin
;;; https://github.com/metosin/compojure-api/blob/master/src/compojure/api/common.clj#L46
(defn fast-map-merge
  [x y]
  (reduce-kv
   (fn [m k v]
     (fast-assoc m k v))
   x
   y))

(defmacro inline-merge
  [& [m & ms]]
  (let [conjs# (map (fn [m] `(conj ~m)) ms)]
    `(-> (or ~m {})
         ~@conjs#)))

(defmacro inline-fast-map-merge
  [& [m & ms]]
  (let [conjs# (map (fn [m] `(fast-map-merge ~m)) ms)]
    `(-> (or ~m {})
         ~@conjs#)))

;;; Credit github.com/joinr: github.com/bsless/clj-fast/issues/1
(defn rmerge! [^clojure.lang.IKVReduce l  r]
  (.kvreduce
   l
   (fn [^clojure.lang.ITransientAssociative acc k v]
     (if-not (acc k)
       (.assoc acc k v)
       acc))
   r))

(defmacro inline-tmerge
  ([] {})
  ([m] m)
  ([m1 m2 & ms]
   (let [ms (list* m1 m2 ms)
         end (last ms)
         ms (reverse (butlast ms))
         ops (map (fn [m] `(rmerge! ~m)) ms)]
     `(->> (transient ~end) ~@ops persistent!))))

(defn- simple?
  [x]
  (or (keyword? x) (symbol? x) (string? x) (int? x)))

(defn lazy?
  [xs]
  (instance? clojure.lang.LazySeq xs))

(def sequence? (some-fn lazy? vector? list?))

(defn- try-resolve
  [sym]
  (when (symbol? sym)
    (when-let [r (resolve sym)]
      (deref r))))

(defn- simple-seq?
  [xs]
  (let [xs (or (try-resolve xs) xs)]
    (and (sequence? xs) (every? simple? xs))))

(defn- simple-seq
  [xs]
  (let [xs (or (try-resolve xs) xs)]
    (and (sequence? xs) (every? simple? xs) (seq xs))))


(comment
  (defmacro fast-get-in-th
    [m ks]
    {:pre [(vector? ks)]}
    `(-> ~m ~@ks)))

(defmacro inline-get-in
  "Like `get-in` but faster and uses code generation.
  `ks` must be either vector, list or set."
  [m ks]
  {:pre [(simple-seq? ks)]}
  (let [ks (simple-seq ks)
        chain#
        (map (fn [k] `(get ~k)) ks)]
    `(-> ~m ~@chain#)))

(defmacro inline-get-some-in
  [m ks]
  {:pre [(simple-seq? ks)]}
  (let [ks (simple-seq ks)
        sym (gensym "m__")
        steps
        (map (fn [step] `(if (nil? ~sym) nil (~sym ~step)))
             ks)]
    `(let [~sym ~m
           ~@(interleave (repeat sym) steps)]
       ~sym)))

(defn- destruct-map
  [m ks]
  (let [gmap (gensym "map__")
        syms (repeatedly (count ks) #(gensym))]
    (vec
     (concat `(~gmap ~m)
             (mapcat
              (fn [sym k]
                `(~sym (get ~gmap ~k)))
              syms
              ks)))))

(defn- extract-syms
  [bs]
  (map first (partition 2 (drop 2 bs))))

(defmacro inline-select-keys
  "Like `select-keys` but faster and uses code generation.
  `ks` must be either vector, list or set."
  [m ks]
  {:pre [(simple-seq? ks)]}
  (let [ks (simple-seq ks)
        bindings (destruct-map m ks)
        syms (extract-syms bindings)
        form (apply hash-map (interleave ks syms))]
    `(let ~bindings
       ~form)))

(comment
  (def ^:private cache (atom {}))

  (defn- anon-record
    [fields]
    (if-let [grec (get @cache fields)]
      grec
      (let [grec (gensym "Rec")]
        (println "defing record of name:" grec "with fields:" fields)
        (eval `(defrecord ~grec ~fields))
        (swap! cache assoc fields grec)
        grec)))

  (defmacro defrec->inline-select-keys
    "Like `select-keys` but faster and uses code generation.
  `ks` must be either vector, list or set."
    [m ks]
    {:pre [(simple-seq? ks)]}
    (let [ks (simple-seq ks)
          fields (mapv symbol ks)
          grec (anon-record fields)
          bindings (destruct-map m ks)
          syms (extract-syms bindings)]
      `(let ~bindings
         (~(symbol (str '-> grec)) ~@syms))))
  )

(defn- do-assoc-in
  [m ks v]
  (let [me {:tag 'clojure.lang.Associative}
        g (with-meta (gensym "m__") me)
        gs (repeatedly (count ks) #(with-meta (gensym) me))
        gs+ (list* g gs)
        bs
        (into
         [g m]
         (mapcat (fn [g- g k]
                   [g- `(get ~g ~k)])
                 (butlast gs)
                 gs+
                 ks))
        iter
        (fn iter
          [[sym & syms] [k & ks] v]
          (if ks
            `(assoc ~sym ~k ~(iter syms ks v))
            `(assoc ~sym ~k ~v)))]
    `(let ~bs
       ~(iter gs+ ks v))))

(defmacro inline-assoc-in
  [m ks v]
  {:pre [(simple-seq? ks)]}
  (do-assoc-in m (simple-seq ks) v))

(defn- do-update-in
  [m ks f args]
  (let [me {:tag 'clojure.lang.Associative}
        g (with-meta (gensym "m__") me)
        gs (repeatedly (count ks) #(with-meta (gensym) me))
        gs+ (list* g gs)
        bs
        (into
         [g m]
         (mapcat (fn [g- g k]
                   [g- `(get ~g ~k)])
                 gs
                 gs+
                 ks))
        iter
        (fn iter
          [[sym & syms] [k & ks]]
          (if ks
            `(assoc ~sym ~k ~(iter syms ks))
            `(assoc ~sym ~k (~f ~(first syms) ~@args))))]
    `(let ~bs
       ~(iter gs+ ks))))

(defmacro inline-update-in
  [m ks f & args]
  {:pre [(simple-seq? ks)]}
  (do-update-in m (simple-seq ks) f args))

(defn entry-at
  {:inline
   (fn [m k]
     `(.entryAt ~(with-meta m {:tag 'clojure.lang.IPersistentMap}) ~k))}
  [^clojure.lang.IPersistentMap m k]
  (.entryAt ^clojure.lang.IPersistentMap m k))

(defmacro find-some-in
  [m ks]
  (let [ks (simple-seq ks)
        sym (gensym "m__")
        steps
        (concat
         (map (fn [step] `(if (nil? ~sym) nil (when-let [e# (entry-at ~sym ~step)] (val e#))))
              (butlast ks))
         (when-let [k (last ks)]
           (list `(if (nil? ~sym) nil (entry-at ~sym ~k)))))]
    `(let [~sym ~m
           ~@(interleave (repeat sym) steps)]
       ~sym)))

(defmacro memoize-n
  [n f]
  (let [args (repeatedly n #(gensym))]
    `(let [mem# (atom {})]
       (fn [~@args]
         (if-let [e# (find-some-in @mem# ~args)]
           (val e#)
           (let [ret# (~f ~@args)]
             (swap! mem# (fn [m# v#] (inline-assoc-in m# [~@args] v#)) ret#)
             ret#))))))

(defmacro ^:private def-memoize*
  "Define a function which dispatches to the memoizing macro `memo`
  up to m, otherwise defaults to core/memoize"
  [name memo m]
  (let [cases
        (mapcat (fn [n] `(~n (~memo ~n ~'f))) (range m))
        ]
    `(defn ~name
       [~'n ~'f]
       (case ~'n
         ~@cases
         (memoize ~'f)))))

(declare memoize*)
(def-memoize* memoize* memoize-n 8)

(defn ->chm
  ([] (ConcurrentHashMap.)))

(defn chm-put!?
  {:inline
   (fn [m k v]
     `(do (.putIfAbsent ~(with-meta m {:tag 'ConcurrentHashMap}) ~k ~v)
          ~m))}
  [^ConcurrentHashMap m k v]
  (.putIfAbsent ^ConcurrentHashMap m k v) m)

(defn chm?
  {:inline
   (fn [m] `(instance? ConcurrentHashMap ~m))}
  [chm]
  (instance? ConcurrentHashMap chm))

(defn chm-get
  [m k]
  {:inline
   (fn [m k]
     `(.get ~(with-meta m {:tag 'ConcurrentHashMap}) ~k)
     m)}
  [^ConcurrentHashMap m k]
  (.get ^ConcurrentHashMap m k))

(defn chm-get?
  [m k]
  {:inline
   (fn [m k]
     `(when (chm? ~m)
        (.get ~(with-meta m {:tag 'ConcurrentHashMap}) ~k))
     m)}
  [m k]
  (when (chm? m)
    (.get ^ConcurrentHashMap m k)))

(defmacro chm-get-in?
  [m ks]
  (let [ks (simple-seq ks)
        sym (gensym "m__")
        steps
        (map (fn [step] `(if (nil? ~sym) nil (chm-get? ~sym ~step)))
             ks)]
    `(let [~sym ~m
           ~@(interleave (repeat sym) steps)]
       ~sym)))

(defmacro chm-put-in!
  [m ks v]
  (let [me {:tag 'ConcurrentHashMap}
        g (with-meta (gensym "m__") me)
        gs (repeatedly (count ks) #(with-meta (gensym) me))
        gs+ (list* g gs)
        bs
        (into
         [g m]
         (mapcat (fn [g- g k]
                   [g- `(or (chm-get? ~g ~k) (->chm))])
                 (butlast gs)
                 gs+
                 ks))
        iter
        (fn iter
          [[sym & syms] [k & ks] v]
          (if ks
            `(chm-put!? ~sym ~k ~(iter syms ks v))
            `(chm-put!? ~sym ~k ~v)))]
    `(let ~bs
       ~(iter gs+ ks v))))

(defmacro memoize-c
  [n f]
  (let [args (repeatedly n #(gensym))]
    `(let [mem# (->chm)]
       (fn [~@args]
         (if-let [e# (chm-get-in? mem# ~args)]
           e#
           (let [ret# (~f ~@args)]
             (chm-put-in! mem# [~@args] ret#)
             ret#))))))

(declare memoize-c*)
(def-memoize* memoize-c* memoize-c 8)

(comment

  (do
    (require '[clj-async-profiler.core :as prof])
    (require '[criterium.core :as cc])
    (prof/clear-results)
    (prof/serve-files 9090))

  (def f (memoize identity))

  (def f' (memoize* 1 identity))

  (f :a)

  (f' :a)

  (prof/profile (dotimes [_ 1e7] (f :a)))

  (prof/profile (dotimes [_ 1e7] (f' :a)))

  (cc/quick-bench (f :a))

  (cc/quick-bench (f' :a))

  (def g (memoize assoc))

  (def g' (memoize* 3 assoc))

  (g {} :a 1)

  (g' {} :a 1)

  (cc/quick-bench (g {} :a 1))

  (cc/quick-bench (g' {} :a 1))

  (prof/profile (dotimes [_ 1e7] (g {} :a 1)))

  (prof/profile (dotimes [_ 1e7] (g' {} :a 1)))


  (cc/quick-bench (g {:b 2} :a 1))

  (cc/quick-bench (g' {:b 2} :a 1))

  (prof/profile (dotimes [_ 1e7] (g {:b 2} :a 1)))

  (prof/profile (dotimes [_ 1e7] (g' {:b 2} :a 1)))


  (def h (memoize vector))

  (def h' (memoize* 3 vector))

  (cc/quick-bench (h :a :b :c))

  (cc/quick-bench (h' :a :b :c))

  (prof/profile (dotimes [_ 1e7] (h :a :b :c)))

  (prof/profile (dotimes [_ 1e7] (h' :a :b :c)))

  )
