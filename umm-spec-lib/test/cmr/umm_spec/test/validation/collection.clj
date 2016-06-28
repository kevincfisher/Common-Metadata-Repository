(ns cmr.umm-spec.test.validation.collection
  "This has tests for UMM validations."
  (:require [clojure.test :refer :all]
            [cmr.umm-spec.models.common :as c]
            [cmr.umm-spec.models.collection :as coll]
            [cmr.umm-spec.test.validation.helpers :as h]))

(deftest collection-temporal-validation
  (testing "valid temporal"
    (let [r1 (h/range-date-time "1999-12-30T19:00:00Z" "1999-12-30T19:00:01Z")
          r2 (h/range-date-time "1999-12-30T19:00:00Z" nil)
          r3 (h/range-date-time "1999-12-30T19:00:00Z" "1999-12-30T19:00:00Z")]
      (h/assert-valid (h/coll-with-range-date-times [[r1]]))
      (h/assert-valid (h/coll-with-range-date-times [[r2]]))
      (h/assert-valid (h/coll-with-range-date-times [[r3]]))
      (h/assert-valid (h/coll-with-range-date-times [[r1] [r2]]))
      (h/assert-valid (h/coll-with-range-date-times [[r1 r2] [r3]]))
      (h/assert-valid (h/coll-with-range-date-times [[r1 r2 r3]]))
      (h/assert-valid (h/coll-with-range-date-times [[r1]] true)))) ; EndsAtPresentFlag = true

  (testing "invalid temporal"
    (testing "single error"
      (let [r1 (h/range-date-time "1999-12-30T19:00:02Z" "1999-12-30T19:00:01Z")
            r2 (h/range-date-time "1999-12-30T19:00:00Z" "1999-12-30T19:00:00Z")]
        (h/assert-invalid
         (h/coll-with-range-date-times [[r1]])
         [:TemporalExtents 0 :RangeDateTimes 0]
         ["BeginningDateTime [1999-12-30T19:00:02.000Z] must be no later than EndingDateTime [1999-12-30T19:00:01.000Z]"])
        (h/assert-invalid
         (h/coll-with-range-date-times [[r2] [r1]])
         [:TemporalExtents 1 :RangeDateTimes 0]
         ["BeginningDateTime [1999-12-30T19:00:02.000Z] must be no later than EndingDateTime [1999-12-30T19:00:01.000Z]"])))

    (testing "multiple errors"
      (let [r1 (h/range-date-time "1999-12-30T19:00:02Z" "1999-12-30T19:00:01Z")
            r2 (h/range-date-time "2000-12-30T19:00:02Z" "2000-12-30T19:00:01Z")]
        (h/assert-multiple-invalid
         (h/coll-with-range-date-times [[r1 r2]])
         [{:path [:TemporalExtents 0 :RangeDateTimes 0],
           :errors
           ["BeginningDateTime [1999-12-30T19:00:02.000Z] must be no later than EndingDateTime [1999-12-30T19:00:01.000Z]"]}
          {:path [:TemporalExtents 0 :RangeDateTimes 1],
           :errors
           ["BeginningDateTime [2000-12-30T19:00:02.000Z] must be no later than EndingDateTime [2000-12-30T19:00:01.000Z]"]}])
        (h/assert-multiple-invalid
         (h/coll-with-range-date-times [[r1] [r2]])
         [{:path [:TemporalExtents 0 :RangeDateTimes 0],
           :errors
           ["BeginningDateTime [1999-12-30T19:00:02.000Z] must be no later than EndingDateTime [1999-12-30T19:00:01.000Z]"]}
          {:path [:TemporalExtents 1 :RangeDateTimes 0],
           :errors
           ["BeginningDateTime [2000-12-30T19:00:02.000Z] must be no later than EndingDateTime [2000-12-30T19:00:01.000Z]"]}])))))
