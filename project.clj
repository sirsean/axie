(defproject axie "0.1.0-SNAPSHOT"
  :description "Dealin' with the Axie Infinity API, which is quicker than their website"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/tools.logging "0.4.1"]
                 [clj-logging-config "1.9.12"]
                 [org.slf4j/slf4j-log4j12 "1.7.1"]
                 [log4j/log4j "1.2.17" :exclusions [javax.mail/mail
                                                    javax.jms/jms
                                                    com.sun.jmdk/jmxtools
                                                    com.sun.jmx/jmxri]]
                 [mount "0.1.16"]
                 [tolitius/mount-up "0.1.2"]
                 [com.climate/claypoole "1.1.4"]
                 [aleph "0.4.6"]
                 [compojure "1.6.1"]
                 [metosin/ring-http-response "0.9.1"]
                 [ring/ring-json "0.4.0"]
                 [ring                       "1.7.1"]
                 [ring.middleware.logger "0.5.0"]
                 [ring-cors "0.1.13"]
                 [byte-streams "0.2.4"]
                 [org.web3j/core "4.2.0"]
                 [camel-snake-kebab "0.4.0"]
                 [cheshire "5.8.1"]
                 [cli-matic "0.3.6"]
                 [manifold "0.1.8"]
                 [clj-time "0.15.0"]
                 [prismatic/plumbing "0.5.5"]
                 [vvvvalvalval/supdate "0.2.3"]
                 [tea-time "1.0.1"]
                 [danlentz/clj-uuid "0.1.9"]
                 [amazonica "0.3.142" :exclusions [com.amazonaws/aws-java-sdk
                                                   com.amazonaws/amazon-kinesis-client]]
                 [com.amazonaws/aws-java-sdk-core "1.11.552"]
                 [com.amazonaws/aws-java-sdk-lambda "1.11.552"]
                 [com.amazonaws/aws-java-sdk-s3 "1.11.552"]
                 [com.amazonaws/aws-java-sdk-simpledb "1.11.552"]
                 [com.grammarly/omniconf "0.3.2"]
                 [com.gfredericks/vcr-clj "0.4.18"]]
  :main ^:skip-aot axie.core
  :target-path "target/%s"
  :repl-options {:init-ns user}
  :profiles {:dev {:source-paths ["dev" "src" "test"]
                   :resource-paths ["resources"]
                   :dependencies [[org.clojure/tools.namespace "0.2.11"]]}
             :uberjar {:aot :all}})
