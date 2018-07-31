package example

import akka.{NotUsed}
import akka.stream._
import akka.stream.scaladsl._
import akka.stream.scaladsl.GraphDSL._

// shape class for a processing module
case class PriorityWorkerPoolShape[In,Out] (
  jobsIn        : Inlet[In],
  priorityJobsIn: Inlet[In],
  resultsOut    : Outlet[Out]
) extends Shape {
  override val inlets: scala.collection.immutable.Seq[Inlet[_]] = jobsIn :: priorityJobsIn :: Nil
  override val outlets: scala.collection.immutable.Seq[Outlet[_]] = resultsOut :: Nil
  override def deepCopy() = PriorityWorkerPoolShape(
    jobsIn.carbonCopy(),
    priorityJobsIn.carbonCopy(),
    resultsOut.carbonCopy()
  )
}

object PriorityWorkerPool {
  
  def apply[In,Out]( worker: Flow[In,Out,Any], workerCount: Int ): Graph[PriorityWorkerPoolShape[In,Out], NotUsed] = {
    
    // create a graph
    GraphDSL.create() { implicit b =>
      import GraphDSL.Implicits._
      
      // 'merger' graph
      val priorityMerge = b.add( MergePreferred[In](1) )
      val balance = b.add( Balance[In](workerCount) )
      val resultMerge = b.add( Merge[Out](workerCount) )
      
      priorityMerge ~> balance
      
      for (i <- 0 until workerCount)
        balance.out(i) ~> worker ~> resultMerge.in(i)
        
      PriorityWorkerPoolShape[In,Out](
        jobsIn = priorityMerge.in(0),
        priorityJobsIn = priorityMerge.preferred,
        resultsOut = resultMerge.out
      )
    }
  }
  
}

object PriorityWorkerPoolExample {
  import akka.actor.ActorSystem
  import akka.stream.ActorMaterializer
  
  def main(args: Array[String]): Unit = {
    implicit val sys = ActorSystem("test-system")
    implicit val mat = ActorMaterializer()
    
    val w1 = Flow[String].map( "step-1" + _ )
    val w2 = Flow[String].map( "step-2" + _ )
    
    RunnableGraph.fromGraph( GraphDSL.create() { implicit b=>
      import GraphDSL.Implicits._
      
      val pp1 = b.add( PriorityWorkerPool(w1, 4) )
      val pp2 = b.add( PriorityWorkerPool(w2, 2) )
      
      Source(1 to 100).map("job: " + _) ~> pp1.jobsIn
      Source(1 to 100).map("priority job: " + _) ~> pp1.priorityJobsIn
      
      pp1.resultsOut ~> pp2.jobsIn
      Source(1 to 100).map("one-step, priority " + _) ~> pp2.priorityJobsIn
      pp2.resultsOut ~> Sink.foreach(println)
      ClosedShape
    } ).run()
  }
}

