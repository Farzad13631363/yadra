data class UnknownEcuIdentification(
    val protocol: String,
    val protocolName: String,
    val scanType: String,
    val supportedCommands: List<String>,
    val commandResponses: Map<String, String>
)