(ns clj-fast.bench
  (:require
   [clojure.set]
   [clojure.tools.cli :as cli]
   [clj-fast.inline :as inline]
   [clj-fast.collections.hash-map :as hm]
   [criterium.core :as cc]
   [clojure.spec.alpha :as s]
   [clojure.spec.gen.alpha :as gen])
  (:gen-class))

;;; JVM utilities

(def ^:dynamic *types* [:int? :keyword? :string?])
(def ^:dynamic *quick* true)

(defmacro bench
  [expr]
  `(if *quick*
     (cc/quick-benchmark ~expr nil)
     (cc/benchmark ~expr nil)))

(defn get-max-memory
  []
  (float (/ (.maxMemory (java.lang.Runtime/getRuntime)) 1024 1024 1024)))

(def max-memory (delay (get-max-memory)))

(defn get-gcs
  []
  (map (fn [e] (.getName e))
       (java.lang.management.ManagementFactory/getGarbageCollectorMXBeans)))

(def gcs (delay
           (clojure.string/replace
            (clojure.string/join ";" (get-gcs))
            #" " "-")))

;;; utilities
(defn ->ns
  [n]
  (Math/round (/ n 1e-9)))

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

(defn genn
  [n spec]
  (drop 1 (gen/sample (s/gen spec) (inc n))))

(defn randmap
  ([n]
   (randmap keyword? n))
  ([p n]
   (into {} (genn n (s/tuple p p)))))

(def mrandmap (memoize randmap))

(declare mrand-nested-map)
(defn rand-nested-map
  [p width depth]
  (if (= 1 depth)
    (mrandmap p width)
    (zipmap (genn width p)
            (repeat width (mrand-nested-map p width (dec depth))))))

(def mrand-nested-map (memoize rand-nested-map))

(def preds
  {:int? int?
   :keyword? keyword?
   :string? string?
   :map? map?})

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

;; Credit metosin
(defn title [s]
  (println
   (str "\n\u001B[35m"
        (apply str (repeat (+ 6 (count s)) "#"))
        "\n## " s " ##\n"
        (apply str (repeat (+ 6 (count s)) "#"))
        "\u001B[0m\n")))

;;; Benches

(comment)

;;; ASSOC

(defn bench-assoc*
  [n m [k1 k2 k3 k4]]
  (case n
    1 (bench (assoc m k1 1))
    2 (bench (assoc m k1 1 k2 2))
    3 (bench (assoc m k1 1 k2 2 k3 3))
    4 (bench (assoc m k1 1 k2 2 k3 3 k4 4))))

(defn bench-inline-assoc*
  [n m [k1 k2 k3 k4]]
  (case n
    1 (bench (inline/assoc m k1 1))
    2 (bench (inline/assoc m k1 1 k2 2))
    3 (bench (inline/assoc m k1 1 k2 2 k3 3))
    4 (bench (inline/assoc m k1 1 k2 2 k3 3 k4 4))))

(defn bench-assoc-rec*
  [m n]
  (case n
    1 (bench (assoc m :x 0))
    2 (bench (assoc m :x 0 :y 1))
    3 (bench (assoc m :x 0 :y 1 :z 2))
    4 (bench (assoc m :x 0 :y 1 :z 2 :w 3))))

(defn bench-fast-assoc*
  [n m [k1 k2 k3 k4]]
  (case n
    1 (bench (inline/fast-assoc m k1 1))
    2 (bench (inline/fast-assoc m k1 1 k2 2))
    3 (bench (inline/fast-assoc m k1 1 k2 2 k3 3))
    4 (bench (inline/fast-assoc m k1 1 k2 2 k3 3 k4 4))))

(defn bench-fast-assoc-rec*
  [m n]
  (case n
    1 (bench (inline/fast-assoc m :x 0))
    2 (bench (inline/fast-assoc m :x 0 :y 1))
    3 (bench (inline/fast-assoc m :x 0 :y 1 :z 2))
    4 (bench (inline/fast-assoc m :x 0 :y 1 :z 2 :w 3))))

(def assoc-rec-fns
  {:assoc-rec        bench-assoc-rec*
   :fast-assoc-rec   bench-fast-assoc-rec*})

(def assoc-fns
  {:assoc            bench-assoc*
   :fast-assoc       bench-fast-assoc*})

(defn bench-assoc-
  [max-log-size max-depth]
  (vec
   (for [e (range 1 (inc max-log-size))
         n (range 1 (inc max-depth))
         pk *types*
         :let [width (int (Math/pow 10 e))
               p (preds pk)
               m (mrandmap p width)
               ks (genn n p)]
         k [:assoc :fast-assoc]
         :let [f (assoc-fns k)
               _ (println 'BENCH k 'WIDTH 10 'e e '* n 'TYPE pk)
               res (f n m ks)
               mn (Math/round (/ (mean res) 1e-9))
               ratio (int (/ mn n))]]
     {:bench k
      :mean mn
      :type pk
      :keys n
      :ratio ratio
      :width e
      :heap @max-memory
      :gc @gcs})))

(defn bench-assoc-rec-
  [_ _]
  (vec
   (let [m (->Foo 1 2 3 4)]
     (for [n [1]
           k [:assoc-rec :fast-assoc-rec]
           :let [f (assoc-rec-fns k)
                 res (f m n)
                 mn (Math/round (/ (mean res) 1e-9))
                 ratio (int (/ mn n))]]
       {:bench k
        :mean mn
        :keys n
        :ratio ratio
        :heap @max-memory
        :gc @gcs}))))

;;; GET

(defn bench-get*
  [method m]
  (let [k (randkey m)]
    (case method
      :get (bench (get m k))
      :keyword (bench (k m))
      :invoke (bench (m k))
      :hashmap (let [m (hm/->hashmap m)]
                 (bench (hm/get m k)))
      :val-at-i (bench (.valAt ^clojure.lang.IPersistentMap m k))
      :val-at-a (bench (.valAt ^clojure.lang.APersistentMap m k))
      :val-at-c (bench (.valAt ^clojure.lang.PersistentHashMap m k))
      )))

(defn bench-get-rec* [method ^Foo r]
  (let [k :c]
    (case method
      :get (bench (get r k))
      :dotget (bench (.get ^Foo r k))
      :keyword (bench (k r))
      :field (bench (.c ^Foo r))
      :val-at (bench (.valAt ^Foo r k)))))

(defn bench-get-
  [max-log-size _]
  (vec
   (for [e (range 1 (inc max-log-size))
         p *types*
         method [:get :keyword :invoke :hashmap :val-at-i :val-at-a :val-at-c]
         :let [width (int (Math/pow 10 e))
               m (randmap (preds p) width)]
         :when (or
                (not= method :keyword)
                (and
                 (= method :keyword)
                 (= p :keyword?)))
         :let [_ (println 'BENCH 'get method 'WIDTH 10 'e e 'TYPE p)
               res (bench-get* method m)
               mn (->ns (mean res))]]
     {:bench :get
      :method method
      :type p
      :mean mn
      :width e
      :heap @max-memory
      :gc @gcs})))

(defn bench-get-rec-
  [_ _]
  (let [r (->Foo 1 2 3 4)]
    (vec
     (for [method [:get :dotget :keyword :field :val-at]
           :let [_ (println 'BENCH 'get-record method)
                 res (bench-get-rec* method r)
                 mn (->ns (mean res))]]
       {:bench :get-rec
        :method method
        :mean mn
        :heap @max-memory
        :gc @gcs}))))

;;; MERGE

(defn bench-merge*
  [n m1 & [m2 m3 m4]]
  (case n
    1 (bench (merge m1))
    2 (bench (merge m1 m2))
    3 (bench (merge m1 m2 m3))
    4 (bench (merge m1 m2 m3 m4))
    ))

(defn bench-inline-merge*
  [n m1 & [m2 m3 m4]]
  (case n
    1 (bench (inline/merge m1))
    2 (bench (inline/merge m1 m2))
    3 (bench (inline/merge m1 m2 m3))
    4 (bench (inline/merge m1 m2 m3 m4))
    ))

(defn bench-inline-fast-map-merge*
  [n m1 & [m2 m3 m4]]
  (case n
    1 (bench (inline/fast-map-merge m1))
    2 (bench (inline/fast-map-merge m1 m2))
    3 (bench (inline/fast-map-merge m1 m2 m3))
    4 (bench (inline/fast-map-merge m1 m2 m3 m4))
    ))

(defn bench-inline-tmerge*
  [n m1 & [m2 m3 m4]]
  (case n
    1 (bench (inline/tmerge m1))
    2 (bench (inline/tmerge m1 m2))
    3 (bench (inline/tmerge m1 m2 m3))
    4 (bench (inline/tmerge m1 m2 m3 m4))
    ))

(def merge-fns
  {:merge bench-merge*
   :inline-merge bench-inline-merge*
   :inline-fast-map-merge bench-inline-fast-map-merge*
   :inline-tmerge bench-inline-tmerge*})

(defn bench-merge-
  [max-log-size nmaps]
  (vec
   (for [e (range 1 (inc max-log-size))
         n (range 1 (inc nmaps))
         pk *types*
         :let [width (int (Math/pow 10 e))
               p (preds pk)
               ms (repeatedly n #(randmap p width))]
         k [:merge :inline-merge :inline-fast-map-merge :inline-tmerge]
         :let [f (merge-fns k)
               _ (println 'BENCH k 'WIDTH 10 'e e '* n 'TYPE pk)
               res (apply f n ms)
               mn (->ns (mean res))
               ratio (int (/ mn n))]]
     {:bench k
      :mean mn
      :width e
      :type pk
      :ratio ratio
      :keys n
      :heap @max-memory
      :gc @gcs})))

;;; GET-IN

(defn bench-get-in*
  [_ m ks]
  (bench (get-in m ks)))

(defn bench-inline-get-in*
  [n m [k1 k2 k3 k4]]
  (case n
    1 (bench (inline/get-in m [k1]))
    2 (bench (inline/get-in m [k1 k2]))
    3 (bench (inline/get-in m [k1 k2 k3]))
    4 (bench (inline/get-in m [k1 k2 k3 k4]))))

(defn bench-inline-get-some-in*
  [n m [k1 k2 k3 k4]]
  (case n
    1 (bench (inline/get-some-in m [k1]))
    2 (bench (inline/get-some-in m [k1 k2]))
    3 (bench (inline/get-some-in m [k1 k2 k3]))
    4 (bench (inline/get-some-in m [k1 k2 k3 k4]))))

(def get-in-bench-fns
  {:get-in bench-get-in*
   :inline-get-in bench-inline-get-in*
   :inline-get-some-in bench-inline-get-some-in*})

(defn bench-get-in-
  [max-log-size max-depth]
  (vec
   (for [e (range 1 (inc max-log-size))
         depth (range 1 (inc max-depth))
         pk *types*
         :let [width (int (Math/pow 10 e))
               p (preds pk)
               m (mrand-nested-map p width depth)
               ks (randpath m)]
         k [:get-in :inline-get-in :inline-get-some-in]
         :let [f (get-in-bench-fns k)
               _ (println 'BENCH k 'WIDTH 10 'e e '* depth 'TYPE pk)
               res (f depth m ks)
               mn (->ns (mean res))
               ratio (int (/ mn depth))]]
     {:bench k
      :mean mn
      :width e
      :type pk
      :ratio ratio
      :keys depth
      :heap @max-memory
      :gc @gcs})))

(defn bench-get-in
  []
  (title "get-in")

  (title "get-in 1")
  (let [m {:d 1}]
    (cc/quick-bench
     (get-in m [:d])))

  (title "fast get-in 1")
  (let [m {:d 1}]
    (cc/quick-bench
     (inline/get-in m [:d])))

  (title "get some in 1")
  (let [m {:a 1}]
    (cc/quick-bench
     (inline/get-some-in m [:a])))

  (title "get-in 2")
  (let [m {:c {:d 1}}]
    (cc/quick-bench
     (get-in m [:c :d])))

  (title "fast get-in 2")
  (let [m {:c {:d 1}}]
    (cc/quick-bench
     (inline/get-in m [:c :d])))

  (title "get some in 2")
  (let [m {:a {:b 1}}]
    (cc/quick-bench
     (inline/get-some-in m [:a :b])))

  (title "get-in 3")
  (let [m {:b {:c {:d 1}}}]
    (cc/quick-bench
     (get-in m [:b :c :d])))

  (title "fast get-in 3")
  (let [m {:b {:c {:d 1}}}]
    (cc/quick-bench
     (inline/get-in m [:b :c :d])))

  (title "get some in 3")
  (let [m {:a {:b {:c 1}}}]
    (cc/quick-bench
     (inline/get-some-in m [:a :b :c])))

  (title "get-in 4")
  (let [m {:a {:b {:c {:d 1}}}}]
    (cc/quick-bench
     (get-in m [:a :b :c :d]))) ;; 195.077302 ns

  (title "fast get-in 4")
  (let [m {:a {:b {:c {:d 1}}}}]
    (cc/quick-bench
     (inline/get-in m [:a :b :c :d]))) ;; 37.911144 ns

  (title "get some in 4")
  (let [m {:a {:b {:c {:d 1}}}}]
    (cc/quick-bench
     (inline/get-some-in m [:a :b :c :d])))

  )

;;; SELECT-KEYS

(defn bench-select-keys*
  [_ m ks]
  (bench (select-keys m ks)))

(defn bench-inline-select-keys*
  [n m [k1 k2 k3 k4]]
  (case n
    1 (bench (inline/select-keys m [k1]))
    2 (bench (inline/select-keys m [k1 k2]))
    3 (bench (inline/select-keys m [k1 k2 k3]))
    4 (bench (inline/select-keys m [k1 k2 k3 k4]))))

(def select-keys-bench-fns
  {:select-keys bench-select-keys*
   :inline-select-keys bench-inline-select-keys*})

(defn bench-select-keys-
  [max-log-size max-width]
  (vec
   (for [e (range 1 (inc max-log-size))
         n (range 1 (inc max-width))
         pk *types*
         :let [width (int (Math/pow 10 e))
               p (preds pk)
               m (mrandmap p width)
               ks (eduction (comp (distinct) (take n)) (repeatedly #(randkey m)))]
         k [:select-keys :inline-select-keys]
         :let [f (select-keys-bench-fns k)
               _ (println 'BENCH k 'WIDTH 10 'e e '* n 'TYPE pk)
               res (f n m ks)
               mn (Math/round (/ (mean res) 1e-9))
               ratio (int (/ mn n))]]
     {:bench k
      :mean mn
      :keys n
      :type pk
      :ratio ratio
      :width e
      :heap @max-memory
      :gc @gcs})))

(defn bench-select-keys
  []

  (title "select keys")

  (title "select 1/4 keys")
  (let [m {:a 1 :b 2 :c 3 :d 4}]
    (cc/quick-bench
     (select-keys m [:a])))

  (title "fast select 1/4 keys")
  (let [m {:a 1 :b 2 :c 3 :d 4}]
    (cc/quick-bench
     (inline/select-keys m [:a])))

  (title "select 2/4 keys")
  (let [m {:a 1 :b 2 :c 3 :d 4}]
    (cc/quick-bench
     (select-keys m [:a :b])))

  (title "fast select 2/4 keys")
  (let [m {:a 1 :b 2 :c 3 :d 4}]
    (cc/quick-bench
     (inline/select-keys m [:a :b])))

  (title "select 3/4 keys")
  (let [m {:a 1 :b 2 :c 3 :d 4}]
    (cc/quick-bench
     (select-keys m [:a :b :c])))

  (title "fast select 3/4 keys")
  (let [m {:a 1 :b 2 :c 3 :d 4}]
    (cc/quick-bench
     (inline/select-keys m [:a :b :c])))

  (title "select 4/4 keys")
  (let [m {:a 1 :b 2 :c 3 :d 4}]
    (cc/quick-bench
     (select-keys m [:a :b :c :d])))

  (title "fast select 4/4 keys")
  (let [m {:a 1 :b 2 :c 3 :d 4}]
    (cc/quick-bench
     (inline/select-keys m [:a :b :c :d])))

  )

;;; ASSOC-IN

(defn bench-assoc-in*
  [_ m ks]
  (bench (assoc-in m ks 0)))

(defn bench-inline-assoc-in*
  [n m [k1 k2 k3 k4]]
  (case n
    1 (bench (inline/assoc-in m [k1] 0))
    2 (bench (inline/assoc-in m [k1 k2] 0))
    3 (bench (inline/assoc-in m [k1 k2 k3] 0))
    4 (bench (inline/assoc-in m [k1 k2 k3 k4] 0))))

(def assoc-in-bench-fns
  {:assoc-in bench-assoc-in*
   :inline-assoc-in bench-inline-assoc-in*})

(defn bench-assoc-in-
  [max-log-size max-depth]
  (vec
   (for [e (range 1 (inc max-log-size))
         depth (range 1 (inc max-depth))
         pk *types*
         :let [width (int (Math/pow 10 e))
               p (preds pk)
               m (mbuild-nested-map width depth)
               ks (genn depth p)]
         k [:assoc-in :inline-assoc-in]
         :let [f (assoc-in-bench-fns k)
               _ (println 'BENCH k 'WIDTH 10 'e e '* depth 'TYPE pk)
               res (f depth m ks)
               mn (->ns (mean res))
               ratio (int (/ mn depth))]]
     {:bench k
      :mean mn
      :keys depth
      :type pk
      :ratio ratio
      :width e
      :heap @max-memory
      :gc @gcs})))

(defn bench-assoc-in
  []

  (title "Assoc In")

  (title "assoc-in 1")
  (cc/quick-bench (assoc-in {} [1] 2))
  (title "assoc-in 2")
  (cc/quick-bench (assoc-in {} [1 2] 3))
  (title "assoc-in 3")
  (cc/quick-bench (assoc-in {} [1 2 3] 4))
  (title "assoc-in 4")
  (cc/quick-bench (assoc-in {} [1 2 3 4] 5))


  (title "inline-assoc-in 1")
  (criterium.core/quick-bench (inline/assoc-in {} [1] 2))
  (title "inline-assoc-in 2")
  (criterium.core/quick-bench (inline/assoc-in {} [1 2] 3))
  (title "inline-assoc-in 3")
  (criterium.core/quick-bench (inline/assoc-in {} [1 2 3] 4))
  (title "inline-assoc-in 4")
  (criterium.core/quick-bench (inline/assoc-in {} [1 2 3 4] 5))

  )

;;; UPDATE-IN

(defn bench-update-in*
  [_ m ks]
  (bench (update-in m ks identity)))

(defn bench-inline-update-in*
  [n m [k1 k2 k3 k4]]
  (case n
    1 (bench (inline/update-in m [k1] identity))
    2 (bench (inline/update-in m [k1 k2] identity))
    3 (bench (inline/update-in m [k1 k2 k3] identity))
    4 (bench (inline/update-in m [k1 k2 k3 k4] identity))))

(def update-in-bench-fns
  {:update-in bench-update-in*
   :inline-update-in bench-inline-update-in*})

(defn bench-update-in-
  [max-log-size max-depth]
  (vec
   (for [e (range 1 (inc max-log-size))
         depth (range 1 (inc max-depth))
         pk *types*
         :let [width (int (Math/pow 10 e))
               p (preds pk)
               m (mrand-nested-map p width depth)
               ks (randpath m)]
         k [:update-in :inline-update-in]
         :let [f (update-in-bench-fns k)
               _ (println 'BENCH k 'WIDTH 10 'e e '* depth 'TYPE pk)
               res (f depth m ks)
               mn (->ns (mean res))
               ratio (int (/ mn depth))]]
     {:bench k
      :mean mn
      :keys depth
      :type pk
      :ratio ratio
      :width e
      :heap @max-memory
      :gc @gcs})))

(defn bench-update-in
  []

  (title "Update In")

  (let [m {:a 1}]
    (title "update-in 1")
    (cc/quick-bench (update-in m [:a] identity))
    (title "inline-update-in 1")
    (cc/quick-bench (inline/update-in m [:a] identity)))

  (let [m {:a {:b 1}}]
    (title "update-in 2")
    (cc/quick-bench (update-in m [:a :b] identity))
    (title "inline-update-in 2")
    (cc/quick-bench (inline/update-in m [:a :b] identity)))

  (let [m {:a {:b {:c 1}}}]
    (title "update-in 3")
    (cc/quick-bench (update-in m [:a :b :c] identity))
    (title "inline-update-in 3")
    (cc/quick-bench (inline/update-in m [:a :b :c] identity)))

  (let [m {:a {:b {:c {:d 1}}}}]
    (title "update-in 4")
    (cc/quick-bench (update-in m [:a :b :c :d] identity))
    (title "inline-update-in 4")
    (cc/quick-bench (inline/update-in m [:a :b :c :d] identity)))

  )

;;; memoize

(defn bench-memoize
  [n [a1 a2 a3 a4]]
  (let [f (memoize vector)]
    (case n
      1 (bench (f a1))
      2 (bench (f a1 a2))
      3 (bench (f a1 a2 a3))
      4 (bench (f a1 a2 a3 a4))
      )))

(defn bench-memoize-n
  [n [a1 a2 a3 a4]]
  (case n
    1 (let [f (inline/memoize* 1 vector)]
      (bench (f a1)))
    2 (let [f (inline/memoize* 2 vector)]
        (bench (f a1 a2)))
    3 (let [f (inline/memoize* 3 vector)]
        (bench (f a1 a2 a3)))
    4 (let [f (inline/memoize* 4 vector)]
        (bench (f a1 a2 a3 a4)))
    ))

(defn bench-memoize-c
  [n [a1 a2 a3 a4]]
  (case n
    1 (let [f (inline/memoize-c* 1 vector)]
        (bench (f a1)))
    2 (let [f (inline/memoize-c* 2 vector)]
        (bench (f a1 a2)))
    3 (let [f (inline/memoize-c* 3 vector)]
        (bench (f a1 a2 a3)))
    4 (let [f (inline/memoize-c* 4 vector)]
        (bench (f a1 a2 a3 a4)))
    ))

(def memoize-benches
  {:memoize bench-memoize
   :memoize-n bench-memoize-n
   :memoize-c bench-memoize-c})

(defn bench-memoize-
  [_ max-depth]
  (vec
   (for [depth (range 1 (inc max-depth))
         pk [:map? :keyword? :int?]
         :let [p (preds pk)
               ms (genn depth p)]
         k [:memoize :memoize-n :memoize-c]
         :let [f (memoize-benches k)
               _ (println 'BENCH k 'WIDTH 10 '* depth 'TYPE pk)
               res (f depth ms)
               mn (->ns (mean res))
               ratio (int (/ mn depth))]]
     {:bench k
      :mean mn
      :keys depth
      :type pk
      :ratio ratio
      :width 1
      :heap @max-memory
      :gc @gcs})))

;;; Executable

(defn- parse-types
  [s]
  (mapv keyword (clojure.string/split s #" ")))

(def cli-options
  [["-n" "--name NAME" "Benchmarks nickname"
    :default ""]
   ["-o" "--out-path" "Output path"
    :default "./benchmarks"]
   ["-q" "--quick QUICK" "quick"
    :parse-fn #(Boolean/parseBoolean %)
    :default true]
   ["-w" "--max-width WIDTH" "Logarithmic maximum width"
    :default 4
    :parse-fn #(Integer/parseInt %)
    :validate [#(<= 1 % 6) "Please Keep max width up to 6"]]
   ["-d" "--max-depth DEPTH" "Logarithmic maximum width"
    :default 4
    :parse-fn #(Integer/parseInt %)
    :validate [#(<= 1 % 4) "Please Keep max width up to 4"]]
   ["-t" "--types TYPES" "Predicates to generate maps from"
    :default []
    :parse-fn parse-types
    :validate [#(clojure.set/subset?
                 (set %)
                #{:keyword? :string? :int?})
               "Type must be one of: keyword? string? int?"]]
   ["-h" "--help"]])

(def benches
  {
   :get bench-get-
   :get-rec bench-get-rec-
   :get-in bench-get-in-
   :assoc bench-assoc-
   :assoc-rec bench-assoc-rec-
   :merge bench-merge-
   :select-keys bench-select-keys-
   :assoc-in bench-assoc-in-
   :update-in bench-update-in-
   :memoize bench-memoize-
   })

(defn validate-args
  [args]
  (let [diff
        (clojure.set/difference
         (set args)
         (set (keys benches)))]
    (when (seq diff)
      (println 'ERROR "Unrecognized args" diff)
      (System/exit 1))))

(defn validate-out
  [out]
  (try
    (.mkdirs (clojure.java.io/file out))
    (catch Exception e
      (println 'ERROR e))))

(defn -main
  [& args]
  (let [{:keys [arguments options summary]}
        (cli/parse-opts args cli-options)
        {:keys [types quick help out-path max-width max-depth name]} options
        ks (map keyword arguments)
        now
        (str (java.time.LocalDateTime/now))
        parts (remove clojure.string/blank? [name "clj-fast" "bench.edn"])
        output
        (clojure.string/join
         "/"
         [out-path
          (clojure.string/join "-" parts)])]
    (when help
      (println summary)
      (System/exit 0))
    (validate-args ks)
    (validate-out out-path)
    (println "Producing report to" output "at" now)
    (binding [*types* types
              *quick* quick]
      (let [results
            (reduce
             (fn [m k]
               (println 'INFO 'benching k)
               (assoc m k ((benches k) max-width max-depth))) {} ks)]
        (spit output results)))))
