package tcpserver

import akka.actor.{Actor, ActorRef, Props}
import akka.io.Tcp._
import akka.io.{IO, Tcp}
import akka.util.ByteString
import main.main.topicManager
import managers.TopicManager.{AddConsumer, DeleteConsumer, topicsConsumers}
import org.json4s.jackson.Serialization.write
import org.json4s.jackson.{JsonMethods, Serialization}
import org.json4s.{Formats, NoTypeHints}

import java.net.InetSocketAddress

case class ReceivedMessage(data: ByteString, caseType: String="ReceivedMessage")
case class GetTopicList(request: String="get", caseType: String="GetTopicList")
case class SubscribeMessage(request: String="sub", topics: List[String], caseType: String="SubscribeMessage")
case class UnsubscribeMessage(request: String="uns", caseType: String="UnsubscribeMessage")
case class ReceiveTopics(data: Set[String], caseType: String="ReceiveTopics")
case class ReceiveSubscribedTopics(topics: List[String], caseType: String="ReceiveSubscribedTopics")
case class ReceiveUnsubscribeTopic(data: String, caseType: String="ReceiveUnsubscribeTopic")
case class SendCreate(request: String="create message", producer: ActorRef, caseType: String="SendCreate")
case class Message(topic: String, text: String, caseType: String="Message")
case class SubscribeFromMain(request: String="sub", caseType: String="SubscribeFromMain")
case class UnsubscribeFromMain(request: String="uns", caseType: String="UnsubscribeFromMain")
case class Subscription(request: String = "sub", topics: List[String], caseType: String="Subscription")
case class Unsubscribe(request: String = "un", topics: List[String], caseType: String="Unsubscribe")
case class RequestTopics(request: String = "get", caseType: String="RequestTopics")
case class SendTopics(topics: Set[String], caseType: String="SendTopics")


class TCPConnectionManager(address: String, port: Int) extends Actor {
    import context.system
    IO(Tcp) ! Bind(self, new InetSocketAddress(address, port))

    override def receive: Receive = {
        case Received(data) =>
            sender() ! Write(data)
        case Bound(local) =>
            println(s"Main.Server started on $local")
        case Connected(remote, local) =>
            val handler = context.actorOf(Props[TCPConnectionHandler])
            println(s"New connection: $local -> $remote")
            sender() ! Register(handler)
    }
}

class TCPConnectionHandler extends Actor {

    //import TCPServer.topicManager

    implicit val formats: Formats = Serialization.formats(NoTypeHints)

    override def receive: Actor.Receive = {
        case Received(data) =>
            val json = JsonMethods.parse(data.utf8String)
            (json \ "caseType").extract[String] match {
                case "Message" =>
                    val topicExtracted = (json \ "topic").extract[String]
                    if (topicsConsumers.nonEmpty) {
                        topicsConsumers.foreach{
                            case(topic, consumerList) =>
                                if (topic == topicExtracted){
                                    for (consumerRef <- consumerList) consumerRef ! Write(data)
                                }
                        }
                    }
                case "GetTopicList" =>
                    val caseTopics = Write(ByteString(write(SendTopics(topicsConsumers.keys.toSet))))
                    sender() ! caseTopics

                case "Subscription" =>
                    val topicList = (json \ "topics").extract[List[String]]
                    for (topic <- topicList){
                        topicManager ! AddConsumer(sender(), topic)
                    }

                case "Unsubscribe" =>
                    val topicList = (json \ "topics").extract[List[String]]
                    for (topic <- topicList) {
                        topicManager ! DeleteConsumer(sender(), topic)
                    }
            }

        case _: ConnectionClosed =>
            println("Connection has been closed")
            context stop self
    }
}