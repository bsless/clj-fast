(defproject clj-fast "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"]]
  :direct-linking true
  :profiles
  {:dev
   {:dependencies
    [[criterium "0.4.5"]]}
   :bench
   [:dev
    {:source-paths
     ["src" "bench"]
     :main clj-fast.bench
     :aot :all}]
   :big-heap {:jvm-opts ["-Xmx9g" "-Xms9G"]}
   :med-heap {:jvm-opts ["-Xmx5g" "-Xms5G"]}
   :small-heap {:jvm-opts ["-Xmx2g" "-Xms2G"]}
   :g1 {:jvm-opts ["-XX:+UseG1GC"]}
   :parallel {:jvm-opts ["-XX:+UseParallelGC"]}
   }
  :repl-options {:init-ns clj-fast.core})
