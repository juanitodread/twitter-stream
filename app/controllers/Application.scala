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

class Application extends Controller {

  val credentials: Option[(ConsumerKey, RequestToken)] = for {
    apiKey <- Play.configuration.getString("twitter.apiKey")
    apiSecret <- Play.configuration.getString("twitter.apiSecret")
    token <- Play.configuration.getString("twitter.token")
    tokenSecret <- Play.configuration.getString("twitter.tokenSecret")
  } yield (
    ConsumerKey(apiKey, apiSecret),
    RequestToken(token, tokenSecret)
  )

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  val loggingIterate = Iteratee.foreach[Array[Byte]] { array =>
    Logger.info(array.map(_.toChar).mkString)
  }
  
  def tweets = Action.async {
    credentials.map { case (consumerKey, requestToken) =>           
      WS.url("https://stream.twitter.com/1.1/statuses/filter.json")
        .sign(OAuthCalculator(consumerKey, requestToken))
        .withQueryString("track" -> "juanitodreadhashtag")
        .get { response => 
            Logger.info(s"Status: ${response.status}")
            loggingIterate
        }.map { response => Ok("Stream closed") }
    } getOrElse {
      Future {
        InternalServerError("Twitter credentials missing")
      }
    }
  }

}
