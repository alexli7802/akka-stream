package example

import akka.stream.scaladsl._
import akka.stream._

object ModularityExample {
  
  val complexSource = Source.single(0).map(_ + 1).named("complexSource")
  val complexFlow = Flow[Int].filter( _ != 0 ).map(_ - 2).named("complexFlow")
  val complexSink = complexFlow.to(Sink.fold(0)(_ + _)).named("complexSink")
  
  val runnableGraph = complexSource.to(complexSink)
}