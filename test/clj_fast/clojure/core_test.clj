;;; Copyright (c) Rich Hickey. All rights reserved.
;;; The use and distribution terms for this software are covered by the
;;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;; which can be found in the file epl-v10.html at the root of this distribution.
;;; By using this software in any fashion, you are agreeing to be bound by
;;; the terms of this license.
;;; You must not remove this notice, or any other, from this software.
;;
;;; Author: Frantisek Sodomka

(ns clj-fast.clojure.core-test
  (:require [clj-fast.clojure.core :as sut]
            [clojure.test :as t]))

(t/deftest test-get
  (let [m {:a 1, :b 2, :c {:d 3, :e 4}, :f nil, :g false, nil {:h 5}}]
    (t/is (thrown? IllegalArgumentException (get-in {:a 1} 5)))
    (t/are [x y] (= x y)
      (sut/get m :a) 1
      (sut/get m :e) nil
      (sut/get m :e 0) 0
      (sut/get m nil) {:h 5}
      (sut/get m :b 0) 2
      (sut/get m :f 0) nil

      (sut/get-in m [:c :e]) 4
      (sut/get-in m '(:c :e)) 4
      (sut/get-in m '[:c :e]) 4
      (sut/get-in m [:c :x]) nil
      (sut/get-in m [:f]) nil
      (sut/get-in m [:g]) false
      (sut/get-in m [:h]) nil
      (sut/get-in m []) m
      (sut/get-in m nil) m

      (sut/get-in m [:c :e] 0) 4
      (sut/get-in m '(:c :e) 0) 4
      (sut/get-in m [:c :x] 0) 0
      (sut/get-in m [:b] 0) 2
      (sut/get-in m [:f] 0) nil
      (sut/get-in m [:g] 0) false
      (sut/get-in m [:h] 0) 0
      (sut/get-in m [:x :y] {:y 1}) {:y 1}
      (sut/get-in m [] 0) m
      (sut/get-in m nil 0) m)))

(t/deftest test-update
  (t/are [result expr] (= result expr)
    {:a [1 2]}   (update {:a [1]} :a conj 2)
    [1]          (update [0] 0 inc)
    ;; higher-order usage
    {:a {:b 2}}  (sut/update-in {:a {:b 1}} [:a] update :b inc)
    ;; missing field = nil
    {:a 1 :b nil} (update {:a 1} :b identity)
    ;; 4 hard-coded arities
    {:a 1} (update {:a 1} :a +)
    {:a 2} (update {:a 1} :a + 1)
    {:a 3} (update {:a 1} :a + 1 1)
    {:a 4} (update {:a 1} :a + 1 1 1)
    ;; rest arity
    {:a 5} (update {:a 1} :a + 1 1 1 1)
    {:a 6} (update {:a 1} :a + 1 1 1 1 1)))

(t/deftest test-assoc
  (t/testing "assoc"
    (t/is (= {:a 1} (sut/assoc {} :a 1)))
    (t/is (= {:a 1 :b 2} (sut/assoc {} :a 1 :b 2)))))

(t/deftest test-merge
  (let [m1 {:a 1} m2 {:b 2} m3 {:a 3}]
    (t/testing "merge"
      (t/is (= m1 (sut/merge m1)))
      (t/is (= {:a 1 :b 2} (sut/merge m1 m2)))
      (t/is (= {:a 3 :b 2} (sut/merge m1 m2 m3))))
    (t/testing "static merge"
      (t/is (= {:a 3 :b 2} (sut/merge m1 {:b 2} {:a 3}))))))

(t/deftest test-get-in
  (let [m {:a {:b 1}}]
    (t/testing "get-in"
      (t/is (nil? (sut/get-in m [:c])))
      (t/is (nil? (sut/get-in m [:c :d])))
      (t/is (= {:b 1} (sut/get-in m [:a])))
      (t/is (= 1 (sut/get-in m [:a :b])))
      (t/is (= 1 (sut/get-in m '(:a :b)))))
    ))

(t/deftest test-select-keys
  (let [m {:a 1 :b 2}]
    (t/testing "select-keys"
      (t/is (= {} (sut/select-keys m [])))
      (t/is (= {:a 1} (sut/select-keys m [:a])))
      (t/is (= {:a 1 :b 2} (sut/select-keys m [:a :b])))
      (t/is (= {:a 1} (sut/select-keys m [:a :c]))))))

(t/deftest test-fast-select-keys
  (let [m {:a 1 :b 2}]
    (t/testing "select-keys"
      (t/is (= {} (sut/select-keys m [])))
      (t/is (= {:a 1} (sut/select-keys m [:a])))
      (t/is (= {:a 1 :b 2} (sut/select-keys m [:a :b])))
      #_(t/is (= {:a 1 :c nil} (sut/select-keys m [:a :c]))))))

(t/deftest test-assoc-in
  (let [m {:a {:b 1}}]
    (t/is (= {:a {:b 2}} (sut/assoc-in m [:a :b] 2)))
    (t/is (= {:a 2} (sut/assoc-in m [:a] 2)))
    (t/is (= {:a {:b 1 4 2}} (sut/assoc-in m [:a (inc 3)] 2)))))

(t/deftest test-update-in
  (let [m {:a {:b 1}}]
    (t/is (= {:a {:b 2}} (sut/update-in m [:a :b] + 1)))
    (t/is (= {:a {:b 1}
              :c {:d true}} (sut/update-in m [:c :d] not))))
  (t/testing "Variadic arity"
    (let [m {:a 1}
          ks [:a]]
      (t/are [result expr] (= result expr)
        {:a 1} (sut/update-in m ks +)
        {:a 2} (sut/update-in m ks + 1)
        {:a 3} (sut/update-in m ks + 1 1)
        {:a 4} (sut/update-in m ks + 1 1 1)
        {:a 5} (sut/update-in m ks + 1 1 1 1)))))
