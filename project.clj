(defproject bsless/clj-fast "0.0.3-alpha"
  :description "Fast Inline Clojure Core functions"
  :url "https://github.com/bsless/clj-fast"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"]]
  :direct-linking true
  :target-path "target/%s"
  :profiles
  {:uberjar
   [:bench :cli
    {:main clj-fast.bench
     :aot :all}]
   :dev
   [:bench :prof :cli
    {:source-paths ["analysis"]
     :dependencies
     [[incanter "1.9.3"]
      [org.clojure/tools.cli "0.4.2"]]}]
   :cli {:dependencies [[org.clojure/tools.cli "0.4.2"]]}
   :bench {:source-paths ["bench"]
           :dependencies [[criterium "0.4.5"]
                          [org.clojure/test.check "0.9.0"]]}
   :prof {:source-paths ["prof"]
          :dependencies [[com.clojure-goes-fast/clj-async-profiler "0.4.0"]]}

   :big-heap {:jvm-opts ["-Xmx9g" "-Xms9G"]}
   :med-heap {:jvm-opts ["-Xmx5g" "-Xms5G"]}
   :small-heap {:jvm-opts ["-Xmx2g" "-Xms2G"]}
   :g1 {:jvm-opts ["-XX:+UseG1GC"]}
   :parallel {:jvm-opts ["-XX:+UseParallelGC"]}
   }
  :repl-options {:init-ns clj-fast.core})
