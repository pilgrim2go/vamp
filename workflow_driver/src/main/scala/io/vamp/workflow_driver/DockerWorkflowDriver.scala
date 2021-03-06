package io.vamp.workflow_driver

import akka.actor.{ ActorRef, ActorSystem }
import io.vamp.common.akka.IoC
import io.vamp.container_driver.docker.DockerDriverActor

import scala.concurrent.Future

class DockerWorkflowDriver(implicit override val actorSystem: ActorSystem) extends DaemonWorkflowDriver {

  override def info: Future[Map[_, _]] = Future.successful(Map("docker" -> None))

  override protected def driverActor: ActorRef = IoC.actorFor[DockerDriverActor]
}
