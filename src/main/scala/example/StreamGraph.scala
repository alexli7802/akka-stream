package example

import scala.concurrent.Future
import akka.{NotUsed}
import akka.stream._
import akka.stream.scaladsl._

// or to extend 'FanInShape'
import FanInShape.{Init,Name}
class PriorityWorkerPoolShape2[In,Out](_init: Init[Out] = Name("PriorityWorkerPool")) extends FanInShape[Out](_init) {
  protected override def construct(i: Init[Out]) = new PriorityWorkerPoolShape2(i)
  
  val jobsIn = newInlet[In]("jobsIn")
  val priorityJobsIn = newInlet[In]("priorityJobsIn")
}    

object StreamGraph {
  
  def combineGraph(): Unit = {
//    val sendRmotely = Sink.actorRef(actorRef, "Done")
    val localProcessing = Sink.foreach[Int](_ => ???)
  }
  
  def flowGraph(): Unit = {
    
    val pairUpWithToString = Flow.fromGraph( GraphDSL.create() { implicit b =>
      import GraphDSL.Implicits._
      
      val broadcast = b.add( Broadcast[Int](2) )
      val zip = b.add( Zip[Int,String]() )
      
      broadcast.out(0).map(identity) ~> zip.in0
      broadcast.out(1).map(_.toString) ~> zip.in1
      
      FlowShape( broadcast.in, zip.out )
    })
  }
  
  def sourceGraph(): Unit = {
    
    val pairs = Source.fromGraph( GraphDSL.create() { implicit b =>
      import GraphDSL.Implicits._
      
      val zip = b.add( Zip[Int,Int] )
      def ints = Source.fromIterator( ()=>Iterator.from(1) )
      
      ints.filter(_ % 2 != 0) ~> zip.in0
      ints.filter(_ % 2 == 0) ~> zip.in1
      
      SourceShape(zip.out)
    } )
    
//    val firstPair: Future[(Int,Int)] = pairs.runWith(Sink.head)
  }
  
  val pickMaxOfThree = GraphDSL.create() { implicit b =>
    import GraphDSL.Implicits._
    
    val zip1 = b.add( ZipWith[Int,Int,Int](math.max _) )
    val zip2 = b.add( ZipWith[Int,Int,Int](math.max _) )
    zip1.out ~> zip2.in0
    
    UniformFanInShape(zip2.out, zip1.in0, zip1.in1, zip2.in1)
  }
  
  def makeGraph4(): Unit = {
    
    
    
    val resultSink = Sink.head[Int]
    
    val g = RunnableGraph.fromGraph( GraphDSL.create(resultSink) { implicit b => sink => 
      import GraphDSL.Implicits._
      
      val pm3 = b.add( pickMaxOfThree )
      Source.single(1) ~> pm3.in(0)
      Source.single(2) ~> pm3.in(1)
      Source.single(3) ~> pm3.in(2)
      
      pm3.out ~> sink.in
      ClosedShape
    } )
    
//    val max: Future[Int] = g.run()
//    Await.result(max, 300.millis) should equal (3)
  }
  
  def makeGraph3(): Unit = {
    val sinks = Seq("a", "b", "c").map(prefix => 
      Flow[String].filter(str => str.startsWith(prefix)).toMat(Sink.head[String])(Keep.right)  
    )
    
    import GraphDSL.Implicits._
    
//    val g: RunnableGraph[Seq[Future[String]]] = RunnableGraph.fromGraph(GraphDSL.create(sinks) { implicit builder=>sinkList=>
//      val broadcast = builder.add(Broadcast[String](sinkList.size))  
//      
//      Source(List("ax","bx","cx")) ~> broadcast
//      sinkList.foreach(sink => broadcast ~> sink)
//      
//      ClosedShape
//    })
    
//    val matList: Seq[Future[String]] = g.run()
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