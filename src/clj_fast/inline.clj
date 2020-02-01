(ns clj-fast.inline
  (:refer-clojure :exclude [merge get-in assoc-in update-in select-keys])
  (:require
   [clojure.core :as c]
   [clj-fast.util :as u]
   [clj-fast.core :as f]
   [clj-fast.collections.concurrent-hash-map :as chm]))

(defmacro fast-assoc*
  [m & kvs]
  {:pre [(even? (count kvs))]}
  (let [chain#
        (map (fn [[k v]] `(f/fast-assoc ~k ~v)) (partition 2 kvs))]
    `(-> ~m ~@chain#)))

(defmacro merge
  [& [m & ms]]
  (let [conjs# (map (fn [m] `(conj ~m)) ms)]
    `(-> (or ~m {})
         ~@conjs#)))

(defmacro fast-map-merge
  [& [m & ms]]
  (let [conjs# (map (fn [m] `(f/fast-map-merge ~m)) ms)]
    `(-> (or ~m {})
         ~@conjs#)))

(defmacro tmerge
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
  [m ks]
  {:pre [(u/simple-seq? ks)]}
  (let [ks (u/simple-seq ks)
        chain#
        (map (fn [k] `(c/get ~k)) ks)]
    `(-> ~m ~@chain#)))

(defmacro get-some-in
  [m ks]
  {:pre [(u/simple-seq? ks)]}
  (let [ks (u/simple-seq ks)
        sym (gensym "m__")
        steps
        (map (fn [step] `(if (nil? ~sym) nil (~sym ~step)))
             ks)]
    `(let [~sym ~m
           ~@(interleave (repeat sym) steps)]
       ~sym)))

(defmacro select-keys
  "Like `select-keys` but faster and uses code generation.
  `ks` must be either vector, list or set."
  [m ks]
  {:pre [(u/simple-seq? ks)]}
  (let [ks (u/simple-seq ks)
        bindings (u/destruct-map m ks)
        syms (u/extract-syms bindings)
        form (apply hash-map (interleave ks syms))]
    `(let ~bindings
       ~form)))

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

(defmacro assoc-in
  [m ks v]
  {:pre [(u/simple-seq? ks)]}
  (do-assoc-in m (u/simple-seq ks) v))

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

(defmacro update-in
  [m ks f & args]
  {:pre [(u/simple-seq? ks)]}
  (do-update-in m (u/simple-seq ks) f args))

(defmacro find-some-in
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

(defmacro memoize-n
  [n f]
  (let [args (repeatedly n #(gensym))]
    `(let [mem# (atom {})]
       (fn [~@args]
         (if-let [e# (find-some-in @mem# ~args)]
           (val e#)
           (let [ret# (~f ~@args)]
             (swap! mem# (fn [m# v#] (assoc-in m# [~@args] v#)) ret#)
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

(declare memoize-c*)
(def-memoize* memoize-c* chm/memoize-c 8)
