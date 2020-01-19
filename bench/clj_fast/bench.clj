(ns clj-fast.bench
  (:require
   [clojure.set]
   [clojure.tools.cli :as cli]
   [clj-fast.core :as sut]
   [criterium.core :as cc]
   [clojure.spec.alpha :as s]
   [clojure.spec.gen.alpha :as gen])
  (:gen-class))

;;; JVM utilities

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

(defn randmap
  ([n]
   (randmap keyword? n))
  ([p n]
   (into {} (drop 1 (gen/sample (s/gen (s/tuple p p)) (inc n))))))

(defn randkey
  [m]
  (rand-nth (keys m)))

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
  [m n]
  (case n
    1 (cc/quick-benchmark (assoc m :x 0) nil)
    2 (cc/quick-benchmark (assoc m :x 0 :y 1) nil)
    3 (cc/quick-benchmark (assoc m :x 0 :y 1 :z 2) nil)
    4 (cc/quick-benchmark (assoc m :x 0 :y 1 :z 2 :w 3) nil)))

(def bench-assoc-rec* bench-assoc*)

(defn bench-fast-assoc*
  [m n]
  (case n
    1 (cc/quick-benchmark (sut/fast-assoc* m :x 0) nil)
    2 (cc/quick-benchmark (sut/fast-assoc* m :x 0 :y 1) nil)
    3 (cc/quick-benchmark (sut/fast-assoc* m :x 0 :y 1 :z 2) nil)
    4 (cc/quick-benchmark (sut/fast-assoc* m :x 0 :y 1 :z 2 :w 3) nil)))

(def bench-fast-assoc-rec* bench-fast-assoc*)

(def assoc-rec-fns
  {:bench-assoc-rec        bench-assoc-rec*
   :bench-fast-assoc-rec   bench-fast-assoc-rec*})

(def assoc-fns
  {:bench-assoc            bench-assoc*
   :bench-fast-assoc       bench-fast-assoc*})

(defn bench-assoc-
  [max-log-size max-depth]
  (vec
   (for [e (range 1 (inc max-log-size))
         n (range 1 (inc max-depth))
         :let [width (int (Math/pow 10 e))
               m (mbuild-map width)]
         k [:bench-assoc :bench-fast-assoc]
         :let [f (assoc-fns k n)
               res (f m n)
               mn (Math/round (/ (mean res) 1e-9))
               ratio (int (/ mn n))]]
     {:bench k
      :mean mn
      :keys n
      :ratio ratio
      :width e
      :heap @max-memory
      :gc @gcs})))

