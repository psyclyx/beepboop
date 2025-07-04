(ns beepboop.server
  (:require
    [beepboop.game :as game]
    [clojure.string :as str]
    [clojure.tools.logging :as log]
    [donut.system :as donut]
    [malli.core :as m]
    [psyclyx.pastry :as pastry])
  (:import
    (java.net
      InetSocketAddress)
    (java.nio
      ByteBuffer)
    (java.nio.channels
      SelectionKey
      Selector
      ServerSocketChannel
      SocketChannel)))


(def Bind
  [:map
   [:host {:optional true} :string]
   [:port [:int {:min 1, :max 65535}]]])


(defn socket-address
  [{:keys [host port] :as bind}]
  (m/assert Bind bind)
  (if host
    (InetSocketAddress. host port)
    (InetSocketAddress. port)))


(defn send-message
  [client-channel message]
  (let [message-bytes (if (string? message)
                        (.getBytes message)
                        (byte-array message))]
    (->> message-bytes
         ByteBuffer/wrap
         (.write client-channel))))


(defn create-server-channel
  [bind]
  (doto (ServerSocketChannel/open)
    (.configureBlocking false)
    (.bind (socket-address bind))))


(defn create-selector-with-server
  [server-channel]
  (let [selector (Selector/open)]
    (.register server-channel selector SelectionKey/OP_ACCEPT)
    selector))


(defn accept-client
  [{:keys [selector connection-handler connections game] :as _context} server-channel]
  (when-let [client-channel (.accept server-channel)]
    (doto client-channel
      (.configureBlocking false)
      (.register selector SelectionKey/OP_READ))
    (swap! connections assoc client-channel (connection-handler client-channel game))))


(defn read-from-client
  [client-channel]
  (let [buffer (ByteBuffer/allocate 1024)
        bytes-read (.read client-channel buffer)]
    (when (pos? bytes-read)
      (byte-array (take bytes-read (.array buffer))))))


(defn handle-client-read
  [{:keys [connections] :as _context} key]
  (let [client-channel (.channel key)
        {:keys [handle-packet handle-disconnect] :as connection} (get @connections client-channel)]
    (if-let [s (read-from-client client-channel)]
      (handle-packet connection s)
      (handle-disconnect connection))))


(defn process-selector-keys
  [{:keys [selector] :as context}]
  (doseq [key (.selectedKeys selector)]
    (cond
      (.isAcceptable key)
      (accept-client context (.channel key))

      (.isReadable key)
      (handle-client-read context key)))
  (.clear (.selectedKeys selector)))


(defn shutdown-server
  [{:keys [server-channel selector connections] :as _context}]
  (doseq [[_channel {:keys [handle-disconnect] :as connection}] @connections]
    (try
      (handle-disconnect connection)
      (catch Exception _ nil)))
  ;; Close all registered channels first
  (doseq [key (.keys selector)]
    (let [channel (.channel key)]
      ;; Don't close server channel twice
      (when (not= channel server-channel)
        (try
          (.close channel)
          (catch Exception _ nil)))))
  ;; Close server channel
  (.close server-channel)
  ;; Close selector last
  (.close selector))


(defn select-and-process
  [{:keys [selector game] :as context}]
  (let [timeout (game/maybe-tick game)]
    (when (pos? (.select selector timeout))
      (process-selector-keys context))))


(defn server-loop
  [{:keys [shutdown] :as context}]
  (loop []
    (if (not @shutdown)
      (do
        (try
          (select-and-process context)
          (catch Exception e (log/error e)))
        (recur))
      (shutdown-server context))))


(defn start-server
  [{{:keys [bind connection-handler game]} ::donut/config}]
  (let [server-channel (create-server-channel bind)
        shutdown (atom false)
        context {:server-channel server-channel
                 :connection-handler connection-handler
                 :connections (atom {})
                 :selector (create-selector-with-server server-channel)
                 :game game
                 :shutdown shutdown}
        server (future (server-loop context))]
    {:shutdown shutdown
     :server server}))


(defn stop-server
  [{{:keys [server shutdown]} ::donut/instance}]
  (reset! shutdown true)
  @server)


(defmethod pastry/->component ::tcp
  [m]
  (assoc m
         ::donut/config-schema [:map
                                [:connection-handler 'fn?]
                                [:bind Bind]]
         ::donut/start start-server
         ::donut/stop stop-server))
