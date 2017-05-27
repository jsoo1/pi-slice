(ns pi-ssh-spec
  (:require [clojure.spec :as s])
  (:import (java.io PipedInputStream PipedOutputStream)))

;; Host
(s/def :pi-ssh/host string?)

;; Agent Options
(s/def :pi-ssh/use-system-ssh-agent boolean?)
(s/def :pi-ssh/agent-options
  (s/keys :opt-un [:pi-ssh/use-system-ssh-agent]))

;; Identity Options
(s/def :pi-ssh/private-key-path string?)
(s/def :pi-ssh/passphrase bytes?)
(s/def :pi-ssh/id-options
  (s/keys :opt-un [:pi-ssh/private-key-path :pi-ssh/passphrase]))

;; Session Options
(s/def :pi-ssh/port integer?)
(s/def :pi-ssh/username string?)
(s/def :pi-ssh/strict-host-key-checking keyword?)
(s/def :pi-ssh/server-alive-interval integer?)
(s/def :pi-ssh/session-options (s/keys
                                :req [:pi-ssh/username]
                                :opt [:pi-ssh/port
                                      :pi-ssh/server-alive-interval
                                      :pi-ssh/strict-host-key-checking]))

;; All together in a configuration map
(s/def :pi-ssh/conf-map
  (s/keys :req [:pi-ssh/host :pi-ssh/session-options]
          :opt [:pi-ssh/agent-options :pi-ssh/id-options]))

;; JSCH in/out streams
(s/def :pi-ssh/in-stream #(= (class %) PipedInputStream))
(s/def :pi-ssh/out-stream #(= (class %) PipedOutputStream))
(s/def :pi-ssh/streams (s/keys :req [:pi-ssh/in-stream :pi-ssh/out-stream]))
