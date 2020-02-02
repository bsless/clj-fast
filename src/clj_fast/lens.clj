(ns clj-fast.lens
  "Code transformation functions which abstract over the behaviors of inlineable
  functions"
  (:refer-clojure :exclude [get update])
  (:require
   [clj-fast.util :as u]))

(defn get
  [f m ks]
  (let [ks (u/simple-seq ks)
        chain#
        (map f ks)]
    `(-> ~m ~@chain#)))

(defn get-some
  "(f sym step) -> expr"
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
  [putter getter m ks v]
  (let [g (gensym "m__")
        gs (repeatedly (count ks) #(gensym))
        gs+ (list* g gs)
        bs
        (into
         [g m]
         (mapcat (fn [g- g k]
                   [g- (getter g k)])
                 (butlast gs)
                 gs+
                 ks))
        iter
        (fn iter
          [[sym & syms] [k & ks] v]
          (if ks
            (putter sym k (iter syms ks v))
            (putter sym k v)))]
    `(let ~bs
       ~(iter gs+ ks v))))

(defn update
  [putter getter m ks f args]
  (let [g (gensym "m__")
        gs (repeatedly (count ks) #(gensym))
        gs+ (list* g gs)
        bs
        (into
         [g m]
         (mapcat (fn [g- g k]
                   [g- (getter g k)])
                 gs
                 gs+
                 ks))
        iter
        (fn iter
          [[sym & syms] [k & ks]]
          (if ks
            (putter sym k (iter syms ks))
            (putter sym k `(~f ~(first syms) ~@args))))]
    `(let ~bs
       ~(iter gs+ ks))))
