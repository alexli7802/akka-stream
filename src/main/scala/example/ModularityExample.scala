package example

import scala.concurrent.{Future,Promise}
import akka.stream.scaladsl._
import akka.stream._
import akka.util.ByteString
import akka.{NotUsed}
import akka.actor.ActorSystem

object ModularityExample {
  
  val complexSource = Source.single(0).map(_ + 1).named("complexSource")
  val complexFlow = Flow[Int].filter( _ != 0 ).map(_ - 2).named("complexFlow")
  val complexSink = complexFlow.to(Sink.fold(0)(_ + _)).named("complexSink")
  
  val runnableGraph = complexSource.to(complexSink)
}

object ComplexGraphExample {
  import GraphDSL.Implicits._
  
  implicit val system = ActorSystem("test-system")
  def example5(): Unit = {
    // step-1: nested source
    val source: Source[Int,Promise[Option[Int]]] = Source.maybe[Int]
    val flow1: Flow[Int,Int,NotUsed] = Flow[Int].take(100)
    
    val nestedSource: Source[Int,Promise[Option[Int]]] = source.viaMat(flow1)(Keep.left).withAttributes(Attributes.name("nestedSource"))            // named("nestedSource")
    
    // step-2: nested flow
    import Tcp._
    val flow2: Flow[Int,ByteString,NotUsed] = Flow[Int].map{ i => ByteString(i.toString) }
    val flow3: Flow[ByteString,ByteString,Future[OutgoingConnection]] = Tcp().outgoingConnection("localhost", 8080)
    val nestedFlow: Flow[Int,ByteString,Future[OutgoingConnection]] = flow2.viaMat(flow3)(Keep.right).named("nestedFlow")
    
    // step-3: nested sink
    val sink: Sink[ByteString,Future[String]] = Sink.fold("")(_ + _.utf8String)
    val nestedSink: Sink[Int, (Future[OutgoingConnection], Future[String])] = nestedFlow.toMat(sink)(Keep.both)
    
    case class MyClass( private val p: Promise[Option[Int]], conn: OutgoingConnection ) {
      def close() = p.trySuccess(None)
    }
    
    def f(p: Promise[Option[Int]], rest: (Future[OutgoingConnection], Future[String])): Future[MyClass] = {
      import scala.concurrent.ExecutionContext.Implicits.global
      val connFuture = rest._1
      connFuture.map( MyClass(p, _) )
    }
    
    val runnableGraph: RunnableGraph[Future[MyClass]] = nestedSource.toMat(nestedSink)(f)
  }
  
  def example4(): Unit = {
    // using fluid DSL: to, via ...
    val runnable1 = Source.single(0).to( Sink.foreach(println) )
    
    val runnable2 = RunnableGraph.fromGraph( GraphDSL.create() { implicit builder =>
      val embeddedClosed: ClosedShape = builder.add( runnable1 )

      embeddedClosed
    })
  }
  
  val example3 = 
    GraphDSL.create() { implicit builder =>
      val B = builder.add( Broadcast[Int](2) )
      val C = builder.add( Merge[Int](2) )
      val E = builder.add( Balance[Int](2) )
      val F = builder.add( Merge[Int](2) )
           C <~ F
      B ~> C ~> F
      B ~> Flow[Int].map(_ + 1) ~> E ~> F
      FlowShape( B.in, E.out(1) )
    }.named("partial")
  
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