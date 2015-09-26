package overlapping.models.secondOrder.multivariate.bayesianEstimators

import breeze.linalg.{DenseVector, DenseMatrix}
import org.apache.spark.rdd.RDD
import org.apache.spark.storage.StorageLevel._
import overlapping.IntervalSize
import overlapping.containers.block.SingleAxisBlock
import overlapping.models.Estimator
import overlapping.models.secondOrder.{ModelSize, SecondOrderEssStat}
import overlapping.models.secondOrder.multivariate.bayesianEstimators.procedures.GradientDescent

import scala.reflect.ClassTag

/**
 * Created by Francois Belletti on 9/16/15.
 */
class VARGradientDescent[IndexT <: Ordered[IndexT] : ClassTag](
    val p: Int,
    val deltaT: Double,
    val loss: AutoregressiveLoss[IndexT],
    val gradient: AutoregressiveGradient[IndexT],
    val stepSize: Int => Double,
    val precision: Double,
    val maxIter: Int,
    val start: Array[DenseMatrix[Double]])
  extends Estimator[IndexT, DenseVector[Double], Array[DenseMatrix[Double]]]{

  override def windowEstimate(slice: Array[(IndexT, DenseVector[Double])]): Array[DenseMatrix[Double]] = {
    GradientDescent.run[Array[(IndexT, DenseVector[Double])]](
      {case (param, data) => loss.setNewX(param); loss.windowStats(data)},
      {case (param, data) => gradient.setNewX(param); gradient.windowStats(data)},
      gradient.getGradientSize,
      stepSize,
      precision,
      maxIter,
      start,
      slice
    )
  }

  override def blockEstimate(block: SingleAxisBlock[IndexT, DenseVector[Double]]): Array[DenseMatrix[Double]] = {
    GradientDescent.run[SingleAxisBlock[IndexT, DenseVector[Double]]](
      {case (param, data) => loss.setNewX(param); loss.blockStats(data)},
      {case (param, data) => gradient.setNewX(param); gradient.blockStats(data)},
      gradient.getGradientSize,
      stepSize,
      precision,
      maxIter,
      start,
      block
    )
  }

  override def estimate(timeSeries: RDD[(Int, SingleAxisBlock[IndexT, DenseVector[Double]])]): Array[DenseMatrix[Double]] = {

    timeSeries.persist(MEMORY_AND_DISK)

    val parameters = GradientDescent.run[RDD[(Int, SingleAxisBlock[IndexT, DenseVector[Double]])]](
      {case (param, data) => loss.setNewX(param); loss.timeSeriesStats(data)},
      {case (param, data) => gradient.setNewX(param); gradient.timeSeriesStats(data)},
      gradient.getGradientSize,
      stepSize,
      precision,
      maxIter,
      start,
      timeSeries
    )

    timeSeries.unpersist(false)

    parameters

  }

}