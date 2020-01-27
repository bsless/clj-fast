(defproject bsless/clj-fast "0.0.2-alpha"
  :description "Fast Inline Clojure Core functions"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"]]
  :direct-linking true
  :target-path "target/%s"
  :profiles
  {:uberjar
   {:source-paths
    ["bench"]
    :dependencies
    [[org.clojure/tools.cli "0.4.2"]
     [org.clojure/test.check "0.9.0"]
     [criterium "0.4.5"]
     [com.clojure-goes-fast/clj-async-profiler "0.4.0"]]
    :main clj-fast.bench
    :aot :all}
   :dev
   {:source-paths
    ["bench" "prof"]
    :dependencies
    [[incanter "1.9.3"]
     [criterium "0.4.5"]
     [org.clojure/test.check "0.9.0"]
     [org.clojure/tools.cli "0.4.2"]
     [com.clojure-goes-fast/clj-async-profiler "0.4.0"]]}
   :bench
   {:source-paths
    ["bench"]
    :dependencies
    [[org.clojure/tools.cli "0.4.2"]
     [criterium "0.4.5"]
     [com.clojure-goes-fast/clj-async-profiler "0.4.0"]]
    :main clj-fast.bench
    :aot :all}
   :prof
   {:source-paths
    ["prof"]
    :dependencies
    [[criterium "0.4.5"]
     [com.clojure-goes-fast/clj-async-profiler "0.4.0"]]
    :main clj-fast.prof
    :aot :all}
   :big-heap {:jvm-opts ["-Xmx9g" "-Xms9G"]}
   :med-heap {:jvm-opts ["-Xmx5g" "-Xms5G"]}
   :small-heap {:jvm-opts ["-Xmx2g" "-Xms2G"]}
   :g1 {:jvm-opts ["-XX:+UseG1GC"]}
   :parallel {:jvm-opts ["-XX:+UseParallelGC"]}
   }
  :repl-options {:init-ns clj-fast.core})
