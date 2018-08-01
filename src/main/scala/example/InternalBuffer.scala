package example

import akka.stream._
import akka.stream.scaladsl._
import akka.actor.{ActorSystem}

object InternalBuffer {
  
  implicit val system = ActorSystem("test-system")
  implicit val materializer = ActorMaterializer(
      ActorMaterializerSettings(system).withInputBuffer(initialSize=64, maxSize=64)  
    )
  
  def example3(): Unit = {
    import scala.concurrent.duration._
    case class Tick()
    
    RunnableGraph.fromGraph( GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._
      val zipper = builder.add( ZipWith[Tick,Int,Int]((tick,count)=>count).async )
      Source.tick(initialDelay = 3.second, interval = 3.second, Tick()) ~> zipper.in0
      Source.tick(initialDelay = 1.second, interval = 3.second, "message!")
        .conflateWithSeed(seed= (_) => 1)((count, _) => count + 1) ~> zipper.in1
      zipper.out ~> Sink.foreach(println)        
      ClosedShape
    } ).run()    
  }
    
  def example2(): Unit = {
    val section = Flow[Int].map(_ * 2).async
                    .addAttributes(Attributes.inputBuffer(initial=1, max=1))
    val flow = section.via( Flow[Int].map(_ / 2) ).async
  }
    
  def example1(): Unit = {
    Source(1 to 3)
      .map { i => println(s"A: $i"); i}.async
      .map { i => println(s"B: $i"); i}.async
      .map { i => println(s"C: $i"); i}.async
      .runWith(Sink.ignore)
  }
  
  def main(args: Array[String]): Unit = {
    example3()    
    
//    system.terminate()
  }
}