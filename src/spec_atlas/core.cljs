(ns spec-atlas.core
  (:require
   [clojure.string :as str]
   [reagent.dom :as d]
   [spec-atlas.ui :as ui]
   [spec-atlas.state :as state]))


(set! (.-toString (.-prototype js/Date))
      (fn [] (this-as this
               (-> (.stringify js/JSON this)
                   (str/replace "\"" "")))))


(state/enqueue! :set-context-path "http://172.28.43.198:3000")
(state/enqueue! :refresh-specs)


(defn mount-root []
  (d/render
   [ui/home-page]
   (.getElementById js/document "app")))


(defn ^:export init! [] (mount-root))
