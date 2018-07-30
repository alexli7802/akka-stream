package example

import scala.concurrent.Future
import akka.{NotUsed}
import akka.stream._
import akka.stream.scaladsl._

object StreamGraph {
  
  def makeGraph3(): Unit = {
    val sinks = Seq("a", "b", "c").map(prefix => 
      Flow[String].filter(str => str.startsWith(prefix)).toMat(Sink.head[String])(Keep.right)  
    )
    
    import GraphDSL.Implicits._
    
    val g: RunnableGraph[Seq[Future[String]]] = RunnableGraph.fromGraph(GraphDSL.create(sinks) { implicit builder=>sinkList=>
      val broadcast = builder.add(Broadcast[String](sinkList.size))  
      
      Source(List("ax","bx","cx")) ~> broadcast
      sinkList.foreach(sink => broadcast ~> sink)
      
      ClosedShape
    })
    
    val matList: Seq[Future[String]] = g.run()
  }
  
  def makeGraph2(): Unit = {
    val topHeadSink = Sink.head[Int]
    val bottomHeadSink = Sink.head[Int]
    val sharedDoubler = Flow[Int].map(_ * 2)
    
    RunnableGraph.fromGraph(GraphDSL.create(topHeadSink, bottomHeadSink)((_,_)) { implicit builder => 
      (topHS, bottomHS) =>
      import GraphDSL.Implicits._
      val broadcast = builder.add(Broadcast[Int](2))
      Source.single(1) ~> broadcast.in
      
      broadcast ~> sharedDoubler ~> topHS.in
      broadcast ~> sharedDoubler ~> bottomHS.in
      ClosedShape
    })
  }
  
  def makeGraph1(): Unit = {
/*
                          ----(f2)----
                          |          |
          in --(f1)--> bcast         +> merge --(f3)--> out
                          |          |
                          ----(f4)----
*/
    
    val g = RunnableGraph.fromGraph(GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
      import GraphDSL.Implicits._
      
      val in = Source(1 to 10)
      val out = Sink.ignore
      
      val bcast = builder.add(Broadcast[Int](2))
      val merge = builder.add(Merge[Int](2))
      
      val f1, f2, f3, f4 = Flow[Int].map(_ + 10)
      in ~> f1 ~> bcast ~> f2 ~> merge ~> f3 ~> out
      bcast ~> f4 ~> merge
      ClosedShape
    })
  }
  
  def main(args: Array[String]): Unit = {
    
  }
}