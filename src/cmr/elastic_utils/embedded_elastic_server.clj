(ns cmr.elastic-utils.embedded-elastic-server
  (:import (org.elasticsearch.common.settings
             ImmutableSettings
             ImmutableSettings$Builder)
           org.elasticsearch.common.logging.log4j.LogConfigurator
           org.elasticsearch.node.NodeBuilder
           org.elasticsearch.node.Node)
  (:require [cmr.common.lifecycle :as lifecycle]
            [clj-http.client :as client]
            [cmr.common.log :as log :refer (debug info warn error)]))

{"path" {"data" "es_data/elastic_utils"}, "index" {"store" {"type" "memory"}}, "node" {"name" "embedded-elastic"}, "script" {"disable_dynamic" "true"}, "transport" {"tcp" {"port" "9334"}}, "http" {"port" "9234"}}


(comment

  (def settings (create-settings {:http-port 9201
                                  :transport-port 9203
                                  :data-dir"es_data2"}))

  (def env (org.elasticsearch.env.Environment. settings))

  (.configFile env)

)

(defn- setup-logging
  "Sets up elastic search logging."
  [settings]
  (LogConfigurator/configure settings))

(defn- create-settings
  "Creates an Elastic Search Immutable Settings"
  [{:keys [http-port transport-port data-dir]}]
  (.. (ImmutableSettings/settingsBuilder)
      (put "node.name" "embedded-elastic")
      (put "path.conf" ".")
      (put "path.data" data-dir)
      (put "http.port" (str http-port))
      (put "transport.tcp.port" (str transport-port))
      (put "index.store.type" "memory")
      ;; Disable dynamic scripting to prevent attacks on developer machines
      ;; See http://bouk.co/blog/elasticsearch-rce/
      (put "script.disable_dynamic" "true")
      build))

(defn- build-node
  "Creates the internal elastic search node that will run."
  [^ImmutableSettings node-settings]
  (let [^NodeBuilder builder (NodeBuilder/nodeBuilder)]
    (.. builder
        (settings node-settings)

        (clusterName "embedded-cluster")

        ;;Is the node going to be allowed to allocate data (shards) to it or not.
        (data true)

        ;;The node is local within a JVM. It will not try to connect to nodes outside
        (local true)

        ;; Starts the node
        node)))

(defrecord ElasticServer
  [
   http-port
   transport-port
   data-dir
   node
   ]

  lifecycle/Lifecycle

  (start
    [this system]
    (let [node-settings (create-settings this)
          _ (setup-logging node-settings)
          this (assoc this :node (build-node node-settings))]
      ;; See http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/cluster-health.html
      (info (pr-str (client/get
                         (format
                           "http://localhost:%s/_cluster/health?wait_for_status=yellow&timeout=50s"
                           (:http-port this)))))
      this))

  (stop
    [this system]
    (when-let [^Node node (:node this)]
      (.close node))
    (assoc this :node nil)))

(defn create-server
  ([]
   (create-server 9200 9300 "data"))
  ([http-port transport-port data-dir]
   (->ElasticServer http-port transport-port data-dir nil)))


(comment

  (def server (create-server))

  (def started-server (lifecycle/start server nil))

  (def stopped-server (lifecycle/stop started-server nil))

)

