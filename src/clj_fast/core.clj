(ns clj-fast.core)

(defn val-at
  {:inline
   (fn [m k]
     `(.valAt ~(with-meta m {:tag 'clojure.lang.IPersistentMap}) ~k))}
  [^clojure.lang.IPersistentMap m k]
  (.valAt ^clojure.lang.IPersistentMap m k))

;;; Credit Metosin
;;; https://github.com/metosin/reitit/blob/0bcfda755f139d14cf4eff37e2b294f573215213/modules/reitit-core/src/reitit/impl.cljc#L136
(defn fast-assoc
  {:inline
   (fn [a k v]
     `(.assoc ~(with-meta a {:tag 'clojure.lang.Assciative}) ~k ~v))}
  [^clojure.lang.Associative a k v]
  (.assoc ^clojure.lang.Associative a k v))

;;; Credit Metosin
;;; https://github.com/metosin/compojure-api/blob/master/src/compojure/api/common.clj#L46
(defn fast-map-merge
  [x y]
  (reduce-kv
   (fn [m k v]
     (fast-assoc m k v))
   x
   y))

;;; Credit github.com/joinr: github.com/bsless/clj-fast/issues/1
(defn rmerge! [^clojure.lang.IKVReduce l  r]
  (.kvreduce
   l
   (fn [^clojure.lang.ITransientAssociative acc k v]
     (if-not (acc k)
       (.assoc acc k v)
       acc))
   r))

(defn entry-at
  {:inline
   (fn [m k]
     `(.entryAt ~(with-meta m {:tag 'clojure.lang.IPersistentMap}) ~k))}
  [^clojure.lang.IPersistentMap m k]
  (.entryAt ^clojure.lang.IPersistentMap m k))
