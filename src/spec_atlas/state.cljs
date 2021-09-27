(ns spec-atlas.state
  (:require
   [clojure.core.async :as async]
   [cljs-http.client :as http]
   [reagent.core :as r]
   [spec-atlas.util :as util]))


(defonce ^:private -state*
  (r/atom {:selected-view :data
           :spec-format   :abbr
           :data-format   :edn}))


(defn snapshot
  ([]      @-state*)
  ([& kws] (get-in @-state* kws)))


(defmulti ^:private exec!
  (fn [event _] event))


(def ^:private -events<
  (let [ch (async/chan)]
    (async/go-loop [[event args] (async/<! ch)]
      (when event
        (prn event)
        (exec! event args)
        (recur (async/<! ch)))
      (println "Channel -events< closed."))
    ch))


(defn enqueue!
  ([event]
   (enqueue! event nil))
  ([event arg]
   (async/put! -events< [event arg])))


(defn http
  ([method url callback]
   (http method url nil callback))
  ([method url params callback]
   (let [url (str (snapshot :context-path) url)
         req (cond-> {:method method, :url url}
               params (assoc :query-params params))]
     (async/take! (http/request req) callback))))


(defmethod exec! :set-context-path [_ cpath] (swap! -state* util/set-context-path cpath))
(defmethod exec! :set-view         [_ view ] (swap! -state* util/set-view view))
(defmethod exec! :set-data-format  [_ fmt  ] (swap! -state* util/set-data-format fmt))
(defmethod exec! :set-spec-format  [_ fmt  ] (swap! -state* util/set-spec-format fmt))
(defmethod exec! :set-spec-usage   [_ usage] (swap! -state* util/set-spec-usage usage))
(defmethod exec! :set-generator    [_ gener] (swap! -state* util/set-generator gener))


(defmethod exec! :refresh-specs
  [_ _]
  (http :get "/specs"
        #(swap! -state* util/refresh-specs (:body %))))


(defmethod exec! :select-spec
  [_ spec]
  (http :get "/spec/definition"
        {:spec spec}
        #(swap! -state* util/select-spec (:body %))))


(defmethod exec! :generate
  [_ spec]
  (http :get "/spec/generate"
        {:spec spec}
        #(swap! -state* util/generate (:body %))))


(defmethod exec! :navigate
  [_ spec]
  (http :get "/spec/definition"
        {:spec spec}
        #(swap! -state* util/navigate (:body %))))
