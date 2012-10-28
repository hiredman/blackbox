(defproject blackbox/blackbox "1.0.0-SNAPSHOT"
  :description "FIXME: write description"
  :dependencies [[org.clojure/clojure "1.5.0-beta1"]
                 #_[jfree/jfreechart "1.0.13"]
                 #_[vespa-crabro "1.0.0-SNAPSHOT"]
                 [ring "1.1.0" :exclusions
                  [org.apache.httpcomponents/httpcore
                   org.apache.httpcomponents/httpcore-nio
                   ring/ring-jetty-adapter]]
                 [com.cemerick/pomegranate "0.0.13"]
                 [org.clojure/tools.nrepl "0.2.0-beta10"]
                 #_[beaglebone-mmap "1.0.0-SNAPSHOT"]
                 [compojure "1.1.3"]
                 [hiccup "1.0.1"]]
  :ring {:handler blackbox.web/handler
         :init blackbox.init/init}
  :min-lein-version "2.0.0"
  :plugins [[lein-ring "0.7.4"]
            [lein-cljsbuild "0.2.9"]]
  :cljsbuild {:builds [{:source-path "cljs/"
                        :notify-command ["growlnotify" "-m"]
                        :compiler {:output-to "resources/boot.js"
                                   :optimizations #_:whitespace :advanced
                                   :pretty-print true}}]}
  :aliases {"build" ["do" "cljsbuild" "once," "ring" "uberwar"]})
