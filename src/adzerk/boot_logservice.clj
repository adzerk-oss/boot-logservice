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
  (pod/with-eval-in worker-pod
    (require '[clojure.tools.logging.impl :refer [slf4j-factory enabled? write! get-logger]]
             '[boot.pod :as pod]
             '[clojure.java.io :as io])
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
          (pod/with-eval-in worker-pod
            (enabled? (get-logger* ~(str logger-ns)) ~level)))
        (write! [logger level e msg]
          (let [msg-str (if e (str msg " - " (class e) ": " (.getMessage e)) msg)]
            (pod/with-eval-in worker-pod
              (let [logger (get-logger* ~(str logger-ns))]
                (condp = ~level
                  :trace (.trace logger ~msg-str)
                  :debug (.debug logger ~msg-str)
                  :info  (.info  logger ~msg-str)
                  :warn  (.warn  logger ~msg-str)
                  :error (.error logger ~msg-str)
                  :fatal (.error logger ~msg-str)
                  (throw (IllegalArgumentException. (str ~level))))))))))))

(defn tools-logging-dep
  [depvec]
  (or (first (for [[sym v :as coord] depvec
                   :when (= sym 'org.clojure/tools.logging)] coord))
      (throw (RuntimeException. "You must supply a org.clojure/tools.logging dependency in order to use boot-logservice"))))

(defn write-logback-xml!
  [worker-pod tempdir xml]
  (when xml
    (spit (io/file tempdir "logback.xml")
          (pod/with-eval-in worker-pod
            (require '[clojure.data.xml :refer [emit-str sexp-as-element emit]])
            (emit-str (sexp-as-element ~xml))))))

(defn make-factory [& [xml]]
  (let [logback-tmpdir (core/tmp-dir!)
        logging-dep    (tools-logging-dep (core/get-env :dependencies))
        pod-env        (assoc (core/get-env)
                         :dependencies (conj *dependencies* logging-dep)
                         :directories #{(.getPath logback-tmpdir)})
        worker-pod     (pod/make-pod pod-env)]
    (write-logback-xml! worker-pod logback-tmpdir xml)
    (slf4j-service-factory-factory worker-pod)))
