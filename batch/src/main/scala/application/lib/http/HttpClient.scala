package application.lib.http

import cats.effect._
import cats.syntax.all._
import org.http4s.client.Client

object HttpClient {
  def make[F[_]: Async]: F[Client[F]] = {
    import org.http4s.client.middleware.{RequestLogger, ResponseLogger}
    import org.http4s.jdkhttpclient.JdkHttpClient

    for {
      httpClient <- JdkHttpClient.simple[F]
      httpClientWithLogger = RequestLogger(logHeaders = true, logBody = true)(ResponseLogger(logHeaders = true, logBody = true)(httpClient))
    } yield httpClientWithLogger
  }

}
