package controllers

import actors.TwitterStreamer
import play.api.Play.current
import play.api.libs.json.JsValue
import play.api.mvc.{Action, Controller, WebSocket}

/**
 * The application controller
 * 
 * @author juanitodread
 * 
 */
class Application extends Controller {

  /**
   * The method controller for the default index page.
   * 
   * @return Action
   */
  def index = Action { implicit request =>
    Ok(views.html.index("Tweets"))
  }
  
  /**
   * The method controller for process the websocket requests.
   * 
   * @return 
   */
  def tweets = WebSocket.acceptWithActor[String, JsValue] { request => 
      out => TwitterStreamer.props(out)
  }
}
