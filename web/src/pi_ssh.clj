(ns pi-ssh
  (:require [clj-ssh.ssh :as jsch]
            [clojure.core.async :as a :refer [chan thread timeout
                                              go go-loop
                                              >! <! >!! <!!
                                              close! put!]]
            [clojure.spec :as s]
            [taoensso.timbre :as t :refer [log trace debug info warn error fatal]]
            [util :as u]))

(defn eval-config
  "conf-path->conf-map"
  [conf-path]
  (->> conf-path
       (str (System/getProperty "user.dir"))
       (slurp)
       (read-string)
       (eval)))

(defn session-from
  "conf map->jsch.Session"
  [{:keys [:pi-ssh/agent-options
           :pi-ssh/id-options
           :pi-ssh/host
           :pi-ssh/session-options]}]
  (let [{:keys [:pi-ssh/use-system-ssh-agent]} agent-options
        {:keys [:pi-ssh/private-key-path :pi-ssh/passphrase]} id-options
        agent (doto (jsch/ssh-agent
                     (if (empty? agent-options)
                       {}
                       {:use-system-ssh-agent use-system-ssh-agent}))
                (jsch/add-identity (if (empty? id-options)
                                     {}
                                     {:private-key-path private-key-path
                                      :passphrase passphrase})))
        ;; Unfortunately, clj-ssh does not accept server-alive-interval
        ;; as an option. So we pull all the stuff out here to provide a
        ;; unified API. This also allows reasonable default values...
        {:keys [:pi-ssh/port
                :pi-ssh/username
                :pi-ssh/strict-host-key-checking
                :pi-ssh/server-alive-interval]} session-options]
    (doto (jsch/session agent
                        host
                        {:port (or port 22)
                         :username (or username "")
                         :strict-host-key-checking
                         (or strict-host-key-checking
                             true)})
      (.setServerAliveInterval (or server-alive-interval 30000)))))

(defn shell-streams
  "Takes a session and returns :in and :out io streams.
  Allows specification of io streams of the session.
  Executes the  under a hopefully (?!) safely open channel."
  ([session] (shell-streams session {}))
  ([session {:keys [in-stream out-stream]}]
   (jsch/with-connection session
     (let [channel (jsch/open-channel session 'shell)
           in (or in-stream (.getInputStream channel))
           out (or out-stream (.getOutputStream channel))]
       (jsch/with-channel-connection channel
         (debug "Channel connected, proceeding")
         ;; Update the state of the connections
         {:in in :out out })))))

;; (defn stream-in
;;   "Work with a shell channel stream.
;;   Occurs within with-channel-connection, hopefully.
;;   <PipedInputStream>ssh-stream."
;;   [channel in-stream]
;;   (go-loop [buffer (make-array Byte/TYPE 1024)
;;             available (.available in-stream)]
;;     (when (pos? available)
;;       (.read buffer 0 available)
;;       (>! channel (u/buffer->str buffer)))
;;     (recur buffer (.available in-stream))))

(defn shell-out
  "Recursively read from PipedInputStream and place onto channel"
  ([in-chan in-stream] (shell-out in-chan in-stream (make-array Byte/TYPE 1024)))
  ([in-chan in-stream buffer]
   (let [available (.available in-stream)]
     (if (pos? available)
       (put! in-chan
             (.read buffer 0 available)
             #(shell-out in-chan in-stream buffer))
       (recur in-chan in-stream buffer)))))
;; (defn stream-out
;;   "Work with a shell channel stream.
;;   Occurs within with-channel-connection, hopefully.
;;   <PipedOutputStream>ssh-stream"
;;   [s ssh-stream]
;;   (go-loop []))

(let [shell-in-chan (chan)
      shell-out-chan (chan)
      conn-chan (go
                  (-> (eval-config "/conf/ssh_aws.edn")
                      (s/conform :pi-ssh/conf-map)
                      (session-from)
                      (shell-streams)))]

  (go
    (let [{:keys [in out]} (<! conn-chan)]
      (shell-out shell-in-chan in)
      (go
        (info (<! shell-in-chan)))

      (go-loop [user-input (read-line)]
        (doto out
          (.flush)
          (.write (bytes user-input) 0 (count user-input)))
        (recur (read-line)))));; Stub of stuff to do potentially with the output from ssh chan)
