(defproject bsless/clj-fast "0.0.1-SNAPSHOT"
  :description "Fast Inline Clojure Core functions"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"]]
  :direct-linking true
  :profiles
  {:dev
   {:dependencies
    [[criterium "0.4.5"]
     [com.clojure-goes-fast/clj-async-profiler "0.4.0"]]}
   :bench
   [:dev
    {:source-paths
     ["bench"]
     :main clj-fast.bench
     :aot :all}]
   :prof
   [:dev
    {:source-paths
     ["prof"]
     :main clj-fast.prof
     :aot :all}]
   :big-heap {:jvm-opts ["-Xmx9g" "-Xms9G"]}
   :med-heap {:jvm-opts ["-Xmx5g" "-Xms5G"]}
   :small-heap {:jvm-opts ["-Xmx2g" "-Xms2G"]}
   :g1 {:jvm-opts ["-XX:+UseG1GC"]}
   :parallel {:jvm-opts ["-XX:+UseParallelGC"]}
   }
  :repl-options {:init-ns clj-fast.core})
