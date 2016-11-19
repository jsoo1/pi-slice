(set-env!
  :resource-paths #{"src" "spec"}
  :dependencies '[[org.clojure/clojure "1.9.0-alpha13"]
                  [org.clojure/core.async "0.2.395"]
                  [com.taoensso/timbre "4.7.4"]
                  [defun "0.3.0-alapha"]
                  [ring/ring-core "1.5.0"]
                  [ring/ring-devel "1.6.0-beta6"]
                  [stylefruits/gniazdo "1.0.0"]
                  [http-kit "2.2.0"]
                  [clj-ssh "0.5.14"]])

(task-options!
  pom {:project 'pi-slice
       :version "0.1.0"}
  jar {:manifest {"Foo" "bar"}})

(require '[boot.boot-build :refer :all])

(println "Welcome to boot")

(deftask dev
  "Profile for development.
  Starting the repl with the dev profile...
  boot dev repl "
  []
  (println "Dev profile running")
  (set-env!
    :init-ns 'user
    :source-paths #(into % ["dev" "src" "spec"])
    :dependencies #(into % '[[org.clojure/tools.namespace "0.2.11"]
                             [proto-repl "0.3.1"]
                             [clj-ssh "0.5.14"]]))

  (require 'clojure.tools.namespace.repl)
  (eval '(apply clojure.tools.namespace.repl/set-refresh-dirs
           (get-env :directories)))
  identity)

