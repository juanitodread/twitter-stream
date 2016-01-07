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
import play.api.libs.json.JsValue
import actors.TwitterStreamer

class Application extends Controller {

  def index = Action { implicit request =>
    Ok(views.html.index("Tweets"))
  }
  
  def tweets = WebSocket.acceptWithActor[String, JsValue] { request => 
    out => TwitterStreamer.props(out)
  }
}
