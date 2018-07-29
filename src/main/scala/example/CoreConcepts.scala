package example

import akka.stream.scaladsl._
import scala.concurrent._
import scala.util.{Success,Failure}
import akka.{NotUsed}
import akka.actor.{Cancellable,ActorSystem,Actor}
import akka.stream.{OverflowStrategy,ActorMaterializer}

final class RunWithMyself extends Actor {
  implicit val mat = ActorMaterializer()
  Source.maybe
    .runWith( Sink.onComplete {
      case Success(done) => println(s"Completed: $done")
      case Failure(ex)   => println(s"Failed: ${ex.getMessage}")
    } )
    
  def receive = {
    case "boom" => context.stop(self)
  }
}

object CoreConcepts {

  implicit val system = ActorSystem("ExampleSystem")
  implicit val mat = ActorMaterializer()
  
  def makeMVP(): Unit = {
    val matValuePoweredSource = Source.actorRef[String](bufferSize = 100, overflowStrategy = OverflowStrategy.fail)
    val (actorRef, source) = matValuePoweredSource.preMaterialize()
    actorRef ! "Hello"
    source.runWith(Sink.foreach(println))
  }
  
  def combineMaterializedValues(): Unit = {
    // a source can be signaled explicitly from outside
    val source: Source[Int, Promise[Option[Int]]] = Source.maybe[Int]    
    
    val flow: Flow[Int, Int, Cancellable] = ???
    
    val sink: Sink[Int, Future[Int]] = Sink.head[Int]
    
    val r1: RunnableGraph[Promise[Option[Int]]] = source.via(flow).to(sink)
    val r2: RunnableGraph[Cancellable] = source.viaMat(flow)(Keep.right).to(sink)
    val r3: RunnableGraph[Future[Int]] = source.via(flow).toMat(sink)(Keep.right)
    
    val r4: Future[Int] = source.via(flow).runWith(sink)
    val r5: Promise[Option[Int]] = flow.to(sink).runWith(source)
    val r6: (Promise[Option[Int]],Future[Int]) = flow.runWith(source, sink)
    
    val r7: RunnableGraph[(Promise[Option[Int]],Cancellable)] = source.viaMat(flow)(Keep.both).to(sink)
    val r8: RunnableGraph[(Promise[Option[Int]],Future[Int])] = source.via(flow).toMat(sink)(Keep.both)
    val r9: RunnableGraph[((Promise[Option[Int]],Cancellable), Future[Int])] = source.viaMat(flow)(Keep.both).toMat(sink)(Keep.both)
    val r10: RunnableGraph[(Cancellable, Future[Int])] = source.viaMat(flow)(Keep.right).toMat(sink)(Keep.both)
    
    val r11: RunnableGraph[(Promise[Option[Int]],Cancellable,Future[Int])] = 
      r9.mapMaterializedValue {
        case ((promise, cancellable), future) => (promise,cancellable,future)
      }
    val (p,c,f) = r11.run()
    
    p.success(None)
    c.cancel()
    f.map(_ + 3)
    
    val r12: RunnableGraph[(Promise[Option[Int]], Cancellable, Future[Int])] = 
      RunnableGraph.fromGraph( GraphDSL.create(source, flow, sink)((_,_,_)) { implicit builder =>
        import GraphDSL.Implicits._
        src ~> f ~> dst
        ClosedShape
      })
  }
  
  def makeFusion(): Unit = {
    Source(List(1,2,3)).map( _ + 1 ).async
      .map( _ * 2 )
      .to(Sink.ignore)
  }
  
  def makeSource(): Unit = {
    val s1 = Source(List(1,2,3))
    val s2 = Source.fromFuture(Future.successful("Hello Streams!"))
    val s3 = Source.single("only me")
    val s4 = Source.empty
  }
  
  def makeSink(): Unit = {
    val s1 = Sink.fold[Int,Int](0)(_ + _)
    val s2 = Sink.head
    val s3 = Sink.ignore
    val s4 = Sink.foreach[String](println(_))
  }
  
  def makePipeline(): Unit = {
    val p1 = Source(1 to 6)
              .via( Flow[Int].map(_ * 2) )
              .to ( Sink.foreach(println(_)) )
              
    val p2 = Source(1 to 6).map( _ * 2 ).to( Sink.foreach(println(_)) )
    
    val sink1: Sink[Int, NotUsed] = Flow[Int].map( _ * 2 ).to(Sink.foreach(println))
    val p3 = Source(1 to 6).to(sink1)
    
    val sink2: Sink[Int, NotUsed] = Flow[Int].alsoTo(Sink.foreach(println)).to(Sink.ignore)
    val p4 = Source(1 to 6).to(sink2)
  }
  
  def main(args: Array[String]): Unit = {
    val source = Source(1 to 10)
    val sink = Sink.fold[Int, Int](0)(_ + _)
    
    val runnable: RunnableGraph[Future[Int]] = source.toMat(sink)(Keep.right)
    
    
    val sum1: Future[Int] = runnable.run()
    val sum2: Future[Int] = runnable.run()
  }
}