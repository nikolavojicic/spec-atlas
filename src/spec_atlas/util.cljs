(ns spec-atlas.util
  (:require
   [clojure.string :as str]
   [clojure.walk :as walk]
   [cljs.reader :as reader]
   [cljs.pprint :as pprint]
   [pretty-spec.core :as ppspec]
   [json-html.core :refer [edn->hiccup]]))


(defn edn?
  [s]
  (boolean
   (try (reader/read-string s)
        (catch js/Error _))))


(defn json?
  [s]
  (boolean
   (try (.parse js/JSON s)
        (catch js/Error _))))


(defn get-element-by-id
  [id]
  (-> js/document
      (.getElementById (name id))
      (.-innerHTML)))


(defn copy-to-clipboard
  [s]
  (let [el (.createElement js/document "textarea")]
    (set! (.-value el) s)
    (.setAttribute el "readonly" "")
    (set! (-> el .-style .-position) "absolute")
    (set! (-> el .-style .-left) "-9999px")
    (-> js/document .-body (.appendChild el))
    (.select el)
    (.execCommand js/document "copy")
    (-> js/document .-body (.removeChild el))))


(defn tval->str
  [x]
  (-> x .-target .-value))


(defn tval->kw
  [x]
  (-> x .-target .-value keyword))


(defn fix-or-ns
  [s]
  (walk/postwalk
   (fn [x]
     (if (= x 'clojure.core/or)
       'or
       x))
   s))


(defn update-vals
  "TODO remove in future ClojureScript version."
  [m f]
  (with-meta
    (persistent!
     (reduce-kv (fn [acc k v] (assoc! acc k (f v)))
                (if (instance? IEditableCollection m)
                  (transient m)
                  (transient {}))
                m))
    (meta m)))


(defn hierarchy
  [specs]
  (-> (group-by namespace specs)
      (update-vals sort)
      sort))


(defn edn->json
  [edn]
  (.stringify js/JSON (clj->js edn) nil 2))


(defn linkify
  [s & abbr?]
  (->> (if abbr?
         "<a class='menuitem' href=\"#\" onclick=\"spec_atlas.ui._pre_navigate('$1')\" title=\"$1\">:../$2</a>"
         "<a class='menuitem' href=\"#\" onclick=\"spec_atlas.ui._pre_navigate('$1')\">$1</a>")
       (str/replace s #"(:\S+/([^\)\]\r\n]+))")))


;; ========== STATE DATA -> STATE DATA


(defn set-context-path
  [state cpath]
  (-> state
      (assoc :context-path cpath)))


(defn set-view
  [state view]
  (-> state
      (assoc :selected-view view)))


(defn set-data-format
  [state fmt]
  (-> state
      (assoc :data-format fmt)))


(defn set-spec-format
  [state format]
  (let [old-format (:spec-format state)]
    (println old-format format)
    (if (= format old-format)
      (-> state
          (dissoc :spec-format))
      (-> state
          (assoc :spec-format format)))))


(defn set-spec-action
  [state action]
  (let [old-action (:spec-action state)]
    (if (= action old-action)
      (-> state
          (dissoc :spec-action))
      (-> state
          (assoc :spec-action action)))))


(defn set-generator
  [state gener]
  (-> state
      (assoc :selected-generator gener)))


(defn set-explain-input
  [state input]
  (-> state
      (assoc-in [:explain :input] (str/triml input))))


(defn toggle-hide-left
  [state]
  (-> state
      (update :hide-left? not)))


(defn toggle-collapse
  [state ns]
  (-> state
      (update :collapsed
              (fn [collapsed]
                (if (collapsed ns)
                  (disj collapsed ns)
                  (conj collapsed ns))))))


(defn refresh-specs
  [state {:keys [data fspec]}]
  (-> state
      (assoc :specs-data  data)
      (assoc :specs-fspec fspec)))


(defn select-spec
  [state selected-spec]
  (-> state
      (assoc :selected-spec selected-spec)))


(defn generate
  [state {:keys [generated-value]}]
  (cond-> state
    generated-value
    (assoc-in [:selected-spec :sgen]
              {:name  (:selected-generator state)
               :value generated-value})))


(defn navigate
  [state selected-spec]
  (let [spec         (-> selected-spec :spath last)
        spath        (-> state :selected-spec :spath vec)
        [npath tail] (split-with (complement #{spec}) spath)
        npath        (conj (vec npath) spec)]
    (-> state
        (assoc :selected-spec selected-spec)
        (assoc-in [:selected-spec :spath] npath))))


(defn explain
  [state {:keys [output error? format]}]
  (let [state (-> state
                  (assoc-in [:explain :output] output)
                  (assoc-in [:explain :error?] error?))]
    (if format
      (assoc-in state [:explain :format] format)
      (update state :explain dissoc :format))))


;; ========== STATE DATA -> UI DATA


(defn component-usages-data
  [state]
  (let [usages (-> state :selected-spec :usages sort)]
    (hierarchy usages)))


(defn component-generate-data
  [state]
  (let [spec                 (-> state :selected-spec :spath last)
        generators           (-> state :selected-spec :generators sort (conj :default))
        generator            (-> state :selected-spec :sgen)
        generated-value      (-> generator :value)
        selected-data-format (-> state :data-format)
        selected-generator   (-> state :selected-generator)]
    (cond-> {:data-formats [:edn :json :html]}
      selected-data-format (assoc :selected-data-format selected-data-format)
      selected-generator   (assoc :selected-generator   selected-generator)
      generators           (assoc :generators           generators)
      spec                 (assoc :spec                 spec)
      generated-value      (assoc :generated-value
                                  (case selected-data-format
                                    nil   ""
                                    :edn  (with-out-str (pprint/pprint generated-value))
                                    :json (edn->json generated-value)
                                    :html (edn->hiccup generated-value))))))


(defn component-explain-data
  [state]
  (let [explain (-> state :explain)
        error?  (-> explain :error?)
        format  (-> explain :format)]
    (cond-> {:spec   (-> state :selected-spec :spath last)
             :input  (-> explain :input)
             :output (-> explain :output)}
      error? (assoc :error? true)
      format (assoc :format format))))


(defn left-panel-data
  [state]
  (if (:hide-left? state)
    {:hide-left? true}
    (let [selected-view (-> state :selected-view)
          selected-spec (-> state :selected-spec :spath last)
          collapsed     (-> state :collapsed)]
      (cond-> {:hide-left? false
               :views      [:data :fspec]
               :collapsed  collapsed}
        selected-view (-> (assoc :selected-view selected-view)
                          (assoc :specs
                                 (hierarchy
                                  (case selected-view
                                    :data  (-> state :specs-data)
                                    :fspec (-> state :specs-fspec)))))
        selected-spec (assoc :selected-spec selected-spec)))))


(defn right-panel-data
  [state]
  (when-some [sdef (:selected-spec state)]
    (let [path                 (-> sdef :spath)
          selected-spec        (-> path last)
          path                 (-> path butlast)
          selected-spec-format (-> state :spec-format)
          selected-spec-action (-> state :spec-action)]
      (merge
       (cond-> {:spec-format  {:formats [:abbr :desc :form]}
                :spec-action  {:actions [:usages :generate :explain]}}
         path                 (assoc :path path)
         selected-spec        (assoc :selected-spec selected-spec)
         selected-spec-action (assoc-in [:spec-action :selected] selected-spec-action)
         selected-spec-format (-> (assoc-in [:spec-format :selected] selected-spec-format)
                                  (assoc :spec-definition
                                         (case selected-spec-format
                                           :abbr (-> sdef :sdesc ppspec/pprint
                                                     with-out-str (linkify :abbr))
                                           :desc (-> sdef :sdesc ppspec/pprint
                                                     with-out-str linkify)
                                           :form (-> sdef :sform fix-or-ns ppspec/pprint
                                                     with-out-str linkify)))))
       (case selected-spec-action
         nil       nil
         :usages   {:usages   (component-usages-data   state)}
         :generate {:generate (component-generate-data state)}
         :explain  {:explain  (component-explain-data  state)})))))
