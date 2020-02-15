(ns clj-fast.util)

(defn lazy?
  [xs]
  (instance? clojure.lang.LazySeq xs))

(def sequence? (some-fn lazy? sequential?))

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

(defn simple-seq
  [xs]
  (let [xs (try-resolve? xs)]
    (and (sequence? xs) (seq xs))))

(defn bind-seq
  [xs]
  (vec (mapcat list (repeatedly gensym) xs)))

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
