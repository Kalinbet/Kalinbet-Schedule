package com.kalinbetschedule.data.backup
enum class Decision {
    UPLOAD,
    IN_SYNC,
    TAKE_REMOTE,
    ASK
}
fun decide(remoteJson: String?, localJson: String, localEmpty: Boolean): Decision = when {
    remoteJson == null -> Decision.UPLOAD
    remoteJson == localJson -> Decision.IN_SYNC
    localEmpty -> Decision.TAKE_REMOTE
    else -> Decision.ASK
}
