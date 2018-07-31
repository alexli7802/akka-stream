package example

import scala.annotation.unchecked._
import scala.collection.immutable
import akka.stream._
import akka.stream.scaladsl._

// shape class for 'BidiFlow'
final case class BidiShape[-In1, +Out1, -In2, +Out2] (
  in1: Inlet[In1 @uncheckedVariance],
  out1: Outlet[Out1 @uncheckedVariance],
  in2: Inlet[In2 @uncheckedVariance],
  out2: Outlet[Out2 @uncheckedVariance]
) extends Shape {
  
  override val inlets: immutable.Seq[Inlet[_]] = in1 :: in2 :: Nil
  override val outlets: immutable.Seq[Outlet[_]] = out1 :: out2 :: Nil
  
  def this(top: FlowShape[In1,Out1], bottom: FlowShape[In2,Out2]) = this(top.in, top.out, bottom.in, bottom.out)
  override def deepCopy(): BidiShape[In1,Out1,In2,Out2] = BidiShape(
    in1.carbonCopy(), out1.carbonCopy(),
    in2.carbonCopy(), out2.carbonCopy()
  )
}

object BidiFlow {
  
}