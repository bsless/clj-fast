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
        {:keys [bindings syms]} (u/extract-bindings ks)
        gs (repeatedly (count ks) #(gensym))
        gs+ (list* g gs)
        bs
        (vec
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
    `(let [~g ~m ~@bindings ~@bs]
       ~(iter gs+ syms v))))

(defn update-many
  "Take three functions, putter, getter and updater, symbol m, sequence of
  pairs of [ks v] such that every ks is a sequence of keys and v is an
  expression and constructs an assoc-in structure, as if inlining core
  Clojure's assoc-in in a minimal number of calls.

  getter must be a mapping of
  (f sym k) -> get-expr, for example:
  (fn [sym k] `(get sym k))

  similarly, putter must do the same with assoc.

  updater is a mapping of
  (f old-val new-val) -> new-expr, for example
  (fn [f v] `(apply ~f ~v))"
  [putter getter updater m kvs]
  (assert (even? (count kvs)))
  (letfn [(collapse ;; plan
            [kvs]
            (reduce
             (fn [m [path v]]
               (assoc-in m (map (fn [k] {::node k}) path) {::leaf v}))
             {}
             kvs))
          (explode ;; translate plan to execution
            [m form]
            (let [parent (gensym "parent__")
                  bindings
                  (reduce
                   (fn [bs [k v]]
                     (if (::leaf v)
                       (conj bs parent
                             (putter parent (::node k)
                                     (updater (::leaf v) (getter parent (::node k)))))
                       (let [child (gensym "child__")]
                         (conj bs
                               child (getter parent (::node k))
                               parent (putter parent (::node k) (explode child v))))))
                   [parent m]
                   form)]
              `(let [~@bindings]
                 ~parent)))]
    (let [{:keys [context kvs]}
          (reduce
           (fn [{:keys [context kvs]} [path v]]
             (let [{:keys [bindings syms]} (u/extract-bindings path)]
               {:context (into context bindings)
                :kvs (conj kvs [syms v])}))
           {:context []
            :kvs []}
           (partition 2 kvs))]
      (if (seq context)
        `(let [~@context]
           ~(explode m (collapse kvs)))
        (explode m (collapse kvs))))))

(defn put-many
  "like `update-many` but overrides the old key values."
  [putter getter m kvs]
  (update-many putter getter (fn [v _] v) m kvs))

(comment
  (update-many
   (fn [m k fv] `(assoc ~m ~k ~fv))
   (fn [m k] `(get ~m ~k))
   (fn [f v] `(apply ~f ~v))
   'm
   '[[:a :b] max
     [:a :c] min])

  (put-many
   (fn [m k v] `(assoc ~m ~k ~v))
   (fn [m k] `(get ~m ~k))
   'm
   '[[:a (rand) :b] 1
     [:a (rand) :c] 2]))

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
        {:keys [bindings syms]} (u/extract-bindings ks)
        gs (repeatedly (count ks) #(gensym))
        gs+ (list* g gs)
        bs
        (vec
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
    `(let [~g ~m ~@bindings ~@bs]
       ~(iter gs+ syms))))

(defn without
  [putter getter remover m ks]
  (if (= 1 (count ks))
    (remover m (first ks))
    (let [k' (last ks)
          ks (butlast ks)
          g (gensym "m__")
          ks (u/simple-seq ks)
          {:keys [bindings syms]} (u/extract-bindings ks)
          gs (repeatedly (count ks) #(gensym))
          gs+ (list* g gs)
          bs
          (vec
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
              (putter sym k (remover (first syms) k'))))]
      `(let [~g ~m ~@bindings ~@bs]
         ~(iter gs+ syms)))))
