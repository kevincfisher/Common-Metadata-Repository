(ns cmr.search.models.query
  "Defines various query models and conditions."
  (:require [cmr.common.services.errors :as errors]
            [cmr.search.validators.temporal :as temporal-validator]
            [cmr.search.validators.date-range :as dr-validator]))

(defprotocol Condition
  "Defines the protocol for validating query conditions.
  All query conditions must implement the validate function to validate self,
  A sequence of errors should be returned if validation fails, otherwise an empty sequence is returned."
  (validate [this] "validate condition"))

(defrecord Query
  [
   ;; The concept type that is being queried.
   concept-type

   ;; The root level condition
   condition
   ]

  Condition
  (validate [this]
            (validate condition)))

(defrecord ConditionGroup
  [
   ;; The operation combining the conditions i.e. :and or :or
   operation

   ;; A sequence of conditions in the group
   conditions
   ]

  Condition
  (validate [this]
            (flatten (concat (map #(validate %) conditions)))))

(defrecord StringCondition
  [
   ;; The field being searched.
   field

   ;; The value to match
   value

   ;; indicates if the search is case sensitive. Defaults to true.
   case-sensitive?

   ;; Indicates if the search contains pattern matching expressions. Defaults to false.
   pattern?
   ]

  Condition
  (validate [this]
            []))

;; ExistCondition represents the specified field must have value, i.e. filed is not null
(defrecord ExistCondition
  [
   ;; The field being searched.
   field
   ]

  Condition
  (validate [this] []))

;; MissingCondition represents the specified field must not have value, i.e. filed is nil
(defrecord MissingCondition
  [
   ;; The field being searched.
   field
   ]

  Condition
  (validate [this] []))

(defrecord DateRangeCondition
  [
   ;; The field being searched.
   field

   ;; The start-date value
   start-date

   ;; The end-date value
   end-date
   ]

  Condition
  (validate [this]
            (dr-validator/validate this)))

(defrecord TemporalCondition
  [
   ;; The field being searched.
   field

   ;; The date range condition
   date-range-condition

   ;; The start-day value
   start-day

   ;; The end-day value
   end-day
   ]

  Condition
  (validate [this]
            (concat
              (temporal-validator/validate this)
              (validate date-range-condition))))

(defrecord MatchAllCondition
  []

  Condition
  (validate [this] []))

(defn query
  "Constructs a query with the given type and root condition.
  If root condition is not provided it matches everything."
  ([type]
   (query type (->MatchAllCondition)))
  ([type condition]
   (->Query type condition)))

(defn string-condition
  "Creates a string condition."
  ([field value]
   (string-condition field value true false))
  ([field value case-sensitive? pattern?]
   (->StringCondition field value case-sensitive? pattern?)))

(defn group-conds
  "Combines the conditions together in the specified type of group."
  [type conditions]
  (cond
    (empty? conditions) (errors/internal-error! "Grouping empty list of conditions")
    (= (count conditions) 1) (first conditions)
    :else (->ConditionGroup type conditions)))

(defn and-conds
  "Combines conditions in an AND condition."
  [conditions]
  (group-conds :and conditions))

(defn or-conds
  "Combines conditions in an OR condition."
  [conditions]
  (group-conds :or conditions))

