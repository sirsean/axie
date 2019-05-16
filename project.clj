(defproject axie "0.1.0-SNAPSHOT"
  :description "Dealin' with the Axie Infinity API, which is quicker than their website"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [aleph "0.4.6"]
                 [byte-streams "0.2.4"]
                 [camel-snake-kebab "0.4.0"]
                 [cheshire "5.8.1"]
                 [cli-matic "0.3.6"]
                 [manifold "0.1.8"]
                 [clj-time "0.15.0"]
                 [amazonica "0.3.142" :exclusions [com.amazonaws/aws-java-sdk
                                                   com.amazonaws/amazon-kinesis-client]]
                 [com.amazonaws/aws-java-sdk-core "1.11.552"]
                 [com.amazonaws/aws-java-sdk-lambda "1.11.552"]
                 [com.amazonaws/aws-java-sdk-s3 "1.11.552"]
                 [com.grammarly/omniconf "0.3.2"]
                 [com.gfredericks/vcr-clj "0.4.18"]]
  :main ^:skip-aot axie.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
