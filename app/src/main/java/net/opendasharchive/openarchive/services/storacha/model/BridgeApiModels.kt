package net.opendasharchive.openarchive.services.storacha.model

// Bridge API task request model
data class BridgeTaskRequest(
    val tasks: List<List<Any>>,
)

// Store/Add task models
data class StoreAddTask(
    val link: Map<String, String>,
    val size: Long,
)

data class StoreAddResponse(
    val p: StoreAddResult,
)

data class StoreAddResult(
    val out: StoreAddOutcome,
)

data class StoreAddOutcome(
    val ok: StoreAddSuccess? = null,
    val error: Map<String, Any>? = null,
)

data class StoreAddSuccess(
    val status: String,
    val url: String? = null,
    val headers: Map<String, String>? = null,
)

// Upload/Add task models
data class UploadAddTask(
    val root: Map<String, String>,
)

data class UploadAddResponse(
    val p: UploadAddResult,
)

data class UploadAddResult(
    val out: UploadAddOutcome,
)

data class UploadAddOutcome(
    val ok: UploadAddSuccess? = null,
    val error: Map<String, Any>? = null,
)

data class UploadAddSuccess(
    val root: Map<String, String>,
    val shards: List<Any> = emptyList(),
)
