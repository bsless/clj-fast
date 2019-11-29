(ns clj-fast.core
  (:import
   (java.util HashMap Map)))

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

(defn- simple?
  [x]
  (or (keyword? x) (symbol? x) (string? x) (int? x)))

(defn- sequence?
  [xs]
  (or (vector? xs) (list? xs) (set? xs)))

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


(defmacro fast-get-in-th
  [m ks]
  {:pre [(vector? ks)]}
  `(-> ~m ~@ks))

(defmacro fast-get-in-inline
  "Like `get-in` but faster and uses code generation.
  `ks` must be either vector, list or set."
  [m ks]
  {:pre [(simple-seq? ks)]}
  (let [ks (simple-seq ks)
        chain#
        (map (fn [k] `(get ~k)) ks)]
    `(-> ~m ~@chain#)))

(defn- destruct-map
  [m ks]
  (let [gmap (gensym "map__")
        syms (map (comp gensym symbol) ks)]
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

(defmacro fast-select-keys-inline
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
