(set-env!
  :resource-paths #{"src"}
  :dependencies '[[defun "0.3.0-alapha"]
                  [proto-repl "0.3.1"]])

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
    :source-paths #(into % ["dev" "src"])
    :dependencies #(into % '[[org.clojure/tools.namespace "0.2.11"]
                             [defun "0.3.0-alapha"]]))

  (require 'clojure.tools.namespace.repl)
  (eval '(apply clojure.tools.namespace.repl/set-refresh-dirs
           (get-env :directories)))
  identity)
