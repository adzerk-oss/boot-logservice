# boot-logservice

[![Clojars Project][clojars-badge]][clojars-page]

This library for [Boot] projects does something boring in an
exciting new way: it configures
[tools.logging][tools-logging] to use
[SLF4J][slf4j] and [logback-classic][logback] in classpath isolation,
using a [pod][pods].

With your logging dependencies isolated, you no longer need to juggle
`:exclusions` in your project's dependencies, and hours are added to
your life.

## Usage

First, craft a [Logback configuration][logback-config] in
[Hiccup][hiccup]-style XML in your `build.boot`, like this:

```clojure
(def log-config
  [:configuration {:scan true, :scanPeriod "10 seconds"}
   [:appender {:name "FILE" :class "ch.qos.logback.core.rolling.RollingFileAppender"}
    [:encoder [:pattern "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"]]
    [:rollingPolicy {:class "ch.qos.logback.core.rolling.TimeBasedRollingPolicy"}
     [:fileNamePattern "logs/%d{yyyy-MM-dd}.%i.log"]
     [:timeBasedFileNamingAndTriggeringPolicy {:class "ch.qos.logback.core.rolling.SizeAndTimeBasedFNATP"}
      [:maxFileSize "64 MB"]]]
    [:prudent true]]
   [:appender {:name "STDOUT" :class "ch.qos.logback.core.ConsoleAppender"}
    [:encoder [:pattern "%-5level %logger{36} - %msg%n"]]
    [:filter {:class "ch.qos.logback.classic.filter.ThresholdFilter"}
     [:level "INFO"]]]
   [:root {:level "INFO"}
    [:appender-ref {:ref "FILE"}]
    [:appender-ref {:ref "STDOUT"}]]
   [:logger {:name "user" :level "ALL"}]
   [:logger {:name "boot.user" :level "ALL"}]])
```

Then, add [tools.logging][tools-logging] and this library to your
dependencies:

```clojure
(set-env! :dependencies '[[org.clojure/tools.logging "0.3.1"]
                          [adzerk/boot-logservice "X.Y.Z"]])
```

Next, `require` things:

```clojure
(require '[adzerk.boot-logservice :as log-service]
         '[clojure.tools.logging  :as log])
```
         
Initialize the log service and configure `tools.logging` to use it:

```clojure
(alter-var-root #'log/*logger-factory* (constantly (log-service/make-factory log-config)))
```

Wow:

```clojure
(log/info "Wow")
```

## Caveats

`clojure.tools.logging/error` takes an Exception argument that is
converted to a string before logging.  This is necessary because only
print-readable objects can pass through the pod to the underlying
logger.  It's not clear to us yet if this limitation prohibits anything important.

## Thanks

Much was learned from the logging configurations in the
[Pedestal samples](https://github.com/pedestal/pedestal/tree/master/samples).

## License

Copyright © 2014 Adzerk

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.

[Boot]:             https://github.com/boot-clj/boot
[clojars-badge]:    http://clojars.org/adzerk/boot-logservice/latest-version.svg?cache=2
[clojars-page]:     https://clojars.org/adzerk/boot-logservice
[SLF4J]:            http://www.slf4j.org/
[logback]:          http://logback.qos.ch/
[pods]:             https://github.com/boot-clj/boot/wiki/Pods
[logback-config]:   http://logback.qos.ch/manual/index.html
[hiccup]:           https://github.com/weavejester/hiccup
[tools-logging]:    https://github.com/clojure/tools.logging
