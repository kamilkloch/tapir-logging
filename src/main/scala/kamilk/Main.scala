package kamilk

import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._
import org.http4s.HttpRoutes
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.server.Router
import sttp.tapir.server.http4s.{Http4sServerInterpreter, Http4sServerOptions}
import org.http4s.server.middleware.{Logger, RequestLogger}

object Main extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = {

    val serverOptions = Http4sServerOptions
      .customiseInterceptors[IO]
      .serverLog {
        Http4sServerOptions
          .defaultServerLog[IO]
          .copy(logWhenReceived = true)
          .doLogWhenReceived(msg => IO.println(msg))
      }
      .options

    val routes1 = Http4sServerInterpreter[IO](serverOptions).toRoutes(Endpoints.apiEndpoints1)

    val routes2 = Http4sServerInterpreter[IO](serverOptions).toRoutes(Endpoints.apiEndpoints2)

    val port = sys.env.get("http.port").map(_.toInt).getOrElse(8080)

    def loggingMiddleware(service: HttpRoutes[IO]): HttpRoutes[IO] = RequestLogger.httpRoutes(logHeaders = false, logBody = false)(service)

    BlazeServerBuilder[IO]
      .bindHttp(port, "localhost")
      .withHttpApp(Router[IO](
        "/" -> loggingMiddleware(routes1),
        "/" -> loggingMiddleware(routes2)
      ).orNotFound)
      .resource
      .useForever
      .as(ExitCode.Success)
  }
}
