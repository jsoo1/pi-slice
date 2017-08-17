(ns ssh-piping
  (:require [clojure.core.async :as a :refer [go go-loop >! <! >!! <!!]]
            [clojure.spec :as s]
            [pi-ssh]
            [taoensso.timbre :as t :refer [debug]]
            [util :as u]))


(def chans (let [{:keys [:pi-ssh/in-stream :pi-ssh/out-stream]}
                 (<!! (go (->> (pi-ssh/eval-config "/conf/ssh_aws.edn")
                               (s/conform :pi-ssh/conf-map)
                               (#(do (debug "Read configuration:" %) %))
                               (pi-ssh/session-from)
                               (pi-ssh/shell-streams))))

                 ssh-out-chan (a/chan)
                 user-in-chan (a/chan)

                 user-in-pipe-chan (go-loop [line (<! user-in-chan)]
                                     (condp = line
                                       ":q" :pi-ssh/quit
                                       nil :pi-ssh/closed
                                       (do (doto out-stream
                                             (.flush)
                                             (.write (.getBytes line) 0 (count line)))
                                           (recur (<! user-in-chan)))))

                 user-in-status-chan        (go-loop [line (read-line)]
                                              (debug line)
                                              (condp = line
                                                ":q" (do (a/close! user-in-chan)
                                                         :pi-ssh/quit)
                                                (recur (read-line))))

                 ssh-out-pipe-chan (let [buffer (make-array Byte/TYPE 1024)]
                                     (go-loop []
                                       (condp = (.available in-stream)
                                         0 :pi-ssh/closed
                                         (do (.read buffer 0 (.available in-stream))
                                             (debug (u/buffer->str buffer))
                                             (>! ssh-out-chan (u/buffer->str buffer))
                                             (recur)))))

                 ssh-display-chan (go-loop [output (<! ssh-out-chan)]
                                    (condp = output
                                      :pi-ssh/closed :pi-ssh/closed
                                      nil :pi-ssh/closed
                                      (do (debug output)
                                          (recur (<! ssh-out-chan)))))]

             {:out ssh-out-chan
              :in user-in-chan
              :in-status user-in-status-chan
              :in-pipe user-in-pipe-chan
              :out-pipe ssh-out-pipe-chan
              :display ssh-display-chan}))
