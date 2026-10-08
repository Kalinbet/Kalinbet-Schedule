package com.kalinbetschedule.data.backup
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import com.kalinbetschedule.data.ScheduleRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
object BackupManager {
    private const val PREFS = "backup"
    private const val KEY_LAST = "last_backup_at"
    const val UPLOAD_DELAY_MS = 5_000L
    data class Summary(val clients: Int, val services: Int, val slots: Int)
    data class Conflict(val remoteJson: String, val remote: Summary, val local: Summary)
    var signedIn by mutableStateOf(false)
        private set
    var working by mutableStateOf(false)
        private set
    var reconciled by mutableStateOf(false)
        private set
    var conflict by mutableStateOf<Conflict?>(null)
        private set
    var lastError by mutableStateOf<String?>(null)
        private set
    var lastBackupAt by mutableLongStateOf(0L)
        private set
    val configured: Boolean get() = YandexAuth.configured
    private var uploadedRevision = -1
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val pending: Boolean
        get() = signedIn && reconciled && ScheduleRepository.revision != uploadedRevision
    private var started = false
    fun init(context: Context) {
        if (started) return
        started = true
        signedIn = YandexAuth.token(context) != null
        lastBackupAt = prefs(context).getLong(KEY_LAST, 0L)
        if (signedIn) reconcile(context)
    }
    fun signIn(context: Context) {
        lastError = null
        context.startActivity(YandexAuth.authorizeIntent())
    }
    fun onRedirect(context: Context, redirect: Uri) {
        scope.launch {
            working = true
            val ok = withContext(Dispatchers.IO) { YandexAuth.completeSignIn(context, redirect) }
            working = false
            signedIn = ok
            lastError = if (ok) null else "Не удалось войти в Яндекс ID"
            if (ok) reconcile(context)
        }
    }
    fun reconcile(context: Context) {
        if (!signedIn || working) return
        val token = YandexAuth.token(context) ?: return
        scope.launch {
            working = true
            val result = withContext(Dispatchers.IO) { runCatching { YandexDisk.download(token) } }
            working = false
            val failure = result.exceptionOrNull()
            if (failure != null) {
                lastError = describe(failure)
                return@launch
            }
            val remoteJson = result.getOrNull()
            val localJson = ScheduleRepository.exportJson()
            when (decide(remoteJson, localJson, ScheduleRepository.isEmpty)) {
                Decision.UPLOAD -> {
                    reconciled = true
                    upload(context)
                }
                Decision.IN_SYNC -> {
                    lastError = null
                    reconciled = true
                    uploadedRevision = ScheduleRepository.revision
                }
                Decision.TAKE_REMOTE -> applyRemote(remoteJson!!)
                Decision.ASK -> conflict = Conflict(
                    remoteJson = remoteJson!!,
                    remote = summarize(remoteJson),
                    local = summarize(localJson)
                )
            }
        }
    }
    fun resolveConflict(context: Context, fromDisk: Boolean) {
        val pendingConflict = conflict ?: return
        conflict = null
        if (fromDisk) {
            applyRemote(pendingConflict.remoteJson)
        } else {
            reconciled = true
            scope.launch { upload(context) }
        }
    }
    fun restoreFromDisk(context: Context) {
        if (!signedIn || working) return
        val token = YandexAuth.token(context) ?: return
        scope.launch {
            working = true
            val result = withContext(Dispatchers.IO) { runCatching { YandexDisk.download(token) } }
            working = false
            val failure = result.exceptionOrNull()
            when {
                failure != null -> lastError = describe(failure)
                result.getOrNull() == null -> lastError = "На Диске пока нет копии"
                else -> applyRemote(result.getOrNull()!!)
            }
        }
    }
    fun backupNow(context: Context) {
        if (!signedIn || working || !reconciled) return
        scope.launch { upload(context) }
    }
    fun signOut(context: Context) {
        YandexAuth.signOut(context)
        signedIn = false
        reconciled = false
        conflict = null
        lastError = null
        uploadedRevision = -1
    }
    private fun applyRemote(remoteJson: String) {
        if (ScheduleRepository.importJson(remoteJson)) {
            lastError = null
            reconciled = true
            uploadedRevision = ScheduleRepository.revision
        } else {
            lastError = "Копия на Диске не читается"
        }
    }
    private suspend fun upload(context: Context) {
        val token = YandexAuth.token(context) ?: return
        val revision = ScheduleRepository.revision
        val json = ScheduleRepository.exportJson()
        working = true
        val failure = withContext(Dispatchers.IO) {
            runCatching { YandexDisk.upload(token, json) }.exceptionOrNull()
        }
        working = false
        if (failure == null) {
            uploadedRevision = revision
            lastError = null
            lastBackupAt = System.currentTimeMillis() / 1000
            prefs(context).edit { putLong(KEY_LAST, lastBackupAt) }
        } else {
            lastError = describe(failure)
        }
    }
    private fun summarize(json: String): Summary {
        val root = runCatching { JSONObject(json) }.getOrNull() ?: return Summary(0, 0, 0)
        return Summary(
            clients = root.optJSONArray("clients")?.length() ?: 0,
            services = root.optJSONArray("services")?.length() ?: 0,
            slots = root.optJSONArray("slots")?.length() ?: 0
        )
    }
    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private fun describe(failure: Throwable): String = when (failure) {
        is IOException -> failure.message ?: "Нет связи с Диском"
        else -> failure.message ?: "Не удалось связаться с Диском"
    }
}
