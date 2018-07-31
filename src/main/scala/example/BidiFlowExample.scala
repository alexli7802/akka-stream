package example

import java.nio.ByteOrder
import scala.annotation.unchecked._
import scala.collection.immutable
import akka.util.ByteString
import akka.stream._
import akka.stream.scaladsl._

trait Message
case class Ping(id: Int) extends Message
case class Pong(id: Int) extends Message

object Message {
  
  def fromBytes(bytes: ByteString): Message = {
    implicit val order = ByteOrder.LITTLE_ENDIAN
    val it = bytes.iterator
    it.getByte match {
      case 1 => Ping(it.getInt)
      case 2 => Pong(it.getInt)
      case other => throw new RuntimeException(s"parse error: expected 1|2 got $other")
    }
  }
  
  def toBytes(msg: Message): ByteString = {
    implicit val order = ByteOrder.LITTLE_ENDIAN
    msg match {
      case Ping(id) => ByteString.newBuilder.putByte(1).putInt(id).result()
      case Pong(id) => ByteString.newBuilder.putByte(2).putInt(id).result()
    }
  }
}

// shape class for 'BidiFlow'
final case class BidiShape1s[-In1, +Out1, -In2, +Out2] (
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

object BidiFlowExample {
  import Message._
  def main(args: Array[String]): Unit = {
    
    val codec = BidiFlow.fromFunctions(toBytes _, fromBytes _)
    
//    val codecVerbose = BidiFlow.fromGraph( GraphDSL.create() { implicit b=>
//      val outbound = b.add( Flow[Message].map(toBytes) )  
//      val inbound = b.add( Flow[ByteString].map(fromBytes) )
//      BidiShape(outbound, inbound)
//    } )
    
    
  }
}