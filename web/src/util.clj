(ns util)

(defn fnil-set
  "x->(coll of coll-type)->(conj coll x)
  Return a function which either creates a set containing x or adds x to an existing set"
  [x]
  #(if %
     (conj % x)
     #{x}))

(defn buffer->str
  "Array<Byte>->String
  Transforms a java array of bytes into a String."
  [buffer]
  (->> buffer
       (filter pos?)
       (map char)
       (clojure.string/join)))
