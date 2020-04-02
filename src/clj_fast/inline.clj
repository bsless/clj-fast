(ns clj-fast.inline
  (:refer-clojure
   :exclude [assoc merge get-in assoc-in update-in select-keys])
  (:require
   [clojure.core :as c]
   [clj-fast.util :as u]
   [clj-fast.core :as f]
   [clj-fast.lens :as lens]
   [clj-fast.collections.concurrent-map :as cm]
   [clj-fast.collections.map :as hm]))

(defmacro assoc
  "Like core/assoc but inlines the association to all the arguments."
  [m & kvs]
  {:pre [(sequential? kvs) (even? (count kvs))]}
  (let [chain#
        (map (fn [[k v]] `(c/assoc ~k ~v)) (partition 2 kvs))]
    `(-> ~m ~@chain#)))

(defmacro fast-assoc
  "Like assoc but uses fast-assoc instead."
  [m & kvs]
  {:pre [(sequential? kvs) (even? (count kvs))]}
  (let [chain#
        (map (fn [[k v]] `(f/fast-assoc ~k ~v)) (partition 2 kvs))]
    `(-> ~m ~@chain#)))

(defmacro merge
  "Like core/merge but inlines the sequence of maps to conj."
  [& [m & ms]]
  (let [conjs# (map (fn [m] `(conj ~m)) ms)]
    `(-> (or ~m {})
         ~@conjs#)))

(defmacro fast-map-merge
  "Like merge but uses fast-map-merge instead."
  [& [m & ms]]
  (let [conjs# (map (fn [m] `(f/fast-map-merge ~m)) ms)]
    `(-> (or ~m {})
         ~@conjs#)))

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
  [m ks]
  {:pre [(u/simple-seq? ks)]}
  (lens/get (fn [k] `(c/get ~k)) m ks))

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

(defmacro assoc-in
  "Like assoc-in but inlines the calls when a static sequence of keys is
  provided."
  [m ks v]
  {:pre [(u/simple-seq? ks)]}
  (lens/put
   (fn [m k v] `(c/assoc ~m ~k ~v))
   (fn [m k] `(c/get ~m ~k))
   m (u/simple-seq ks) v))

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
        (case ~'n
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
  hm/memoize* 20)
