;;; Copyright (c) Rich Hickey. All rights reserved.
;;; The use and distribution terms for this software are covered by the
;;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;; which can be found in the file epl-v10.html at the root of this distribution.
;;; By using this software in any fashion, you are agreeing to be bound by
;;; the terms of this license.
;;; You must not remove this notice, or any other, from this software.

(ns clj-fast.clojure.core
  (:refer-clojure
   :exclude
   [get assoc get-in merge assoc-in update-in select-keys memoize])
  (:require
   [clj-fast.core :as c]
   [clj-fast.util :as u]
   [clj-fast.inline :as inline]
   [clj-fast.collections.map :as m]))

;;; redefine clojure.core functions
;;; NOTE: The functions have to be either redefined or wrapped.
;; Avoid wrapping as it defeats the purpose of this exercise.
;; Can't `alter-meta!` as it'll recur infinitely when macro-expanding

(defn get
  "Returns the value mapped to key, not-found or nil if key not present."
  {:inline
   (fn [m k & nf]
     (case (:tag (meta m))
       (IPersistentMap clojure.lang.IPersistentMap
        PersistentArrayMap clojure.lang.PersistentArrayMap
        PersistentHashMap clojure.lang.PersistentHashMap)
       `(c/val-at ~m ~k ~@nf)
       (PersistentVector clojure.lang.PersistentVector)
       `(.nth ~(with-meta m {:tag clojure.lang.PersistentVector}) ~k ~@nf)
       (Map HashMap java.util.Map java.util.HashMap)
       `(m/get ~m ~k ~@nf)
       `(. clojure.lang.RT (get ~m ~k ~@nf))))
   :inline-arities #{2 3}
   :added "1.0"}
  ([map key]
   (. clojure.lang.RT (get map key)))
  ([map key not-found]
   (. clojure.lang.RT (get map key not-found))))

(defn assoc
  "assoc[iate]. When applied to a map, returns a new map of the
    same (hashed/sorted) type, that contains the mapping of key(s) to
    val(s). When applied to a vector, returns a new vector that
    contains val at index. Note - index must be <= (count vector)."
  {:arglists '([map key val] [map key val & kvs])
   :added "1.0"
   :static true
   :inline
   (fn [m & kvs]
     (if (u/simple-seq? kvs)
       `(inline/assoc ~m ~@kvs)
       `(clojure.core/assoc ~m ~@kvs)))}
  ([map key val] (clojure.lang.RT/assoc map key val))
  ([map key val & kvs]
   (let [ret (clojure.lang.RT/assoc map key val)]
     (if kvs
       (if (next kvs)
         (recur ret (first kvs) (second kvs) (nnext kvs))
         (throw (IllegalArgumentException.
                 "assoc expects even number of arguments after map/vector, found odd number")))
       ret))))

(defn assoc-in
  "Associates a value in a nested associative structure, where ks is a
  sequence of keys and v is the new value and returns a new nested structure.
  If any levels do not exist, hash-maps will be created."
  {:added "1.0"
   :static true
   :inline
   (fn [m ks v]
     (if (u/simple-seq? ks)
       `(inline/assoc-in ~m ~ks ~v)
       `(clojure.core/assoc-in ~m ~ks ~v)))}
  [m [k & ks] v]
  (if ks
    (clojure.core/assoc m k (assoc-in (clojure.core/get m k) ks v))
    (clojure.core/assoc m k v)))

(defn update-in
  "'Updates' a value in a nested associative structure, where ks is a
  sequence of keys and f is a function that will take the old value
  and any supplied args and return the new value, and returns a new
  nested structure.  If any levels do not exist, hash-maps will be
  created."
  {:added "1.0"
   :static true
   :inline
   (fn [m ks f & args]
     (if (u/simple-seq? ks)
       `(inline/update-in ~m ~ks ~f ~@args)
       `(clojure.core/update-in ~m ~ks ~f ~@args)))}
  ([m ks f & args]
   (let [up (fn up [m ks f args]
              (let [[k & ks] ks]
                (if ks
                  (clojure.core/assoc m k (up (clojure.core/get m k) ks f args))
                  (clojure.core/assoc m k (apply f (clojure.core/get m k) args)))))]
     (up m ks f args))))

(defn get-in
  "Returns the value in a nested associative structure,
  where ks is a sequence of keys. Returns nil if the key
  is not present, or the not-found value if supplied."
  {:added "1.2"
   :static true
   :inline-arities #{2}
   :inline
   (fn [m ks]
     (if (u/simple-seq? ks)
       `(inline/get-in ~m ~ks)
       `(clojure.core/get-in ~m ~ks)))}
  ([m ks]
   (#'clojure.core/reduce1 clojure.core/get m ks))
  ([m ks not-found]
   (loop [sentinel (Object.)
          m m
          ks (seq ks)]
     (if ks
       (let [m (clojure.core/get m (first ks) sentinel)]
         (if (identical? sentinel m)
           not-found
           (recur sentinel m (next ks))))
       m))))

(defn select-keys
  "Returns a map containing all the values of the selected keys when a
  resolvable sequence is supplied, otherwise, a containing only those
  entries in map whose key is in keys"
  {:added "1.0"
   :static true
   :inline
   (fn [m ks]
     (if (u/simple-seq? ks)
       `(inline/select-keys ~m ~ks)
       `(clojure.core/select-keys ~m ~ks)))}
  [map keyseq]
  (loop [ret {} keys (seq keyseq)]
    (if keys
      (let [entry (. clojure.lang.RT (find map (first keys)))]
        (recur
         (if entry
           (conj ret entry)
           ret)
         (next keys)))
      (with-meta ret (meta map)))))

(defn merge
  "Returns a map that consists of the rest of the maps conj-ed onto
  the first.  If a key occurs in more than one map, the mapping from
  the latter (left-to-right) will be the mapping in the result."
  {:added "1.0"
   :static true
   :inline
   (fn [& maps]
     (if (u/simple-seq? maps)
       `(inline/merge ~@maps)
       `(clojure.core/merge ~@maps)))}
  [& maps]
  (when (some identity maps)
    (#'clojure.core/reduce1 #(conj (or %1 {}) %2) maps)))
