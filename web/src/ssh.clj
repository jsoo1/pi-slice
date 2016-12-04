(ns pi-ssh
  (:require [clj-ssh.ssh :as jsch]
            [clojure.core.async :as a :refer [chan thread >! <! >!! <!!
                                              close! timeout go go-loop]]
            [clojure.java.io :as io]
            [taoensso.timbre :as timbre
             :refer [log trace debug info warn error fatal report logf tracef
                     debugf infof warnf errorf fatalf reportf spy get-env]]
            [util :as u]))

;; SSH state tracking atoms
;; TODO Determine if really needed as the implementation might be out of scope.
;; Else, implement them.
;; (def agents
;;   ^{:doc "{jsch/Agent #{jsch/Session}}
;; A map of jsch/Agents to a set of jsch/Sessions using the associated agent."}
;;   (atom {}))
;; (def connections
;;   ^{:doc "{jsch/Session #{jsch/channel}}
;; A map of jsch/Sessions to a set of jsch/Channels using the associated session."}
;;   (atom {}))

(defn eval-config
  "conf-path->
      {:host String
      :agent-options {:use-system-ssh-agent bool}
      :id-options {:private-key-path String :passphrase ByteArray}
      :session-options {:port int
                        :username String
                        :strict-host-key-checking keyword
                        :server-alive-interval int}}"
  [conf-path]
  (->> conf-path
       (str (System/getProperty "user.dir"))
       (slurp)
       (read-string)
       (#(do (debug (str "Read config: " conf-path)) %))
       (eval)))

(defn session-from
  "conf map->jsch.Session"
  [{:keys [agent-options id-options host session-options]}]
  (let [agent (doto (jsch/ssh-agent #dbg agent-options)
                (jsch/add-identity #dbg id-options))
        ;; Unfortunately, clj-ssh does not accept server-alive-interval
        ;; as an option. So we pull all the stuff out here to provide a
        ;; unified API. This also allows reasonable default values...
        {port ::port
         username ::username
         strict-host-key-checking ::strict-host-key-checking
         server-alive-interval ::server-alive-interval} session-options]
    (doto (jsch/session agent
                        host
                        {::port (or port 22)
                         ::username (or username "")
                         ::strict-host-key-checking (or strict-host-key-checking
                                                       true)})
      (.setServerAliveInterval (or server-alive-interval 30000)))))

(defn print-do
  [x]
  (do (clojure.pprint/pprint x) x))

(defn run-in-channel
  "Takes a session and a continuation: (fn \"handler\" [in out])
  to execute with the channel open."
  ([session handler] (run-in-channel session handler {}))
  ([session handler {:keys [server-alive-interval in-stream out-stream]}]
   (do
     (doto session
       )
     (jsch/with-connection session
       (let [channel (atom (jsch/open-channel session 'shell))
             in (or in-stream (.getInputStream @channel))
             out (or out-stream (.getOutputStream @channel))]
         (jsch/with-channel-connection @channel
           (debug "Channel connected, proceeding")
           ;; Update the state of the connections
           (handler in out)))))))

(defn slurp-stream
  "[stream, (optional)size]->java Array<Byte> of length size
  Creates a realized buffer array of all input from a stream."
  ([stream] (slurp-stream stream 1024))
  ([stream size]
   (let [buffer (make-array Byte/TYPE size)]
     (do #dbg (.read stream buffer 0 (.available stream))
        (seq buffer)))))

(defn chandler
  "Work with a shell channel streams.
  Occurs within with-channel-connection, hopefully.
  <PipedInputStream>in and <PipedOutputStream>out.

  A note on the streams: in is where ssh output goes,
  and out is where to put ssh input, in keeping with java io."
  [in out]
  (let [in-chan (chan)
        out-chan (chan)]
    (go-loop [])))

(-> "/conf/ssh.edn"
    (eval-config)
    (session-from)
    (run-in-channel chandler))

(do (println "connections:\t")
    (clojure.pprint/pprint @connections)
    (println "agents:\t")
    (clojure.pprint/pprint @agents))

