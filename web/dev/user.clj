(ns user
  (:require clojure.tools.namespace.repl
            clojure.test
            [clojure.repl :refer :all]
            [proto-repl.saved-values]
            web.core
            defun))


(defn reset []
  (clojure.tools.namespace.repl/refresh))

(println "web.core REPL Leiningen project started")
