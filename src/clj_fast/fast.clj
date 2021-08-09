(ns clj-fast.fast
  (:refer-clojure :exclude [list? instance?])
  (:require
   [clj-fast.inline :as inline])
  (:import
   (bsless.clj_fast ArrayUtil)
   (java.util Collection List)
   (clojure.lang Indexed ISeq Counted PersistentVector)))

(set! *warn-on-reflection* true)

(definline instance?
  [^Class c x]
  `(.isInstance  ~c ~x))

(definline list?
  "Returns true if x implements IPersistentList"
  [x]
  `(instance? clojure.lang.IPersistentList ~x))

(defn iterable-dispatch
    ([^Iterable xs f g]
     (case (.size ^List xs)
       0 (f)
       1 (let [it (.iterator xs)] (f (.next it)))
       2 (let [it (.iterator xs)] (f (.next it) (.next it)))
       3 (let [it (.iterator xs)] (f (.next it) (.next it) (.next it)))
       4 (let [it (.iterator xs)] (f (.next it) (.next it) (.next it) (.next it)))
       (g xs)))
    ([^Iterable xs f g a]
     (case (.size ^List xs)
       0 (f a)
       1 (let [it (.iterator xs)] (f a (.next it)))
       2 (let [it (.iterator xs)] (f a (.next it) (.next it)))
       3 (let [it (.iterator xs)] (f a (.next it) (.next it) (.next it)))
       4 (let [it (.iterator xs)] (f a (.next it) (.next it) (.next it) (.next it)))
       (g a xs)))
    ([^Iterable xs f g a b]
     (case (.size ^List xs)
       0 (f a b)
       1 (let [it (.iterator xs)] (f a b (.next it)))
       2 (let [it (.iterator xs)] (f a b (.next it) (.next it)))
       3 (let [it (.iterator xs)] (f a b (.next it) (.next it) (.next it)))
       4 (let [it (.iterator xs)] (f a b (.next it) (.next it) (.next it) (.next it)))
       (g a b xs)))
    ([^Iterable xs f g a b c]
     (case (.size ^List xs)
       0 (f a b c)
       1 (let [it (.iterator xs)] (f a b c (.next it)))
       2 (let [it (.iterator xs)] (f a b c (.next it) (.next it)))
       3 (let [it (.iterator xs)] (f a b c (.next it) (.next it) (.next it)))
       4 (let [it (.iterator xs)] (f a b c (.next it) (.next it) (.next it) (.next it)))
       (g a b c xs)))
    ([^Iterable xs f g a b c d]
     (case (.size ^List xs)
       0 (f a b c d)
       1 (let [it (.iterator xs)] (f a b c d (.next it)))
       2 (let [it (.iterator xs)] (f a b c d (.next it) (.next it)))
       3 (let [it (.iterator xs)] (f a b c d (.next it) (.next it) (.next it)))
       4 (let [it (.iterator xs)] (f a b c d (.next it) (.next it) (.next it) (.next it)))
       (g a b c d xs))))

