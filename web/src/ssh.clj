(ns pi-ssh
  (:require [clj-ssh.ssh :as jsch]
            [clojure.core.async :as a :refer [chan thread >! <! >!! <!!
                                              close! timeout go go-loop]]
            [clojure.spec :as s]
            [taoensso.timbre :refer [log trace debug info warn error fatal
                                     report logf tracef debugf infof warnf errorf
                                     fatalf reportf spy get-env]]
            [util :as u]))

;; Host
(s/def ::host string?)
;; Agent Options
(s/def ::use-system-ssh-agent boolean?)
(s/def ::agent-options
  (s/keys :opt-un [::use-system-ssh-agent]))
;; Identity Options
(s/def ::private-key-path string?)
(s/def ::passphrase bytes?)
(s/def ::id-options
  (s/keys :opt-un [::private-key-path ::passphrase]))
;; Session Options
(s/def ::port integer?)
(s/def ::username string?)
(s/def ::strict-host-key-checking keyword?)
(s/def ::server-alive-interval integer?)
(s/def ::session-options
  (s/keys* :req-un [::username]
           :opt-un [::port ::server-alive-interval ::strict-host-key-checking]))
;; All Together in a configuration map
(s/def ::conf-map
  (s/keys :req [::host ::session-options]
          :opt [::agent-options ::id-options]))

(defn eval-config
  "conf-path->conf-map"
  [conf-path]
  (->> conf-path
       (str (System/getProperty "user.dir"))
       (slurp)
       (read-string)
       (do (debug "Read config:" conf-path))
       (eval)))

(defn session-from
  "conf map->jsch.Session"
  [{:keys [:pi-ssh/agent-options :pi-ssh/id-options
           :pi-ssh/host :pi-ssh/session-options]}]
  (let [agent (doto (jsch/ssh-agent (or agent-options {}))
                (jsch/add-identity (or id-options {})))
        ;; Unfortunately, clj-ssh does not accept server-alive-interval
        ;; as an option. So we pull all the stuff out here to provide a
        ;; unified API. This also allows reasonable default values...
        {:keys [:port :username :strict-host-key-checking
                :server-alive-interval]} session-options]
    (doto (jsch/session agent
                        host
                        {:port (or port 22)
                         :username (or username "")
                         :strict-host-key-checking (or strict-host-key-checking
                                                       true)})
      (.setServerAliveInterval (or server-alive-interval 30000)))))

(defn run-in-channel
  "Takes a session and a continuation: (fn \"handler\" [in out])
  Allows specification of io streams of the session.
  Executes the handler under a hopefully safely open channel."
  ([session handler] (run-in-channel session handler {}))
  ([session handler {:keys [in-stream out-stream]}]
   (jsch/with-connection session
     (let [channel (jsch/open-channel session 'shell)
           in (or in-stream (.getInputStream channel))
           out (or out-stream (.getOutputStream channel))]
       (jsch/with-channel-connection channel
         (debug "Channel connected, proceeding")
         ;; Update the state of the connections
         (handler in out))))))

(defn chandler
  "Work with a shell channel streams.
  Occurs within with-channel-connection, hopefully.
  <PipedInputStream>in and <PipedOutputStream>out.

  A note on the streams: in is where ssh output goes,
  and out is where to put ssh input, in keeping with java io."
  [ssh-stream]
  (go-loop [channel (chan)
            buffer (make-array Byte/TYPE 1024)
            char-count (.read ssh-stream buffer)]
    (if-not (pos? char-count)
      ())))

(-> "/conf/ssh.edn"
    (eval-config)
    (session-from)
    (run-in-channel chandler))
