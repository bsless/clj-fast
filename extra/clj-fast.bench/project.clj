(defproject clj-fast.bench "0.0.0-SNAPSHOT"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.3"]
                 [bsless/clj-fast "0.0.10-SNAPSHOT"]
                 [criterium "0.4.5"]
                 [com.clojure-goes-fast/clj-async-profiler "0.4.0"]
                 [org.clojure/test.check "0.9.0"]
                 [org.clojure/tools.cli "0.4.2"]]
  :main clj-fast.bench
  :target-path "target/%s"
  :uberjar-name "bench.jar"
  :profiles {:uberjar {:jvm-opts ["-Dclojure.compiler.direct-linking=true"]
                       :aot :all}})