(let [i0 (int 0) i1 (int 1) i2 (int 2) i3 (int 3)]
  (defn indexed-dispatch
    ([^Indexed xs f g]
     (case (.count xs)
       0 (f)
       1 (f (.nth xs i0))
       2 (f (.nth xs i0) (.nth xs i1))
       3 (f (.nth xs i0) (.nth xs i1) (.nth xs i2))
       4 (f (.nth xs i0) (.nth xs i1) (.nth xs i2) (.nth xs i3))
       (g xs)))
    ([^Indexed xs f g a]
     (case (.count xs)
       0 (f a)
       1 (f a (.nth xs i0))
       2 (f a (.nth xs i0) (.nth xs i1))
       3 (f a (.nth xs i0) (.nth xs i1) (.nth xs i2))
       4 (f a (.nth xs i0) (.nth xs i1) (.nth xs i2) (.nth xs i3))
       (g a xs)))
    ([^Indexed xs f g a b]
     (case (.count xs)
       0 (f a b)
       1 (f a b (.nth xs i0))
       2 (f a b (.nth xs i0) (.nth xs i1))
       3 (f a b (.nth xs i0) (.nth xs i1) (.nth xs i2))
       4 (f a b (.nth xs i0) (.nth xs i1) (.nth xs i2) (.nth xs i3))
       (g a b xs)))
    ([^Indexed xs f g a b c]
     (case (.count xs)
       0 (f a b c)
       1 (f a b c (.nth xs i0))
       2 (f a b c (.nth xs i0) (.nth xs i1))
       3 (f a b c (.nth xs i0) (.nth xs i1) (.nth xs i2))
       4 (f a b c (.nth xs i0) (.nth xs i1) (.nth xs i2) (.nth xs i3))
       (g a b c xs)))
    ([^Indexed xs f g a b c d]
     (case (.count xs)
       0 (f a b c d)
       1 (f a b c d (.nth xs i0))
       2 (f a b c d (.nth xs i0) (.nth xs i1))
       3 (f a b c d (.nth xs i0) (.nth xs i1) (.nth xs i2))
       4 (f a b c d (.nth xs i0) (.nth xs i1) (.nth xs i2) (.nth xs i3))
       (g a b c d xs)))))

