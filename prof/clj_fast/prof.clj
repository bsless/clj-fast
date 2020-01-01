(ns clj-fast.prof
  (:gen-class)
  (:require
   [clj-async-profiler.core :as prof]))

(def cnt (atom 1))
(defn incme [] (swap! cnt inc))

(defmacro do-prof
  ([body]
   `(do-prof ~body 1e9))
  ([body times]
   `(do
      (incme)
      (prof/profile
       (dotimes [_# ~times]
         ~body)) )))

(defrecord Foo [a b c d])

(defn -main []
  (prof/clear-results)
  (let [m {:a 1 :b 2 :c 3 :d 4 "x" "y"}
        foo (->Foo 1 2 3 4)]
    (println @cnt "get keyword from map")
    (do-prof (get m :c))
    (println @cnt "map on keyword")
    (do-prof (m :c))
    (println @cnt "keyword on map")
    (do-prof (:c m))

    (println @cnt "get string from map")
    (do-prof (get m "x"))
    (println @cnt "map on string")
    (do-prof (m "x"))

    (println @cnt "get keyword from rec")
    (do-prof (get foo :c))
    (println @cnt ".get keyword from rec")
    (do-prof (.get ^Foo foo :c))
    (println @cnt "keyword on rec")
    (do-prof (:c foo))
    (println @cnt ".field on rec")
    (do-prof (.c ^Foo foo))

    )

  (let [kws [:a :b :c]
        ks ["a" "b" "c"]
        m {kws 1 ks 2}]
    (println @cnt "get vector of keywords from map")
    (do-prof (get m kws) 1e8)
    (println @cnt "map on keywords vector")
    (do-prof (m kws) 1e8)

    (println @cnt "get strings vector from map")
    (do-prof (get m ks) 1e8)
    (println @cnt "map on strings vector")
    (do-prof (m ks) 1e8)

    )

)
