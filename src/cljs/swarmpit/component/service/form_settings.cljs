(ns swarmpit.component.service.form-settings
  (:require [material.component :as comp]
            [swarmpit.storage :as storage]
            [swarmpit.component.state :as state]
            [swarmpit.routes :as routes]
            [rum.core :as rum]
            [ajax.core :as ajax]))

(enable-console-print!)

(def cursor [:page :service :wizard :settings])

(def form-mode-style
  {:display   "flex"
   :marginTop "14px"})

(def form-mode-replicated-style
  {:width "170px"})

(def form-image-style
  {:color "rgb(117, 117, 117)"})

(defn- form-image [value]
  (comp/form-comp
    "IMAGE"
    (comp/vtext-field
      {:name          "image"
       :key           "image"
       :required      true
       :disabled      true
       :underlineShow false
       :inputStyle    form-image-style
       :value         value})))

(defn- form-image-tag-ac [value tags]
  "Preload tags for services created via swarmit"
  (comp/form-comp
    "IMAGE TAG"
    (comp/autocomplete {:name          "imageTagAuto"
                        :key           "imageTagAuto"
                        :searchText    value
                        :onUpdateInput (fn [v] (state/update-value [:repository :tag] v cursor))
                        :dataSource    tags})))

(defn- form-image-tag [value]
  "For services created by docker cli there is no preload"
  (comp/form-comp
    "IMAGE TAG"
    (comp/text-field
      {:name     "imageTag"
       :key      "imageTag"
       :value    value
       :onChange (fn [_ v]
                   (state/update-value [:repository :tag] v cursor))})))

(defn- form-name [value update-form?]
  (comp/form-comp
    "SERVICE NAME"
    (comp/vtext-field
      {:name     "serviceName"
       :key      "serviceName"
       :required true
       :disabled update-form?
       :value    value
       :onChange (fn [_ v]
                   (state/update-value [:serviceName] v cursor))})))

(defn- form-mode [value update-form?]
  (comp/form-comp
    "MODE"
    (comp/radio-button-group
      {:name          "mode"
       :key           "mode"
       :style         form-mode-style
       :valueSelected value
       :onChange      (fn [_ v]
                        (state/update-value [:mode] v cursor))}
      (comp/radio-button
        {:name     "replicated-mode"
         :key      "replicated-mode"
         :disabled update-form?
         :label    "Replicated"
         :value    "replicated"
         :style    form-mode-replicated-style})
      (comp/radio-button
        {:name     "global-mode"
         :key      "global-mode"
         :disabled update-form?
         :label    "Global"
         :value    "global"}))))

(defn- form-replicas [value]
  (comp/form-comp
    "REPLICAS"
    (comp/vtext-field
      {:name     "replicas"
       :key      "replicas"
       :required true
       :type     "number"
       :min      1
       :value    value
       :onChange (fn [_ v]
                   (state/update-value [:replicas] (js/parseInt v) cursor))})))

(defn dockerhub-image-tags-handler
  [user repository]
  (ajax/GET (routes/path-for-backend :dockerhub-tags)
            {:headers {"Authorization" (storage/get "token")}
             :params  {:repository repository
                       :user       user}
             :handler (fn [response]
                        (state/update-value [:repository :tags] response cursor))}))

(defn registry-image-tags-handler
  [registry repository]
  (ajax/GET (routes/path-for-backend :repository-tags {:registry registry})
            {:headers {"Authorization" (storage/get "token")}
             :params  {:repository repository}
             :handler (fn [response]
                        (state/update-value [:repository :tags] response cursor))}))

(rum/defc form < rum/reactive [update-form?]
  (let [{:keys [repository
                serviceName
                mode
                replicas]} (state/react cursor)]
    [:div.form-edit
     (comp/form
       {:onValid   #(state/update-value [:isValid] true cursor)
        :onInvalid #(state/update-value [:isValid] false cursor)}
       (form-image (:name repository))
       (if update-form?
         (form-image-tag (:tag repository))
         (form-image-tag-ac (:tag repository)
                            (:tags repository)))
       (form-name serviceName update-form?)
       (form-mode mode update-form?)
       (if (= "replicated" mode)
         (form-replicas replicas)))]))