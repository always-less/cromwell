package cromwell.backend.impl.volc

import com.typesafe.config.Config
import cromwell.backend.io.WorkflowPaths
import cromwell.backend.{BackendJobDescriptorKey, BackendWorkflowDescriptor}
import cromwell.core.path.PathBuilder

case class VolcWorkflowPaths(volcConfiguration: VolcConfiguration,
                             override val workflowDescriptor: BackendWorkflowDescriptor,
                             override val config: Config,
                             override val pathBuilders: List[PathBuilder] = WorkflowPaths.DefaultPathBuilders) extends WorkflowPaths {

  override lazy val executionRootString: String = s"${volcConfiguration.tos.mountPath}/${volcConfiguration.executionRootDir}"

  override def toJobPaths(workflowPaths: WorkflowPaths,
                          jobKey: BackendJobDescriptorKey): VolcJobPaths = {
    VolcJobPaths(workflowPaths.asInstanceOf[VolcWorkflowPaths], jobKey)
  }

  override protected def withDescriptor(workflowDescriptor: BackendWorkflowDescriptor): WorkflowPaths = this.copy(workflowDescriptor = workflowDescriptor)
}
