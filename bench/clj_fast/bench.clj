(ns clj-fast.bench
  (:require
   [clj-fast.core :as sut]
   [criterium.core :as cc]))

(defrecord Foo [a b c d])

;; Credit metosin
(defn title [s]
  (println
   (str "\n\u001B[35m"
        (apply str (repeat (+ 6 (count s)) "#"))
        "\n## " s " ##\n"
        (apply str (repeat (+ 6 (count s)) "#"))
        "\u001B[0m\n")))

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
     (sut/fast-select-keys-inline m [:a])))

  #_#_
  (title "fast record select 1/4 keys")
  (let [m {:a 1 :b 2 :c 3 :d 4}]
    (cc/quick-bench
     (sut/defrec->inline-select-keys m [:a])))

  (title "select 2/4 keys")
  (let [m {:a 1 :b 2 :c 3 :d 4}]
    (cc/quick-bench
     (select-keys m [:a :b])))

  (title "fast select 2/4 keys")
  (let [m {:a 1 :b 2 :c 3 :d 4}]
    (cc/quick-bench
     (sut/fast-select-keys-inline m [:a :b])))

  #_#_
  (title "fast record select 2/4 keys")
  (let [m {:a 1 :b 2 :c 3 :d 4}]
    (cc/quick-bench
     (sut/defrec->inline-select-keys m [:a :b])))

  (title "select 3/4 keys")
  (let [m {:a 1 :b 2 :c 3 :d 4}]
    (cc/quick-bench
     (select-keys m [:a :b :c])))

  (title "fast select 3/4 keys")
  (let [m {:a 1 :b 2 :c 3 :d 4}]
    (cc/quick-bench
     (sut/fast-select-keys-inline m [:a :b :c])))

  #_#_
  (title "fast record select 3/4 keys")
  (let [m {:a 1 :b 2 :c 3 :d 4}]
    (cc/quick-bench
     (sut/defrec->inline-select-keys m [:a :b :c])))

  (title "select 4/4 keys")
  (let [m {:a 1 :b 2 :c 3 :d 4}]
    (cc/quick-bench
     (select-keys m [:a :b :c :d])))

  (title "fast select 4/4 keys")
  (let [m {:a 1 :b 2 :c 3 :d 4}]
    (cc/quick-bench
     (sut/fast-select-keys-inline m [:a :b :c :d])))

  #_#_
  (title "fast record select 4/4 keys")
  (let [m {:a 1 :b 2 :c 3 :d 4}]
    (cc/quick-bench
     (sut/defrec->inline-select-keys m [:a :b :c :d])))

  )

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

(defn -main
  []
  (bench-assoc)
  (bench-assoc-in)
  (bench-get)
  (bench-merge)
  (bench-get-in)
  (bench-select-keys)
  )
