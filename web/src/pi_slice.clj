(ns pi-slice
  (:gen-class))

(require '[defun :refer [defun]])

(defn -main
  []
  (println "Hello World"))

(defun my-print
  "Print different things for :user, :admin or other"
  ([:user] (println "Hello :user"))
  ([:admin] (println "Hello :admin"))
  ([other] (str "Hello " other)))
