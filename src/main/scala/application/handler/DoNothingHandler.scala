package application.handler

import application.ApplicationContext
import cats.data.ReaderT
import cats.effect.IO

object DoNothingHandler {
  def handle: ApplicationContext.Handler[Unit] = ReaderT { _ =>
    IO.unit
  }
}
