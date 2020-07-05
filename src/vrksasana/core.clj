(ns vrksasana.core
  (:require [vrksasana.catalog :as catalog]
            [vrksasana.season :as season]
            [vrksasana.ground :as ground]
            [vrksasana.tree :as tree]
            [vrksasana.fruit :as fruit]
            [vrksasana.ast :as ast]))

(defn init []
  (catalog/reset))

(defn ground-to-use [{:keys [ground]}]
  (or ground
      (catalog/default-ground)))

(defn season-to-use [{:keys [season] :as options}]
  (or season
      (-> options
          ground-to-use
          (season/current-season))))

(defn plant
  ([seedling]
   (plant seedling nil))
  ([seedling options]
   (let [ground (ground-to-use options)]
     (tree/->tree (:tree-name options)
                  (ground/seedling->ast ground seedling)))))

(defn pick
  ([tree]
   (pick tree nil))
  ([tree options]
   (let [season         (season-to-use options)
         ground         (season/ground season)
         refined-context (assoc options :season season)
         var-name (->> tree
                       :tree-name
                       (ground/tree-name->var-name ground))]
     (doseq [dep (ast/tree->deps tree)]
       (pick dep refined-context))
     (catalog/add-tree-to-season tree season)
     (->> tree
          :ast
          (ground/ast->code ground)
          (ground/assignment-code ground var-name)
          (season/eval-code season)
          (fruit/->Fruit season tree)))))


(defn fruit->data [fruit]
  (let [fresh-fruit (fruit/get-fresh fruit)]
    (season/fruit-value->data
     (:season fresh-fruit)
     (:value fresh-fruit))))

(defn data->fruit
  ([data]
   (data->fruit data nil))
  ([data options]
   (let [season (season-to-use options)
         tree (-> data
                  ast/->data-dep-ast
                  tree/->tree)]
     (->> data
          (season/data->fruit-value season)
          (fruit/->Fruit season tree)))))