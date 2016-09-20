(ns web.core
  (:gen-class)
  (require '[defun :as [defun]]
           [clojure.repl :as repl]))

(refer-clojure)

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println (str "Hello, World!" args)))

; (defun my-print
;   [:hello]
;   (println :hello))
