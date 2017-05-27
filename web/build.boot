(set-env!
 :source-paths #{"src" "spec"}
 :dependencies '[[org.clojure/clojure "1.9.0-alpha14"]
                 [org.clojure/core.async "0.3.443"]
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
 jar {:manifest {"Foo" "bar"}}
 repl {:init-ns 'pi-slice})


(require '[boot.boot-build :refer :all])

(println "Welcome to boot")

(deftask dev "Profile for development with Proto Repl in Atom"
  []
  (println "Dev profile running")
  (set-env!
   :init-ns 'pi-ssh
   :source-paths #(into % ["dev" "src" "spec"])
   :dependencies #(into % '[[org.clojure/tools.namespace "0.2.11"]
                            [proto-repl "0.3.1"]
                            [clj-ssh "0.5.14"]]))

  (require 'clojure.tools.namespace.repl)
  (eval '(apply clojure.tools.namespace.repl/set-refresh-dirs
                (get-env :directories)))
  identity)

(deftask cider "CIDER profile"
  []
  (set-env!
   :source-paths #{"src" "spec"}
   :dependencies '[[org.clojure/clojure "1.9.0-alpha14"]
                   [org.clojure/core.async "0.3.442"]
                   [com.taoensso/timbre "4.7.4"]
                   [defun "0.3.0-alapha"]
                   [ring/ring-core "1.5.0"]
                   [ring/ring-devel "1.6.0-beta6"]
                   [stylefruits/gniazdo "1.0.0"]
                   [http-kit "2.2.0"]
                   [clj-ssh "0.5.14"]]) (require 'boot.repl)
  (swap! @(resolve 'boot.repl/*default-dependencies*)
         concat '[[org.clojure/tools.nrepl "0.2.12"]
                  [cider/cider-nrepl "0.10.0"]
                  [refactor-nrepl "2.0.0-SNAPSHOT"]])
  (swap! @(resolve 'boot.repl/*default-middleware*)
         concat '[cider.nrepl/cider-middleware
                  refactor-nrepl.middleware/wrap-refactor])
  identity)
