package security

import com.google.inject.Inject
import models.User
import play.api.mvc.{ActionBuilder, AnyContent, BodyParser, BodyParsers, Request, Result}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global

class JWTAuthentication @Inject()(p: BodyParsers.Default, jWTService: JWTService)(implicit ec: ExecutionContext) extends ActionBuilder[AuthenticatedRequest, AnyContent] {
  override def invokeBlock[A](request: Request[A], block: AuthenticatedRequest[A] => Future[Result]): Future[Result] = {
    val jwtToken = request.headers.get("dm874_jw_token")

    jwtToken match {
      case Some(token) => jWTService.tryDecode(token).map{ claims =>
          import io.circe._, io.circe.generic.auto._, io.circe.parser._, io.circe.syntax._
          decode[User](claims).toOption.map{ u =>
            block(AuthenticatedRequest(u, request))
          }.getOrElse(Future.successful(play.api.mvc.Results.Unauthorized("User could not be decoded")))
        }.getOrElse(Future.successful(play.api.mvc.Results.Unauthorized("Could not decode JWT token")))

      case None => Future.successful(play.api.mvc.Results.Unauthorized("Invalid credentials!"))
    }
  }

  override def parser: BodyParser[AnyContent] = p
  override def executionContext: ExecutionContext = ec
}
