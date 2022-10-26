(ns web-server.core
  (:import [java.net ServerSocket]
           [java.io BufferedReader BufferedWriter InputStreamReader OutputStreamWriter]
           [java.nio.file Files Paths]
           [java.nio.charset StandardCharsets]
           [java.util.concurrent Executors ArrayBlockingQueue ThreadFactory TimeUnit]))

(defrecord Status [code msg])
(defrecord Response [status content])

(def http-version "HTTP/1.1")
(def http-status-codes {200 "OK"
                        404 "Not Found"
                        405 "Method Not Allowed"
                        500 "Internal Server Error"})
(def log-queue (ArrayBlockingQueue. 128))
(def static-dir (atom "."))

(defn- start-logger
  []
  (-> (Thread. #(loop []
                  (println (.take log-queue))
                  (recur)) "logger")
      (.start)))

(defn- thread-factory
  [name]
  (let [n (atom 0)]
    (reify ThreadFactory
      (newThread [_ runnable]
        (swap! n inc)
        (Thread. runnable (str name "-" @n))))))

(defn- log
  [msg]
  (.offer log-queue
          (str "[" (.getName (Thread/currentThread)) "] " msg)
          100 TimeUnit/MILLISECONDS))

(defn- read-http-request
  [in]
  (let [[method path version] (.split (.readLine in) " ")]
    {:method method
     :path path
     :version version}))

(defn- response
  ([status-code]
   (response status-code nil))
  ([status-code content]
   (Response. (Status. status-code (http-status-codes status-code)) content)))

(defn- write-http-response
  [out {{code :code msg :msg} :status content :content}]
  (.write out (str http-version " " code " " msg "\r\n"))
  (when content
    (.write out "\r\n")
    (.write out (str content "\r\n"))))

(defn- full-path
  [path]
  (Paths/get @static-dir (into-array [path])))

(defn- path-exists?
  [path]
  (Files/exists path (into-array java.nio.file.LinkOption [])))

(defn- read-file
  [path]
  (String. (Files/readAllBytes path) StandardCharsets/UTF_8))

(defn- handle-request
  [{:keys [method path]}]
  (if (= "GET" method)
    (let [path (if (= "/" path) "/index.html" path)
          path (full-path path)]
      (try
        (if (path-exists? path)
          (response 200 (read-file path))
          (response 404))
        (catch Throwable e
          (response 500 (.getMessage e)))))
    (response 405)))

(defn- handle-connection
  [conn]
  (with-open [in (BufferedReader. (InputStreamReader. (.getInputStream conn)))
              out (BufferedWriter. (OutputStreamWriter. (.getOutputStream conn)))]
    (let [req (read-http-request in)
          res (handle-request req)]
      (write-http-response out res)
      (log (str (req :method) " " (req :path) " - " (get-in res [:status :code]))))))

(defn parse-opts
  [args]
  (->> args
       (partition 2)
       (map (fn [[opt val]]
              (cond
                (#{"-p" "--port"} opt) {:port (Integer/parseInt val)}
                (#{"-d" "--static-dir"} opt) {:static-dir val}
                (#{"-s" "--pool-size"} opt) {:pool-size (Integer/parseInt val)}
                :else {})))
       (reduce merge {:port 0
                      :static-dir "."
                      :pool-size (* (.availableProcessors (Runtime/getRuntime)) 2)})))

(defn -main
  [& args]
  (let [{port :port sd :static-dir ps :pool-size} (parse-opts args)]
    (swap! static-dir (fn [_] sd))
    (with-open [ss (ServerSocket. port)]
      (start-logger)
      (let [es (Executors/newFixedThreadPool ps (thread-factory "web-server"))]
        (println (str "Server started on " (.getLocalSocketAddress ss)))
        (println (str "Serving static content in " sd))
        (loop  []
          (let [conn (.accept ss)]
            (.submit es ^Runnable #(handle-connection conn)))
          (recur))))))
