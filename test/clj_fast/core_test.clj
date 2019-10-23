(ns clj-fast.core-test
  (:require [clojure.test :as t]
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

;;; Fast assoc

(let [m {:a 1 :b 2 :c 3 :d 4}]
  (cc/quick-bench
   (sut/fast-assoc m :x "y"))) ;; 34.418271 ns, 62.417304 ns

(let [m {:a 1 :b 2 :c 3 :d 4}]
  (cc/bench
   (sut/fast-assoc m :x "y"))) ;; 31.409151 ns

(let [m (->Foo 1 2 3 4)]
  (cc/quick-bench
   (sut/fast-assoc m :x "y"))) ;; 119 - 123.171412 ns

(let [m (->Foo 1 2 3 4)]
  (cc/bench
   (sut/fast-assoc m :x "y"))) ;; 105.788254 ns

(let [m {:a 1 :b 2 :c 3 :d 4}]
  (cc/quick-bench
   (assoc m :x "y"))) ;; 44.996516 ns

(let [m {:a 1 :b 2 :c 3 :d 4}]
  (cc/bench
   (assoc m :x "y"))) ;; 42.187467 ns

(let [m (->Foo 1 2 3 4)]
  (cc/quick-bench
   (assoc m :x "y"))) ;; 121.102786 ns

;;; Fast Get

(let [m (sut/fast-map {:a 1 :b 2 :c 3 :d 4})]
  (cc/quick-bench
   (sut/fast-get m :c))) ;; 17.998342 ns

(let [m (sut/fast-map {:a 1 :b 2 :c 3 :d 4})]
  (cc/bench
   (sut/fast-get m :c))) ;; 15.341296 ns

(let [m (sut/fast-map {:a 1 :b 2 :c 3 :d 4})]
  (cc/quick-bench
   (get m :c))) ;; 45.384442 ns

(let [m (sut/fast-map {:a 1 :b 2 :c 3 :d 4})]
  (cc/bench
   (get m :c))) ;; 38.447998 ns

(let [m {:a 1 :b 2 :c 3 :d 4}]
  (cc/quick-bench
   (get m :c))) ;; 24.625179 ns

(let [m (->Foo 1 2 3 4)]
  (cc/quick-bench
   (get m :c))) ;; 15.707654 ns

;;; Fast Merge

(let [m {:a 1 :b 2 :c 3 :d 4}
      n {:x 1 :y 2 :z 3 :u 4}]
  (cc/quick-bench
   (merge m n))) ;; 1.095517 µs

(let [m {:a 1 :b 2 :c 3 :d 4}
      n {:x 1 :y 2 :z 3 :u 4}]
  (cc/bench
   (merge m n))) ;; 572.398767 ns

(let [m {:a 1 :b 2 :c 3 :d 4}
      n {:x 1 :y 2 :z 3 :u 4}]
  (cc/quick-bench
   (sut/fast-map-merge m n))) ;; 1.025504 µs

(let [m {:a 1 :b 2 :c 3 :d 4}
      n {:x 1 :y 2 :z 3 :u 4}]
  (cc/bench
   (sut/fast-map-merge m n))) ;; 541.253515

(let [m {:a 1 :b 2 :c 3 :d 4}]
  (cc/quick-bench
   (-> m
       (sut/fast-assoc :x 1)
       (sut/fast-assoc :y 2)
       (sut/fast-assoc :z 3)
       (sut/fast-assoc :u 4)))) ;; 291.939426 ns

;;; Fast Get-in

(let [m {:a {:b {:c {:d 1}}}}]
  (cc/quick-bench
   (get-in m [:a :b :c :d]))) ;; 243.855034 ns

(let [m {:a {:b {:c {:d 1}}}}]
  (cc/bench
   (get-in m [:a :b :c :d]))) ;; 195.077302 ns

(let [m {:a {:b {:c {:d 1}}}}]
  (cc/quick-bench
   (-> m :a :b :c :d))) ;; 110.540735 ns


(let [m {:a {:b {:c {:d 1}}}}]
  (cc/quick-bench
   (sut/fast-get-in-th m [:a :b :c :d]))) ;; 117.304192 ns

(let [m {:a {:b {:c {:d 1}}}}]
  (cc/quick-bench
   (sut/fast-get-in-inline m [:a :b :c :d]))) ;; 54.218510 ns

(let [m {:a {:b {:c {:d 1}}}}]
  (cc/bench
   (sut/fast-get-in-inline m [:a :b :c :d]))) ;; 37.911144 ns

;;; Select Keys

(let [m {:a 1 :b 2 :c 3 :d 4}
      ks [:a :b :c]]
  (cc/quick-bench
   (select-keys m [:a :b :c]))) ;; 962.497057 ns

(let [m {:a 1 :b 2 :c 3 :d 4}
      ks [:a :b :c]]
  (cc/quick-bench
   (reduce (fn [t k] (assoc t k (get m k))) {} ks))) ;; 594.369400 ns

(let [m {:a 1 :b 2 :c 3 :d 4}
      ks [:a :b :c]]
  (cc/quick-bench
   (reduce (fn [t k] (if-some [v (get m k)]
                      (assoc t k v)
                      t))
           {}
           ks))) ;; 594.108635 ns

(let [m {:a 1 :b 2 :c 3 :d 4}]
  (cc/quick-bench
   (let [a (get m :a)
         b (get m :b)
         c (get m :c)]
     {:a a :b b :c c}))) ;; 83.125434 ns

(let [m {:a 1 :b 2 :c 3 :d 4}]
  (cc/quick-bench
   (let [a (get m :a)
         b (get m :b)
         c (get m :c)]
     (hash-map :a a :b b :c c)))) ;; 933.370648 ns

(let [m {:a 1 :b 2 :c 3 :d 4}]
  (cc/quick-bench
   (sut/fast-select-keys-inline m [:a :b :c]))) ;; 75.913866 ns

(let [m {:a 1 :b 2 :c 3 :d 4}]
  (cc/bench
   (sut/fast-select-keys-inline m [:a :b :c]))) ;; 60.196715 ns
;; 51~52 ns

(let [m {:a 1 :b 2 :c 3 :d 4}]
  (cc/quick-bench
   (sut/defrec->inline-select-keys m [:a :b :c])))

(let [m {:a 1 :b 2 :c 3 :d 4}]
  (cc/bench
   (sut/defrec->inline-select-keys m [:a :b :c]))) ;; 48.355857 ns
