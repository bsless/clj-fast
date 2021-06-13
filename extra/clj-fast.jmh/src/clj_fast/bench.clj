(ns clj-fast.bench
  (:require
   [clojure.set]
   [clojure.string]
   [clojure.java.io]
   [clj-fast.inline :as inline]
   [clj-fast.collections.map :as hm]
   [clj-fast.collections.concurrent-map :as cm]
   [clojure.spec.alpha :as s]
   [clojure.test.check.generators]
   [clojure.spec.gen.alpha :as gen])
  (:gen-class))

;;; utilities
(def mrange (memoize range))

(defn build-map
  [width]
  (zipmap (mrange width) (mrange width)))

(def mbuild-map (memoize build-map))

(declare mbuild-nested-map)

(defn build-nested-map
  [width depth]
  (if (= 1 depth)
    (mbuild-map width)
    (zipmap (range width)
            (map (fn [_] (mbuild-nested-map width (dec depth))) (range width)))))

(def mbuild-nested-map (memoize build-nested-map))

(def mean (comp first :mean))

(defrecord Foo [a b c d])

(defn vmap
  [f m]
  (reduce-kv (fn [m k v] (assoc m k (f v))) {} m))

(defonce -s "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ")
(defonce -strs (for [a -s b -s c -s d -s e -s] (str a b c d e)))
(defonce -kws (map keyword -strs))
(defonce -syms (map symbol -strs))
(defonce r (range))

(defonce gens
  {:keyword? (fn [n] (take n -kws))
   :map? (fn [n] (drop 1 (gen/sample (s/gen map?) (inc n))))
   :string? (fn [n] (take n -strs))
   :int? (fn [n] (take n r))
   :integer? (fn [n] (take n r))
   :symbol? (fn [n] (take n -syms))})

#_(defn genn!
  [n spec]
  (drop 1 (gen/sample- (s/gen spec) (inc n))))

(defn genn!
  [n spec]
  (sequence
   (comp
    (drop 1)
    (distinct)
    (take n))
   (clojure.test.check.generators/sample-seq (s/gen spec))))

(defn genn
  [n p]
  ((gens p) n))

(defn randmap!
  ([n]
   (randmap! keyword? n))
  ([p n]
   (let [s (genn! n p)]
     (zipmap s s))))

(defn randmaps!
  [n p size]
  (repeatedly n (fn [] (clj-fast.bench/randmap! p size))))

(defn randmap
  #_([n]
   (randmap keyword? n))
  ([p n]
   (let [coll (genn n p)]
     (zipmap coll coll))))

(defonce mrandmap (memoize randmap))

(declare mrand-nested-map)
(defn rand-nested-map
  [p width depth]
  (if (= 1 depth)
    (mrandmap p width)
    (zipmap (genn width p)
            (repeat width (mrand-nested-map p width (dec depth))))))
#_(defn rand-nested-map
  [p width depth]
  (if (= 1 depth)
    (mrandmap p width)
    (zipmap (genn width p)
            (repeat width (mrand-nested-map p width (dec depth))))))

(defonce mrand-nested-map (memoize rand-nested-map))

(def preds identity)
(def preds!
  {:int? int?
   :keyword? keyword?
   :string? string?
   :map? map?
   :symbol? symbol?})

(defn randkey
  [m]
  (rand-nth (keys m)))

(defn randpath
  [mm]
  (letfn
      [(iter [m ks]
         (if (map? m)
           (let [k (randkey m)
                 v (m k)]
             (iter v (conj ks k)))
           ks))]
    (iter mm [])))

;;; Benches

