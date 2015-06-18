package cromwell.engine.db

import java.util.Date

import cromwell.engine.{SymbolStoreEntry, WorkflowId, WorkflowState}

case class QueryWorkflowExecutionResult(workflowId: WorkflowId, wdlUri: String, state: WorkflowState,
                                        startTime: Date, endTime: Option[Date],
                                        calls: Seq[CallInfo], symbols: Seq[SymbolStoreEntry])
