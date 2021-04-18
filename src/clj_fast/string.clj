(ns clj-fast.string
  (:import
   (java.util.regex Pattern Matcher)))

(defn streq?
  {:inline
   (fn [a b]
     (cond
       (and (string? a) (string? b)) (= a b)
       (string? a) `(.equals ~a ~b)
       (string? b) `(.equals ~b ~a)
       (and (nil? a) (nil? b)) true
       (nil? a) `(nil? ~b)
       (nil? b) `(nil? ~a)
       :else `(if (nil? ~a)
                (nil? ~b)
                (.equals ~(with-meta a {:tag "String"}) ~b))))}
  [^String s s']
  (if (nil? s)
    (nil? s')
    (.equals s s')))

(defn matches?
  [^String s ^Pattern re]
  (boolean (.find ^Matcher (. re (matcher s)))))
