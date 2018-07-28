package example

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import akka.stream._
import scala.concurrent.Future

final case class Author(handle: String)
final case class Hashtag(name: String)
final case class Tweet(author: Author, timestamp: Long, body: String) {
  def hashtags: Set[Hashtag] = body.split(" ").collect {
    case t if t.startsWith("#") => Hashtag(t.replaceAll("[^#\\w]", ""))
  }.toSet
}

object QuickStart2 {

  val akkaTag = Hashtag("#akka")

  val tweets: Source[Tweet, NotUsed] = Source(
    Tweet(Author("rolandkuhn"), System.currentTimeMillis, "#akka rocks!") ::
    Tweet(Author("patriknw"), System.currentTimeMillis, "#akka !") ::
    Tweet(Author("bantonsson"), System.currentTimeMillis, "#akka !") ::
    Tweet(Author("drewhk"), System.currentTimeMillis, "#akka !") ::
    Tweet(Author("ktosopl"), System.currentTimeMillis, "#akka on the rocks!") ::
    Tweet(Author("mmartynas"), System.currentTimeMillis, "wow #akka !") ::
    Tweet(Author("akkateam"), System.currentTimeMillis, "#akka rocks!") ::
    Tweet(Author("bananaman"), System.currentTimeMillis, "#bananas rock!") ::
    Tweet(Author("appleman"), System.currentTimeMillis, "#apples rock!") ::
    Tweet(Author("drama"), System.currentTimeMillis, "we compared #apples to #oranges!") ::
    Nil)  
    
  def main(args: Array[String]): Unit = {
    
	  implicit val system = ActorSystem("reactive-tweets")
	  implicit val materializer = ActorMaterializer()
	  
	  val hashtags: Source[Hashtag, NotUsed] = tweets.mapConcat(t => t.hashtags.toList)
	  
	  val count: Flow[Tweet, Int, NotUsed] = Flow[Tweet].map(_ => 1)
	  val sumSink: Sink[Int, scala.concurrent.Future[Int]] = Sink.fold[Int, Int](0)(_+_)
	  val counterGraph: RunnableGraph[Future[Int]] = tweets.via(count).toMat(sumSink)(Keep.right)
	  val sum: Future[Int] = counterGraph.run()
	  sum.foreach(c => println(s"Total tweets processed: $c"))(system.dispatcher)
			  
	  val writeAuthors: Sink[Author, NotUsed] = ???
	  val writeHashtags: Sink[Hashtag, NotUsed] = ???
	  
	  val g = RunnableGraph.fromGraph(GraphDSL.create() { implicit gb =>
	    import GraphDSL.Implicits._
	    
	    val bcast = gb.add(Broadcast[Tweet](2))
	    tweets ~> bcast.in
	    bcast.out(0) ~> Flow[Tweet].map(_.author) ~> writeAuthors
	    bcast.out(1) ~> Flow[Tweet].mapConcat(_.hashtags.toList) ~> writeHashtags
	    ClosedShape	    
	  })
	  
	  g.run()
/*	  
		tweets.map(_.hashtags)
  	  .reduce(_ ++ _)
  	  .mapConcat(identity)
  	  .map(_.name.toUpperCase)
  	  .runWith(Sink.foreach(println))
*/  	  
  }
}