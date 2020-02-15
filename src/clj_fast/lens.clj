(ns clj-fast.lens
  "Code transformation functions which abstract over the behaviors of inlineable
  functions"
  (:refer-clojure :exclude [get update])
  (:require
   [clj-fast.util :as u]))

(defn get
  "Takes a function f, symbol m and sequence ks and constructs a nested
  get structure.
  f must be a mapping of
  (f sym k) -> get-expr, for example:
  (fn [sym k] `(get sym k))"
  [f m ks]
  (let [ks (u/simple-seq ks)
        chain#
        (map f ks)]
    `(-> ~m ~@chain#)))

(defn get-some
  "Takes a function f, symbol m and sequence ks and constructs a linear
  get structure, as in some->.
  f must be a mapping of
  (f sym k) -> get-expr, for example:
  (fn [sym k] `(get sym k))"
  [f m ks]
  (let [ks (u/simple-seq ks)
        sym (gensym "m__")
        steps
        (map (fn [step] `(if (nil? ~sym) nil ~(f sym step)))
             ks)]
    `(let [~sym ~m
           ~@(interleave (repeat sym) steps)]
       ~sym)))

(defn put
  "Take two functions, putter and getter, symbol m, sequence ks and
  symbol v and constructs an assoc-in structure, as if inlining
  core Clojure's assoc-in.
  getter must be a mapping of
  (f sym k) -> get-expr, for example:
  (fn [sym k] `(get sym k))

  similarly, putter must to the same with assoc."
  [putter getter m ks v]
  (let [g (gensym "m__")
        ks (u/simple-seq ks)
        bindings (u/bind-seq ks)
        syms (u/extract-syms bindings)
        gs (repeatedly (count ks) #(gensym))
        gs+ (list* g gs)
        bs
        (into
         [g m]
         (mapcat (fn [g- g k]
                   [g- (getter g k)])
                 (butlast gs)
                 gs+
                 syms))
        iter
        (fn iter
          [[sym & syms] [k & ks] v]
          (if ks
            (putter sym k (iter syms ks v))
            (putter sym k v)))]
    `(let [~@bindings ~@bs]
       ~(iter gs+ syms v))))

(defn update
  "Take two functions, putter and getter, symbol m, sequence ks and
  symbol v and constructs an update-in structure, as if inlining
  core Clojure's update-in.
  getter must be a mapping of
  (f sym k) -> get-expr, for example:
  (fn [sym k] `(get sym k))

  similarly, putter must to the same with assoc."
  [putter getter m ks f args]
  (let [g (gensym "m__")
        ks (u/simple-seq ks)
        bindings (u/bind-seq ks)
        syms (u/extract-syms bindings)
        gs (repeatedly (count ks) #(gensym))
        gs+ (list* g gs)
        bs
        (into
         [g m]
         (mapcat (fn [g- g k]
                   [g- (getter g k)])
                 gs
                 gs+
                 syms))
        iter
        (fn iter
          [[sym & syms] [k & ks]]
          (if ks
            (putter sym k (iter syms ks))
            (putter sym k `(~f ~(first syms) ~@args))))]
    `(let [~@bindings ~@bs]
       ~(iter gs+ syms))))