(defmacro def-inline-assoc
  [name sym & more]
  (let [n 6
        g (gensym "m")
        ks (mapv (fn [n] (symbol (str "k" n))) (range n))
        args (conj ks :as 'ks)
        cases (apply concat (drop 1 (map-indexed (fn [i ks] [i `(fn [~g] (~sym ~g ~@(interleave ks (range)) ~@more))]) (reductions conj [] ks))))]
    `(defn ~name
       [~args]
       (case (count ~'ks)
         ~@cases))))

;;; ASSOC

(declare -assoc inline-assoc inline-fast-assoc)
(def-inline-assoc -assoc assoc)
(def-inline-assoc inline-assoc inline/assoc)
(def-inline-assoc inline-fast-assoc inline/fast-assoc)

;;; MERGE

(defmacro def-inline-merge
  [name sym & more]
  (let [n 6
        ks (mapv (fn [n] (symbol (str "m" n))) (range n))
        args (conj ks :as 'args)
        cases (apply concat (drop 1 (map-indexed (fn [i ks] [i `(fn [] (~sym ~@ks ~@more))]) (reductions conj [] ks))))]
    `(defn ~name
       [~args]
       (case (count ~'args)
         ~@cases))))

(declare -merge inline-merge inline-fast-map-merge inline-tmerge)
(def-inline-merge -merge merge)
(def-inline-merge inline-merge inline/merge)
(def-inline-merge inline-fast-map-merge inline/fast-map-merge)
(def-inline-merge inline-tmerge inline/tmerge)

;;; GET-IN

(defmacro def-inline-ks
  [name sym & more]
  (let [n 6
        g (gensym)
        ks (mapv (fn [n] (symbol (str "k" n))) (range n))
        args (conj ks :as 'ks)
        cases (apply concat (drop 1 (map-indexed (fn [i ks] [i `(fn [~g] (~sym ~g [~@ks] ~@more))]) (reductions conj [] ks))))]
    `(defn ~name
       [~args]
       (case (count ~'ks)
         ~@cases))))

(defn invoke
  ([f] (f))
  ([f a] (f a))
  ([f a b] (f a b)))

(declare inline-get-in-fn inline-get-some-in-fn)
(def-inline-ks inline-get-in-fn inline/get-in)
(def-inline-ks inline-get-some-in-fn inline/get-some-in)

;;; SELECT-KEYS

(declare inline-select-keys inline-fast-select-keys)
(def-inline-ks inline-select-keys inline/select-keys)
(def-inline-ks inline-fast-select-keys inline/fast-select-keys)

;;; ASSOC-IN

(declare inline-assoc-in)
(def-inline-ks inline-assoc-in inline/assoc-in 0)

;;; UPDATE-IN

(declare inline-update-in)
(def-inline-ks inline-update-in inline/update-in identity)

;;; memoize

(defn noop
  "Why 7?"
  ([] 7)
  ([_] 7)
  ([_ _] 7)
  ([_ _ _] 7)
  ([_ _ _ _] 7)
  ([_ _ _ _ _] 7)
  ([_ _ _ _ _ _] 7))

(defmacro def-inline-memoize-fn
  [name sym]
  (let [n 6
        ks (mapv (fn [n] (symbol (str "m" n))) (range n))
        args (conj ks :as 'args)
        cases (apply concat (drop 1 (map-indexed (fn [i ks] [i `(let [f# (~sym ~i noop)] (fn [] (f# ~@ks)))]) (reductions conj [] ks))))]
    `(defn ~name
       [~args]
       (case (count ~'args)
         ~@cases))))

(defmacro def--memoize-fn
  [name sym]
  (let [n 6
        ks (mapv (fn [n] (symbol (str "m" n))) (range n))
        args (conj ks :as 'args)
        cases (apply concat (drop 1 (map-indexed (fn [i ks] [i `(let [f# (~sym noop)] (fn [] (f# ~@ks)))]) (reductions conj [] ks))))]
    `(defn ~name
       [~args]
       (case (count ~'args)
         ~@cases))))

(declare -memoize cm-memoize hm-memoize)
(def--memoize-fn -memoize memoize)
(def--memoize-fn cm-memoize cm/memoize)
(def--memoize-fn hm-memoize hm/memoize)

(declare memoize-n memoize-h memoize-c)
(def-inline-memoize-fn memoize-n inline/memoize*)
(def-inline-memoize-fn memoize-c inline/memoize-c*)
(def-inline-memoize-fn memoize-h inline/memoize-h*)
