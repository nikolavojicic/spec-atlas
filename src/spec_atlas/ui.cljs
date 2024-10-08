(ns spec-atlas.ui
  (:require
   [clojure.string :as str]
   [cljs.reader :as reader]
   [spec-atlas.util :as util]
   [spec-atlas.state :refer [enqueue! snapshot with-typing-timeout]]))


;; ====================


(defn _pre_navigate [x]
  (enqueue! :navigate (reader/read-string x)))


;; ====================


(defn component-views-opener
  []
  [:button.action
   {:style    {:float :right :margin-right 10}
    :on-click #(enqueue! :toggle-hide-left)}
   "+"])


(defn component-views
  [views selected-view]
  [:div.sticky
   (for [view views] ^{:key view}
     [:button.tab
      {:disabled (= view selected-view)
       :on-click #(enqueue! :set-view view)}
      (name view)])
   [:button.action
    {:style    {:float :right :margin-right 10}
     :on-click #(enqueue! :toggle-hide-left)}
    "-"]
   [:br] [:br]])


(defn component-spec-list
  [specs selected-spec collapsed]
  [:ul {:style {:margin-top 0 :padding-left 0}}
   (for [[ns specs] specs
         :let [collapsed? (collapsed ns)]] ^{:key ns}
     [:li
      [:a.green {:href     "#"
                 :on-click #(enqueue! :toggle-collapse ns)}
       (str (if collapsed? "+ " "- ") ns)]
      (when (not collapsed?)
        [:ul
         (for [{:keys [name method spec]} specs] ^{:key spec}
           [:li
            [:a {:href     "#"
                 :on-click #(enqueue! :select-spec spec)
                 :class    (if (= spec selected-spec)
                             [:menuitem :selected]
                             [:menuitem])}
             (util/spec-rel-name name method)]])])
      (when collapsed? [:br])
      [:br]])])


(defn component-spec-path
  [path]
  (when path
    [:div
     (for [[i spec] (map-indexed vector path)] ^{:key i}
       [:div
        [:a.menuitem
         {:href     "#"
          :title    (str spec)
          :on-click #(enqueue! :navigate spec)}
         (str "> " (str/join (repeat i "...")) (name spec))]])]))


(defn component-spec-name
  [{:keys [ns name method]}]
  [:h3
   [:div
    (if method
      [:span.green ns]
      [:span
       [:span.green (str ":" ns)]
       [:span "/"]])
    [:span.blue (util/spec-rel-name name method)]]])


(defn component-spec-format
  [{:keys [formats selected]}]
  [:div
   (for [format formats] ^{:key format}
     [:button.tab
      {:on-click #(enqueue! :set-spec-format format)
       :class    (when (= format selected) :selected)}
      (name format)])])


(defn component-spec-definition
  [spec-definition]
  [:div
   [:pre {:dangerouslySetInnerHTML
          {:__html spec-definition}}]])


(defn component-spec-action
  [{:keys [actions selected]}]
  [:div
   (for [action actions] ^{:key action}
     [:button.tab
      {:class    (when (= action selected) :selected)
       :on-click #(enqueue! :set-spec-action action)}
      action])])


(defn component-usages
  [usages]
  (if (seq usages)
    [:ul {:style {:margin 0 :padding 0}}
     (for [[ns specs] usages] ^{:key ns}
       [:li
        [:span.green ns]
        [:ul
         (for [{:keys [name method spec]} specs] ^{:key spec}
           [:li
            [:a.menuitem {:href     "#"
                          :on-click #(enqueue! :select-spec spec)}
             (util/spec-rel-name name method)]])]
        [:br]])]
    [:label.red "There are no usages."]))


(defn component-generate
  [{:keys [spec
           generators
           selected-generator
           data-formats
           selected-data-format
           show-conformed
           generated-value]}]
  [:div
   [:table
    [:tbody
     [:tr
      [:td [:label "generator"]]
      [:td
       [:select {:on-change #(enqueue! :set-generator (util/tval->kw %))
                 :value     selected-generator}
        (for [generator generators] ^{:key generator}
          [:option generator])]]
      [:td
       [:button.action {:on-click #(enqueue! :generate [spec selected-generator])}
        "exec"]]]
     [:tr
      [:td [:label "previewer"]]
      [:td
       [:select {:on-change #(enqueue! :set-data-format (util/tval->kw %))
                 :value     selected-data-format}
        (for [data-format data-formats] ^{:key data-format}
          [:option (name data-format)])]]]]]
   (when generated-value [:span.spacer])
   (when generated-value
     (if (symbol? spec)
       [:table {:style {:width "100%"}}
        [:tbody
         [:tr
          [:th {:style {:font-weight "normal"}}
           [:h3.green "args data"]
           [:button.action
            {:on-click #(-> :generated-args
                            util/get-element-value-by-id
                            util/copy-to-clipboard)}
            "copy"]
           [:button.action
            {:on-click #(enqueue! :toggle-conformed :args)}
            (if (:args show-conformed) "unform" "conform")]]
          [:th {:style {:font-weight "normal"}}
           [:h3.green "ret data"]
           [:button.action
            {:on-click #(-> :generated-ret
                            util/get-element-value-by-id
                            util/copy-to-clipboard)}
            "copy"]
           [:button.action
            {:on-click #(enqueue! :toggle-conformed :ret)}
            (if (:ret show-conformed) "unform" "conform")]]]
         [:tr
          [:td {:style {:vertical-align "top" }}
           [:pre#generated-args (:args generated-value)]]
          [:td {:style {:vertical-align "top" }}
           [:pre#generated-ret  (:ret  generated-value)]]]]]
       [:div
        [:div
         [:button.action
          {:on-click #(-> :generated-value
                          util/get-element-value-by-id
                          util/copy-to-clipboard)}
          "copy"]
         [:button.action
          {:on-click #(enqueue! :toggle-conformed :value)}
          (if (:value show-conformed) "unform" "conform")]]
        [:span.spacer]
        [:pre#generated-value generated-value]]))])


(defn component-explain
  [{:keys [spec input output error? format]}]
  [:table {:style {:width "100%" :table-layout "fixed"}}
   [:tbody
    [:tr
     [:th {:style {:font-weight "normal"}}
      [:h3.green (str "input data"
                      (if format
                        (str " (" (name format) ")")
                        ""))]]
     (when (seq input)
       [:th {:style {:font-weight "normal"}}
        (if error?
          [:h3.red   "error"]
          [:h3.green "conformed data (edn)"])])]
    [:tr
     [:td {:style {:vertical-align "top"}}
      [:textarea
       {:on-change  #(enqueue! :set-explain-input (util/tval->str %))
        :on-key-up  #(with-typing-timeout (fn [] (enqueue! :explain [spec input])))
        :rows       (max 20 (inc (count (str/split-lines input))))
        :style      {:padding 10 :background-color "rgb(255, 255, 240)"}
        :spellCheck "false"
        :auto-focus true
        :value      input}]]
     (when (seq input)
       [:td {:style {:vertical-align "top" :padding 10}}
        [:pre
         (if (or error? (empty? (str output)))
           output
           (with-out-str (cljs.pprint/pprint output)))]])]]])


;; ====================


(defn left-panel
  [{:keys [views selected-view specs selected-spec hide-left? collapsed]}]
  (if hide-left?
    [:div.col-left-small
     (component-views-opener)]
    [:div.col-left
     (component-views views selected-view)
     (component-spec-list specs selected-spec collapsed)]))


(defn right-panel
  [{:keys [path selected-spec selected-spec-details spec-format spec-definition spec-action]
    :as   right-panel-data}]
  (when selected-spec
    [:div.col-right
     (when path
       [:div
        (component-spec-path path)
        [:span.spacer]])
     (component-spec-name selected-spec-details)
     [:span.spacer]
     [:div.menu
      (component-spec-format spec-format)
      (when spec-definition
        [:div
         [:span.spacer]
         (component-spec-definition spec-definition)])]
     [:span.spacer]
     [:span.spacer]
     [:div.menu
      (component-spec-action spec-action)
      [:span.spacer]
      (when-some [usages (:usages right-panel-data)]
        [:div
         (component-usages usages)])
      (when-some [generate (:generate right-panel-data)]
        (component-generate generate))
      (when-some [explain (:explain right-panel-data)]
        [:div
         (component-explain explain)])]]))


(defn home-page
  []
  (let [state (snapshot)]
    [:div.wrapper
     (-> state util/left-panel-data  left-panel)
     (-> state util/right-panel-data right-panel)]))
