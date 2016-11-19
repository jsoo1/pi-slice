(ns pi-ssh
  (:require [clj-ssh.ssh :as jsch]
            [taoensso.timbre :as timbre
             :refer [log trace debug info warn error fatal report logf tracef
                     debugf infof warnf errorf fatalf reportf spy get-env]]
            [clojure.core.async :as a :refer [chan thread >! <! >!! <!! close! timeout]]))

(defn eval-config!
  "conf-path->
      {:host String
      :agent-options {:use-system-ssh-agent bool}
      :id-options {:private-key-path String :passphrase ByteArray}
      :session-options {:port int
                        :username String
                        :strict-host-key-checking keyword}}"
  [conf-path]
  (->> conf-path
       (str (System/getProperty "user.dir"))
       (slurp)
       (read-string)
       (#(do (info (str "Read config: " conf-path)) %))
       (eval)))

(defn sesh
  "conf map->jsch.Session"
  [{:keys [agent-options id-options host session-options]}]
  (let [agent (jsch/ssh-agent agent-options)]
    (jsch/add-identity agent id-options)
    (let [session (jsch/session agent host session-options)]
      session)))

(defn connect-sesh!
  "[conf keep-alive-interval]->connected session.
  Sets ServerAliveInterval to 30sec by default."
  ([session]
   (doto session
     (.setServerAliveInterval 30000)
     (.connect)))
  ([session keep-alive]
     (doto session
       (.setServerAliveInterval keep-alive)
       (.connect))))

(defn transmit-to!
  "[session, chan]->core.async channel waiting for data in."
  [session c]
  (let [channel (jsch/open-channel session 'shell)]
    (jsch/with-channel-connection channel
      ())))
