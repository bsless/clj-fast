(ns clj-fast.analysis
  "Interactive notebook namespace to load, parse and chart benchmark results"
  (:require
   [incanter
    [core :as i]
    [charts :as charts]]))

(defn load-results
  [s]
  (clojure.edn/read-string (slurp s)))

(def common
  #{:assoc :merge :get-in :select-keys :assoc-in :update-in :memoize})

(defn title
  [bench ctrl n]
  (clojure.string/join
   " "
   ["Benchmark" (name bench) "-" (str (name ctrl) ":") n]))

(defn common-charts
  ([raw-data]
   (common-charts raw-data :width :keys))
  ([raw-data x-dim y-dim]
   (reduce
    (fn [m [run-type run-data]]
      (let [by-x (group-by x-dim run-data)
            by-y (group-by y-dim run-data)
            y-label "mean execution time (ns)"
            y-charts
            (reduce
             (fn [m [y-val y-data]]
               (let [y-ds (i/dataset y-data)
                     x-label (case x-dim
                               :width "log10 map size (elements)"
                               :type "data type")
                     chart
                     (charts/bar-chart
                      x-dim :mean
                      :group-by :bench
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
                      y-dim :mean
                      :group-by :bench
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
    (filter (fn [[k _]] (common k)) raw-data))))

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
  (let [other-dim (if (= dim :keys) :width :keys)]
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
  (let [xs (vals (get-in raw-data [k :width]))
        ys (vals (get-in raw-data [k :keys]))]
    (doseq [chart (concat xs ys)]
      (set-log-axis! chart 3))))

(comment

  (def raw-data
    (-> "./benchmarks/all-clj-fast-bench.edn"
        load-results
        (update :get-rec #(map (fn [m] (assoc m :width 0)) %))
        (update :merge #(remove (comp #{1} :keys) %))))

  (def all-charts
    (merge
     (common-charts raw-data)
     (chart-get :get raw-data)
     (chart-get :get-rec raw-data)
     (chart-assoc-rec raw-data)))

  (logify :merge all-charts)

  ;;; Format merge nicely because the results vary widely
  (map (fn [e c] (set-log-axis! c e))
       [3 4 5 6 7]
       (vals (get-in all-charts [:merge :width])))

  (write-charts all-charts)

  ;;; memoize results
  (def raw-data
    (-> "./benchmarks/memo-clj-fast-bench.edn"
        load-results))

  (def charts
    (common-charts
     raw-data :type :keys))

  (write-charts charts)

  )
