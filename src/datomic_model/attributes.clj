(ns datomic-model.attributes
  (:require [datomic.api :as d]))

;;;; Namespace convenience functions

(defn- prefix-ident
  [ns ident]
  {:pre [(keyword? ident)]}
  (let [root-ns (namespace ident)
        root-ns (when-not (empty? root-ns) (str root-ns "/"))
        root (str root-ns (name ident))
        join (if (re-find #"/" root) "." "/")]
    (keyword (str (name ns) join root))))

(comment
  (prefix-ident :bar :baz) ;; => :bar/baz
  (prefix-ident :foo :bar/baz) ;; => :foo.bar/baz
  )

(defn namespace-attribute
  [ns attr]
  (if (map? attr)
    (update attr :db/ident (partial prefix-ident ns))
    (update attr 0 (partial prefix-ident ns))))

(comment
  (namespace-attribute :foo {:db/ident :baz}) ;; => {:db/ident :foo/baz}
  (namespace-attribute :foo [:baz :db.type/float]) ;; => [:foo/baz :db.type/float]
  )

;;;; Compact Attributes

(defn- partition-keyword?
  [key]
  (and key (namespace key) (re-find #"db\." (namespace key))))

(defn- get-enums
  "Given a list of enums base strings, return transaction statements to
   intern the enum keywords into the user partition or, if the first
   argument to the enum set is a partition-style keyword such as db.part/foobar
   then use that instead"
  [root-str enums]
  {:pre [string? root-str]}
  (let [part (if (partition-keyword? (first enums)) (first enums) :db.part/user)
        enums (if (partition-keyword? (first enums)) (rest enums) enums)]
    (map (fn [n]
           (let [nm (if (string? n) (.replaceAll (.toLowerCase ^String n) " " "-") (name n))]
             [:db/add (d/tempid part) :db/ident (keyword root-str nm)]))
         enums)))

(def unique-mapping
  {:db.unique/value :db.unique/value
   :db.unique/identity :db.unique/identity
   :unique-value :db.unique/value
   :unique-identity :db.unique/identity})

(defn- expand-compact
  "Take a vector attribute and expand into a complete attribute"
  [[aname type & flags :as cattr]]
  {:pre [(keyword? aname)]}
  (let [uniq (first (remove nil? (map unique-mapping flags)))
        dbtype (keyword "db.type" (if (= type :enum) "ref" (name type)))
        doc (first (filter string? flags))
        flags (set flags)
        index? (flags :index)
        fulltext? (flags :fulltext)
        component? (flags :component)
        nohistory? (flags :nohistory)
        expanded (cond-> {:db/ident aname
                          :db/valueType dbtype
                          :db/cardinality (if (flags :many) :db.cardionality/many :db.cardionality/one)}
                   doc (assoc :db/doc doc)
                   index? (assoc :db/index true)
                   fulltext? (assoc :db/fulltext true)
                   component? (assoc :db/isComponent true)
                   nohistory? (assoc :db/noHistory true))
        enums (get-enums (if (namespace aname)
                           (str (namespace aname) "." (name aname))
                           (name aname))
                         (first (filter vector? flags)))]
    (concat [expanded] enums)))

(comment
 (expand-compact [:foo string "test" :index :nohistory]))

(defn expand-attribute
  "Given an attribute, return the fully expanded
   version."
  [attr]
  (if (and (map? attr) (:db/ident attr))
    [attr]
    (expand-compact attr)))


(defn expand-attributes
  [attrs]
  (mapcat expand-attribute attrs))

;;;; Transacting attributes


(defn alter!
  [attr]
  (assoc attr
         :db.alter/_attribute :db.part/db
         :db/id (d/tempid :db.part/db)))

(defn install!
  [attr]
  (assoc attr
         :db.install/_attribute :db.part/db
         :db/id (d/tempid :db.part/db)))
