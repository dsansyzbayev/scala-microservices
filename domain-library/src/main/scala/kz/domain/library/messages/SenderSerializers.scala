package kz.domain.library.messages

import org.json4s.{Formats, ShortTypeHints}
import org.json4s.jackson.Serialization

trait SenderSerializers {
  implicit val formats: AnyRef with Formats =
    Serialization.formats(
      ShortTypeHints(List(classOf[TelegramSender], classOf[HttpSender])))
}
