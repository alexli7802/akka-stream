package example

import akka.stream.scaladsl._
import akka.stream._

object ModularityExample {
  
  val complexSource = Source.single(0).map(_ + 1).named("complexSource")
  val complexFlow = Flow[Int].filter( _ != 0 ).map(_ - 2).named("complexFlow")
  val complexSink = complexFlow.to(Sink.fold(0)(_ + _)).named("complexSink")
  
  val runnableGraph = complexSource.to(complexSink)
}

object ComplexGraphExample {
  import GraphDSL.Implicits._
  
  val example1 = 
    RunnableGraph.fromGraph( GraphDSL.create() { implicit builder=>
      val A: Outlet[Int] = builder.add(Source.single(0)).out
      val B: UniformFanOutShape[Int,Int] = builder.add( Broadcast[Int](2) )
      val C: UniformFanInShape[Int,Int] = builder.add( Merge[Int](2) )
      val D: FlowShape[Int,Int] = builder.add( Flow[Int].map(_ + 1) )
      val E: UniformFanOutShape[Int,Int] = builder.add( Balance[Int](2) )
      val F: UniformFanInShape[Int, Int] = builder.add( Merge[Int](2) )
      val G: Inlet[Any] = builder.add( Sink.foreach(println) ).in
                C     <~      F
      A ~> B ~> C     ~>      F
           B ~> D  ~>  E  ~>  F
                       E  ~>  G
      ClosedShape
    } )
    
  val example2 = 
    RunnableGraph.fromGraph( GraphDSL.create() { implicit builder=>
      val B: UniformFanOutShape[Int,Int] = builder.add( Broadcast[Int](2) )
      val C: UniformFanInShape[Int,Int] = builder.add( Merge[Int](2) )
      val E: UniformFanOutShape[Int,Int] = builder.add( Balance[Int](2) )
      val F: UniformFanInShape[Int, Int] = builder.add( Merge[Int](2) )

      Source.single(0) ~> B.in; B.out(0) ~> C.in(1); C.out ~> F.in(0)
      C.in(0) <~ F.out
      
      B.out(1).map(_ + 1) ~> E.in; E.out(0) ~> F.in(1)
      E.out(1) ~> Sink.foreach(println)

      ClosedShape
    } )
}