(ns ssh-piping
  (:require [clojure.core.async
             :as a
             :refer [go go-loop >! <! >!! <!!]]
            [clojure.spec :as s]
            [pi-ssh]
            [taoensso.timbre
             :as t
             :refer [debug]]
            [util :as u]))

(defn shell-out
  "Recursively read from PipedInputStream and place onto c.
  Largely for piping user input somewhere."
  ([c stream] (shell-out c stream (make-array Byte/TYPE 1024)))
  ([c stream buffer]
   (if-let [available (.available stream)]
     (if (pos? available)
       (do (.read buffer 0 available)
           (a/put! c (u/buffer->str buffer) #(shell-out c stream buffer)))))))

(defn user-in
  "Recursively readline from user and place onto PipedOutputStream"
  [c stream]
  (let [line (read-line)]
    (when line
           (doto stream
             (.flush)
             (.write (bytes user-in) 0 (count user-in))))
    (a/put! c line #(user-in c stream))))

(let [conn-chan (go (->> (pi-ssh/eval-config "/conf/ssh_aws.edn")
                         (s/conform :pi-ssh/conf-map)
                         (#(do (debug "Read configuration:" %) %))
                         (pi-ssh/session-from)
                         (pi-ssh/shell-streams)))
      ssh-out-c (a/chan)
      user-in-c (a/chan)]

  (a/take! conn-chan (fn [{in :pi-ssh/in out :pi-ssh/out}]
                       (shell-out ssh-out-c in)
                       #dbg
                       (user-in user-in-c out))
           ;; false
           )

  (a/take! ssh-out-c (fn [str] (debug str))
           ;; false
           ))
