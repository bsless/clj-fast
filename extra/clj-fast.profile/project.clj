(defproject clj-fast.profile "0.0.0-SNAPSHOT"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.3"]
                 [com.clojure-goes-fast/clj-async-profiler "0.4.0"]]
  :repl-options {:init-ns clj-fast.profile})
