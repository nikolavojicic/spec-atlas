(ns spec-atlas.ui
  (:require
   [clojure.string :as str]
   [spec-atlas.util :as util]
   [spec-atlas.state :refer [enqueue! snapshot]]))


;; ====================


(defn component-spec-list-category
  [views selected-view]
  [:div.sticky
   (for [view views] ^{:key view}
     [:button {:disabled (= view selected-view)
               :on-click #(enqueue! :set-view view)}
      (name view)])])


(defn component-spec-list
  [specs selected-spec]
  [:ul {:style {:margin-top 0}} ;; TODO remove after sticky
   (for [[ns specs] specs] ^{:key ns}
     [:li
      [:span.green ns]
      [:ul
       (for [spec specs] ^{:key spec}
         [:li
          [:a {:href     "#"
               :on-click #(enqueue! :select-spec spec)
               :class    (if (= spec selected-spec)
                           [:menuitem :selected]
                           [:menuitem])}
           (keyword spec)]])]])])


(defn component-spec-path
  [path]
  (when path
    [:div
     (for [[i spec] (map-indexed vector path)] ^{:key i}
       [:div
        [:a.menuitem {:href     "#"
                      :on-click #(enqueue! :navigate spec)}
         (str "> " (str/join (repeat i "...")) (name spec))]])]))


(defn component-spec-name
  [spec]
  [:h3
   (let [[src name] (str/split (str spec) "/")]
     [:div
      [:span.green src]
      [:span       "/"]
      [:span.blue  name]])])


(defn component-spec-format
  [{:keys [formats selected]}]
  [:div
   (for [format formats] ^{:key format}
     [:button
      {:on-click #(enqueue! :set-spec-format format)
       :class    (when (= format selected) :selected)}
      (name format)])])


(defn component-spec-definition
  [spec-definition]
  [:div
   [:pre {:dangerouslySetInnerHTML
          {:__html spec-definition}}]])


(defn component-spec-usage
  [{:keys [usages selected]}]
  [:div
   (for [usage usages] ^{:key usage}
     [:button {:class    (when (= usage selected) :selected)
               :on-click #(enqueue! :set-spec-usage usage)}
      usage])])


(defn component-generate-menu
  [{:keys [spec
           generators
           selected-generator
           data-formats
           selected-data-format]}]
  [:table
   [:tbody
    [:tr
     [:td [:label "generator"]]
     [:td
      [:select {:on-change #(enqueue! :set-generator (util/tval->kw %))
                :value     (or selected-generator "")}
       (for [generator generators] ^{:key generator}
         [:option generator])]
      [:button.action {:on-click #(enqueue! :generate spec)}
       "exec"]]]
    [:tr
     [:td [:label "viewer"]]
     [:td
      (for [data-format data-formats] ^{:key data-format}
        [:button
         {:disabled (= data-format selected-data-format)
          :on-click #(enqueue! :set-data-format data-format)}
         (name data-format)])]]]])


(defn component-generate-value
  [{:keys [generated-value]}]
  [:pre generated-value])


;; ====================


(defn left-panel
  [{:keys [views selected-view specs selected-spec]}]
  [:div
   (component-spec-list-category views selected-view)
   (component-spec-list specs selected-spec)])


(defn right-panel
  [{:keys [path selected-spec spec-format spec-definition spec-usage]
    :as   right-panel-data}]
  (when selected-spec
    [:div
     (component-spec-path path)
     [:span.spacer]
     (component-spec-name selected-spec)
     [:span.spacer]
     [:hr]
     [:span.spacer]
     [:div
      (component-spec-format spec-format)
      (when spec-definition
        [:div
         [:span.spacer]
         (component-spec-definition spec-definition)])]
     [:span.spacer]
     [:hr]
     [:span.spacer]
     (when (keyword? selected-spec) ;; TODO
       [:div
        (component-spec-usage spec-usage)
        [:span.spacer]
        (when-some [generate (:generate right-panel-data)]
          [:div
           (component-generate-menu generate)
           [:span.spacer]
           (component-generate-value generate)])])]))


(defn home-page
  []
  (let [state (snapshot)]
    [:div.wrapper
     [:div.col-left  (-> state util/left-panel-data  left-panel)]
     [:div.col-right (-> state util/right-panel-data right-panel)]]))
