(ns clj-fast.util)

(defn simple?
  [x]
  (or (keyword? x) (symbol? x) (string? x) (int? x)))

(defn lazy?
  [xs]
  (instance? clojure.lang.LazySeq xs))

(def sequence? (some-fn lazy? vector? list?))

(defn try-resolve
  [sym]
  (when (symbol? sym)
    (when-let [r (resolve sym)]
      (deref r))))

(defn simple-seq?
  [xs]
  (let [xs (or (try-resolve xs) xs)]
    (and (sequence? xs) (every? simple? xs))))

(defn simple-seq
  [xs]
  (let [xs (or (try-resolve xs) xs)]
    (and (sequence? xs) (every? simple? xs) (seq xs))))

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
  (map first (partition 2 (drop 2 bs))))
