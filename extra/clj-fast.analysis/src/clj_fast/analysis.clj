(ns clj-fast.analysis
  "Interactive notebook namespace to load, parse and chart benchmark results"
  (:require
   [clojure.edn]
   [clojure.set :as set]
   [clojure.string]
   [incanter
    [core :as i]
    [charts :as charts]]))

(def default-opts
  {:x-dim :log-size
   :y-dim :count})

(def case-opts
  {:memoize {:x-dim :type}})

(defn load-results
  [s]
  (clojure.edn/read-string (slurp s)))

(defn relative-results
  [run xs]
  (let [{:keys [x-dim y-dim]} (merge default-opts (get case-opts run))]
    (for [[_count g] (group-by y-dim xs)
          [_size g] (group-by x-dim g)
          :let [base (first (filter (fn [rec] (= "core" (namespace (:name rec)))) g))
                base-score (:score base)]
          rec g
          :let [score (:score rec)]]
      (assoc rec :relative-score (* 100 (- (/ score base-score) 1))))))

(defn parse-run
  [{label :name [score] :score :as m}]
  (set/rename-keys
   (merge
    (:params m)
    {:name label :score score :time (/ 1 score)})
   {:log-map-size :log-size}))

(defn load-run
  [run]
  (->>
   (load-results (str "../clj-fast.jmh/results/" (name run) ".edn"))
   (mapv parse-run)
   (relative-results run)
   (into [])
   (array-map run)))

(defn title
  [bench ctrl n]
  (clojure.string/join
   " "
   ["Benchmark" (name bench) "-" (str (name ctrl) ":") n]))

(def labels
  {:score "throughput (ops/s)"
   :relative-score "throughput change %"})

(defn bar-charts
  ([raw-data]
   (bar-charts raw-data :score))
  ([raw-data metric]
   (reduce
    (fn [m [run-type run-data]]
      (let [{:keys [x-dim y-dim]} (merge default-opts (get case-opts run-type))
            by-x (group-by x-dim run-data)
            by-y (group-by y-dim run-data)
            y-label (get labels metric)
            y-charts
            (reduce
             (fn [m [y-val y-data]]
               (let [y-ds (i/dataset y-data)
                     x-label (case x-dim
                               (:log-size :width) "log10 map size (elements)"
                               :type "data type")
                     chart
                     (charts/bar-chart
                      x-dim metric
                      :group-by :name
                      :data y-ds
                      :legend true
                      :title (title run-type y-dim y-val)
                      :x-label x-label
                      :y-label y-label)]
                 (assoc m y-val chart)))
             {}
             by-y)
            x-charts
            (reduce
             (fn [m [x-val x-data]]
               (let [x-ds (i/dataset x-data)
                     x-label "number of elements"
                     chart
                     (charts/bar-chart
                      y-dim metric
                      :group-by :name
                      :data x-ds
                      :legend true
                      :title (title run-type x-dim x-val)
                      :x-label x-label
                      :y-label y-label)]
                 (assoc m x-val chart)))
             {}
             by-x)]
        (assoc m run-type {x-dim x-charts y-dim y-charts})))
    {}
    raw-data)))

(defn chart-get
  [k raw-data]
  (when-let [data (get raw-data k)]
    {k
     {:keys
      {1
       (charts/bar-chart
        :width :mean
        :group-by :method
        :data (i/dataset data)
        :legend true
        :x-label "width"
        :y-label "mean (ns)")}}}))

(defn chart-get-rec
  [k raw-data]
  (when-let [data (get raw-data k)]
    {k
     {:keys
      {1
       (charts/bar-chart
        :method :mean
        :group-by :method
        :data (i/dataset data)
        :legend true
        :x-label "width"
        :y-label "mean (ns)")}}}))

(defn chart-assoc-rec
  [raw-data]
  (when-let [data (get raw-data :assoc-rec)]
    {:assoc-rec
     {:width
      {0
       (charts/bar-chart
        :keys :mean
        :group-by :bench
        :data (i/dataset data)
        :legend true
        :x-label "#keys"
        :y-label "mean (ns)")}}}))

(defn flatten-map
  [m]
  (letfn
      [(iter
         [m acc]
         (if (map? m)
           (map (fn [[k v]] (iter v (conj acc k))) m)
           {acc m}))]
    (reduce conj (flatten (iter m [])))))

(defn- string
  [x]
  (if (keyword? x) (name x) (str x)))

(defn ks->path
  [[run dim num]]
  (let [other-dim (if (= dim :count) :log-size :count)]
    (clojure.string/join
     "_"
     (mapv
      (comp #(clojure.string/replace % #"\?" "-p") string)
      [run dim other-dim num]))))

(defn set-log-axis!
  [chart e]
  (let [axis (new org.jfree.chart.axis.LogAxis)
        plot (.getCategoryPlot chart)
        current-axis (.getRangeAxis plot)
        label (.getLabel current-axis)]
    (.setSmallestValue axis (int (Math/pow 10 e)))
    (.setLabel axis label)
    (.setRangeAxis plot axis)))

(defn write-charts
  ([all-charts]
   (write-charts all-charts {}))
  ([all-charts {:keys [prefix]
                :or {prefix ""}}]
   (let [flat (flatten-map all-charts)]
     (doseq [[ks chart] flat
             :let [fname (ks->path ks)
                   path (str "./doc/images/" prefix fname ".png")]]
       (i/save chart path)))))

(defn logify
  [k raw-data]
  (let [xs (vals (get-in raw-data [k :log-size]))
        ys (vals (get-in raw-data [k :count]))]
    (doseq [chart (concat xs ys)]
      (set-log-axis! chart 3))))

(comment
  #_jmh

  (def run :memoize)
  (def raw-data
    (->
     (into {} (map load-run) [:get-in :assoc-in :merge :assoc :update-in :select-keys :memoize])
     (update :merge #(filterv (complement (comp #{1} :count)) %))))

  (def cs (bar-charts (select-keys raw-data [:memoize])))
  (logify :merge cs)
  (write-charts cs)

  (def rcs (bar-charts (select-keys raw-data [:memoize]) :relative-score))
  (write-charts rcs {:prefix "relative-"})

  (i/view (get-in cs [run :log-size 1]))
  (i/view (get-in rcs [run :log-size 1]))
  (i/view (get-in cs [run :log-size 3]))
  (i/view (get-in cs [run :count 1]))
  (i/view (get-in cs [run :count 4]))

  ,)
