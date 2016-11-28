(ns datomic-model.core
  (:require [datomic.api :as d]
            [datomic.function :as df]
            [datomic-model.attributes :as attr]))

;;;; Model Registry

(defonce registry (atom {}))

(defn register-model
  "Add a new model to the registry"
  [name attributes]
  (swap! registry assoc name attributes)
  attributes)

;;;; Defining Models

(defmacro defmodel
  "Define and register a model, possibly using shorthand 
   attribute notations."
  [name & attributes]
  `(let [#attrs (attr/expand-attributes ~attributes)]
     (datomic-model.core/register-model ~name #attrs)
     (def ~name #attrs)))

(defn with-namespace
  "Update the attributes to include the provided namespace prefix
   to all idents (works on both compact and fully expanded encodings)"
  [ns & attrs]
  (mapv (partial attr/namespace-attribute ns) attrs))

;;;; Functions

(defmacro dbfn
  "Generate a database function transaction record for the
   provided function name."
  [name doc params partition & code]
  (let [code `(do ~@code)]
    `{:db/id (datmic.api/tempid ~partition)
      :db/doc doc
      :db/ident ~(keyword name)
      :db/fn (df/construct
              {:lang "clojure"
               :params '~params
               :code '~code})}))

(defmacro def-dbfn
  "Define and register a database function that is also 
   available in the local image."
  [name & args]
  (let [doc? (string? (first args))
        doc (when doc? (first args))
        [params partition & code] (if doc? (rest args) args)]
    `(let [#dbf (dbfn ~name ~params  ~partition ~@code)]
       (datomic-model.core/register ~name #dbf)
       (def ~name
         (with-meta
           (fn ~name [~@params]
             ~@code)
           {:tx #dbf
            :doc doc})))))


;;;; Install Schema

