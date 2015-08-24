(ns cmr.umm-spec.xml-to-umm-mappings.iso19115-2
  "Defines mappings from ISO19115-2 XML to UMM records"
  (:require clojure.string
            [cmr.umm-spec.xml-to-umm-mappings.dsl :refer :all]
            [cmr.umm-spec.xml-to-umm-mappings.add-parse-type :as apt]
            [cmr.umm-spec.json-schema :as js]))

;;; Path Utils

(def md-data-id-base-xpath
  "/gmi:MI_Metadata/gmd:identificationInfo/gmd:MD_DataIdentification")

(def citation-base-xpath
  (str md-data-id-base-xpath "/gmd:citation/gmd:CI_Citation"))

(def identifier-base-xpath
  (str citation-base-xpath "/gmd:identifier/gmd:MD_Identifier"))

;;; Mapping

(def temporal-xpath
  (str md-data-id-base-xpath "/gmd:extent/gmd:EX_Extent/gmd:temporalElement/gmd:EX_TemporalExtent/gmd:extent"))

(def precision-xpath (str "/gmi:MI_Metadata/gmd:dataQualityInfo/gmd:DQ_DataQuality/gmd:report"
                          "/gmd:DQ_AccuracyOfATimeMeasurement/gmd:result"
                          "/gmd:DQ_QuantitativeResult/gmd:value"
                          "/gco:Record[@xsi:type='gco:Real_PropertyType']/gco:Real"))

(def temporal-mappings
  (for-each temporal-xpath
    (object {:PrecisionOfSeconds (xpath precision-xpath)
             :RangeDateTimes (for-each "gml:TimePeriod"
                               (object {:BeginningDateTime (xpath "gml:beginPosition")
                                        :EndingDateTime    (xpath "gml:endPosition")}))
             :SingleDateTimes (select "gml:TimeInstant/gml:timePosition")})))

(def platforms-xpath
  (str "/gmi:MI_Metadata/gmi:acquisitionInformation/gmi:MI_AcquisitionInformation/gmi:platform"
       "/eos:EOS_Platform"))

(def platform-long-name-xpath
  "gmi:identifier/gmd:MD_Identifier/gmd:description/gco:CharacterString")

(def platform-short-name-xpath
  "gmi:identifier/gmd:MD_Identifier/gmd:code/gco:CharacterString")

(def iso19115-2-xml-to-umm-c
  (apt/add-parsing-types
    js/umm-c-schema
    (object {:EntryId (char-string-xpath identifier-base-xpath "/gmd:code")
             :EntryTitle (char-string-xpath citation-base-xpath "/gmd:title")
             :Version (char-string-xpath identifier-base-xpath "/gmd:version")
             :Abstract (char-string-xpath md-data-id-base-xpath "/gmd:abstract")
             :Purpose (char-string-xpath md-data-id-base-xpath "/gmd:purpose")
             :DataLanguage (char-string-xpath md-data-id-base-xpath "/gmd:language")
             :TemporalExtents temporal-mappings
             :Platforms (for-each platforms-xpath
                          (object {:ShortName (xpath platform-short-name-xpath)
                                   :LongName (xpath platform-long-name-xpath)
                                   :Type (xpath "gmi:description/gco:CharacterString")}))})))
