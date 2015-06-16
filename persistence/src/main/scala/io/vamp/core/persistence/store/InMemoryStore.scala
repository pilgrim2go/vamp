package io.vamp.core.persistence.store

import com.typesafe.scalalogging.Logger
import io.vamp.common.http.OffsetEnvelope
import io.vamp.common.notification.NotificationProvider
import io.vamp.core.model.artifact._
import io.vamp.core.model.serialization._
import io.vamp.core.model.workflow.{ScheduledWorkflow, Workflow}
import io.vamp.core.persistence.notification.{ArtifactAlreadyExists, ArtifactNotFound, PersistenceNotificationProvider, UnsupportedPersistenceRequest}
import org.json4s.native.Serialization.write
import org.slf4j.LoggerFactory

import scala.collection.mutable
import scala.concurrent.ExecutionContext
import scala.language.postfixOps

case class InMemoryStoreInfo(`type`: String, artifacts: Map[String, Map[String, Any]])

class InMemoryStore(executionContext: ExecutionContext) extends Store with TypeOfArtifact with PersistenceNotificationProvider {

  private val logger = Logger(LoggerFactory.getLogger(classOf[InMemoryStore]))
  implicit val formats = CoreSerializationFormat.default

  val store: mutable.Map[String, mutable.Map[String, Artifact]] = new mutable.HashMap()

  def info = InMemoryStoreInfo("in-memory", store.map {
    case (key, value) => key -> Map[String, Any]("count" -> value.values.size)
  } toMap)

  def all(`type`: Class[_ <: Artifact]): List[Artifact] = {
    logger.trace(s"InMemory persistence: all [${`type`.getSimpleName}]")
    store.get(typeOf(`type`)) match {
      case None => Nil
      case Some(map) => map.values.toList
    }
  }

  def all(`type`: Class[_ <: Artifact], page: Int, perPage: Int): ArtifactResponseEnvelope = {
    val artifacts = all(`type`)
    val total = artifacts.size
    val (p, pp) = OffsetEnvelope.normalize(page, perPage, ArtifactResponseEnvelope.maxPerPage)
    val (rp, rpp) = OffsetEnvelope.normalize(total, p, pp, ArtifactResponseEnvelope.maxPerPage)

    ArtifactResponseEnvelope(artifacts.slice((p - 1) * pp, p * pp), total, rp, rpp)
  }

  def create(artifact: Artifact, ignoreIfExists: Boolean = true): Artifact = {
    logger.trace(s"InMemory persistence: create [${artifact.getClass.getSimpleName}] - ${write(artifact)}")
    artifact match {
      case blueprint: DefaultBlueprint => blueprint.clusters.flatMap(_.services).map(_.breed).foreach(breed => create(breed, ignoreIfExists = true))
      case _ =>
    }

    val branch = typeOf(artifact.getClass)
    store.get(branch) match {
      case None =>
        val map = new mutable.HashMap[String, Artifact]()
        map.put(artifact.name, artifact)
        store.put(branch, map)
      case Some(map) => map.get(artifact.name) match {
        case None => map.put(artifact.name, artifact)
        case Some(_) =>
          if (!ignoreIfExists) error(ArtifactAlreadyExists(artifact.name, artifact.getClass))
          map.put(artifact.name, artifact)
      }
    }
    artifact
  }

  def read(name: String, `type`: Class[_ <: Artifact]): Option[Artifact] = {
    logger.trace(s"InMemory persistence: read [${`type`.getSimpleName}] - $name}")
    store.get(typeOf(`type`)) match {
      case None => None
      case Some(map) => map.get(name)
    }
  }

  def update(artifact: Artifact, create: Boolean = false): Artifact = {
    logger.trace(s"InMemory persistence: update [${artifact.getClass.getSimpleName}] - ${write(artifact)}")
    store.get(typeOf(artifact.getClass)) match {
      case None => if (create) this.create(artifact) else error(ArtifactNotFound(artifact.name, artifact.getClass))
      case Some(map) =>
        if (map.get(artifact.name).isEmpty)
          if (create) this.create(artifact) else error(ArtifactNotFound(artifact.name, artifact.getClass))
        else
          map.put(artifact.name, artifact)
    }
    artifact
  }

  def delete(name: String, `type`: Class[_ <: Artifact]): Artifact = {
    logger.trace(s"InMemory persistence: delete [${`type`.getSimpleName}] - $name}")
    store.get(typeOf(`type`)) match {
      case None => error(ArtifactNotFound(name, `type`))
      case Some(map) =>
        if (map.get(name).isEmpty)
          error(ArtifactNotFound(name, `type`))
        else
          map.remove(name).get
    }
  }
}

trait TypeOfArtifact {
  this: NotificationProvider =>

  def typeOf(`type`: Class[_]): String = `type` match {
    case t if classOf[Deployment].isAssignableFrom(t) => "deployments"
    case t if classOf[Breed].isAssignableFrom(t) => "breeds"
    case t if classOf[Blueprint].isAssignableFrom(t) => "blueprints"
    case t if classOf[Sla].isAssignableFrom(t) => "slas"
    case t if classOf[Scale].isAssignableFrom(t) => "scales"
    case t if classOf[Escalation].isAssignableFrom(t) => "escalations"
    case t if classOf[Routing].isAssignableFrom(t) => "routings"
    case t if classOf[Filter].isAssignableFrom(t) => "filters"
    case t if classOf[Workflow].isAssignableFrom(t) => "workflows"
    case t if classOf[ScheduledWorkflow].isAssignableFrom(t) => "scheduled-workflows"
    case _ => error(UnsupportedPersistenceRequest(`type`))
  }
}

