(defproject clj-linux-net "0.2.0-SNAPSHOT"
  :description "Clojure Linux Network integration"
  :url "http://github.com/profitware/clj-linux-net"
  :license {:name "Apache License 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}
  :plugins [[lein-shell "0.5.0"]]
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [com.google.guava/guava "16.0.1"]
                 [org.slf4j/slf4j-api "1.7.25"]
                 [org.slf4j/slf4j-simple "1.7.25"]]
  :source-paths ["src/clojure"]
  :java-source-paths ["src/main/java"]
  :javac-options ["-target" "1.8" "-source" "1.8"]
  :jvm-opts [~(str "-Djava.library.path=native/:" (System/getenv "LD_LIBRARY_PATH"))]
  :target-path "target/"
  :aliases {"compile-native" [["shell" "sh" "-c" "cd src/native; make clean; make"]]}
  :prep-tasks ["javac" "compile-native"]
  :profiles {:uberjar {:aot :all}
             :dev {:dependencies [[org.clojure/tools.namespace "0.2.11"]]}})
