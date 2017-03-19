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
