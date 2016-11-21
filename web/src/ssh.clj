(ns pi-ssh
  (:require [clj-ssh.ssh :as jsch]
            [clojure.java.io :as io]
            [taoensso.timbre :as timbre
             :refer [log trace debug info warn error fatal report logf tracef
                     debugf infof warnf errorf fatalf reportf spy get-env]]
            [clojure.core.async :as a :refer [chan thread >! <! >!! <!!
                                              close! timeout]]))
;; {(atom jsch.Agent) #{(atom jsch.Session)}}
(def agents (atom {}))
;; {(atom jsch.Session) #{(atom jsch.channel)}}
(def connections (atom {}))

(defn eval-config
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
       (#(do (debug (str "Read config: " conf-path)) %))
       (eval)))

(defn fnil-set
  "x->(coll of coll-type)->(conj coll x)
  Return a function which either creates a set containing x or adds x to an existing set"
  [x]
  #(if %
     (conj % x)
     #{x}))

(defn session-from
  "conf map->(atom jsch.Session)"
  [{:keys [agent-options id-options host session-options]}]
  (let [agent (atom (jsch/ssh-agent agent-options))]
    (swap! agent #(do (jsch/add-identity % id-options) %))
    (let [session (atom (jsch/session @agent host session-options))]
      ;; Update the state of the agents
      ;; TODO Make this actually have distinct sessions.
      (swap! agents #(update % agent (fnil-set session)))
      session)))

(defn run-in-channel
  "Takes a session and a continuation: (fn \"handler\" [channel-atom in out])
  to execute with the channel open."
  ([session handler] (run-in-channel session handler {}))
  ([session handler {:keys [server-alive-interval in-stream out-stream]}]
   (swap! session
          (fn [sesh]
            (do
              (doto sesh
                (.setServerAliveInterval (or server-alive-interval 30000)))
              (jsch/with-connection sesh
                (let [channel (atom (jsch/open-channel sesh 'shell))
                      in (or in-stream (.getInputStream @channel))
                      out (or out-stream (.getOutputStream @channel))]
                  (jsch/with-channel-connection @channel
                    (debug "Channel connected, proceeding")
                    ;; Update the state of the connections
                    (swap! connections #(update % session (fnil-set channel)))
                    (handler channel in out)))))))))

(defn buffer->str
  "Array<Byte>->String
  Transforms a java array of bytes into a String."
  [buffer]
  (->> buffer
       (seq)
       (filter pos?)
       (map char)
       (clojure.string/join)))

(defn chandler
  "Work with a (atom channel), and its streams.
  (in is where ssh output goes, and out is where to put ssh input,
  in keeping with jsch \"conventions\")"
  [ch in out]
  (let [buffer (make-array Byte/TYPE 1024)
        num-chars (.read in buffer)
        connected (jsch/connected-channel? @ch)]
    (println (buffer->str buffer))
    (debug "Collected stdout, channel connected:\t" connected)
    (if connected
      (let [input (.getBytes "ls")]
        (let [new-ch (doto out
                       (.flush)
                       (.write input 0 2))
              buff (make-array Byte/TYPE 1024)
              num-cs (.read in buff)]
          (println (buffer->str buff)))
        (debug "Channel disconnected")))))

(-> "/conf/ssh.edn"
    (eval-config)
    (session-from)
    (run-in-channel chandler))

(do (println "connections:\t")
    (clojure.pprint/pprint @connections)
    (println "agents:\t")
    (clojure.pprint/pprint @agents))

;; (let [dir (System/getProperty "user.dir")
      ;; buffer (make-array Byte/TYPE 1024)]
  ;; (with-open [in (io/input-stream (str dir "/in.log"))
              ;; out (io/output-stream (str dir "/out.log"))]
    ;; (let [num-chars (.read in buffer)]
      ;; ( ->> buffer
       ;; (seq)
       ;; (filter #(not (zero? %)))
       ;; (map char)
       ;; (clojure.string/join))
      ;; (.write out buffer 0 num-chars)))
  ;; (slurp (str dir "/out.log")))
