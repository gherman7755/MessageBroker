package main

import akka.actor.{ActorSystem, Props}
import managers.TopicManager
import org.json4s.{Formats, NoTypeHints}
import org.json4s.jackson.Serialization
import tcpserver.TCPConnectionManager

import scala.concurrent.ExecutionContextExecutor

object main extends App {
    implicit val system: ActorSystem = ActorSystem()
    implicit val formats: Formats = Serialization.formats(NoTypeHints)
    implicit val executor: ExecutionContextExecutor = system.dispatcher

    val tcpServer = system.actorOf(Props(classOf[TCPConnectionManager], "localhost", 8080))
    val topicManager = system.actorOf(Props(classOf[TopicManager]))
}
