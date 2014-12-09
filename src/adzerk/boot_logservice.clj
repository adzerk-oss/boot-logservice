(ns adzerk.boot-logservice
  (:require [boot.pod                   :as    pod]
            [boot.util                  :as    util]
            [boot.core                  :as    core]
            [boot.task.built-in         :as    task]
            [clojure.java.io            :as    io]
            [boot.util                  :refer [with-let]]
            [clojure.tools.logging      :refer [*logger-factory*]]
            [clojure.tools.logging.impl :refer [Logger LoggerFactory]]))

(def ^:dynamic *dependencies*
  '[[ch.qos.logback/logback-classic "1.1.2" :exclusions [org.slf4j/slf4j-api]]
    [org.slf4j/jul-to-slf4j         "1.7.7"]
    [org.slf4j/jcl-over-slf4j       "1.7.7"]
    [org.slf4j/log4j-over-slf4j     "1.7.7"]
    [org.clojure/data.xml           "0.0.8"]])

(defn slf4j-service-factory-factory    ;lol
  [worker-pod]
  (pod/eval-in worker-pod
    (require '[clojure.tools.logging.impl :refer [slf4j-factory enabled? write! get-logger]]
             '[boot.pod :as pod])
    (def factory (slf4j-factory))
    (def loggers (atom {}))
    (defn get-logger* [logger-ns]
      (if-let [logger (get @loggers logger-ns)]
        logger
        (let [new-logger (get-logger factory logger-ns)]
          (swap! loggers assoc logger-ns new-logger)
          new-logger))))
  (reify LoggerFactory
    (name [_] "org.slf4j-service")
    (get-logger [_ logger-ns]
      (reify Logger
        (enabled? [logger level]
          (pod/eval-in worker-pod
            (enabled? (get-logger* ~(str logger-ns)) ~level)))
        (write! [logger level e msg]
          (pod/eval-in worker-pod
            (write! (get-logger* ~(str logger-ns)) ~level ~(str e) ~msg)))))))

(defn stringify-xml
  [worker-pod xml]
  (pod/eval-in worker-pod
    (require '[clojure.data.xml :refer [emit-str sexp-as-element emit]])
    (emit-str (sexp-as-element ~xml))))

(defn tools-logging-dep
  [depvec]
  (or (first (for [[sym v :as coord] depvec
                   :when (= sym 'org.clojure/tools.logging)] coord))
      (throw (RuntimeException. "Must provide org.clojure/tools.logging"))))

(defn make-factory [& [xml]]
  (let [logback-tmpdir (core/mksrcdir!)
        logging-dep    (tools-logging-dep (core/get-env :dependencies))
        worker-pod     (pod/make-pod
                        (assoc (core/get-env)
                          :dependencies (conj *dependencies* logging-dep)
                          :src-paths #{(.getPath logback-tmpdir)}))]
    (when xml
      (let [xml-string (stringify-xml worker-pod xml)]
        (spit (io/file logback-tmpdir "logback.xml") xml-string)))
    (slf4j-service-factory-factory worker-pod)))
