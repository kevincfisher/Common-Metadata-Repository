(ns cmr.system-int-test.data2.granule
  "Contains data generators for example based testing in system integration tests."
  (:require [cmr.umm.granule :as g]
            [cmr.umm.collection :as c]
            [cmr.umm.granule.temporal :as gt]
            [cmr.system-int-test.data2.core :as d]
            [cmr.common.date-time-parser :as p]))

(defn psa
  "Creates product specific attribute ref"
  [name values]
  (g/map->ProductSpecificAttributeRef
    {:name name
     :values values}))

(defn temporal
  "Return a temporal with range date time of the given date times"
  [attribs]
  (let [{:keys [beginning-date-time ending-date-time]} attribs
        begin (when beginning-date-time (p/parse-datetime beginning-date-time))
        end (when ending-date-time (p/parse-datetime ending-date-time))]
    (when (or begin end)
      (gt/temporal {:range-date-time (c/->RangeDateTime begin end)}))))

(defn granule
  "Creates a granule"
  ([collection]
   (granule collection {}))
  ([collection attribs]
   (let [coll-ref (g/collection-ref (:entry-title collection))
         minimal-gran {:granule-ur (d/unique-str "ur")
                       :collection-ref coll-ref}
         temporal {:temporal (temporal attribs)}]
     (g/map->UmmGranule (merge minimal-gran temporal attribs)))))
