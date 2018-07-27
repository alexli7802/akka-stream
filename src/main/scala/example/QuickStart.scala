package example

import akka.stream._
import akka.stream.scaladsl._
import akka.{NotUsed,Done}
import akka.actor.ActorSystem
import akka.util.ByteString
import scala.concurrent._
import scala.concurrent.duration._
import java.nio.file.Paths

object QuickStart1 {
  
  def lineSink(filename: String): Sink[String, Future[IOResult]] = {
    Flow[String].map(s => ByteString(s + "\n")).toMat( FileIO.toPath(Paths.get(filename)) )(Keep.right)
  }
  
  def main(args: Array[String]): Unit = {
	  implicit val system = ActorSystem("QuickStart")

	  //materializer makes streams run
	  implicit val materializer = ActorMaterializer()
    
	  //source describes the data you want to run
    val source: Source[Int, NotUsed] = Source(1 to 100)
    val done: Future[Done] = source.runForeach( i => println(i) )(materializer)
    
    val factorials = source.scan(BigInt(1))((acc, next) => acc * next)
    factorials.zipWith(Source(0 to 100))((num, idx) => s"$idx! = $num").throttle(1, 1.second).runForeach(println)
        
/*    
    factorials.map(_.toString).runWith(lineSink("./test/factorials2.txt"))

*/
/*    
    val result: Future[IOResult] = factorials
        .map(num => ByteString(s"$num\n"))
        .runWith(FileIO.toPath(Paths.get("factorials.txt")))
*/                                     
    
    implicit val ec = system.dispatcher
//    done.onComplete(_ => system.terminate())
  }
}