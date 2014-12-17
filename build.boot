(set-env!
 :source-paths  #{"src"}
 :dependencies '[[org.clojure/clojure "1.6.0"     :scope "provided"]
                 [boot/core           "2.0.0-rc1" :scope "provided"]
                 [adzerk/bootlaces    "0.1.5"     :scope "test"]])

(require '[adzerk.bootlaces :refer :all])

(def +version+ "1.0.0-SNAPSHOT")

(bootlaces! +version+)

(task-options!
 pom  {:project     'adzerk/boot-logservice
       :version     +version+
       :description "Carefree Logback logging in boot projects"
       :url         "https://github.com/adzerk/boot-logservice"
       :scm         {:url "https://github.com/adzerk/boot-logservice"}
       :license     {:name "Eclipse Public License"
                     :url  "http://www.eclipse.org/legal/epl-v10.html"}})
