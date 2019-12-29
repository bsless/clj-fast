(ns clj-fast.prof
  (:gen-class)
  (:require
   [clj-async-profiler.core :as prof]))

(defmacro do-prof
  [body]
  `(prof/profile
    (dotimes [_# 1e9]
      ~body)))

(defrecord Foo [a b c d])

(defn -main []
  (prof/clear-results)
  (let [m {:a 1 :b 2 :c 3 :d 4}
        foo (->Foo 1 2 3 4)]
    (println "1 get keyword from map")
    (do-prof (get m :c))
    (println "2 map on keyword")
    (do-prof (m :c))
    (println "3 keyword on map")
    (do-prof (:c m))

    (println "4 get keyword from rec")
    (do-prof (get foo :c))
    (println "5 .get keyword from rec")
    (do-prof (.get ^Foo foo :c))
    (println "6 keyword on rec")
    (do-prof (:c foo))
    (println "7 .field on rec")
    (do-prof (.c ^Foo foo))
    )
)
