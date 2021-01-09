(ns app.home
  (:require [reagent.core :as r]
            [app.config :as c]
            [clojure.string :as str]
            ["@auth0/auth0-spa-js" :as auth0]))

(def click-count (r/atom 0))

(defonce auth0-client
  (auth0/Auth0Client.
   (clj->js c/auth0)))

(defonce auth0client
  (auth0/Auth0Client.
   (clj->js c/auth0)))


(defn url-search-params []
  (-> js/window.location.href
      (str/split "?")
      (get 1)
      (js/URLSearchParams.)))

(defn handle-redirect?
  "Handle auth0 redirect?"
  []
  (let [params (url-search-params)]
    (and
     (.has params "state")
     (.has params "code"))))

(def raw_token (atom nil))

(defn auth-action-to-take []
  (pr "What auth action to take?")
  (if (handle-redirect?)
    :handle-redirect
    :load-silently))

(def profile (r/atom {}))
(def errors  (r/atom {}))

(defn set-error [e] (reset! errors e))

(defn id-token-claims-to-user [claims]
  (let [claims-map (js->clj claims :keywordize-keys true)]
    ;; save the claims to
    (reset! profile claims-map)))

(defn load-profile
  "Should be an effect"
  []
  (pr "load profile")
  (->
   (.getIdTokenClaims auth0client)
   (.then id-token-claims-to-user)
   (.catch set-error)
   (.finally (pr "load-profile: finally"))))

(defn handle-auth-redirect []
  (->
   (.handleRedirectCallback auth0client)
   (.then load-profile)))

(defn load-silently []
  (pr "load silently")
  (-> (.getTokenSilently auth0client)
      (.then load-profile)
      (.catch #(pr %))))

(defn on-window-load []
  (pr "on window load")
  (case (auth-action-to-take)
    :handle-redirect  (handle-auth-redirect)
    :load-silently  (load-silently)
    (pr "no action")))

(.addEventListener js/window "load" on-window-load)

(defn on-login [] (.loginWithRedirect auth0client))
(defn on-logout [] (.logout auth0client))

(defn login-button []
  [:button
   {:on-click on-login} "Login"])

(defn logout-button []
  [:button
   {:on-click on-logout} "Logout"])

(defn view []
  [:<>
   [:h1 "Auth0 SPA CLJS"]
   (when (not @profile) [login-button])
   (when @profile [logout-button])
   [:pre (.stringify js/JSON (clj->js c/auth0) nil 2)]
   [:pre (.stringify js/JSON (clj->js @errors) nil 2)]
   [:pre (.stringify js/JSON (clj->js @profile) nil 2)]])
