(ns spec-atlas.util
  (:require
   [clojure.string :as str]
   [clojure.walk :as walk]
   [cljs.pprint :as pprint]
   [pretty-spec.core :as ppspec]
   [json-html.core :refer [edn->hiccup]]))


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
         "<a class='menuitem' href=\"#\" onclick=\"spec_atlas.core._pre_navigate('$1')\">:../$2</a>"
         "<a class='menuitem' href=\"#\" onclick=\"spec_atlas.core._pre_navigate('$1')\">$1</a>")
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


(defn set-spec-usage
  [state usage]
  (let [old-usage (:spec-usage state)]
    (if (= usage old-usage)
      (-> state
          (dissoc :spec-usage))
      (-> state
          (assoc :spec-usage usage)))))


(defn set-generator
  [state gener]
  (-> state
      (assoc :selected-generator gener)))


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


;; ========== STATE DATA -> UI DATA


(defn component-generate-data
  [state]
  (let [spec                 (-> state :selected-spec :spath last)
        generators           [:default :navigation] ;; TODO
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


(defn left-panel-data
  [state]
  (let [selected-view (-> state :selected-view)
        selected-spec (-> state :selected-spec :spath last)]
    (cond-> {:views [:data :fspec]}
      selected-view (-> (assoc :selected-view selected-view)
                        (assoc :specs
                               (hierarchy
                                (case selected-view
                                  :data  (-> state :specs-data)
                                  :fspec (-> state :specs-fspec)))))
      selected-spec (assoc :selected-spec selected-spec))))


(defn right-panel-data
  [state]
  (when-some [sdef (:selected-spec state)]
    (let [path                 (-> sdef :spath)
          selected-spec        (-> path last)
          path                 (-> path butlast)
          selected-spec-format (-> state :spec-format)
          selected-spec-usage  (-> state :spec-usage)]
      (merge
       (cond-> {:spec-format  {:formats [:abbr :desc :form]}
                :spec-usage   {:usages  [:usages :generate :explain]}}
         path                 (assoc :path path)
         selected-spec        (assoc :selected-spec selected-spec)
         selected-spec-usage  (assoc-in [:spec-usage :selected] selected-spec-usage)
         selected-spec-format (-> (assoc-in [:spec-format :selected] selected-spec-format)
                                  (assoc :spec-definition
                                         (case selected-spec-format
                                           :abbr (-> sdef :sdesc ppspec/pprint
                                                     with-out-str (linkify :abbr))
                                           :desc (-> sdef :sdesc ppspec/pprint
                                                     with-out-str linkify)
                                           :form (-> sdef :sform fix-or-ns ppspec/pprint
                                                     with-out-str linkify)))))
       (case selected-spec-usage
         nil       nil
         :usages   nil
         :generate {:generate (component-generate-data state)}
         :explain  nil)))))
