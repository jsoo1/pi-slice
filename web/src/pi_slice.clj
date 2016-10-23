(ns pi-slice
  (:require [org.httpkit.server :refer :all]
            [ring.middleware.reload :refer [wrap-reload]]
            [defun :refer [defun]]))

(defun handler
  [request]
  (with-channel request channel
    (if (websocket? channel)
      (on-receive channel (fn received-callback [data]
                            (send! channel data)))
      (send! channel {:status 200
                      :headers {"Content Type" "text/plain"}
                      :body "Long-polling?"}))))

(defn dev-app
  "The whole dev app"
  []
  (println "Starting http-kit on port 3030 under symbol 'server'")

  (run-server (wrap-reload handler {:port 9999})))
