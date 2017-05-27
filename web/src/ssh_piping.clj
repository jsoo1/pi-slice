(ns ssh-piping
  (:require [clojure.core.async :as a :refer [go go-loop >! <! >!! <!!]]
            [clojure.spec :as s]
            [pi-ssh]
            [taoensso.timbre :as t :refer [debug]]
            [util :as u]))

(let [{in :pi-ssh/in-stream out :pi-ssh/out-stream}
      (<!! (go (->> (pi-ssh/eval-config "/conf/ssh_aws.edn")
                    (s/conform :pi-ssh/conf-map)
                    (#(do (debug "Read configuration:" %) %))
                    (pi-ssh/session-from)
                    (pi-ssh/shell-streams)
                    (#(do (debug "streams" %) %)))))
      ssh-out-c (a/chan)
      user-in-c (a/chan)]

  (let [buffer (make-array Byte/TYPE 1024)]
    (go-loop []
      (when (pos? (.available in))
        (do (.read buffer 0 (.available in))
            (>! ssh-out-c (u/buffer->str buffer))))
      (recur)))

  (go-loop [line nil]
    (when line
      (doto out
        (.flush)
        (.write (.getBytes line) 0 (count line))))
    (recur (<! user-in-c)))

  (go-loop [line nil]
    (when line (debug line))
    (recur (<! ssh-out-c)))

  (go-loop [line (read-line)]
    (debug line)
    (>! user-in-c line)
    (recur (read-line))))
