package application.lib

import io.circe.generic.extras.Configuration

object CirceConfig {
  implicit val config: Configuration = Configuration.default.withSnakeCaseMemberNames
}