(defn bench-assoc-rec-
  [_ max-depth]
  (vec
   (let [m (->Foo 1 2 3 4)]
     (for [n (range 1 (inc max-depth))
           k [:bench-assoc-rec :bench-fast-assoc-rec]
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

(defn bench-assoc
  []
  (title "Assoc")

  (title "assoc to map")
  (let [m {:a 1 :b 2 :c 3 :d 4}]
    (cc/quick-bench
     (assoc m :x "y"))) ;; 42.187467 ns

  (title "assoc to record")
  (let [m (->Foo 1 2 3 4)]
    (cc/quick-bench
     (assoc m :x "y"))) ;; 121.102786 ns

  (title "fast-assoc to map")
  (let [m {:a 1 :b 2 :c 3 :d 4}]
    (cc/quick-bench
     (sut/fast-assoc m :x "y"))) ;; 31.409151 ns

  (title "fast-assoc to record")
  (let [m (->Foo 1 2 3 4)]
    (cc/quick-bench
     (sut/fast-assoc m :x "y"))) ;; 105.788254 ns
  )

;;; GET

(defn bench-get*
  [method m]
  (let [k (randkey m)]
    (case method
      :get (cc/quick-benchmark (get m k) nil)
      :keyword (cc/quick-benchmark (k m) nil)
      :invoke (cc/quick-benchmark (m k) nil)
      :val-at (cc/quick-benchmark (.valAt ^clojure.lang.IPersistentMap m k) nil))))

(defn bench-get-rec* [method ^Foo r]
  (let [k :c]
    (case method
      :get (cc/quick-benchmark (get r k) nil)
      :dotget (cc/quick-benchmark (.get ^Foo r k) nil)
      :keyword (cc/quick-benchmark (k r) nil)
      :field (cc/quick-benchmark (.c ^Foo r) nil)
      :val-at (cc/quick-benchmark (.valAt ^Foo r k) nil))))

(def preds
  {:int? int?
   :keyword? keyword?
   :string? string?})

(defn bench-get-
  [max-log-size _]
  (vec
   (for [e (range 1 (inc max-log-size))
         p [:int? :keyword? :string?]
         method [:get :keyword :invoke :val-at]
         :let [width (int (Math/pow 10 e))
               m (randmap (preds p) width)]
         :when (or
                (not= method :keyword)
                (and
                 (= method :keyword)
                 (= p :keyword?)))
         :let [res (bench-get* method m)
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
           :let [res (bench-get-rec* method r)
                 mn (->ns (mean res))]]
       {:bench :get-rec
        :method method
        :mean mn
        :heap @max-memory
        :gc @gcs}))))

(defn bench-get+
  [max-log-size _]
  (vec
   (concat
    (bench-get- max-log-size nil)
    (bench-get-rec- nil nil))))

(defn bench-get
  []
  (title "get")

  (title "get from map")
  (let [m {:a 1 :b 2 :c 3 :d 4}]
    (cc/quick-bench
     (get m :c)))

  (title "map on keyword")
  (let [m {:a 1 :b 2 :c 3 :d 4}]
    (cc/quick-bench
     (m :c)))

  (title "keyword on map")
  (let [m {:a 1 :b 2 :c 3 :d 4}]
    (cc/quick-bench
     (:c m)))

  (title "get from record")
  (let [m (->Foo 1 2 3 4)]
    (cc/quick-bench
     (get m :c)))

  (title "keyword on record")
  (let [m (->Foo 1 2 3 4)]
    (cc/quick-bench
     (:c m)))

  (title ".get from record")
  (let [m (->Foo 1 2 3 4)]
    (cc/quick-bench
     (.get ^Foo m :c)))

  (title "get field from record")
  (let [m (->Foo 1 2 3 4)]
    (cc/quick-bench
     (.c ^Foo m)))

  (title "get from fast-map")
  (let [m (sut/fast-map {:a 1 :b 2 :c 3 :d 4})]
    (cc/quick-bench
     (get m :c))) ;; 38.447998 ns

  (title "fast-get from fast-map")
  (let [m (sut/fast-map {:a 1 :b 2 :c 3 :d 4})]
    (cc/quick-bench
     (sut/fast-get m :c))) ;; 15.341296 ns

  )

;;; MERGE

(defn bench-merge*
  [n m1 & [m2 m3 m4]]
  (case n
    1 (cc/quick-benchmark (merge m1) nil)
    2 (cc/quick-benchmark (merge m1 m2) nil)
    3 (cc/quick-benchmark (merge m1 m2 m3) nil)
    4 (cc/quick-benchmark (merge m1 m2 m3 m4) nil)
    ))

(defn bench-inline-merge*
  [n m1 & [m2 m3 m4]]
  (case n
    1 (cc/quick-benchmark (sut/inline-merge m1) nil)
    2 (cc/quick-benchmark (sut/inline-merge m1 m2) nil)
    3 (cc/quick-benchmark (sut/inline-merge m1 m2 m3) nil)
    4 (cc/quick-benchmark (sut/inline-merge m1 m2 m3 m4) nil)
    ))

(defn bench-inline-fast-map-merge*
  [n m1 & [m2 m3 m4]]
  (case n
    1 (cc/quick-benchmark (sut/inline-fast-map-merge m1) nil)
    2 (cc/quick-benchmark (sut/inline-fast-map-merge m1 m2) nil)
    3 (cc/quick-benchmark (sut/inline-fast-map-merge m1 m2 m3) nil)
    4 (cc/quick-benchmark (sut/inline-fast-map-merge m1 m2 m3 m4) nil)
    ))

(defn bench-inline-tmerge*
  [n m1 & [m2 m3 m4]]
  (case n
    1 (cc/quick-benchmark (sut/inline-tmerge m1) nil)
    2 (cc/quick-benchmark (sut/inline-tmerge m1 m2) nil)
    3 (cc/quick-benchmark (sut/inline-tmerge m1 m2 m3) nil)
    4 (cc/quick-benchmark (sut/inline-tmerge m1 m2 m3 m4) nil)
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
         :let [width (int (Math/pow 10 e))
               ms (repeatedly n #(randmap width))]
         k [:merge :inline-merge :inline-fast-map-merge :inline-tmerge]
         :let [f (merge-fns k)
               res (apply f n ms)
               mn (->ns (mean res))
               ratio (int (/ mn n))]]
     {:bench k
      :mean mn
      :width e
      :ratio ratio
      :depth n
      :heap @max-memory
      :gc @gcs})))

(defn bench-merge
  []
  (title "merge vs. fast-map-merge")

  (title "merge maps")
  (let [m {:a 1 :b 2 :c 3 :d 4}
        n {:x 1 :y 2 :z 3 :u 4}]
    (cc/quick-bench
     (merge m n))) ;; 572.398767 ns

  (title "fast merge maps")
  (let [m {:a 1 :b 2 :c 3 :d 4}
        n {:x 1 :y 2 :z 3 :u 4}]
    (cc/quick-bench
     (sut/fast-map-merge m n))) ;; 1.025504 µs

  (title "merge vs. inline merge")

  (title "merge 2 maps")
  (let [m {:a 1 :b 2 :c 3 :d 4}
        n {:x 1 :y 2 :z 3 :u 4}]
    (cc/quick-bench
     (merge m n)))

  (title "inline merge 2 maps")
  (let [m {:a 1 :b 2 :c 3 :d 4}
        n {:x 1 :y 2 :z 3 :u 4}]
    (cc/quick-bench
     (sut/inline-merge m n)))

  (title "inline fast merge 2 maps")
  (let [m {:a 1 :b 2 :c 3 :d 4}
        n {:x 1 :y 2 :z 3 :u 4}]
    (cc/quick-bench
     (sut/inline-fast-map-merge m n)))

  (title "merge 3 maps")
  (let [m {:a 1 :b 2 :c 3 :d 4}
        n {:x 1 :y 2 :z 3 :u 4}
        l {:u 1 :v 2 :w 3 :z 3}]
    (cc/quick-bench
     (merge m n l)))

  (title "inline merge 3 maps")
  (let [m {:a 1 :b 2 :c 3 :d 4}
        n {:x 1 :y 2 :z 3 :u 4}
        l {:u 1 :v 2 :w 3 :z 3}]
    (cc/quick-bench
     (sut/inline-merge m n l)))

  (title "inline fast merge 3 maps")
  (let [m {:a 1 :b 2 :c 3 :d 4}
        n {:x 1 :y 2 :z 3 :u 4}
        l {:u 1 :v 2 :w 3 :z 3}]
    (cc/quick-bench
     (sut/inline-fast-map-merge m n l)))

  (title "merge 4 maps")
  (let [m {:a 1 :b 2 :c 3 :d 4}
        n {:x 1 :y 2 :z 3 :u 4}
        l {:u 1 :v 2 :w 3 :z 3}
        o {:a 9 :y 8 :z 3 :u 4}]
    (cc/quick-bench
     (merge m n l o)))

  (title "inline merge 4 maps")
  (let [m {:a 1 :b 2 :c 3 :d 4}
        n {:x 1 :y 2 :z 3 :u 4}
        l {:u 1 :v 2 :w 3 :z 3}
        o {:a 9 :y 8 :z 3 :u 4}]
    (cc/quick-bench
     (sut/inline-merge m n l o)))

  (title "inline fast merge 4 maps")
  (let [m {:a 1 :b 2 :c 3 :d 4}
        n {:x 1 :y 2 :z 3 :u 4}
        l {:u 1 :v 2 :w 3 :z 3}
        o {:a 9 :y 8 :z 3 :u 4}]
    (cc/quick-bench
     (sut/inline-fast-map-merge m n l o)))


  )

;;; GET-IN

(defn bench-get-in*
  [m n]
  (case n
    1 (cc/quick-benchmark (get-in m [3]) nil)
    2 (cc/quick-benchmark (get-in m [3 3]) nil)
    3 (cc/quick-benchmark (get-in m [3 3 3]) nil)
    4 (cc/quick-benchmark (get-in m [3 3 3 3]) nil)))

(defn bench-inline-get-in*
  [m n]
  (case n
    1 (cc/quick-benchmark (sut/inline-get-in m [3]) nil)
    2 (cc/quick-benchmark (sut/inline-get-in m [3 3]) nil)
    3 (cc/quick-benchmark (sut/inline-get-in m [3 3 3]) nil)
    4 (cc/quick-benchmark (sut/inline-get-in m [3 3 3 3]) nil)))

(defn bench-inline-get-some-in*
  [m n]
  (case n
    1 (cc/quick-benchmark (sut/inline-get-some-in m [3]) nil)
    2 (cc/quick-benchmark (sut/inline-get-some-in m [3 3]) nil)
    3 (cc/quick-benchmark (sut/inline-get-some-in m [3 3 3]) nil)
    4 (cc/quick-benchmark (sut/inline-get-some-in m [3 3 3 3]) nil)))

(def get-in-bench-fns
  {:get-in bench-get-in*
   :inline-get-in bench-inline-get-in*
   :inline-get-some-in bench-inline-get-some-in*})

(defn bench-get-in-
  [max-log-size max-depth]
  (vec
   (for [e (range 1 (inc max-log-size))
         depth (range 1 (inc max-depth))
         :let [width (int (Math/pow 10 e))
               m (mbuild-nested-map width depth)]
         k [:get-in :inline-get-in :inline-get-some-in]
         :let [f (get-in-bench-fns k)
               res (f m depth)
               mn (->ns (mean res))
               ratio (int (/ mn depth))]]
     {:bench k
      :mean mn
      :width e
      :ratio ratio
      :depth depth
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
     (sut/inline-get-in m [:d])))

  (title "get some in 1")
  (let [m {:a 1}]
    (cc/quick-bench
     (sut/inline-get-some-in m [:a])))

  (title "get-in 2")
  (let [m {:c {:d 1}}]
    (cc/quick-bench
     (get-in m [:c :d])))

  (title "fast get-in 2")
  (let [m {:c {:d 1}}]
    (cc/quick-bench
     (sut/inline-get-in m [:c :d])))

  (title "get some in 2")
  (let [m {:a {:b 1}}]
    (cc/quick-bench
     (sut/inline-get-some-in m [:a :b])))

  (title "get-in 3")
  (let [m {:b {:c {:d 1}}}]
    (cc/quick-bench
     (get-in m [:b :c :d])))

  (title "fast get-in 3")
  (let [m {:b {:c {:d 1}}}]
    (cc/quick-bench
     (sut/inline-get-in m [:b :c :d])))

  (title "get some in 3")
  (let [m {:a {:b {:c 1}}}]
    (cc/quick-bench
     (sut/inline-get-some-in m [:a :b :c])))

  (title "get-in 4")
  (let [m {:a {:b {:c {:d 1}}}}]
    (cc/quick-bench
     (get-in m [:a :b :c :d]))) ;; 195.077302 ns

  (title "fast get-in 4")
  (let [m {:a {:b {:c {:d 1}}}}]
    (cc/quick-bench
     (sut/inline-get-in m [:a :b :c :d]))) ;; 37.911144 ns

  (title "get some in 4")
  (let [m {:a {:b {:c {:d 1}}}}]
    (cc/quick-bench
     (sut/inline-get-some-in m [:a :b :c :d])))

  )

;;; SELECT-KEYS

(defn bench-select-keys*
  [m n]
  (case n
    1 (cc/quick-benchmark (select-keys m [3]) nil)
    2 (cc/quick-benchmark (select-keys m [3 4]) nil)
    3 (cc/quick-benchmark (select-keys m [3 4 5]) nil)
    4 (cc/quick-benchmark (select-keys m [3 4 5 6]) nil)))

(defn bench-inline-select-keys*
  [m n]
  (case n
    1 (cc/quick-benchmark (sut/inline-select-keys m [3]) nil)
    2 (cc/quick-benchmark (sut/inline-select-keys m [3 4]) nil)
    3 (cc/quick-benchmark (sut/inline-select-keys m [3 4 5]) nil)
    4 (cc/quick-benchmark (sut/inline-select-keys m [3 4 5 6]) nil)))

(def select-keys-bench-fns
  {:select-keys bench-select-keys*
   :inline-select-keys bench-inline-select-keys*})

(defn bench-select-keys-
  [max-log-size max-width]
  (vec
   (for [e (range 1 (inc max-log-size))
         n (range 1 (inc max-width))
         :let [width (int (Math/pow 10 e))
               m (mbuild-map width)]
         k [:select-keys :inline-select-keys]
         :let [f (select-keys-bench-fns k)
               res (f m n)
               mn (Math/round (/ (mean res) 1e-9))
               ratio (int (/ mn n))]]
     {:bench k
      :mean mn
      :keys n
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
     (sut/inline-select-keys m [:a])))

  (title "select 2/4 keys")
  (let [m {:a 1 :b 2 :c 3 :d 4}]
    (cc/quick-bench
     (select-keys m [:a :b])))

  (title "fast select 2/4 keys")
  (let [m {:a 1 :b 2 :c 3 :d 4}]
    (cc/quick-bench
     (sut/inline-select-keys m [:a :b])))

  (title "select 3/4 keys")
  (let [m {:a 1 :b 2 :c 3 :d 4}]
    (cc/quick-bench
     (select-keys m [:a :b :c])))

  (title "fast select 3/4 keys")
  (let [m {:a 1 :b 2 :c 3 :d 4}]
    (cc/quick-bench
     (sut/inline-select-keys m [:a :b :c])))

  (title "select 4/4 keys")
  (let [m {:a 1 :b 2 :c 3 :d 4}]
    (cc/quick-bench
     (select-keys m [:a :b :c :d])))

  (title "fast select 4/4 keys")
  (let [m {:a 1 :b 2 :c 3 :d 4}]
    (cc/quick-bench
     (sut/inline-select-keys m [:a :b :c :d])))

  )

;;; ASSOC-IN

(defn bench-assoc-in*
  [m n]
  (case n
    1 (cc/quick-benchmark (assoc-in m [3] 0) nil)
    2 (cc/quick-benchmark (assoc-in m [3 4] 0) nil)
    3 (cc/quick-benchmark (assoc-in m [3 4 5] 0) nil)
    4 (cc/quick-benchmark (assoc-in m [3 4 5 6] 0) nil)))

(defn bench-inline-assoc-in*
  [m n]
  (case n
    1 (cc/quick-benchmark (sut/inline-assoc-in m [3] 0) nil)
    2 (cc/quick-benchmark (sut/inline-assoc-in m [3 4] 0) nil)
    3 (cc/quick-benchmark (sut/inline-assoc-in m [3 4 5] 0) nil)
    4 (cc/quick-benchmark (sut/inline-assoc-in m [3 4 5 6] 0) nil)))

(def assoc-in-bench-fns
  {:assoc-in bench-assoc-in*
   :inline-assoc-in bench-inline-assoc-in*})

(defn bench-assoc-in-
  [max-log-size max-depth]
  (vec
   (for [e (range 1 (inc max-log-size))
         depth (range 1 (inc max-depth))
         :let [width (int (Math/pow 10 e))
               m (mbuild-nested-map width depth)]
         k [:assoc-in :inline-assoc-in]
         :let [f (assoc-in-bench-fns k)
               res (f m depth)
               mn (->ns (mean res))
               ratio (int (/ mn depth))]]
     {:bench k
      :mean mn
      :depth depth
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
  (criterium.core/quick-bench (sut/inline-assoc-in {} [1] 2))
  (title "inline-assoc-in 2")
  (criterium.core/quick-bench (sut/inline-assoc-in {} [1 2] 3))
  (title "inline-assoc-in 3")
  (criterium.core/quick-bench (sut/inline-assoc-in {} [1 2 3] 4))
  (title "inline-assoc-in 4")
  (criterium.core/quick-bench (sut/inline-assoc-in {} [1 2 3 4] 5))

  )

;;; UPDATE-IN

(defn bench-update-in*
  [m n]
  (case n
    1 (cc/quick-benchmark (update-in m [3] identity) nil)
    2 (cc/quick-benchmark (update-in m [3 4] identity) nil)
    3 (cc/quick-benchmark (update-in m [3 4 5] identity) nil)
    4 (cc/quick-benchmark (update-in m [3 4 5 6] identity) nil)))

(defn bench-inline-update-in*
  [m n]
  (case n
    1 (cc/quick-benchmark (sut/inline-update-in m [3] identity) nil)
    2 (cc/quick-benchmark (sut/inline-update-in m [3 4] identity) nil)
    3 (cc/quick-benchmark (sut/inline-update-in m [3 4 5] identity) nil)
    4 (cc/quick-benchmark (sut/inline-update-in m [3 4 5 6] identity) nil)))

(def update-in-bench-fns
  {:update-in bench-update-in*
   :inline-update-in bench-inline-update-in*})

(defn bench-update-in-
  [max-log-size max-depth]
  (vec
   (for [e (range 1 (inc max-log-size))
         depth (range 1 (inc max-depth))
         :let [width (int (Math/pow 10 e))
               m (mbuild-nested-map width depth)]
         k [:update-in :inline-update-in]
         :let [f (update-in-bench-fns k)
               res (f m depth)
               mn (->ns (mean res))
               ratio (int (/ mn depth))]]
     {:bench k
      :mean mn
      :depth depth
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
    (cc/quick-bench (sut/inline-update-in m [:a] identity)))

  (let [m {:a {:b 1}}]
    (title "update-in 2")
    (cc/quick-bench (update-in m [:a :b] identity))
    (title "inline-update-in 2")
    (cc/quick-bench (sut/inline-update-in m [:a :b] identity)))

  (let [m {:a {:b {:c 1}}}]
    (title "update-in 3")
    (cc/quick-bench (update-in m [:a :b :c] identity))
    (title "inline-update-in 3")
    (cc/quick-bench (sut/inline-update-in m [:a :b :c] identity)))

  (let [m {:a {:b {:c {:d 1}}}}]
    (title "update-in 4")
    (cc/quick-bench (update-in m [:a :b :c :d] identity))
    (title "inline-update-in 4")
    (cc/quick-bench (sut/inline-update-in m [:a :b :c :d] identity)))

  )

(def cli-options
  [["-n" "--name NAME" "Benchmarks nickname"
    :default ""]
   ["-o" "--out-path" "Output path"
    :default "./benchmarks"]
   ["-w" "--max-width WIDTH" "Logarithmic maximum width"
    :default 4
    :parse-fn #(Integer/parseInt %)
    :validate [#(<= 1 % 6) "Please Keep max width up to 6"]]
   ["-d" "--max-depth DEPTH" "Logarithmic maximum width"
    :default 4
    :parse-fn #(Integer/parseInt %)
    :validate [#(<= 1 % 4) "Please Keep max width up to 4"]]
   ["-h" "--help"]])

(def benches
  {
   :get-in bench-get-in-
   :assoc bench-assoc-
   :assoc-rec bench-assoc-rec-
   :merge :bench-merge-
   :select-keys bench-select-keys-
   :assoc-in bench-assoc-in-
   :update-in bench-update-in-
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
  (let [{:keys [arguments options]}
        (cli/parse-opts args cli-options)
        {:keys [out-path max-width max-depth name]} options
        ks (map keyword arguments)
        now
        (clojure.string/replace
         (str (java.time.LocalDateTime/now)) #":" "-")
        parts (remove nil? [name "clj-fast" now "bench.edn"])
        output
        (clojure.string/join
         "/"
         [out-path
          (clojure.string/join "-" parts)])]
    (validate-args ks)
    (validate-out out-path)
    (println "Producing report to" output)
    (let [results
          (reduce
           (fn [m k]
             (println 'INFO 'benching k)
             (assoc m k ((benches k) max-width max-depth))) {} ks)]
      (spit output results)))
  #_#_
  (bench-assoc)
  (bench-assoc-in)
  #_
  (bench-update-in)
  #_#_
  (bench-get)
  (bench-merge)
  #_#_
  (bench-get-in)
  (bench-select-keys)
  )
