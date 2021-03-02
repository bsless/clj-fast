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
   [get nth assoc get-in merge assoc-in update-in select-keys memoize destructure let fn loop defn defn-])
  (:require
   [clojure.core :as core]
   [clj-fast.util :as u]
   [clj-fast.core :as c]
   [clj-fast.inline :as inline]))

;;; redefine clojure.core functions
;;; NOTE: The functions have to be either redefined or wrapped.
;; Avoid wrapping as it defeats the purpose of this exercise.
;; Can't `alter-meta!` as it'll recur infinitely when macro-expanding

(core/defn get
  "Returns the value mapped to key, not-found or nil if key not present."
  {:inline
   (core/fn [m k & nf]
     (apply inline/-get m k nf))
   :inline-arities #{2 3}
   :added "1.0"}
  ([map key]
   (. clojure.lang.RT (get map key)))
  ([map key not-found]
   (. clojure.lang.RT (get map key not-found))))

(core/defn nth
  "Returns the value at the index. get returns nil if index out of
  bounds, nth throws an exception unless not-found is supplied.  nth
  also works for strings, Java arrays, regex Matchers and Lists, and,
  in O(n) time, for sequences."
  {:inline
   (core/fn
     ([c i]
      (inline/-nth2 c i))
     ([c i nf]
      (inline/-nth3 c i nf)))
   :inline-arities #{2 3}
   :added "1.0"}
  ([coll index] (. clojure.lang.RT (nth coll index)))
  ([coll index not-found] (. clojure.lang.RT (nth coll index not-found))))

(core/defn assoc
  "assoc[iate]. When applied to a map, returns a new map of the
    same (hashed/sorted) type, that contains the mapping of key(s) to
    val(s). When applied to a vector, returns a new vector that
    contains val at index. Note - index must be <= (count vector)."
  {:arglists '([map key val] [map key val & kvs])
   :added "1.0"
   :static true
   :inline
   (core/fn [m & kvs]
     (if (u/simple-seq? kvs)
       `(inline/assoc ~m ~@kvs)
       `(core/assoc ~m ~@kvs)))}
  ([map key val] (clojure.lang.RT/assoc map key val))
  ([map key val & kvs]
   (core/let [ret (clojure.lang.RT/assoc map key val)]
     (if kvs
       (if (next kvs)
         (recur ret (first kvs) (second kvs) (nnext kvs))
         (throw (IllegalArgumentException.
                 "assoc expects even number of arguments after map/vector, found odd number")))
       ret))))

(core/defn assoc-in
  "Associates a value in a nested associative structure, where ks is a
  sequence of keys and v is the new value and returns a new nested structure.
  If any levels do not exist, hash-maps will be created."
  {:added "1.0"
   :static true
   :inline
   (core/fn [m ks v]
     (if (u/simple-seq? ks)
       `(inline/assoc-in ~m ~ks ~v)
       `(core/assoc-in ~m ~ks ~v)))}
  [m [k & ks] v]
  (if ks
    (core/assoc m k (assoc-in (core/get m k) ks v))
    (core/assoc m k v)))

(core/defn update-in
  "'Updates' a value in a nested associative structure, where ks is a
  sequence of keys and f is a function that will take the old value
  and any supplied args and return the new value, and returns a new
  nested structure.  If any levels do not exist, hash-maps will be
  created."
  {:added "1.0"
   :static true
   :inline
   (core/fn [m ks f & args]
     (if (u/simple-seq? ks)
       `(inline/update-in ~m ~ks ~f ~@args)
       `(c/fast-update-in ~m ~ks ~f ~@args)))}
  ([m ks f]
   (core/let [up (core/fn up [m ks f]
              (core/let [[k & ks] ks]
                (if ks
                  (core/assoc m k (up (core/get m k) ks f))
                  (core/assoc m k (f (core/get m k))))))]
     (up m ks f)))
  ([m ks f a]
   (core/let [up (core/fn up [m ks f a]
              (core/let [[k & ks] ks]
                (if ks
                  (core/assoc m k (up (core/get m k) ks f a))
                  (core/assoc m k (f (core/get m k) a)))))]
     (up m ks f a)))
  ([m ks f a b]
   (core/let [up (core/fn up [m ks f a b]
              (core/let [[k & ks] ks]
                (if ks
                  (core/assoc m k (up (core/get m k) ks f a b))
                  (core/assoc m k (f (core/get m k) a b)))))]
     (up m ks f a b)))
  ([m ks f a b c]
   (core/let [up (core/fn up [m ks f a b c]
              (core/let [[k & ks] ks]
                (if ks
                  (core/assoc m k (up (core/get m k) ks f a b c))
                  (core/assoc m k (f (core/get m k) a b c)))))]
     (up m ks f a b c)))
  ([m ks f a b c & args]
   (core/let [up (core/fn up [m ks f a b c args]
              (core/let [[k & ks] ks]
                (if ks
                  (core/assoc m k (up (core/get m k) ks f a b c args))
                  (core/assoc m k (apply f (core/get m k) a b c args)))))]
     (up m ks f a b c args))))

