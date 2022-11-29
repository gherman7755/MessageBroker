package managers

import managers.TopicManager.{AddConsumer, DeleteConsumer, topicsConsumers}
import akka.actor.{Actor, ActorRef}
import org.json4s.{Formats, NoTypeHints}
import org.json4s.jackson.Serialization
import org.json4s.native.JsonMethods

import scala.collection.convert.ImplicitConversions.`collection asJava`
import scala.collection.mutable

class TopicManager extends Actor {
    implicit val formats: Formats = Serialization.formats(NoTypeHints)

    override def receive: Receive = {
        case AddConsumer(reference, topic) =>
            if (topicsConsumers.keys.contains(topic))
                topicsConsumers += (topic -> topicsConsumers(topic).+(reference))
            else
                topicsConsumers += (topic -> Set(reference))

        case DeleteConsumer(reference, topic) =>
            if (topicsConsumers.keys.contains(topic)) {
                if (topicsConsumers(topic).contains(reference))
                    topicsConsumers += (topic -> topicsConsumers(topic).-(reference))
            } else
                topicsConsumers += (topic -> Set())
    }
}

object TopicManager {
    var topicsConsumers: mutable.Map[String, Set[ActorRef]] = mutable.HashMap()
    case class AddConsumer(reference: ActorRef, topic: String)
    case class DeleteConsumer(reference: ActorRef, topic: String)
}