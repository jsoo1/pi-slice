(ns pi-ssh
  (:require [clj-ssh.ssh :as jsch]
            [clojure.spec :as s]))

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
                         (or strict-host-key-checking true)})
      (.setServerAliveInterval (or server-alive-interval 30000)))))

(defn shell-streams
  "Takes a session and returns :in and :out io streams.
  Allows specification of io streams of the session.
  Executes the  under a hopefully (?!) safely open channel."
  ([session] (shell-streams session {}))
  ([session {:keys [in-stream out-stream]}]
   (jsch/with-connection session
     (let [channel (jsch/open-channel session 'shell)
           ^PipedInputStream in (or in-stream (.getInputStream channel))
           ^PipedOutputStream out (or out-stream (.getOutputStream channel))]
       (jsch/with-channel-connection channel
         ;; Update the state of the connections
         {::in in ::out out })))))
