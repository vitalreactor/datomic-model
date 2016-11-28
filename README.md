# datomic-model

After four years of working with Datomic I've found a few conventions
to be particularly useful and worth extracting into an open library.
This library partially overlaps the functionality of
[datomic-schema](https://github.com/Yuppiechef/datomic-schema) and 
[conformity](https://github.com/rkneufeld/conformity/blob/master/project.clj)
but has enough differences to warrant an independent library.

The goal of this library is to enable you to define your data models
inline with the methods that manipulate models vs. using a separate
schema file.  We also want to support the common case of model
evolution we see in practice.  Most users of Datomic find that schema
evolution is additive and backwards compatible (adding or altering
schemas) once your system is in production.

The library is similar to conformity in that it will only transact
schema changes once by relying on the Clojure hash code of the schema
for each model and storing that in the database.  It also supports
explicit model versioning, enabling you to hook into the upgrade
procedure to perform pre and post schema update operations (such as
copying data from an old attribute to a new attribute).

## Defining Schemas

### Model Definitions

### Namespace convenience methods

### Function Definitions

## Applying Schemas to a Database

### Validating an update

### Updating the database schema

### Dump the schema to a file

## Transacting models

### Model creation functions

### Transaction validation


## License

Copyright © 2016 Vital Reactor, LLC

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
