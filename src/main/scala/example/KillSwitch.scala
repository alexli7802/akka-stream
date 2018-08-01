package example

/*
 *   KillSwitch terminates 'FlowShape' operators from outside, uses a flow element to link to the target that needs completion control!
 * 
 * */

import scala.concurrent._
import scala.concurrent.duration._
import akka.actor.ActorSystem
import akka.stream.scaladsl._
import akka.stream._
import akka.NotUsed

object KillSwitch {
  
  implicit val system = ActorSystem("test-system")
  implicit val materializer = ActorMaterializer()

  /*
   * 'BroadcastHub' allows consumers to be attached to producer at later stage. In producer's perspective, 
   * it's a Sink! 
   * */
  def try_broadcasthub(): Unit = {
    val producer = Source.tick(1.second, 1.second, "New message")
    val runnableGraph: RunnableGraph[Source[String,NotUsed]] = producer.toMat(BroadcastHub.sink(bufferSize=256))(Keep.right)
    
    val fromProducer: Source[String,NotUsed] = runnableGraph.run()
    fromProducer.runForeach(msg => println("consumer1: " + msg))
    fromProducer.runForeach(msg => println("consumer2: " + msg))
  }
  
  
  /*
   * Use 'MergeHub' to implement a dynamic fan-in junction point
   * */
  def try_mergehub(): Unit = {
    val consumer = Sink.foreach(println)
    
    val runnableGraph: RunnableGraph[Sink[String,NotUsed]] = MergeHub.source[String](perProducerBufferSize = 16).to(consumer)
    
    val toConsumer: Sink[String,NotUsed] = runnableGraph.run()
    
    Source.single("Hello!").runWith(toConsumer)
    Source.single("Hub!").runWith(toConsumer)
  }
  
  /*
   *  'SharedKillSwitch' controls multiple 'FlowShape' Graphs to complete.
   * */
  def try_SharedKillSwitch(): Unit = {
    val countingSrc = Source(Stream.from(1)).delay(1.second, DelayOverflowStrategy.backpressure)
    val lastSnk = Sink.last[Int]
    val sharedKillSwitch = KillSwitches.shared("my-kill-switch")
    
    val last = countingSrc.via(sharedKillSwitch.flow).runWith(lastSnk)
    
    val delayedLast = countingSrc.delay(1.second, DelayOverflowStrategy.backpressure)
                        .via(sharedKillSwitch.flow)
                        .runWith(lastSnk)
                        
    doSomethingElse()
    sharedKillSwitch.shutdown()
    
//    Await.result(last, 1.second) shouldBe 2
//    Await.result(delayedLast, 1.second) shouldBe 1
  }
  
  /*
   *  UniqueKillSwitch controls the completion of one materialized 'FlowShape' Graph.
   */
  def try_UniqueKillSwith(): Unit = {
    val countingSrc = Source(Stream.from(1)).delay(1.second, DelayOverflowStrategy.backpressure)
    val lastSnk = Sink.last[Int]
    
    val (killSwitch, last) = countingSrc
                               .viaMat(KillSwitches.single)(Keep.right)
                               .toMat(lastSnk)(Keep.both)
                               .run()
                               
    doSomethingElse()
    
    killSwitch.shutdown()
//    Await.result(last, 1.second) shouldBe 2
  }
  
  def doSomethingElse(): Unit = ???
}