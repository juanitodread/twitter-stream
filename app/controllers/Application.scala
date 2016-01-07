package controllers

import play.api._
import play.api.mvc._
import play.api.libs.oauth.ConsumerKey
import play.api.libs.oauth.RequestToken
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.oauth.ConsumerKey
import scala.concurrent.Future
import play.api.libs.ws._
import play.api.libs.oauth.OAuthCalculator
import play.api.libs.iteratee.Iteratee
import play.api.libs.iteratee.Concurrent
import play.api.libs.iteratee.Enumerator
import play.api.libs.json.JsObject
import play.extras.iteratees._
import play.api.libs.iteratee.Enumeratee

class Application extends Controller {

  val credentials: Option[(ConsumerKey, RequestToken)] = for {
    apiKey <- Play.configuration.getString("twitter.apiKey")
    apiSecret <- Play.configuration.getString("twitter.apiSecret")
    token <- Play.configuration.getString("twitter.token")
    tokenSecret <- Play.configuration.getString("twitter.tokenSecret")
  } yield (
    ConsumerKey(apiKey, apiSecret),
    RequestToken(token, tokenSecret))
  
  val (iteratee, enumerator) = Concurrent.joined[Array[Byte]]
  
  val jsonStream: Enumerator[JsObject] = enumerator &> 
      Encoding.decode() &> 
      Enumeratee.grouped(JsonIteratees.jsSimpleObject)  
      
  val loggingIterate = Iteratee.foreach[JsObject] { value =>
    Logger.info(value.toString)
  }

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }
  
  jsonStream.run(loggingIterate)
  
  def tweets = Action.async {
    credentials.map { case (consumerKey, requestToken) =>           
      WS.url("https://stream.twitter.com/1.1/statuses/filter.json")
        .sign(OAuthCalculator(consumerKey, requestToken))
        .withQueryString("track" -> "cat")
        .get { response => 
            Logger.info(s"Status: ${response.status}")
            iteratee
        }.map { response => Ok("Stream closed") }
    } getOrElse {
      Future {
        InternalServerError("Twitter credentials missing")
      }
    }
  }

}
