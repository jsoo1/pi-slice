(ns pi-slice
  (:require [org.httpkit.server :refer :all]
            [ring.middleware.reload :refer [wrap-reload]]
            [defun :refer [defun]]
            [taoensso.timbre :as timbre
             :refer [log trace debug info warn error fatal report logf tracef
                     debugf infof warnf errorf fatalf reportf spy get-env]]))

(defn handler
  [request]
  (with-channel request channel
    (on-close channel (fn closing-callback [status] (println "channel closed: " status)))
    (on-receive channel (fn received-callback [data]
                          (send! channel data)))))

(defn dev-app
  "The whole dev app"
  []
  (info "Starting http-kit on port 9999")
  (run-server (wrap-reload handler {:port 9999})))
