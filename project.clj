(defproject bsless/clj-fast "0.0.11"
  :description "Fast Inline Clojure Core functions"
  :url "https://github.com/bsless/clj-fast"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"]]
  :target-path "target/%s"
  :deploy-repositories [["clojars" {:url "https://clojars.org/repo"
                                    :username :env/clojars_user
                                    :password :env/clojars_token
                                    :sign-releases false}]
                        ["releases" :clojars]
                        ["snapshots" :clojars]]
  :profiles
  {:dev
   {:jvm-opts ["-Djdk.attach.allowAttachSelf"
               "-XX:+UnlockDiagnosticVMOptions"
               "-XX:+DebugNonSafepoints"]
    :dependencies
    [[criterium "0.4.6"]
     [com.clojure-goes-fast/clj-async-profiler "0.5.0"]]}}
  :repl-options {:init-ns clj-fast.core})
