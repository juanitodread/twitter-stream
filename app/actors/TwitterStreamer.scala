package actors

import akka.actor.ActorRef
import akka.actor.Actor
import play.api.Logger
import play.api.libs.json.Json
import akka.actor.Props
import play.api.libs.iteratee.Enumerator
import play.api.libs.json.JsObject
import play.api.libs.ws._
import play.api.libs.oauth.OAuthCalculator
import play.api._
import play.api.Play.current
import play.api.libs.oauth.ConsumerKey
import play.api.libs.oauth.RequestToken
import play.api.libs.iteratee.{Concurrent, Enumeratee}
import play.extras.iteratees._
import play.api.libs.iteratee.Iteratee

import play.api.libs.concurrent.Execution.Implicits._

class TwitterStreamer(out: ActorRef) extends Actor {

  def receive = {

    case "subscribe" =>
      Logger.info("Received subscription from a client")

      TwitterStreamer.subscribe(out)
  }

}

object TwitterStreamer {

  val credentials: Option[(ConsumerKey, RequestToken)] = for {
    apiKey <- Play.configuration.getString("twitter.apiKey")
    apiSecret <- Play.configuration.getString("twitter.apiSecret")
    token <- Play.configuration.getString("twitter.token")
    tokenSecret <- Play.configuration.getString("twitter.tokenSecret")
  } yield (ConsumerKey(apiKey, apiSecret),
           RequestToken(token, tokenSecret))
  
  private var broadcastEnumerator: Option[Enumerator[JsObject]] = None
  
  def props(out: ActorRef) = Props(new TwitterStreamer(out))
  
  def connect(): Unit = {
    credentials.map { case (consumerKey, requestToken) =>      
      
      val (iteratee, enumerator) = Concurrent.joined[Array[Byte]]
      
      val jsonStream: Enumerator[JsObject] = enumerator &> 
        Encoding.decode() &> 
        Enumeratee.grouped(JsonIteratees.jsSimpleObject) 
        
      val (be, _) = Concurrent.broadcast(jsonStream)
      broadcastEnumerator = Some(be)
      
      val url = "https://stream.twitter.com/1.1/statuses/filter.json"
      
      WS.url(url)
        .withRequestTimeout(-1)
        .sign(OAuthCalculator(consumerKey, requestToken))
        .withQueryString("track" -> "messi")
        .get { response => 
            Logger.info(s"Status: ${response.status}")
            iteratee
        }.map { _ => Logger.info("Twitter stream closed") }
    } getOrElse {
      Logger.error("Twitter credentials missing")
    }
  }
  
  def subscribe(out: ActorRef): Unit = {
    if(broadcastEnumerator == None) {
      connect()
    }
    
    val twitterClient = Iteratee.foreach[JsObject] { t => out ! t }
    broadcastEnumerator.map { enumerator => 
      enumerator run twitterClient
    }
  }
  
}