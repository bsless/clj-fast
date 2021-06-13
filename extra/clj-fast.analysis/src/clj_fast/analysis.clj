(ns clj-fast.analysis
  "Interactive notebook namespace to load, parse and chart benchmark results"
  (:require
   [clojure.edn]
   [clojure.string]
   [incanter
    [core :as i]
    [charts :as charts]]))

(defn load-results
  [s]
  (clojure.edn/read-string (slurp s)))

(defn load-run
  [run]
  (->>
   (load-results (str "../clj-fast.jmh/results/" (name run) ".edn"))
   (mapv (fn [{{n :count size :log-map-size} :params label :name [score] :score}]
           {:count n :log-size size :name label :score score :time (/ 1 score)}))
   (array-map run)))

(defn title
  [bench ctrl n]
  (clojure.string/join
   " "
   ["Benchmark" (name bench) "-" (str (name ctrl) ":") n]))

(defn bar-charts
  ([raw-data x-dim y-dim]
   (reduce
    (fn [m [run-type run-data]]
      (let [by-x (group-by x-dim run-data)
            by-y (group-by y-dim run-data)
            y-label "throughput (ops/s)"
            y-charts
            (reduce
             (fn [m [y-val y-data]]
               (let [y-ds (i/dataset y-data)
                     x-label (case x-dim
                               (:log-size :width) "log10 map size (elements)"
                               :type "data type")
                     chart
                     (charts/bar-chart
                      x-dim :score
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
                      y-dim :score
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
  [all-charts]
  (let [flat (flatten-map all-charts)]
    (doseq [[ks chart] flat
            :let [fname (ks->path ks)
                  path (str "./doc/images/" fname ".png")]]
      (i/save chart path))))

(defn logify
  [k raw-data]
  (let [xs (vals (get-in raw-data [k :log-size]))
        ys (vals (get-in raw-data [k :count]))]
    (doseq [chart (concat xs ys)]
      (set-log-axis! chart 3))))

(comment
  #_jmh

  (def run :merge)
  (def raw-data (into {} (map load-run) [:get-in :assoc-in :merge :assoc :update-in]))
  (def cs (bar-charts raw-data :log-size :count))
  (logify :merge cs)
  (write-charts cs)
  (i/view (get-in cs [run :log-size 1]))
  (i/view (get-in cs [run :log-size 3]))
  (i/view (get-in cs [run :count 1]))
  (i/view (get-in cs [run :count 4]))

  ,)
