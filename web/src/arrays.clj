(ns nested-arrays
  (:require [clojure.string :refer [index-of last-index-of join]]
            [clojure.spec :as s]))

(def find-box-point-free
  (let [with-box (comp first (partial filter (partial not-every? (partial = \1))))
        boundaries (partial (juxt (comp (partial apply index-of) (partial conj '(\0)))
                            (comp (partial apply last-index-of) (partial conj '(\0)))))]
    (comp (partial map reverse)
       (partial apply (partial map vector))
       (partial (juxt
           ;; rows
           (comp boundaries with-box (partial map join))
           ;; cols
           (comp boundaries with-box (partial apply (partial map str))))))))

(defn bounds
  [arr]
  (->> (filter (fn [x] (not-every? #(= 1 %) x)) arr)
       (first)
       (join)
       (#(vector (index-of % \0) (last-index-of % \0)))))

(defn find-box-threading
  [rows]
  (let [cols (apply (partial map vector) rows)]
    (->> [rows cols]
         (map bounds)
         (apply (partial map vector))
         (map reverse))))

;; Spec Tutorial
(s/def ::name-or-id (s/or :name string?
                          :id int?))

(s/def ::small-odd (s/and odd? #(< 0 % 10)))

(s/def ::suit #{::heart ::spade ::diamond ::club})

(s/explain ::suit ::heart)

(-> (s/explain-data ::name-or-id :nested-arrays/heart)
    (clojure.pprint/pprint))

(def email-regex #"^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,63}$")
(s/def ::email-type (s/and string? #(re-matches email-regex %)))
(s/def ::acctid int?)
(s/def ::last-name string?)
(s/def ::email ::email-type)

(s/def ::person (s/keys :req [::first-name ::last-name ::email]
                        :opt [::phone]))
(s/def :unq/person
  (s/keys :req-un [::first-name ::last-name ::email]
          :opt-un [::phone]))

(defrecord Person [first-name last-name email phone])
(s/explain :unq/person (->Person "Elon" nil nil nil))
(s/explain :unq/person (->Person "Elon" "Musk" "elon@example.com" nil))
