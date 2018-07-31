package example

import java.nio.ByteOrder
import akka.util.ByteString
import akka.stream._
import akka.stream.scaladsl._
import akka.stream.scaladsl.GraphDSL._
import akka.stream.stage.{GraphStage,GraphStageLogic,OutHandler,InHandler}

object LocalImplicits {
  implicit val order = ByteOrder.LITTLE_ENDIAN
}
class FrameParser extends GraphStage[FlowShape[ByteString, ByteString]] {
  import LocalImplicits._
  
  val in = Inlet[ByteString]("FrameParser.in")
  val out = Outlet[ByteString]("FrameParser.out")
  override val shape = FlowShape.of(in,out)
  
  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = 
    new GraphStageLogic(shape) {
      var stash = ByteString.empty
      var needed = -1
      setHandler(out, new OutHandler {
        override def onPull(): Unit = if (isClosed(in)) run() else pull(in)
      })
      
      setHandler(in, new InHandler {
        override def onPush(): Unit = { val bytes = grab(in); stash = stash ++ bytes; run(); }

        override def onUpstreamFinish(): Unit = {
          if (stash.isEmpty) completeStage() else if ( isAvailable(out) ) run()
        }
      })


      private def run(): Unit = {
        if (needed == -1) {
          if (stash.length < 4) {
            if (isClosed(in)) completeStage() else pull(in)
          } else {
            needed = stash.iterator.getInt
            stash = stash.drop(4)
            run()
          }
        } else if (stash.length < needed) {
          if ( isClosed(in) ) completeStage() else pull(in)
        } else {
          val emit = stash.take(needed)
          stash = stash.drop(needed)
          needed = -1
          push(out,emit)
        }
      }

    }
}

object BidiFlowFraming {
  import LocalImplicits._
  def addLengthHeader(bytes: ByteString) = {
    val len = bytes.length
    ByteString.newBuilder.putInt(len).append(bytes).result()
  }
  
  val framing = BidiFlow.fromGraph( GraphDSL.create() { implicit b=>
    import GraphDSL.Implicits._
    
    val outbound = b.add( Flow[ByteString].map(addLengthHeader(_)) )
    val inbound = b.add( Flow[ByteString].via(new FrameParser) )
    BidiShape.fromFlows(outbound, inbound)
  })
}