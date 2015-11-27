package io.vamp.model.notification

import io.vamp.common.notification.Notification
import io.vamp.model.artifact.{ AbstractCluster, Breed }

case class UnresolvedEndpointPortError(name: String, value: Any) extends Notification

case class UnresolvedEnvironmentVariableError(name: String, value: Any) extends Notification

case class NonUniqueBlueprintBreedReferenceError(name: String) extends Notification

case class UnresolvedBreedDependencyError(breed: Breed, dependency: (String, Breed)) extends Notification

case class RoutingWeightError(cluster: AbstractCluster) extends Notification

case class UnresolvedScaleEscalationTargetCluster(cluster: AbstractCluster, target: String) extends Notification

case class MissingEnvironmentVariableError(breed: Breed, name: String) extends Notification

object NoServiceError extends Notification

case class NotificationMessageNotRestored(message: String) extends Notification

case class UndefinedStateIntentionError(name: String) extends Notification

case class UndefinedStateStepError(name: String) extends Notification

case class IllegalRoutingStickyValue(sticky: String) extends Notification
