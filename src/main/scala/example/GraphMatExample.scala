package example

import scala.concurrent.{Future}
import akka.stream._
import akka.stream.scaladsl._

object GraphMatExample {
  
  import GraphDSL.Implicits._
  
  def deadLock = {
    RunnableGraph.fromGraph( GraphDSL.create() { implicit builder => 
      val merge = builder.add( Merge[Int](2) )
      val bcast = builder.add( Broadcast[Int](2) )
      
//      source ~> merge ~> Flow[Int].map { s => println(s); s} ~> bcast ~> Sink.ignore
//                merge <~ Flow[Int].buffer(10, OverflowStrategy.dropHead) <~ bcast
      ClosedShape
    } )
  }
  
  // define a graph for 'Flow'
  val fGraph = GraphDSL.create( Sink.fold[Int,Int](0)(_+_) ) { implicit builder => fold =>
    FlowShape( fold.in, builder.materializedValue.mapAsync(4)(identity).outlet )
  }
  
  val foldFlow: Flow[Int,Int,Future[Int]] = Flow.fromGraph( fGraph )
  
  // bad example
  val graph1 = GraphDSL.create( Sink.fold[Int,Int](0)(_+_)) { implicit builder => fold =>
    builder.materializedValue.mapAsync(4)(identity) ~> fold
    SourceShape( builder.materializedValue.mapAsync(4)(identity).outlet )
  }
  val cyclicFold: Source[Int,Future[Int]] = Source.fromGraph(graph1)
}