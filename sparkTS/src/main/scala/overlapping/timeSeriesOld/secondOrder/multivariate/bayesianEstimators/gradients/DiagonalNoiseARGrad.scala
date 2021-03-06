package main.scala.overlapping.timeSeriesOld.secondOrder.multivariate.bayesianEstimators.gradients

import breeze.linalg.{diag, DenseVector, DenseMatrix}
import main.scala.overlapping.containers.TSInstant
import org.apache.spark.broadcast.Broadcast

import main.scala.overlapping.timeSeries._

/**
 * Created by Francois Belletti on 9/28/15.
 */
class DiagonalNoiseARGrad[IndexT](
   val sigmaEps: DenseVector[Double],
   val nSamples: Long,
   val mean: Broadcast[DenseVector[Double]])
  extends Serializable{

  val d = sigmaEps.size
  val precisionMatrix = DenseVector.ones[Double](d)
  precisionMatrix :/= sigmaEps

  val precisionMatrixAsDiag = diag(precisionMatrix)

  def apply(params: Array[DenseMatrix[Double]],
            data: Array[(TSInstant[IndexT], DenseVector[Double])]): Array[DenseMatrix[Double]] = {

    val p = params.length
    val totGradient   = Array.fill(p){DenseMatrix.zeros[Double](d, d)}
    val prevision     = DenseVector.zeros[Double](d)

    val meanValue = mean.value

    for(h <- 1 to p){
      prevision += params(h - 1) * (data(p - h)._2 - meanValue)
    }

    val normError = precisionMatrixAsDiag * (data(p)._2 - prevision)

    for(h <- 1 to p){
      totGradient(h - 1) :-= normError * (data(p - h)._2 - meanValue).t * 2.0 / nSamples.toDouble
    }

    totGradient

  }

}