(definline -aget
  [arr i]
  `(ArrayUtil/aget ~arr ~i))

(let [i0 (int 0)]
  (defn array-dispatch0
    ([^objects arr f]         (f         (-aget arr i0)))
    ([^objects arr f a]       (f a       (-aget arr i0)))
    ([^objects arr f a b]     (f a b     (-aget arr i0)))
    ([^objects arr f a b c]   (f a b c   (-aget arr i0)))
    ([^objects arr f a b c d] (f a b c d (-aget arr i0)))))

(let [i0 (int 0) i1 (int 1)]
  (defn array-dispatch1
    ([^objects arr f]         (f         (-aget arr i0) (-aget arr i1)))
    ([^objects arr f a]       (f a       (-aget arr i0) (-aget arr i1)))
    ([^objects arr f a b]     (f a b     (-aget arr i0) (-aget arr i1)))
    ([^objects arr f a b c]   (f a b c   (-aget arr i0) (-aget arr i1)))
    ([^objects arr f a b c d] (f a b c d (-aget arr i0) (-aget arr i1)))))

(let [i0 (int 0) i1 (int 1) i2 (int 2)]
  (defn array-dispatch2
    ([^objects arr f]         (f         (-aget arr i0) (-aget arr i1) (-aget arr i2)))
    ([^objects arr f a]       (f a       (-aget arr i0) (-aget arr i1) (-aget arr i2)))
    ([^objects arr f a b]     (f a b     (-aget arr i0) (-aget arr i1) (-aget arr i2)))
    ([^objects arr f a b c]   (f a b c   (-aget arr i0) (-aget arr i1) (-aget arr i2)))
    ([^objects arr f a b c d] (f a b c d (-aget arr i0) (-aget arr i1) (-aget arr i2)))))

(let [i0 (int 0) i1 (int 1) i2 (int 2) i3 (int 3)]
  (defn array-dispatch3
    ([^objects arr f]         (f         (-aget arr i0) (-aget arr i1) (-aget arr i2) (-aget arr i3)))
    ([^objects arr f a]       (f a       (-aget arr i0) (-aget arr i1) (-aget arr i2) (-aget arr i3)))
    ([^objects arr f a b]     (f a b     (-aget arr i0) (-aget arr i1) (-aget arr i2) (-aget arr i3)))
    ([^objects arr f a b c]   (f a b c   (-aget arr i0) (-aget arr i1) (-aget arr i2) (-aget arr i3)))
    ([^objects arr f a b c d] (f a b c d (-aget arr i0) (-aget arr i1) (-aget arr i2) (-aget arr i3)))))

(let [i0 (int 0)]
  (defn array-dispatch
    ([^PersistentVector xs f]
     (let [arr (.arrayFor xs i0)]
       (case (.count xs)
         1 (array-dispatch0 arr f)
         2 (array-dispatch1 arr f)
         3 (array-dispatch2 arr f)
         4 (array-dispatch3 arr f)
         nil)))))

(let [i0 (int 0)
      fs (into-array clojure.lang.IFn [identity array-dispatch0 array-dispatch1 array-dispatch2 array-dispatch2])]
  (defn array-dispatch
    ([^PersistentVector xs f]         (let [arr (.arrayFor xs i0)] ((-aget fs (.count xs)) arr f)))
    ([^PersistentVector xs f a]       (let [arr (.arrayFor xs i0)] ((-aget fs (.count xs)) arr f a)))
    ([^PersistentVector xs f a b]     (let [arr (.arrayFor xs i0)] ((-aget fs (.count xs)) arr f a b)))
    ([^PersistentVector xs f a b c]   (let [arr (.arrayFor xs i0)] ((-aget fs (.count xs)) arr f a b c)))
    ([^PersistentVector xs f a b c d] (let [arr (.arrayFor xs i0)] ((-aget fs (.count xs)) arr f a b c d)))))

(defn vector-dispatch
  ([^PersistentVector xs f g]
   (if (< (.count xs) 5)
     (array-dispatch xs f)
     (g xs)))
  ([^PersistentVector xs f g a]
   (if (< (.count xs) 5)
     (array-dispatch xs f a)
     (g a xs)))
  ([^PersistentVector xs f g a b]
   (if (< (.count xs) 5)
     (array-dispatch xs f a b)
     (g a b xs)))
  ([^PersistentVector xs f g a b c]
   (if (< (.count xs) 5)
     (array-dispatch xs f a b c)
     (g a b c xs)))
  ([^PersistentVector xs f g a b c d]
   (if (< (.count xs) 5)
     (array-dispatch xs f a b c d)
     (g a b c d xs))))

(defn seq-dispatch
  ([^ISeq xs f g]
   (case (.count xs)
     0 (f)
     1 (let [x0 (.first xs)]
         (f x0))
     2 (let [x0 (.first xs) xs (.next xs) x1 (.first xs)]
         (f x0 x1))
     3 (let [x0 (.first xs) xs (.next xs) x1 (.first xs) xs (.next xs) x2 (.next xs)]
         (f x0 x1 x2))
     4 (let [x0 (.first xs) xs (.next xs) x1 (.first xs) xs (.next xs) x2 (.first xs) xs (.next xs) x3 (.first xs)]
         (f x0 x1 x2 x3))
     (g xs)))
  ([^ISeq xs f g a]
   (case (.count xs)
     0 (f a)
     1 (let [x0 (.first xs)]
         (f a x0))
     2 (let [x0 (.first xs) xs (.next xs) x1 (.first xs)]
         (f a x0 x1))
     3 (let [x0 (.first xs) xs (.next xs) x1 (.first xs) xs (.next xs) x2 (.next xs)]
         (f a x0 x1 x2))
     4 (let [x0 (.first xs) xs (.next xs) x1 (.first xs) xs (.next xs) x2 (.first xs) xs (.next xs) x3 (.first xs)]
         (f a x0 x1 x2 x3))
     (g a xs)))
  ([^ISeq xs f g a b]
   (case (.count xs)
     0 (f a b)
     1 (let [x0 (.first xs)]
         (f a b x0))
     2 (let [x0 (.first xs) xs (.next xs) x1 (.first xs)]
         (f a b x0 x1))
     3 (let [x0 (.first xs) xs (.next xs) x1 (.first xs) xs (.next xs) x2 (.next xs)]
         (f a b x0 x1 x2))
     4 (let [x0 (.first xs) xs (.next xs) x1 (.first xs) xs (.next xs) x2 (.first xs) xs (.next xs) x3 (.first xs)]
         (f a b x0 x1 x2 x3))
     (g a b xs)))
  ([^ISeq xs f g a b c]
   (case (.count xs)
     0 (f a b c)
     1 (let [x0 (.first xs)]
         (f a b c x0))
     2 (let [x0 (.first xs) xs (.next xs) x1 (.first xs)]
         (f a b c x0 x1))
     3 (let [x0 (.first xs) xs (.next xs) x1 (.first xs) xs (.next xs) x2 (.next xs)]
         (f a b c x0 x1 x2))
     4 (let [x0 (.first xs) xs (.next xs) x1 (.first xs) xs (.next xs) x2 (.first xs) xs (.next xs) x3 (.first xs)]
         (f a b c x0 x1 x2 x3))
     (g a b c xs)))
  ([^ISeq xs f g a b c d]
   (case (.count xs)
     0 (f a b c d)
     1 (let [x0 (.first xs)]
         (f a b c d x0))
     2 (let [x0 (.first xs) xs (.next xs) x1 (.first xs)]
         (f a b c d x0 x1))
     3 (let [x0 (.first xs) xs (.next xs) x1 (.first xs) xs (.next xs) x2 (.next xs)]
         (f a b c d x0 x1 x2))
     4 (let [x0 (.first xs) xs (.next xs) x1 (.first xs) xs (.next xs) x2 (.first xs) xs (.next xs) x3 (.first xs)]
         (f a b c d x0 x1 x2 x3))
     (g a b c d xs))))

(defn dispatcher
  ([p0 d0 d1]
   (fn
     ([xs f g]
      (if (p0 xs)
        (d0 xs f g)
        (d1 xs f g)))
     ([xs f g a]
      (if (p0 xs)
        (d0 xs f g a)
        (d1 xs f g a)))
     ([xs f g a b]
      (if (p0 xs)
        (d0 xs f g a b)
        (d1 xs f g a b)))
     ([xs f g a b c]
      (if (p0 xs)
        (d0 xs f g a b c)
        (d1 xs f g a b c)))
     ([xs f g a b c d]
      (if (p0 xs)
        (d0 xs f g a b c d)
        (d1 xs f g a b c d)))))
  ([p0 d0 p1 d1 d2]
   (fn
     ([xs f g]
      (if (p0 xs)
        (d0 xs f g)
        (if (p1 xs)
          (d1 xs f g)
          (d2 xs f g))))
     ([xs f g a]
      (if (p0 xs)
        (d0 xs f g a)
        (if (p1 xs)
          (d1 xs f g a)
          (d2 xs f g a))))
     ([xs f g a b]
      (if (p0 xs)
        (d0 xs f g a b)
        (if (p1 xs)
          (d1 xs f g a b)
          (d2 xs f g a b))))
     ([xs f g a b c]
      (if (p0 xs)
        (d0 xs f g a b c)
        (if (p1 xs)
          (d1 xs f g a b c)
          (d2 xs f g a b c))))
     ([xs f g a b c d]
      (if (p0 xs)
        (d0 xs f g a b c d)
        (if (p1 xs)
          (d1 xs f g a b c d)
          (d2 xs f g a b c d)))))))

(defn instance-dispatcher
  ([^Class c0 d0 d1]
   (fn
     ([xs f g]
      (if (instance? c0 xs)
        (d0 xs f g)
        (d1 xs f g)))
     ([xs f g a]
      (if (instance? c0 xs)
        (d0 xs f g a)
        (d1 xs f g a)))
     ([xs f g a b]
      (if (instance? c0 xs)
        (d0 xs f g a b)
        (d1 xs f g a b)))
     ([xs f g a b c]
      (if (instance? c0 xs)
        (d0 xs f g a b c)
        (d1 xs f g a b c)))
     ([xs f g a b c d]
      (if (instance? c0 xs)
        (d0 xs f g a b c d)
        (d1 xs f g a b c d)))))
  ([^Class c0 d0 ^Class c1 d1 d2]
   (fn
     ([xs f g]
      (if (instance? c0 xs)
        (d0 xs f g)
        (if (instance? c1 xs)
          (d1 xs f g)
          (d2 xs f g))))
     ([xs f g a]
      (if (instance? c0 xs)
        (d0 xs f g a)
        (if (instance? c1 xs)
          (d1 xs f g a)
          (d2 xs f g a))))
     ([xs f g a b]
      (if (instance? c0 xs)
        (d0 xs f g a b)
        (if (instance? c1 xs)
          (d1 xs f g a b)
          (d2 xs f g a b))))
     ([xs f g a b c]
      (if (instance? c0 xs)
        (d0 xs f g a b c)
        (if (instance? c1 xs)
          (d1 xs f g a b c)
          (d2 xs f g a b c))))
     ([xs f g a b c d]
      (if (instance? c0 xs)
        (d0 xs f g a b c d)
        (if (instance? c1 xs)
          (d1 xs f g a b c d)
          (d2 xs f g a b c d)))))))

#_
(defn dispatch
  "Dispatch to specialized `f` implementation based on the count of `xs`.
  `f` must support an unrolled implementation of up to (count xs) == 4.
  `g` is called if (count xs) > 4.
  a-d are additional arguments passed to f and g."
  ([xs f g]
   (if (instance? Indexed xs)
     (indexed-dispatch xs f g)
     (seq-dispatch xs f g)))
  ([xs f g a]
   (if (instance? Indexed xs)
     (indexed-dispatch xs f g a)
     (seq-dispatch xs f g a)))
  ([xs f g a b]
   (if (instance? Indexed xs)
     (indexed-dispatch xs f g a b)
     (seq-dispatch xs f g a b)))
  ([xs f g a b c]
   (if (instance? Indexed xs)
     (indexed-dispatch xs f g a b c)
     (seq-dispatch xs f g a b c)))
  ([xs f g a b c d]
   (if (instance? Indexed xs)
     (indexed-dispatch xs f g a b c d)
     (seq-dispatch xs f g a b c d))))

(defn dispatch
  "Dispatch to specialized `f` implementation based on the count of `xs`.
  `f` must support an unrolled implementation of up to (count xs) == 4.
  `g` is called if (count xs) > 4.
  a-d are additional arguments passed to f and g."
  ([xs f g]
   (if (instance? PersistentVector xs)
     (vector-dispatch xs f g)
     (seq-dispatch xs f g)))
  ([xs f g a]
   (if (instance? PersistentVector xs)
     (vector-dispatch xs f g a)
     (seq-dispatch xs f g a)))
  ([xs f g a b]
   (if (instance? PersistentVector xs)
     (vector-dispatch xs f g a b)
     (seq-dispatch xs f g a b)))
  ([xs f g a b c]
   (if (instance? PersistentVector xs)
     (vector-dispatch xs f g a b c)
     (seq-dispatch xs f g a b c)))
  ([xs f g a b c d]
   (if (instance? PersistentVector xs)
     (vector-dispatch xs f g a b c d)
     (seq-dispatch xs f g a b c d))))

#_
(defn dispatch
  "Dispatch to specialized `f` implementation based on the count of `xs`.
  `f` must support an unrolled implementation of up to (count xs) == 4.
  `g` is called if (count xs) > 4.
  a-d are additional arguments passed to f and g."
  ([xs f g]
   (if (list? xs)
     (seq-dispatch xs f g)
     (iterable-dispatch xs f g)))
  ([xs f g a]
   (if (list? xs)
     (seq-dispatch xs f g a)
     (iterable-dispatch xs f g a)))
  ([xs f g a b]
   (if (list? xs)
     (seq-dispatch xs f g a b)
     (iterable-dispatch xs f g a b)))
  ([xs f g a b c]
   (if (list? xs)
     (seq-dispatch xs f g a b c)
     (iterable-dispatch xs f g a b c)))
  ([xs f g a b c d]
   (if (list? xs)
     (seq-dispatch xs f g a b c d)
     (iterable-dispatch xs f g a b c d))))

#_
(defn dispatch
  "Dispatch to specialized `f` implementation based on the count of `xs`.
  `f` must support an unrolled implementation of up to (count xs) == 4.
  `g` is called if (count xs) > 4.
  a-d are additional arguments passed to f and g."
  ([xs f g]
   (iterable-dispatch xs f g))
  ([xs f g a]
   (iterable-dispatch xs f g a))
  ([xs f g a b]
   (iterable-dispatch xs f g a b))
  ([xs f g a b c]
   (iterable-dispatch xs f g a b c))
  ([xs f g a b c d]
   (iterable-dispatch xs f g a b c d)))

(defn get-in*
  ([m] m)
  ([m a] (inline/get-in m [a]))
  ([m a b] (inline/get-in m [a b]))
  ([m a b c] (inline/get-in m [a b c]))
  ([m a b c d] (inline/get-in m [a b c d])))

(defn get-in-
  [m ks]
  (reduce get m ks))

(defn -get-in
  [m xs]
  (dispatch xs get-in* get-in- m))

(comment
  (require
   '[clojure.string :as str]
   '[criterium.core :as cc]
   '[clojure.java.io :as io])
  (def ks [:a :b :c :d :e])
  (def m {:a {:b {:c {:d {:e 1}}}}})
  (def rep
   (into
     []
     (let [m m
           ks ks]
       (for [n (range 1 6)
             f [get-in- -get-in get-in]
             :let [kv (into [] (take n ks))
                   kl (apply list (take n ks))
                   ll (map identity kl)]
             l [kv kl ll]
             :let [t (type l)
                   clazz (.getName (class f))]]
         (do
           (println "bench" n clazz (type l))
           (time
            {:mean (:mean (cc/quick-benchmark (f m l) {}))
             :type t
             :count n
             :name clazz}))
         ))))

  (cc/quick-bench (-get-in m [:a]))
  (cc/quick-bench (get-in m [:a]))
  (cc/quick-bench (get-in- m [:a]))

  (def res (mapv (fn [m] (update m :mean  #(* 1e9 (first %)))) rep))


  (def header [:name :type :count :mean])
  (with-open [w (io/writer (io/file "out.csv"))]
    (.write w (str/join "," (map name header)))
    (.write w "\n")
    (doseq [line res]
      (.write w (str/join "," (map line header)))
      (.write w "\n")
      )
    )

  (defn map-function-on-map-vals
    "Take a map and apply a function on its values. From [1].
   [1] http://stackoverflow.com/a/1677069/500207"
    [m f]
    (zipmap (keys m) (map f (vals m))))

  (defn nested-group-by
    "Like group-by but instead of a single function, this is given a list or vec
   of functions to apply recursively via group-by. An optional `final` argument
   (defaults to identity) may be given to run on the vector result of the final
   group-by."
    [fs coll & [final-fn]]
    (if (empty? fs)
      ((or final-fn identity) coll)
      (map-function-on-map-vals (group-by (first fs) coll)
                                #(nested-group-by (rest fs) % final-fn))))

  (nested-group-by [:type :count] res #(into {} (map (juxt :name :mean) %)))


  (require '[clj-async-profiler.core :as prof])

  (time
   (dotimes [_ 1e8]
     (-get-in m ks)))

  (time
   (prof/profile
    (dotimes [_ 1e9]
      (get-in m [:a]))))

  (time
   (prof/profile
    (dotimes [_ 1e9]
      (get-in- m [:a]))))

  (time
   (prof/profile
    (dotimes [_ 1e9]
      (-get-in m [:a]))))

  (time
   (prof/profile
    (dotimes [_ 1e9]
      (get-in- m ks))))

  (time
   (prof/profile
    (dotimes [_ 1e9]
      (-get-in m ks))))

  (prof/profile
   (dotimes [_ 1e8]
     (get-in- m ks)))

  (cc/quick-bench (get-in- m ks))
  (cc/quick-bench (get-in m ks))

  (prof/serve-files 7777)
  (prof/clear-results)

  (defn noop
    "Why 7?"
    ([] 7)
    ([_] 7)
    ([_ _] 7)
    ([_ _ _] 7)
    ([_ _ _ _] 7)
    ([_ _ _ _ _] 7)
    ([_ _ _ _ _ _] 7))

  (let [noop noop]
    (defn stub
      [xs]
      (dispatch xs noop noop)))

  (time
   (prof/profile
    (dotimes [_ 1e10]
      (stub [1 2 3 4 5]))))

  (time
   (dotimes [_ 1e10]
     (stub [1 2 3 4 5])))


  )
