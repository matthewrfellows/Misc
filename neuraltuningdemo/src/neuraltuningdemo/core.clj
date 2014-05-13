(ns neuraltuningdemo.core
  (:require [clojure.string :as str]
            [clojure.pprint :as pprint]
            [clojure.stacktrace :as st]
            [clojure.core.matrix :as mtx]
            [incanter.core :as incant]
            [incanter.stats :as istats]
            [incanter.charts :as icharts]
            [incanter.distributions :as idists]
            [incanter.optimize :as ioptim]
            [incanter.io :as iio])
  (:gen-class))


;; -------------------------------------

;; Demo of neural tuning and analysis of sensory integration in a
;; neural population.

;; Simulate data from a center-out integration experiment. "Behavior"
;; is initial hand position, visFB position, and target position.
;; Use various FBshift directions. Add noise to vis and prop inputs. Simulate
;; Poisson neurons with planar tuning to Integ vectors with various alphas. 
;;
;; See examples at bottom.

;; ----------------------------------------
(def Xtgt [0 0])
(def nom-start-dist 10)
(def nom-start-dirs [0 (/ Math/PI 2) Math/PI (* 3 (/ Math/PI 2))])
(def nom-start-locs (map (fn [th] (map #(* nom-start-dist %) [(Math/cos th) (Math/sin th)])) nom-start-dirs))
(def start-loc-variances [1 2])

(def shift-dirs (map #(* Math/PI %) [0 1/4 1/2 3/4 1 5/4 3/2 7/4]))
(def shift-mag 1)
(def fbshifts (map (fn [th] (map #(* shift-mag %) [(Math/cos th) (Math/sin th)])) shift-dirs))

(def nominal-co-target-dirs (map #(* Math/PI %) [0 1/4 1/2 3/4 1 5/4 6/4 7/4]))

(defn pol2cart
  [[r th]]
  [(* r (Math/cos th)) (* r (Math/sin th))])
            
(defn get-rotation-mtx
  [th]
  (incant/matrix [[(Math/cos th) (- (Math/sin th))] [(Math/sin th) (Math/cos th)]]))

(defn sample-nom-start-idx
  [N]
  (let [nom-idxs (istats/sample-uniform N :min 0 :max 3 :integers true)]
    (if (= N 1)
      (first nom-idxs)
      nom-idxs)))

(defn sample-Xprop
  [nom-idx]
  (let [;;nom-idx (first (istats/sample-uniform 1 :min 0 :max 3 :integers true))
        nom-start (nth nom-start-locs nom-idx)
        nom-dir (nth nom-start-dirs nom-idx)
        V0 (incant/identity-matrix 2)
        R (get-rotation-mtx nom-dir)
        D (incant/diag start-loc-variances)
        X (istats/sample-mvn 1 :mean [0 0])]
    (incant/to-vect (incant/plus (incant/mmult R D (incant/trans X)) (incant/matrix nom-start)))))

(defn sample-Xprops
  [N]
  (let [inds (sample-nom-start-idx N)]
    (map #(sample-Xprop %) inds)))
  

(defn sample-fbshifts
  [N]
  (if (= N 0)
    []
    (let [inds (istats/sample-uniform N :min 0 :max (dec (count fbshifts)) :integers true)]
      (if (= N 1)
        (nth fbshifts (first inds))
        (map #(nth fbshifts %) inds)))))

;; -- Neural  --

(defn sample-tuning-params
  [N]
  (let [mag-mean -2.2
        mag-sd 0.5
        offset-mean 0.45
        offset-sd 0.5
        PDs (istats/sample-uniform N :min 0 :max (* 2 Math/PI))
        mags (map #(Math/exp %) (istats/sample-normal N :mean mag-mean :sd mag-sd))
        offsets (istats/sample-normal N :mean offset-mean :sd offset-sd)]
    (map (fn [c th r] [c (* r (Math/cos th)) (* r (Math/sin th))]) offsets PDs mags)))

(defn get-firing-rate
  [dx dy beta]
  (let [[b0 bx by] beta
        rate (fn [x y] (Math/exp (+ b0 (* bx x) (* by y))))]
    (if (seq? dx)
      (map rate dx dy)
      (rate dx dy))))

(defn sample-spike-counts
  [lambda]
  (if-not (coll? lambda)
    (istats/sample-poisson 1 :lambda lambda)
    (map #(istats/sample-poisson 1 :lambda %) lambda)))

;; --------------

(defn make-center-out-beh-dataset
  [N]
  (let [start-sigma (incant/mult 0.1 (incant/identity-matrix 2))
        end-sigma (incant/mult 0.2 (incant/identity-matrix 2))
        starts (incant/to-vect (istats/sample-mvn N :mean [0 0] :sigma start-sigma))
        reach-dists [10];[7 12]
        nom-tgt-dirs nominal-co-target-dirs ;(map #(* Math/PI %) [0 1/4 1/2 3/4 1 5/4 6/4 7/4])
        nom-dir-dists (for [r reach-dists th nom-tgt-dirs]
                        [r th])
        nom-tgt-xy (map pol2cart nom-dir-dists)
        tgt-idx (istats/sample-uniform N :min 0 :max (dec (count nom-tgt-xy)) :integers true)
        nom-tgts (map #(nth nom-tgt-xy %) tgt-idx)
        tgt-dir-dists (map #(nth nom-dir-dists %) tgt-idx)
        end-xy (map (fn [xy] (incant/to-vect (istats/sample-mvn 1 :mean xy :sigma end-sigma))) nom-tgts)]
    (incant/to-dataset
     (map (fn [i s dd nomtgt exy]
            (hash-map :tgt-idx i :start-xy s :tgt-dir-dist dd :tgt-xy nomtgt :end-xy exy))
          tgt-idx starts tgt-dir-dists nom-tgts end-xy))))

(defn make-co-integ-beh-dataset
  [N & args]
  (let [{:keys [shifts? start-sd end-sd] :or {shifts? false start-sd 1 end-sd 0.1}} args
        start-sigma (incant/mult start-sd (incant/identity-matrix 2))
        end-sigma (incant/mult end-sd (incant/identity-matrix 2))
        Xprops (if (> start-sd 0)
                 (incant/to-vect (istats/sample-mvn N :mean [0 0] :sigma start-sigma))
                 (repeat N [0 0]))
        fbshifts (if shifts?
                   (sample-fbshifts N)
                   (repeat N [0 0]))
        Xvis (map #(incant/to-vect (incant/plus %1 %2)) Xprops fbshifts)
        reach-dists [10]
        nom-tgt-dirs nominal-co-target-dirs ;(map #(* Math/PI %) [0 1/4 1/2 3/4 1 5/4 6/4 7/4])
        nom-dir-dists (for [r reach-dists th nom-tgt-dirs]
                        [r th])
        nom-tgt-xy (map pol2cart nom-dir-dists)
        tgt-idx (istats/sample-uniform N :min 0 :max (dec (count nom-tgt-xy)) :integers true)
        nom-tgts (map #(nth nom-tgt-xy %) tgt-idx)
        tgt-dir-dists (map #(nth nom-dir-dists %) tgt-idx)
        end-xy (if (> end-sd 0)
                 (map (fn [xy] (incant/to-vect (istats/sample-mvn 1 :mean xy :sigma end-sigma))) nom-tgts)
                 nom-tgts)]
    (incant/to-dataset
     (map (fn [i xp xv dd nomtgt exy]
            (hash-map :tgt-idx i :xprop xp :xvis xv :tgt-dir-dist dd :tgt-xy nomtgt :end-xy exy))
          tgt-idx Xprops Xvis tgt-dir-dists nom-tgts end-xy))))

(defn make-neural-population
  [N]
  (let [params (sample-tuning-params N)
        rate-funcs (map (fn [b] (fn [dx dy] (get-firing-rate dx dy b))) params)]
    (map (fn [id beta rate-fn cnt-fn]
           (hash-map :id id :beta beta :rate-func rate-fn :counts-func cnt-fn))
         (range) params rate-funcs (repeat #(sample-spike-counts %)))))
        
;; ------------------------

(defn get-eta-decode-func
  [tgt beta]
  (let [[b0 bx by] beta
        [xt yt] tgt
        k (+ b0 (* bx xt) (* by yt))]
    (fn [xhat yhat] (- k (+ (* bx xhat) (* by yhat))))))
  
(defn get-decode-obj-func
  [tgt neurons counts]
  (let [betas (map #(get % :beta) neurons)
        eta-funcs (map #(get-eta-decode-func tgt %) betas)
        obj-func (fn [[xhat yhat]]
                   (loop [efs eta-funcs cnts counts negll 0]
                     (if (empty? efs)
                       negll
                       (let [eta ((first efs) xhat yhat)
                             cnt (first cnts)]
                         (recur (rest efs) (rest cnts) (+ negll (- (Math/exp eta) (* cnt eta))))))))
        obj-func-der (fn [[xhat yhat]]
                       (loop [efs eta-funcs cnts counts bs betas der1 0 der2 0]
                         (if (empty? efs)
                           [der1 der2]
                           (let [eta ((first efs) xhat yhat)
                                 cnt (first cnts)
                                 g (- (Math/exp eta) cnt)
                                 [b0 bx by] (first bs)
                                 d1 (* (- bx) g)
                                 d2 (* (- by) g)]
                             (recur (rest efs) (rest cnts) (rest bs) (+ der1 d1) (+ der2 d2))))))]
    [obj-func obj-func-der]))

(defn gen-spike-count
  [neuron [dx dy]]
  (let [rate-fn (get neuron :rate-func)
        cnt-fn (get neuron :counts-func)
        rate (rate-fn dx dy)]
    (cnt-fn rate)))

(defn plot-Xprops
  [N]
  (let [X (sample-Xprops N)]
    (incant/view (icharts/scatter-plot (incant/sel X :cols 0) (incant/sel X :cols 1)))))

(defn get-Xprop-Xvis-chart
  [Xprops Xvis]
  (let [plt (doto (icharts/scatter-plot (incant/sel Xprops :cols 0) (incant/sel Xprops :cols 1))
              (icharts/add-points (incant/sel Xvis :cols 0) (incant/sel Xvis :cols 1))
              (icharts/set-stroke-color java.awt.Color/black :series 0)
              (icharts/set-stroke-color java.awt.Color/red :series 1)
              )]
    (doseq [[p v] (map list Xprops Xvis)]
      (icharts/add-lines plt [(first p) (first v)] [(second p) (second v)]))
    plt))

(defn plot-Xprop-Xvis
  [& args]
  (incant/view (apply get-Xprop-Xvis-chart args)))

(defn plot-Xprop-fbshift
  [Xprops fbshifts]
  (let [Xvis (map incant/plus Xprops fbshifts)]
    (plot-Xprop-Xvis Xprops Xvis)))

(defn get-connected-points-chart
  [start end]
  (let [chart (doto (icharts/xy-plot (map first start) (map second start))
                (icharts/add-points (map first end) (map second end)))]
    (doseq [[s e] (map list start end)]
      (icharts/add-lines chart [(first s) (first e)] [(second s) (second e)]))
    chart))
  
(defn plot-integ-behavior
  [beh-ds]
  (plot-Xprop-Xvis (incant/sel beh-ds :cols :xprop) (incant/sel beh-ds :cols :fbshift)))

(defn plot-co-behavior
  [co-ds]
  (let [chart (get-connected-points-chart (incant/sel co-ds :cols :start-xy) (incant/sel co-ds :cols :end-xy))]
    (incant/view chart)))

(defn plot-co-integ-behavior
  [ds]
  (let [xprop (incant/sel ds :cols :xprop)
        xvis (incant/sel ds :cols :xvis)
        end-xy (incant/sel ds :cols :end-xy)
        chart (get-Xprop-Xvis-chart xprop xvis)]
    (doto chart
      (icharts/add-points (map first end-xy) (map second end-xy))
      incant/view)))

(defn get-tuning-charts
  [neuron vects]
  (let [neur-id (get neuron :id)
        rate-fn (get neuron :rate-func)
        cnt-fn (get neuron :counts-func)
        [dx dy] [(map first vects) (map second vects)]
        beta (get neuron :beta)
        [b0 bx by] beta
        rates (rate-fn dx dy)
        linpred (map #(Math/log %) rates)
        counts (cnt-fn rates)
        titlstr (str "neur " neur-id ": " beta)
        xyc-chart  (doto (icharts/scatter-plot dx dy :title titlstr)
                     (icharts/set-stroke-color java.awt.Color/white :series 0)
                     (icharts/add-lines [0 (* 30 bx)] [0 (* 30 by)])
                     (icharts/add-points 0 0))
        xb-chart (doto (icharts/scatter-plot linpred counts :title titlstr)
                   (icharts/add-function #(Math/exp %) (apply min linpred) (apply max linpred)))]
    (doseq [[x y c] (map vector dx dy counts)]
      (icharts/add-text xyc-chart x y (str c)))
    [xyc-chart xb-chart]))

(defn plot-tuning
  [neuron vects]
  (let [[xyc-chart xb-chart] (get-tuning-charts neuron vects)]
    (incant/view xyc-chart)
    (incant/view xb-chart)))

(defn plot-co-tuning
  [co-ds neurons]
  (assert (<= (count neurons) 50) (str "this will generate too many plots"))
  (let [vects (map incant/to-vect (incant/$map incant/minus [:end-xy :start-xy] co-ds))]
    (doseq [n (if (seq? neurons) neurons (list neurons))]
      (plot-tuning n vects))))

;; --------------------------------------

(defn example0
  []
  (let [co-ds (make-center-out-beh-dataset 75)]
    (plot-co-behavior co-ds)))
        
(defn example1
  []
  (let [co-ds (make-center-out-beh-dataset 75)
        neurons (make-neural-population 5)]
    (plot-co-tuning co-ds neurons)
    (plot-co-behavior co-ds)))

(defn example2
  "First create a neural population with 'make-neural-population'. Then pass that population in."
  [neur-pop & args]
  (let [{:keys [alpha n-neurons tuning?] :or {alpha 0.5 n-neurons 5 tuning? true}} args
        max-disp-n 5
        tgt-idx 0
        tgt-dir (nth nominal-co-target-dirs tgt-idx)
        tgt-dist 10
        shift-idx 2
        shift-dir (nth shift-dirs shift-idx)
        shift-mag 5
        fbshift (pol2cart [shift-mag shift-dir])
        Xprop [0 0]
        Xvis (vec (map #(+ %1 %2) Xprop fbshift))
        Xhat (vec (map #(+ %1 %2) Xprop (map #(* alpha %) fbshift)))
        Xtgt (pol2cart [tgt-dist tgt-dir])
        vect-hat [(- (first Xtgt) (first Xhat)) (- (second Xtgt) (second Xhat))]
        neurons (take n-neurons neur-pop)
        spike-counts (map #(gen-spike-count % vect-hat) neurons)
        [obj-func obj-func-der] (get-decode-obj-func Xtgt neurons spike-counts)
        optim-map (ioptim/minimize obj-func [0 0] obj-func-der)
        xhathat (:value optim-map)
        co-ds (make-center-out-beh-dataset 50)
        co-vects (map incant/to-vect (incant/$map incant/minus [:end-xy :start-xy] co-ds))
        tuning-charts (map #(get-tuning-charts % co-vects) neurons)
        all-nom-tgt-xy (map pol2cart (map vector (repeat tgt-dist) nominal-co-target-dirs))
        beh-chart (doto (icharts/scatter-plot (map first all-nom-tgt-xy) (map second all-nom-tgt-xy))
                    (icharts/set-stroke-color java.awt.Color/gray)
                    (icharts/add-points (first Xprop) (second Xprop))
                    (icharts/add-points (first Xvis) (second Xvis))
                    (icharts/add-points (first Xhat) (second Xhat))
                    (icharts/add-points (first Xtgt) (second Xtgt)))]
    (if tuning?
      (doseq [[tc sc] (take max-disp-n (map list tuning-charts spike-counts))]
        (let [xyc-chart (first tc)
              xb-chart (second tc)
              xb-xrange [(-> xb-chart .getPlot .getDomainAxis .getRange .getLowerBound)
                         (-> xb-chart .getPlot .getDomainAxis .getRange .getUpperBound)]]
              (doto xb-chart
                (icharts/set-stroke-color java.awt.Color/white :series 0)
                (icharts/add-lines xb-xrange [sc sc])
                (incant/view)))))
    (doto beh-chart
      (icharts/add-points (first xhathat) (second xhathat))
      incant/view)))

