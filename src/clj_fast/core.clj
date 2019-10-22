(ns clj-fast.core
  (:import
   (java.util HashMap Map)))

;;; Credit Metosin
;;; https://github.com/metosin/reitit/blob/0bcfda755f139d14cf4eff37e2b294f573215213/modules/reitit-core/src/reitit/impl.cljc#L136
(defn fast-assoc
  [^clojure.lang.Associative a k v]
  (.assoc a k v))

(defmacro fast-assoc*
  [m & kvs]
  {:pre [(even? (count kvs))]}
  (let [chain#
        (map (fn [[k v]] `(fast-assoc ~k ~v)) (partition 2 kvs))]
    `(-> ~m ~@chain#)))

;;; Credit Metosin
;;; https://github.com/metosin/reitit/blob/0bcfda755f139d14cf4eff37e2b294f573215213/modules/reitit-core/src/reitit/impl.cljc#L136
(defn fast-map [m]
  (let [m (or m {})]
    (HashMap. ^Map m)))

;;; Credit Metosin
(defn fast-get
  [^HashMap m k]
  (.get m k))


;;; Credit Metosin
;;; https://github.com/metosin/compojure-api/blob/master/src/compojure/api/common.clj#L46
(defn fast-map-merge
  [x y]
  (reduce-kv
   (fn [m k v]
     (fast-assoc m k v))
   x
   y))

(defmacro fast-get-in-th
  [m ks]
  {:pre [(vector? ks)]}
  `(-> ~m ~@ks))

(defmacro fast-get-in-inline
  "Like `get-in` but faster and uses code generation.
  Must accept vector as `ks`"
  [m ks]
  {:pre [(vector? ks)]}
  (let [chain#
        (map (fn [k] `(get ~k)) ks)]
    `(-> ~m ~@chain#)))


(defmacro fast-select-keys-inline
  "Like `select-keys` but faster and uses code generation.
  Must accept vector as `ks`"
  [m ks]
  {:pre [(vector? ks)]}
  (let [target (gensym "m__")
        syms (map (comp gensym symbol) ks)
        bindings (vec
                  (concat `(~target ~m)
                          (mapcat (fn [sym k] `(~sym (get ~target ~k))) syms ks)))
        final-form (interleave ks syms)
        hm-form (apply hash-map final-form)]
    `(let ~bindings
       ~hm-form)))

(defmacro defrec->inline-select-keys
  "Like `select-keys` but faster and uses code generation.
  Must accept vector as `ks`"
  [m ks]
  {:pre [(vector? ks)]}
  (let [target (gensym "m__")
        fields (mapv symbol ks)
        syms (map gensym fields)
        recname (gensym "Rec")
        bindings (vec
                  (concat `(~target ~m)
                          (mapcat (fn [sym k] `(~sym (get ~target ~k))) syms ks)))
        final-form (interleave ks syms)
        hm-form (apply hash-map final-form)]
    (do
      (println "defing record of name:" recname "with fields:" fields)
      (eval `(defrecord ~recname ~fields))
      `(let ~bindings
         (~(resolve (symbol (str '-> recname))) ~@syms)))))
