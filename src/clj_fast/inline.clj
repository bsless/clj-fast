(ns clj-fast.inline
  (:refer-clojure
   :exclude [get nth assoc merge get-in assoc-in update-in select-keys dissoc])
  (:require
   [clojure.core :as c]
   [clj-fast.util :as u]
   [clj-fast.core :as f]
   [clj-fast.lens :as lens]
   [clj-fast.collections.concurrent-map :as cm]
   [clj-fast.collections.map :as m]))

(defn -get
  [m k & nf]
  (if-let [t (:tag (meta m))]
    (let [c (when-let [c (resolve t)] c)]
      (if (and c (.isAssignableFrom clojure.lang.IRecord c))
        (let [fields (u/record-fields c)]
          (if (and (nil? (first nf)) (fields k))
            `(. ~m ~(symbol k))
            `(.valAt ~m ~k ~@nf)))
        (case t
          (IPersistentMap
           clojure.lang.IPersistentMap
           PersistentArrayMap
           clojure.lang.PersistentArrayMap
           PersistentHashMap
           clojure.lang.PersistentHashMap)
          `(f/val-at ~m ~k ~@nf)
          (PersistentVector clojure.lang.PersistentVector clojure.lang.Indexed Indexed)
          `(.nth ~(with-meta m {:tag clojure.lang.PersistentVector}) ~k ~@nf)
          (Map HashMap java.util.Map java.util.HashMap)
          `(m/get ~m ~k ~@nf)
          (let [k (or (u/try-resolve k) k)]
            (if (keyword? k)
              `(~k ~m ~@nf)
              `(clojure.lang.RT/get ~m ~k ~@nf))))))
    (let [k (or (u/try-resolve k) k)]
      (if (keyword? k)
        `(~k ~m ~@nf)
        `(clojure.lang.RT/get ~m ~k ~@nf)))))

(defmacro get
  ([m k]
   (-get m k))
  ([m k nf]
   (-get m k nf)))

(defn -nth2
  [c i]
  (let [t (:tag (meta c))]
    (case t
      (java.lang.CharSequence java.lang.String String CharSequence)
      `(.charAt ~c ~i)
      (booleans bytes chars doubles floats ints longs shorts)
      `(aget ~c ~i)
      (clojure.lang.Indexed Indexed clojure.lang.PersistentVector PersistentVector)
      `(.nth ~(with-meta c {:tag 'clojure.lang.Indexed}) ~i)
      (if (try (.isArray (Class/forName t)) (catch Throwable _))
        `(aget ~c ~i)
        `(clojure.lang.RT/nth ~c ~i)))))

(defn- emit-array-nth
  [c i i' nf]
  (let [arr (with-meta (gensym "arr__") (meta c))]
    `(let [~i' ~i
           ~arr ~c]
       (if (< ~i' (alength ~arr))
         (aget ~arr ~i')
         ~nf))))

(defn -nth3
  [c i nf]
  (let [t (:tag (meta c))
        i' (gensym "i__")]
    (case t
      (java.lang.CharSequence java.lang.String String CharSequence)
      (let [cs (with-meta (gensym "cs__") (meta c))]
        `(let [~i' ~i
               ~cs ~c]
           (if (< ~i' (.length ~cs))
             (.charAt ~cs ~i')
             ~nf)))
      (booleans bytes chars doubles floats ints longs shorts)
      (emit-array-nth c i i' nf)
      (clojure.lang.Indexed Indexed clojure.lang.PersistentVector PersistentVector)
      `(.nth ~(with-meta c {:tag 'clojure.lang.Indexed}) ~i ~nf)
      (if (try (.isArray (Class/forName t)) (catch Throwable _))
       (emit-array-nth c i i' nf)
        `(clojure.lang.RT/nth ~c ~i ~nf)))))

(defmacro nth
  ([c i]
   (-nth2 c i))
  ([c i nf]
   (-nth3 c i nf)))

(defmacro assoc
  "Like core/assoc but inlines the association to all the arguments."
  [m & kvs]
  {:pre [(sequential? kvs) (even? (count kvs))]}
  (let [chain#
        (map (fn [[k v]] `(c/assoc ~k ~v)) (partition 2 kvs))]
    `(-> ~m ~@chain#)))

(defmacro dissoc
  "Like core/assoc but inlines the association to all the arguments."
  [m & ks]
  {:pre [(sequential? ks)]}
  (let [chain#
        (map (fn [k] `(c/dissoc ~k)) ks)]
    `(-> ~m ~@chain#)))

(defmacro fast-assoc
  "Like assoc but uses fast-assoc instead."
  [m & kvs]
  {:pre [(sequential? kvs) (even? (count kvs))]}
  (let [chain#
        (map (fn [[k v]] `(f/fast-assoc ~k ~v)) (partition 2 kvs))]
    `(-> ~m ~@chain#)))

;;; Credit joinr for the idea, see #6.
(defn- static-merge
  ([m]
   (static-merge m `conj))
  ([m op]
   (if (map? m)
     (let [args (apply concat m)]
       `(assoc ~@args))
     `(~op ~m))))

(defmacro merge
  "Like core/merge but inlines the sequence of maps to conj."
  [& [m & ms]]
  (let [ops# (map static-merge ms)
        m0 (if (map? m) m `(or ~m {}))]
    `(-> ~m0 ~@ops#)))

(defmacro fast-map-merge
  "Like merge but uses fast-map-merge instead."
  [& [m & ms]]
  (let [ops# (map (fn [m] (static-merge m `f/fast-map-merge)) ms)
        m0 (if (map? m) m `(or ~m {}))]
    `(-> ~m0 ~@ops#)))

(defmacro tmerge
  "Like merge but uses rmerge! and an intermediate transient map."
  ([] {})
  ([m] m)
  ([m1 m2 & ms]
   (let [ms (list* m1 m2 ms)
         end (last ms)
         ms (reverse (butlast ms))
         ops (map (fn [m] `(f/rmerge! ~m)) ms)]
     `(->> (transient ~end) ~@ops persistent!))))

(defmacro get-in
  "Like `get-in` but faster and uses code generation.
  `ks` must be either vector, list or set."
  ([m ks]
   {:pre [(u/simple-seq? ks)]}
   (lens/get (fn [k] `(c/get ~k)) m ks))
  ([m ks nf]
   {:pre [(u/simple-seq? ks)]}
   (let [g (gensym)]
     `(let [~g ~nf]
        ~(lens/get (fn [k] `(c/get ~k ~g)) m ks)))))

(defmacro get-some-in
  "Like get-in, but nil-checks every intermediate value."
  [m ks]
  {:pre [(u/simple-seq? ks)]}
  (lens/get-some (fn [m k] `(~m ~k))
   m ks))

(defmacro select-keys
  "Returns a map containing only those entries in map whose key is in keys"
  [m ks]
  (let [g (gensym "m__")
        gs (repeatedly (count ks) (partial gensym "f__"))
        bs (reduce (fn [bs [sym k]]
                     (conj bs sym `(find ~g ~k)))
                   [g m]
                   (map list gs ks))
        pairs (mapcat (fn [g] `(~g (conj ~g))) gs)]
    `(let ~bs
       (cond-> {}
         ~@pairs))))

(defmacro fast-select-keys
  "Like `select-keys` but faster and uses code generation.
  `ks` must be either vector, list or set."
  [m ks]
  {:pre [(u/simple-seq? ks)]}
  (let [ks (u/simple-seq ks)
        bindings (u/destruct-map m ks)
        syms (u/extract-syms (drop 2 bindings))
        form (apply hash-map (interleave ks syms))]
    `(let ~bindings
       ~form)))

#_
(defmacro assoc-in
  "Like assoc-in but inlines the calls when a static sequence of keys is
  provided."
  [m ks v]
  {:pre [(u/simple-seq? ks)]}
  (lens/put
   (fn [m k v] `(c/assoc ~m ~k ~v))
   (fn [m k] `(c/get ~m ~k))
   m (u/simple-seq ks) v))

(defmacro assoc-in
  "Like assoc-in but inlines the calls when a static sequence of keys is
  provided.
  Can take an unlimited number of [ks v] pairs.
  Caution:
  For more than one path-value pair this macro will reorder code."
  [m & ksvs]
  {:pre [(every? u/simple-seq? (take-nth 2 ksvs))]}
  (lens/put-many
   (fn [m k v] `(c/assoc ~m ~k ~v))
   (fn [m k] `(c/get ~m ~k))
   m ksvs))

(defmacro update-in
  "Like update-in but inlines the calls when a static sequence of keys is
  provided."
  [m ks f & args]
  {:pre [(u/simple-seq? ks)]}
  (lens/update
   (fn [m k v] `(c/assoc ~m ~k ~v))
   (fn [m k] `(c/get ~m ~k))
   m (u/simple-seq ks) f args))

(defmacro dissoc-in
  "Like update-in but inlines the calls when a static sequence of keys is
  provided."
  [m ks]
  {:pre [(u/simple-seq? ks)]}
  (lens/without
   (fn [m k v] `(c/assoc ~m ~k ~v))
   (fn [m k] `(c/get ~m ~k))
   (fn [m k] `(c/dissoc ~m ~k))
   m (u/simple-seq ks)))

(defmacro find-some-in
  "Like get-some-in but returns a map-entry in the end."
  [m ks]
  (let [ks (u/simple-seq ks)
        sym (gensym "m__")
        steps
        (concat
         (map (fn [step]
                `(if (nil? ~sym)
                   nil
                   (when-let [e# (f/entry-at ~sym ~step)]
                     (val e#))))
              (butlast ks))
         (when-let [k (last ks)]
           (list `(if (nil? ~sym) nil (f/entry-at ~sym ~k)))))]
    `(let [~sym ~m
           ~@(interleave (repeat sym) steps)]
       ~sym)))

(defmacro ^:private memoize-n
  [n f]
  (if (zero? n)
    `(u/memoize0 ~f)
    (let [args (repeatedly n #(gensym))]
      `(let [mem# (atom {})]
         (fn [~@args]
           (if-let [e# (find-some-in @mem# ~args)]
             (val e#)
             (let [ret# (~f ~@args)]
               (swap! mem# (fn [m# v#] (assoc-in m# [~@args] v#)) ret#)
               ret#)))))))

(defmacro ^:private def-memoize*
  "Define a function which dispatches to the memoizing macro `memo`
  up to m, otherwise defaults to core/memoize"
  ([name memo m]
   (let [doc (str "Memoize using " memo " up to " m
                  " otherwise use core/memoize")]
     `(def-memoize* ~name ~doc ~memo ~m)))
  ([name doc memo m]
   (let [cases
         (mapcat (fn [n] `(~n (~memo ~n ~'f))) (range m))
         ]
     `(defn ~name
        ~doc
        [~'n ~'f]
        (case (int ~'n)
          ~@cases
          (memoize ~'f))))))

(declare memoize*)
(def-memoize* memoize*
  "Memoize using memoize-n functions of up to 8 arguments. Falls back on
  core/memoize. Faster for keyword and symbols arguments than core/memoize."
  memoize-n 20)

(declare memoize-c*)
(def-memoize* memoize-c*
  "Memoize using memoize-c functions of up to 8 arguments. Falls back on
  core/memoize. Faster than core memoize. Uses a concurrent-hash-map."
  cm/memoize* 20)

(declare memoize-h*)
(def-memoize* memoize-h*
  "Memoize using memoize-c functions of up to 8 arguments. Falls back on
  core/memoize. Faster than core memoize. Uses a concurrent-hash-map."
  m/memoize* 20)
