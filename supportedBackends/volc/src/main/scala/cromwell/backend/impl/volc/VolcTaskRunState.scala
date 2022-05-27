package cromwell.backend.impl.volc

import java.time.Instant

sealed trait VolcTaskRunState {
  def status: String

  def terminal: Boolean

  override def toString: String = status
}

case class VolcTaskJobRunning(validUntil: Option[Instant]) extends VolcTaskRunState {
  override def terminal: Boolean = false

  override def status = "Running"

  // Whether this running state is stale (ie has the 'validUntil' time passed?)
  def stale: Boolean = validUntil.exists(t => t.isBefore(Instant.now))
}

case class VolcTaskJobWaitingForReturnCode(waitUntil: Option[Instant]) extends VolcTaskRunState {
  override def terminal: Boolean = false

  override def status = "WaitingForReturnCode"

  // Whether or not to give up waiting for the RC to appear (ie has the 'waitUntil' time passed?)
  def giveUpWaiting: Boolean = waitUntil.exists(_.isBefore(Instant.now))
}

case object VolcTaskJobDone extends VolcTaskRunState {
  override def terminal: Boolean = true

  override def status = "Done"
}

case object VolcTaskJobFailed extends VolcTaskRunState {
  override def terminal: Boolean = true

  override def status = "Failed"
}

case object VolcTaskJobInitialized extends VolcTaskRunState {
  override def terminal: Boolean = false

  override def status = "Initialized"
}

case object VolcTaskJobQueue extends VolcTaskRunState {
  override def terminal: Boolean = false

  override def status = "Queue"
}

case object VolcTaskJobStaging extends VolcTaskRunState {
  override def terminal: Boolean = false

  override def status = "Staging"
}

case object VolcTaskJobKilling extends VolcTaskRunState {
  override def terminal: Boolean = false

  override def status = "Killing"
}

case object VolcTaskJobSuccess extends VolcTaskRunState {
  override def terminal: Boolean = true

  override def status = "Success"
}

case object VolcTaskJobCancelled extends VolcTaskRunState {
  override def terminal: Boolean = true

  override def status = "Cancelled"
}

case object VolcTaskJobAbnormal extends VolcTaskRunState {
  override def terminal: Boolean = true

  override def status = "Abnormal"
}

case object VolcTaskJobSuccessHold extends VolcTaskRunState {
  override def terminal: Boolean = true

  override def status = "SuccessHold"
}

case object VolcTaskJobFailedHold extends VolcTaskRunState {
  override def terminal: Boolean = true

  override def status = "FailedHold"
}







