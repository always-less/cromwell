package cromwell.backend.impl.volc

object VolcRuntimeAttributeKeys {
  val taskName = "task_name"
  val description = "description"
  val tags = "tags"
  val envs = "envs"
  val image = "image"
  val imageUrl = "image_url"
  val imageCredential = "image_credential"
  val sidecarImage = "sidecar_image"
  val resourceQueueId = "resource_queue_id"
  val framework = "framework"
  val taskRoleSpecs = "task_role_specs"
  val activeDeadlineSeconds = "active_deadline_seconds"
  val flavor = "flavor"
  val enableTensorboard = "enable_tensorboard"
  val sidecarMemoryRatio = "sidecar_memory_ratio"
  val storages = "storages"

  val DockerWorkingDirKey = "dockerWorkingDir"
  val DiskSizeKey = "disk"
  val PreemptibleKey = "preemptible"
}
