(ns clj-fast.inline-test
  (:refer-clojure
   :exclude
   [assoc merge get-in select-keys assoc-in update-in memoize])
  (:require
   [clj-fast.inline :as sut]
   [clojure.test :as t]))

(t/deftest assoc
  (t/testing "assoc"
    (t/is (= {:a 1} (sut/assoc {} :a 1)))
    (t/is (= {:a 1 :b 2} (sut/assoc {} :a 1 :b 2))))
  (t/testing "fast-assoc"
    (t/is (= {:a 1} (sut/fast-assoc {} :a 1)))
    (t/is (= {:a 1 :b 2} (sut/fast-assoc {} :a 1 :b 2)))))

(t/deftest merge
  (let [m1 {:a 1} m2 {:b 2} m3 {:a 3}]
    (t/testing "merge"
      (t/is (= m1 (sut/merge m1)))
      (t/is (= {:a 1 :b 2} (sut/merge m1 m2)))
      (t/is (= {:a 3 :b 2} (sut/merge m1 m2 m3))))
    (t/testing "fast-map-merge"
      (t/is (= m1 (sut/fast-map-merge m1)))
      (t/is (= {:a 1 :b 2} (sut/fast-map-merge m1 m2)))
      (t/is (= {:a 3 :b 2} (sut/fast-map-merge m1 m2 m3))))
    (t/testing "tmerge"
      (t/is (= m1 (sut/tmerge m1)))
      (t/is (= {:a 1 :b 2} (sut/tmerge m1 m2)))
      (t/is (= {:a 3 :b 2} (sut/tmerge m1 m2 m3))))))

(t/deftest get-in
  (let [m {:a {:b 1}}]
    (t/testing "get-in"
      (t/is (nil? (sut/get-in m [:c])))
      (t/is (nil? (sut/get-in m [:c :d])))
      (t/is (= {:b 1} (sut/get-in m [:a])))
      (t/is (= 1 (sut/get-in m [:a :b]))))
    (t/testing "get-some-in"
      (t/is (nil? (sut/get-some-in m [:c])))
      (t/is (nil? (sut/get-some-in m [:c :d])))
      (t/is (= {:b 1} (sut/get-some-in m [:a])))
      (t/is (= 1 (sut/get-some-in m [:a :b]))))))

(t/deftest select-keys
  (let [m {:a 1 :b 2}]
    (t/testing "select-keys"
      (t/is (= {} (sut/select-keys m [])))
      (t/is (= {:a 1} (sut/select-keys m [:a])))
      (t/is (= {:a 1 :b 2} (sut/select-keys m [:a :b])))
      (t/is (= {:a 1 :c nil} (sut/select-keys m [:a :c]))))))

(t/deftest assoc-in
  (let [m {:a {:b 1}}]
    (t/is (= {:a {:b 2}} (sut/assoc-in m [:a :b] 2)))
    (t/is (= {:a 2} (sut/assoc-in m [:a] 2)))
    (t/is (= {:a {:b 1 4 2}} (sut/assoc-in m [:a (inc 3)] 2)))))

(t/deftest update-in
  (let [m {:a {:b 1}}]
    (t/is (= {:a {:b 2}} (sut/update-in m [:a :b] + 1)))
    (t/is (= {:a {:b 1}
              :c {:d true}} (sut/update-in m [:c :d] not)))))

(t/deftest dissoc-in
  (let [m {:a {:b {:c 1}}}]
    (t/is (= {} (sut/dissoc-in m [:a])))
    (t/is (= {:a {}} (sut/dissoc-in m [:a :b])))
    (t/is (= {:a {:b {}}} (sut/dissoc-in m [:a :b :c])))))

(t/deftest memoize
  (let [f0 conj
        f1 inc
        f2 conj]
    (t/testing "memoize*"
      (let [f0* (sut/memoize* 0 f0)
            f1* (sut/memoize* 1 f1)
            f2* (sut/memoize* 2 f2)]
        (t/is (= [] (f0*)))
        (t/is (= [] (f0*)))
        (t/is (= 1 (f1* 0)))
        (t/is (= 1 (f1* 0)))
        (t/is (= [1] (f2* [] 1)))
        (t/is (= [1] (f2* [] 1)))))
    (t/testing "memoize*"
      (let [f0* (sut/memoize-c* 0 f0)
            f1* (sut/memoize-c* 1 f1)
            f2* (sut/memoize-c* 2 f2)]
        (t/is (= [] (f0*)))
        (t/is (= [] (f0*)))
        (t/is (= 1 (f1* 0)))
        (t/is (= 1 (f1* 0)))
        (t/is (= [1] (f2* [] 1)))
        (t/is (= [1] (f2* [] 1)))))))
