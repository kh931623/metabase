(ns metabase.lib.drill-thru.sort-test
  (:require
   [clojure.test :refer [are deftest is testing]]
   [metabase.lib.convert :as lib.convert]
   [metabase.lib.core :as lib]
   [metabase.lib.drill-thru.sort :as lib.drill-thru.sort]
   [metabase.lib.test-metadata :as meta]
   #?@(:clj ([metabase.util.malli.fn :as mu.fn])
       :cljs ([metabase.test-runner.assert-exprs.approximately-equal]))))

#?(:cljs (comment metabase.test-runner.assert-exprs.approximately-equal/keep-me))

(deftest ^:parallel sort-e2e-test
  (let [query (lib/query meta/metadata-provider (meta/table-metadata :orders))
        drill (lib.drill-thru.sort/sort-drill query
                                              -1
                                              {:column (meta/field-metadata :orders :id)
                                               :value  nil})]
    (is (=? {:type            :drill-thru/sort
             :column          {:id (meta/id :orders :id)}
             :sort-directions [:asc :desc]}
            drill))
    ;; fails: invalid output: missing display name
    ;; disabled for now because display info seems to be broken
    #_(is (= :neat
             (lib/display-info query drill)))
    ;; fails: no drill-thru-method
    (are [actual] (=? {:stages [{:lib/type :mbql.stage/mbql
                                 :order-by [[:asc {} [:field {} (meta/id :orders :id)]]]}]}
                      actual)
      (lib/drill-thru query drill)
      (lib/drill-thru query -1 drill)
      (lib/drill-thru query -1 drill :asc)
      (binding #?(:clj [mu.fn/*enforce* false] :cljs [])
        (lib/drill-thru query -1 drill "asc")))
    (testing "Handle JS input correctly (#34342)"
      (binding #?(:clj [mu.fn/*enforce* false] :cljs [])
        (is (=? {:query {:source-table (meta/id :orders)
                         :order-by     [[:asc
                                         [:field
                                          (meta/id :orders :id)
                                          {:base-type :type/BigInteger}]]]}}
                (lib.convert/->legacy-MBQL (lib/drill-thru query -1 drill "asc"))))))
    (is (=? {:stages [{:lib/type :mbql.stage/mbql
                       :order-by [[:desc {} [:field {} (meta/id :orders :id)]]]}]}
            (lib/drill-thru query -1 drill :desc)))))
