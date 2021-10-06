(ns spec-atlas.state
  (:require
   [clojure.core.async :as async]
   [cljs-http.client :as http]
   [reagent.core :as r]
   [spec-atlas.util :as util]))


(defonce ^:private -state*
  (r/atom {:selected-generator :default
           :selected-view      :data
           :spec-format        :abbr
           :data-format        :edn
           :hide-left?         false
           :collapsed          #{}}))


(defonce ^:private -typing-timeout*
  (r/atom nil))


(defn with-typing-timeout
  [callback]
  (js/clearTimeout @-typing-timeout*)
  (reset! -typing-timeout*
          (js/setTimeout callback 500)))


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
         req (cond-> {:method method, :url url} ;; TODO FIXME
               (and (= method :get ) params) (assoc :query-params params)
               (and (= method :post) params) (assoc :form-params  params))]
     (async/take! (http/request req) callback))))


(defmethod exec! :set-context-path  [_ cpath ] (swap! -state* util/set-context-path cpath))
(defmethod exec! :set-view          [_ view  ] (swap! -state* util/set-view view))
(defmethod exec! :set-data-format   [_ fmt   ] (swap! -state* util/set-data-format fmt))
(defmethod exec! :set-spec-format   [_ fmt   ] (swap! -state* util/set-spec-format fmt))
(defmethod exec! :set-spec-action   [_ action] (swap! -state* util/set-spec-action action))
(defmethod exec! :set-generator     [_ gener ] (swap! -state* util/set-generator gener))
(defmethod exec! :set-explain-input [_ input ] (swap! -state* util/set-explain-input input))
(defmethod exec! :toggle-hide-left  [_ _     ] (swap! -state* util/toggle-hide-left))
(defmethod exec! :toggle-collapse   [_ ns    ] (swap! -state* util/toggle-collapse ns))
(defmethod exec! :toggle-conformed  [_ kw    ] (swap! -state* util/toggle-conformed kw))

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
  [_ [spec generator]]
  (http :get "/spec/generate"
        {:spec spec :generator generator}
        #(swap! -state* util/generate (:body %))))


(defmethod exec! :navigate
  [_ spec]
  (http :get "/spec/definition"
        {:spec spec}
        #(swap! -state* util/navigate (:body %))))


(defmethod exec! :explain
  [_ [spec input]]
  (let [format (cond
                 (empty? input)     :empty
                 (util/json? input) :json
                 (util/edn?  input) :edn
                 :else              :unknown)]
    (case format
      :empty   (swap! -state* util/explain {:output ""})
      :unknown (swap! -state* util/explain {:output "Invalid EDN / JSON." :error? true})
      (http :post "/spec/explain"
            {:spec spec :input input :format format}
            #(swap! -state* util/explain (assoc (:body %) :format format))))))