(core/defn get-in
  "Returns the value in a nested associative structure,
  where ks is a sequence of keys. Returns nil if the key
  is not present, or the not-found value if supplied."
  {:added "1.2"
   :static true
   :inline-arities #{2}
   :inline
   (core/fn [m ks]
     (if (u/simple-seq? ks)
       `(inline/get-in ~m ~ks)
       `(core/get-in ~m ~ks)))}
  ([m ks]
   (reduce core/get m ks))
  ([m ks not-found]
   (core/loop [sentinel (Object.)
               m m
               ks (seq ks)]
     (if ks
       (core/let [m (core/get m (first ks) sentinel)]
         (if (identical? sentinel m)
           not-found
           (recur sentinel m (next ks))))
       m))))

(core/defn select-keys
  "Returns a map containing all the values of the selected keys when a
  resolvable sequence is supplied, otherwise, a containing only those
  entries in map whose key is in keys"
  {:added "1.0"
   :static true
   :inline
   (core/fn [m ks]
     (if (u/simple-seq? ks)
       `(inline/select-keys ~m ~ks)
       `(core/select-keys ~m ~ks)))}
  [map keyseq]
  (core/loop [ret {} keys (seq keyseq)]
    (if keys
      (core/let [entry (. clojure.lang.RT (find map (first keys)))]
        (recur
         (if entry
           (conj ret entry)
           ret)
         (next keys)))
      (with-meta ret (meta map)))))

(core/defn merge
  "Returns a map that consists of the rest of the maps conj-ed onto
  the first.  If a key occurs in more than one map, the mapping from
  the latter (left-to-right) will be the mapping in the result."
  {:added "1.0"
   :static true
   :inline
   (core/fn [& maps]
     (if (u/simple-seq? maps)
       `(inline/merge ~@maps)
       `(core/merge ~@maps)))}
  [& maps]
  (when (some identity maps)
    (reduce #(conj (or %1 {}) %2) maps)))

(core/defn destructure [bindings]
  (core/let [bents (partition 2 bindings)
             pb (core/fn pb [bvec b v]
                  (core/let [pvec
                             (core/fn [bvec b val]
                               (core/let [m (meta val)
                                          gvec (with-meta (gensym "vec__") m)
                                          gseq (gensym "seq__")
                                          gfirst (gensym "first__")
                                          has-rest (some #{'&} b)]
                                 (core/loop [ret (core/let [ret (conj bvec gvec val)]
                                                   (if has-rest
                                                     (conj ret gseq (list `seq gvec))
                                                     ret))
                                             n 0
                                             bs b
                                             seen-rest? false]
                                   (if (seq bs)
                                     (core/let [firstb (first bs)]
                                       (cond
                                         (= firstb '&) (recur (pb ret (second bs) gseq)
                                                              n
                                                              (nnext bs)
                                                              true)
                                         (= firstb :as) (pb ret (second bs) gvec)
                                         :else (if seen-rest?
                                                 (throw (new Exception "Unsupported binding form, only :as can follow & parameter"))
                                                 (recur (pb (if has-rest
                                                              (conj ret
                                                                    gfirst `(first ~gseq)
                                                                    gseq `(next ~gseq))
                                                              ret)
                                                            firstb
                                                            (if has-rest
                                                              gfirst
                                                              (list `nth gvec n nil)))
                                                        (inc n)
                                                        (next bs)
                                                        seen-rest?))))
                                     ret))))
                             pmap
                             (core/fn [bvec b v]
                               (core/let [t (:tag (meta v))
                                          gmap (with-meta (gensym "map__") {:tag (or t 'clojure.lang.IPersistentMap)})
                                          gmapseq (with-meta gmap {:tag 'clojure.lang.ISeq})
                                          defaults (:or b)]
                                 (core/loop [ret (-> bvec (conj gmap) (conj v)
                                                     (conj gmap) (conj `(if (seq? ~(with-meta gmap {})) (clojure.lang.PersistentHashMap/create (seq ~gmapseq)) ~gmap))
                                                     ((core/fn [ret]
                                                        (if (:as b)
                                                          (conj ret (:as b) gmap)
                                                          ret))))
                                             bes (core/let [transforms
                                                            (reduce
                                                             (core/fn [transforms mk]
                                                               (if (keyword? mk)
                                                                 (core/let [mkns (namespace mk)
                                                                            mkn (name mk)]
                                                                   (cond (= mkn "keys") (assoc transforms mk #(keyword (or mkns (namespace %)) (name %)))
                                                                         (= mkn "syms") (assoc transforms mk #(list `quote (symbol (or mkns (namespace %)) (name %))))
                                                                         (= mkn "strs") (assoc transforms mk str)
                                                                         :else transforms))
                                                                 transforms))
                                                             {}
                                                             (keys b))]
                                                   (reduce
                                                    (core/fn [bes entry]
                                                      (reduce #(assoc %1 %2 ((val entry) %2))
                                                              (dissoc bes (key entry))
                                                              ((key entry) bes)))
                                                    (dissoc b :as :or)
                                                    transforms))]
                                   (if (seq bes)
                                     (core/let [bb (key (first bes))
                                                bk (val (first bes))
                                                local (if (instance? clojure.lang.Named bb) (with-meta (symbol nil (name bb)) (meta bb)) bb)
                                                bv (if (contains? defaults local)
                                                     (list `get gmap bk (defaults local))
                                                     (list `get gmap bk))]
                                       (recur (if (ident? bb)
                                                (-> ret (conj local bv))
                                                (pb ret bb bv))
                                              (next bes)))
                                     ret))))]
                    (cond
                      (symbol? b) (-> bvec (conj b) (conj v))
                      (vector? b) (pvec bvec b v)
                      (map? b) (pmap bvec b v)
                      :else (throw (new Exception (str "Unsupported binding form: " b))))))
             process-entry (core/fn [bvec b] (pb bvec (first b) (second b)))]
    (if (every? symbol? (map first bents))
      bindings
      (reduce process-entry [] bents))))

