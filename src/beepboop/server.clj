(ns beepboop.server
  (:require
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


(defn send-response
  [client-channel response]
  (some->> response
           .getBytes
           ByteBuffer/wrap
           (.write client-channel)))


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
  [server-channel selector]
  (when-let [client-channel (.accept server-channel)]
    (doto client-channel
      (.configureBlocking false))
    (.register client-channel selector SelectionKey/OP_READ)))


(defn read-from-client
  [client-channel]
  (let [buffer (ByteBuffer/allocate 1024)
        bytes-read (.read client-channel buffer)]
    (when (pos? bytes-read)
      (String. (.array buffer) 0 bytes-read))))


(defn handle-client-read
  [key handler]
  (let [client-channel (.channel key)]
    (if-let [s (read-from-client client-channel)]
      (handler client-channel s)
      (.close client-channel))))


(defn process-selector-keys
  [selector handler]
  (doseq [key (.selectedKeys selector)]
    (cond
      (.isAcceptable key)
      (accept-client (.channel key) selector)

      (.isReadable key)
      (handle-client-read key handler)))
  (.clear (.selectedKeys selector)))


(defn shutdown-server
  [server-channel selector]
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
  [selector handler]
  (when (pos? (.select selector 1000))
    (process-selector-keys selector handler)))


(defn server-loop
  [server-channel selector handler shutdown]
  (loop []
    (if (not @shutdown)
      (do
        (select-and-process selector handler)
        (recur))
      (shutdown-server server-channel selector))))


(defn start-server
  [{{:keys [bind handler]} ::donut/config}]
  (let [server-channel (create-server-channel bind)
        selector (create-selector-with-server server-channel)
        shutdown (atom false)
        server (future (server-loop server-channel selector handler shutdown))]
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
                                [:handler 'fn?]
                                [:bind Bind]]
         ::donut/start start-server
         ::donut/stop stop-server))
