(ns clj-fast.instrument
  (:require
   [clj-fast.core :as c])
  (:import
   (java.util Queue)
   (java.util.concurrent ConcurrentLinkedQueue)))

(defonce
  ^{:doc "All instrumented vars are registered in this set,
  Allows for manipulation and querying of their metadata."}
  instrumented-vars
  (atom #{}))

(def ^:private argvs
  (reductions
   conj
   []
   (map
    (fn [n] (symbol (str "x" n)))
    (range 20))))

(defn- emit-instrumenting-expr
  [before after]
  (let [bodies
        (map
         (fn [argv]
           `([~@argv]
             (let [start# (~before)
                   ret# (~'f ~@argv)]
               (~after start#)
               ret#)))
         argvs)]
    `(fn [~'f] (fn ~@bodies))))

(comment
  (emit-instrumenting-expr 'before 'after))

(defmacro instrumenting
  "Emit a function which when applied to another function will wrap it,
  executing `before` before calling it, and `after` afterwards, with the
  result of calling `before`.
  Usage suggestion:
  (alter-var-root v (instrumenting before after))"
  [before after]
  (let [b (gensym) a (gensym)]
    `(let [~b ~before
           ~a ~after]
       ~(emit-instrumenting-expr b a))))

(defn timing-before
  "Nano time, suitable as a `before` function."
  ^long [] (System/nanoTime))

(defn timing-after-fn
  "Nano time difference which will push into a Queue `q`, suitable as
  `after` function, must take a Queue."
  [^Queue q]
  (fn [^long start]
    (let [end (System/nanoTime)]
      (.add q (unchecked-subtract end start)))))

(defn instrument-var
  "Wrap a var `v` with `f` and replace its meta with `m`"
  ([v f]
   (instrument-var v f nil))
  ([v f m]
   (alter-var-root v f)
   (when (map? m)
     (reset-meta! v m))
   (swap! instrumented-vars conj v)
   v))

(defn instrument-var-fn
  "Return a function for instrumenting a var with `before` and `after`.
  `extra-meta` is a map with extra metadata which will be attached to the var."
  ([before after]
   (instrument-var-fn before after nil))
  ([before after extra-meta]
   (fn [v]
     (let [old-meta (meta v)
           r @v
           m (merge old-meta {:old-root r :old-meta old-meta} extra-meta)]
       (instrument-var v (instrumenting before after) m)))))

(defn noop-fn [] (fn ([]) ([_])))

(defn instrument-var-visitor
  "Create an instrumentation function which registers visited vars in a
  shared (atom set).
  Useful for narrowing down the search space before causal profiling."
  [a]
  (fn [v]
    (let [before (fn [] (swap! a conj v))
          old-meta (meta v)
          r @v
          m (merge old-meta {:old-root r :old-meta old-meta})]
      (instrument-var v (instrumenting before noop-fn) m))))

(defn instrument-var-with-timing-fn
  "Return a function which instruments a var with timing measurements in
  nanosecods and appends the results to a Queue which will be attached
  to the var's metadata."
  []
  (let [q (ConcurrentLinkedQueue.)]
    (instrument-var-fn
     timing-before
     (timing-after-fn q)
     {:queue q})))

(defn sleep-cause-fn
  [b]
  (fn [] (Thread/sleep (long (c/unbox! b)))))

(defn instrument-var-cause-fn
  "Return a function which instruments a var for causal profiling.
  The slow-down amount is saved in a box attached to the var's
  metadata."
  []
  (let [b (c/box! 1)]
    (instrument-var-fn
     (sleep-cause-fn b)
     (noop-fn)
     {:box b})))

(defn restore-var
  "Restore var to its pre-instrumentation state."
  [v]
  (let [{:keys [old-root old-meta]}
        (select-keys (meta v) [:old-root :old-meta])]
    (alter-var-root v (constantly old-root))
    (reset-meta! v old-meta)
    (swap! instrumented-vars disj v)
    v))

(comment
  (defn foo
    [x]
    (Thread/sleep 20)
    (inc x))

  ((instrument-var-with-timing-fn) #'foo)
  ((instrument-var-cause-fn) #'foo)
  (restore-var #'foo)

  (dotimes [_ 1e3]
    (foo 2))

  (into [] (:queue (meta #'foo))))

;;; Statistical analysis

(defn square
  ^long [^long x]
  (unchecked-multiply x x))

(defn mean
  ^double [coll]
  (double (/ (reduce unchecked-add coll) (count coll))))

(defn stddev
  [a]
  (let [mn (mean a)]
    (Math/sqrt
     (/ (reduce #(+ %1 (square (- %2 mn))) 0 a)
        (dec (count a))))))

(comment
  (- (mean (into [] (:queue (meta #'foo)))) 2e7))
