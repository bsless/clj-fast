(ns clj-fast.util
  (:require
   [clojure.string]))

(defn eq?
  {:inline
   (fn [o1 o2]
     `(.equals ~(with-meta o1 {:tag 'java.lang.Object}) ~o2))}
  [^Object o1 o2]
  (.equals o1 o2))

(defn lazy?
  [xs]
  (instance? clojure.lang.LazySeq xs))

(defn- quoted-coll?
  [xs]
  (and (coll? xs)
       (= 'quote (first xs))))

(def sequence? (some-fn lazy? vector? quoted-coll?))

(defn try-resolve
  [sym]
  (when (symbol? sym)
    (when-let [r (resolve sym)]
      (deref r))))

(defn try-resolve?
  [sym]
  (or (try-resolve sym) sym))

(defn simple-seq?
  [xs]
  (let [xs (try-resolve? xs)]
    (sequence? xs)))

(defn- dequote
  [xs]
  (if (quoted-coll? xs)
    (second xs)
    xs))

(defn simple-seq
  [xs]
  (let [xs (try-resolve? xs)]
    (when (sequential? xs) (into [] (seq (dequote xs))))))

(defn bind-seq
  [xs]
  (vec (mapcat list (repeatedly gensym) xs)))

(defn any-expression?
  "True if anything about `e` looks like a call and not literal
  expressions."
  [e]
  (cond
    (list? e) true
    (coll? e) (boolean (some any-expression? e))
    :else false))

(comment
  (any-expression? [1 2 3])
  (any-expression? '[1 (inc 2) 3])
  (any-expression? '[1 [[[[[[(inc 2)]]]]]] 3])
  (any-expression? '[1 [[[[[[2]]]]]] 3])
  (any-expression? '[1 {:a 2} 3])
  (any-expression? '[1 {:a (inc 2)} 3]))

(defn extract-bindings
  "Analyzes in input sequences of code, xs, and extracts any collection
  out of it to be replaced by a gensym and its respective binding."
  [xs]
  (loop [xs xs
         bindings []
         syms []]
    (if xs
      (let [x (first xs)]
        (if (coll? x)
          (let [sym (gensym)]
            (recur (next xs) (conj bindings sym x) (conj syms sym)))
          (recur (next xs) bindings (conj syms x))))
      {:bindings bindings :syms syms})))

(defn destruct-map
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

(defn extract-syms
  [bs]
  (map first (partition 2 bs)))

(defn memoize0
  [f]
  (let [sentinel (new Object)
        mem (atom sentinel)]
    (fn []
      (let [e @mem]
        (if (= e sentinel)
          (let [ret (f)]
            (reset! mem ret)
            ret)
          e)))))

(defn record-fields*
  [clazz]
  (into
   #{}
   (comp
    (map #(.getName %))
    (remove #(clojure.string/starts-with? % "__"))
    (remove #(clojure.string/starts-with? % "const__"))
    (map keyword))
   (.getFields clazz)))

(def record-fields (memoize record-fields*))