(defmacro let
  "binding => binding-form init-expr

  Evaluates the exprs in a lexical context in which the symbols in
  the binding-forms are bound to their respective init-exprs or parts
  therein."
  {:added "1.0", :special-form true, :forms '[(let [bindings*] exprs*)]}
  [bindings & body]
  (#'core/assert-args
   (vector? bindings) "a vector for its binding"
   (even? (count bindings)) "an even number of forms in binding vector")
  `(let* ~(destructure bindings) ~@body))

(core/defn- maybe-destructured
  [params body]
  (if (every? symbol? params)
    (cons params body)
    (core/loop [params params
                new-params (with-meta [] (meta params))
                lets []]
      (if params
        (if (symbol? (first params))
          (recur (next params) (conj new-params (first params)) lets)
          (core/let [p (first params)
                     m (meta p)
                     gparam (with-meta (gensym "p__") m)]
            (recur (next params) (conj new-params gparam)
                   (-> lets (conj p) (conj gparam)))))
        `(~new-params
          (let ~lets
            ~@body))))))

(defmacro fn
  "params => positional-params* , or positional-params* & next-param
  positional-param => binding-form
  next-param => binding-form
  name => symbol

  Defines a function"
  {:added "1.0", :special-form true,
   :forms '[(fn name? [params* ] exprs*) (fn name? ([params* ] exprs*)+)]}
  [& sigs]
  (core/let
      [name (if (symbol? (first sigs)) (first sigs) nil)
       sigs (if name (next sigs) sigs)
       sigs (if (vector? (first sigs))
              (list sigs)
              (if (seq? (first sigs))
                sigs
                ;; Assume single arity syntax
                (throw (IllegalArgumentException.
                        (if (seq sigs)
                          (str "Parameter declaration "
                               (first sigs)
                               " should be a vector")
                          (str "Parameter declaration missing"))))))
       psig (fn* [sig]
                 ;; Ensure correct type before destructuring sig
                 (when (not (seq? sig))
                   (throw (IllegalArgumentException.
                           (str "Invalid signature " sig
                                " should be a list"))))
                 (core/let
                     [[params & body] sig
                      _ (when (not (vector? params))
                          (throw (IllegalArgumentException.
                                  (if (seq? (first sigs))
                                    (str "Parameter declaration " params
                                         " should be a vector")
                                    (str "Invalid signature " sig
                                         " should be a list")))))
                      conds (when (and (next body) (map? (first body)))
                              (first body))
                      body (if conds (next body) body)
                      conds (or conds (meta params))
                      pre (:pre conds)
                      post (:post conds)
                      body (if post
                             `((core/let [~'% ~(if (< 1 (count body))
                                                 `(do ~@body)
                                                 (first body))]
                                 ~@(map (fn* [c] `(assert ~c)) post)
                                 ~'%))
                             body)
                      body (if pre
                             (concat (map (fn* [c] `(assert ~c)) pre)
                                     body)
                             body)]
                   (maybe-destructured params body)))
       new-sigs (map psig sigs)]
    (with-meta
      (if name
        (list* 'fn* name new-sigs)
        (cons 'fn* new-sigs))
      (meta &form))))

(defmacro loop
  "Evaluates the exprs in a lexical context in which the symbols in
  the binding-forms are bound to their respective init-exprs or parts
  therein. Acts as a recur target."
  {:added "1.0", :special-form true, :forms '[(loop [bindings*] exprs*)]}
  [bindings & body]
  (#'core/assert-args
   (vector? bindings) "a vector for its binding"
   (even? (count bindings)) "an even number of forms in binding vector")
  (core/let
      [db (destructure bindings)]
    (if (= db bindings)
      `(loop* ~bindings ~@body)
      (core/let
          [vs (take-nth 2 (drop 1 bindings))
           bs (take-nth 2 bindings)
           gs (map (core/fn [b] (if (symbol? b) b (gensym))) bs)
           bfs (reduce (core/fn [ret [b v g]]
                         (if (symbol? b)
                           (conj ret g v)
                           (conj ret g v b g)))
                       [] (map vector bs vs gs))]
        `(let ~bfs
           (loop* ~(vec (interleave gs gs))
                  (let ~(vec (interleave bs gs))
                    ~@body)))))))

(def

  ^{:doc "Same as (def name (fn [params* ] exprs*)) or (def
    name (fn ([params* ] exprs*)+)) with any doc-string or attrs added
    to the var metadata. prepost-map defines a map with optional keys
    :pre and :post that contain collections of pre or post conditions."
    :arglists '([name doc-string? attr-map? [params*] prepost-map? body]
                [name doc-string? attr-map? ([params*] prepost-map? body)+ attr-map?])
    :added "1.0"}
  defn (core/fn defn [&form &env name & fdecl]
         ;; Note: Cannot delegate this check to def because of the call to (with-meta name ..)
         (if (instance? clojure.lang.Symbol name)
           nil
           (throw (IllegalArgumentException. "First argument to defn must be a symbol")))
         (core/let
             [m (if (string? (first fdecl))
                  {:doc (first fdecl)}
                  {})
              fdecl (if (string? (first fdecl))
                      (next fdecl)
                      fdecl)
              m (if (map? (first fdecl))
                  (conj m (first fdecl))
                  m)
              fdecl (if (map? (first fdecl))
                      (next fdecl)
                      fdecl)
              fdecl (if (vector? (first fdecl))
                      (list fdecl)
                      fdecl)
              m (if (map? (last fdecl))
                  (conj m (last fdecl))
                  m)
              fdecl (if (map? (last fdecl))
                      (butlast fdecl)
                      fdecl)
              m (conj {:arglists (list 'quote (#'core/sigs fdecl))} m)
              m (core/let
                    [inline (:inline m)
                     ifn (first inline)
                     iname (second inline)]
                  ;; same as: (if (and (= 'fn ifn) (not (symbol? iname))) ...)
                  (if (if (clojure.lang.Util/equiv 'fn ifn)
                        (if (instance? clojure.lang.Symbol iname) false true))
                    ;; inserts the same fn name to the inline fn if it does not have one
                    (assoc m :inline (cons ifn (cons (clojure.lang.Symbol/intern (.concat (.getName ^clojure.lang.Symbol name) "__inliner"))
                                                     (next inline))))
                    m))
              m (conj (if (meta name) (meta name) {}) m)]
           (list 'def (with-meta name m)
                 ;;todo - restore propagation of fn name
                 ;;must figure out how to convey primitive hints to self calls first
                 ;;(cons `fn fdecl)
                 (with-meta (cons `fn fdecl) {:rettag (:tag m)})))))

(. (var defn) (setMacro))

(defmacro defn-
  "same as defn, yielding non-public def"
  {:added "1.0"}
  [name & decls]
  (list* `defn (with-meta name (assoc (meta name) :private true)) decls))
