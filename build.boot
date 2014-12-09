(set-env!
 :src-paths    #{"src"}
 :dependencies '[[org.clojure/clojure       "1.6.0"       :scope "provided"]
                 [boot/core                 "2.0.0-pre28" :scope "provided"]
                 [tailrecursion/boot-useful "0.1.3"       :scope "test"]])

(require '[tailrecursion.boot-useful :refer :all])

(def +version+ "1.0.0-SNAPSHOT")

(useful! +version+)

(task-options!
 pom  [:project     'adzerk/boot-logservice
       :version     +version+
       :description "Carefree Logback logging in boot projects"
       :url         "https://github.com/adzerk/boot-logservice"
       :scm         {:url "https://github.com/adzerk/boot-logservice"}
       :license     {:name "Eclipse Public License"
                     :url  "http://www.eclipse.org/legal/epl-v10.html"}])
