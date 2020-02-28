(ns clj-fast.collections.map-test
  (:refer-clojure :exclude [memoize])
  (:require [clj-fast.collections.map :as sut]
            [clojure.test :as t]))

(t/deftest memoize
  (let [f0 conj
        f1 inc
        f2 conj]
    (t/testing "memoize"
      (let [f0* (sut/memoize f0)
            f1* (sut/memoize f1)
            f2* (sut/memoize f2)]
        (t/is (= [] (f0*)))
        (t/is (= [] (f0*)))
        (t/is (= 1 (f1* 0)))
        (t/is (= 1 (f1* 0)))
        (t/is (= [1] (f2* [] 1)))
        (t/is (= [1] (f2* [] 1)))))
    (t/testing "memoize nested hashmap"
      (let [f0* (sut/memoize* 0 f0)
            f1* (sut/memoize* 1 f1)
            f2* (sut/memoize* 2 f2)]
        (t/is (= [] (f0*)))
        (t/is (= [] (f0*)))
        (t/is (= 1 (f1* 0)))
        (t/is (= 1 (f1* 0)))
        (t/is (= [1] (f2* [] 1)))
        (t/is (= [1] (f2* [] 1)))))))
