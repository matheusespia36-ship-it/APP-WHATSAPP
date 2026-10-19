package com.disparador.app.data

import com.disparador.app.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*

/**
 * Repositório REAL: conversa por HTTP com o server.js (Node + Baileys) que roda dentro do app
 * em http://127.0.0.1:3000. Não existe mais nenhum dado de exemplo nem envio simulado.
 *
 * O agendador agora roda no Node (server.js), não aqui. Este repositório só:
 *  - consulta o status/grupos/disparos/histórico a cada poucos segundos;
 *  - manda as ações do usuário (criar, editar, enviar, parear, resetar...).
 */
class DispatchRepository(private val coroutineScope: CoroutineScope) {

    private val _dispatches = MutableStateFlow<List<Dispatch>>(emptyList())
    val dispatches: StateFlow<List<Dispatch>> = _dispatches.asStateFlow()

    private val _history = MutableStateFlow<List<HistoryEntry>>(emptyList())
    val history: StateFlow<List<HistoryEntry>> = _history.asStateFlow()

    private val _groups = MutableStateFlow<List<WhatsAppGroup>>(emptyList())
    val groups: StateFlow<List<WhatsAppGroup>> = _groups.asStateFlow()

    private val _manualGroups = MutableStateFlow<List<WhatsAppGroup>>(emptyList())

    private val _waStatus = MutableStateFlow(WhatsAppStatus.CONNECTING)
    val waStatus: StateFlow<WhatsAppStatus> = _waStatus.asStateFlow()

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    private val _serverUrl = MutableStateFlow("http://127.0.0.1:3000")
    val serverUrl: StateFlow<String> = _serverUrl.asStateFlow()

    private val _apiToken = MutableStateFlow("minhasenha")
    val apiToken: StateFlow<String> = _apiToken.asStateFlow()

    private val _pairingCode = MutableStateFlow("")
    val pairingCode: StateFlow<String> = _pairingCode.asStateFlow()

    private val _backgroundPriorityEnabled = MutableStateFlow(true)
    val backgroundPriorityEnabled: StateFlow<Boolean> = _backgroundPriorityEnabled.asStateFlow()

    @Volatile
    private var serverReachable: Boolean? = null

    init {
        addLog("Aguardando o servidor Node.js interno iniciar...", LogType.NORMAL)
        startSync()
    }

    // ------------------------------------------------------------------ HTTP

    private suspend fun call(
        method: String,
        path: String,
        body: JSONObject? = null,
        timeoutMs: Int = 20_000
    ): JSONObject = withContext(Dispatchers.IO) {
        val conn = URL(_serverUrl.value.trim().trimEnd('/') + path).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = method
            conn.connectTimeout = 4_000
            conn.readTimeout = timeoutMs
            conn.setRequestProperty("x-token", _apiToken.value)
            conn.setRequestProperty("Accept", "application/json")
            if (body != null) {
                val bytes = body.toString().toByteArray(Charsets.UTF_8)
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conn.setFixedLengthStreamingMode(bytes.size)
                conn.outputStream.use { it.write(bytes) }
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            val json = try {
                if (text.isBlank()) JSONObject() else JSONObject(text)
            } catch (e: Exception) {
                JSONObject().put("error", text.take(300))
            }
            if (code !in 200..299) {
                throw Exception(json.optString("error").ifBlank { "HTTP $code" })
            }
            json
        } finally {
            conn.disconnect()
        }
    }

    // ------------------------------------------------------------------ sincronização

    private fun startSync() {
        coroutineScope.launch(Dispatchers.IO) {
            var tick = 0
            while (isActive) {
                try {
                    val st = call("GET", "/status", timeoutMs = 5_000)
                    if (serverReachable != true) {
                        serverReachable = true
                        addLog("Servidor Node.js conectado em ${_serverUrl.value}", LogType.OK)
                    }
                    val status = mapStatus(st.optString("whatsapp"))
                    if (status != _waStatus.value) setWhatsAppStatus(status)
                    if (tick % 4 == 0) {
                        refreshDispatches()
                        refreshHistory()
                    }
                    if (status == WhatsAppStatus.OPEN && (_groups.value.isEmpty() || tick % 20 == 0)) {
                        refreshGroups()
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    if (serverReachable != false) {
                        serverReachable = false
                        addLog("Servidor Node.js indisponível: ${e.message}", LogType.ERROR)
                    }
                    if (_waStatus.value != WhatsAppStatus.OFFLINE) _waStatus.value = WhatsAppStatus.OFFLINE
                }
                tick++
                delay(3_000L)
            }
        }
    }

    private fun mapStatus(raw: String): WhatsAppStatus = when (raw) {
        "open" -> WhatsAppStatus.OPEN
        "needsPairing", "loggedOut" -> WhatsAppStatus.NEEDS_PAIRING
        "starting", "connecting", "reconnecting" -> WhatsAppStatus.CONNECTING
        else -> WhatsAppStatus.OFFLINE
    }

    private suspend fun refreshDispatches() {
        val arr = call("GET", "/dispatches").optJSONArray("dispatches") ?: return
        _dispatches.value = arr.objects().map { parseDispatch(it) }.reversed()
    }

    private suspend fun refreshHistory() {
        val arr = call("GET", "/history").optJSONArray("history") ?: return
        _history.value = arr.objects().map { parseHistory(it) }.take(150)
    }

    private suspend fun refreshGroups() {
        val arr = call("GET", "/groups").optJSONArray("groups") ?: return
        val remote = arr.objects().map {
            WhatsAppGroup(
                id = it.optString("id"),
                name = it.optString("name").ifBlank { it.optString("id") },
                participants = it.optInt("participants", 0)
            )
        }
        val manual = _manualGroups.value.filter { m -> remote.none { it.id == m.id } }
        _groups.value = manual + remote
    }

    // ------------------------------------------------------------------ ações

    /** Envia agora. Chamar de dentro de uma coroutine (o ViewModel já faz isso). */
    suspend fun executeDispatch(slug: String, force: Boolean = false): Result<String> {
        if (_waStatus.value != WhatsAppStatus.OPEN) {
            addLog("Tentativa de disparo falhou: WhatsApp offline", LogType.ERROR)
            return Result.failure(Exception("WhatsApp não está conectado"))
        }
        return runCatching {
            val res = call(
                "POST",
                "/send/" + URLEncoder.encode(slug, "UTF-8"),
                JSONObject().put("force", force),
                timeoutMs = 120_000
            )
            val msg = res.optString("message").ifBlank { "Disparado com sucesso" }
            addLog("[MANUAL] $msg", LogType.OK)
            refreshDispatches()
            refreshHistory()
            msg
        }.onFailure { addLog("Falha ao disparar \"$slug\": ${it.message}", LogType.ERROR) }
    }

    fun saveDispatch(dispatch: Dispatch) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                call("POST", "/dispatches", toJson(dispatch), timeoutMs = 120_000)
                addLog("Disparo \"${dispatch.name}\" salvo (${dispatch.effectiveGroups.size} grupo(s))", LogType.OK)
                refreshDispatches()
            } catch (e: Exception) {
                addLog("Falha ao salvar \"${dispatch.name}\": ${e.message}", LogType.ERROR)
            }
        }
    }

    fun deleteDispatch(slug: String) {
        _dispatches.value = _dispatches.value.filter { it.slug != slug }
        coroutineScope.launch(Dispatchers.IO) {
            try {
                call("DELETE", "/dispatches/" + URLEncoder.encode(slug, "UTF-8"))
                addLog("Disparo \"$slug\" removido", LogType.NORMAL)
            } catch (e: Exception) {
                addLog("Falha ao remover \"$slug\": ${e.message}", LogType.ERROR)
            }
            runCatching { refreshDispatches() }
        }
    }

    fun toggleDispatchActive(slug: String) {
        val current = _dispatches.value.find { it.slug == slug } ?: return
        val newState = !current.active
        _dispatches.value = _dispatches.value.map { if (it.slug == slug) it.copy(active = newState) else it }
        coroutineScope.launch(Dispatchers.IO) {
            try {
                call(
                    "PUT",
                    "/dispatches/" + URLEncoder.encode(slug, "UTF-8"),
                    JSONObject().put("active", newState)
                )
                addLog("Disparo \"${current.name}\" ${if (newState) "ativado" else "pausado"}", LogType.NORMAL)
            } catch (e: Exception) {
                addLog("Falha ao alterar \"${current.name}\": ${e.message}", LogType.ERROR)
            }
            runCatching { refreshDispatches() }
        }
    }

    /** Grupo digitado à mão (JID). Fica só no app; o servidor aceita qualquer JID válido. */
    fun addGroup(name: String, jid: String): WhatsAppGroup {
        val newGroup = WhatsAppGroup(id = jid, name = name, participants = 0)
        _manualGroups.value = listOf(newGroup) + _manualGroups.value.filter { it.id != jid }
        _groups.value = listOf(newGroup) + _groups.value.filter { it.id != jid }
        addLog("Grupo \"$name\" ($jid) adicionado manualmente", LogType.NORMAL)
        return newGroup
    }

    fun clearHistory() {
        _history.value = emptyList()
        coroutineScope.launch(Dispatchers.IO) {
            try {
                call("DELETE", "/history")
                addLog("Histórico de envios limpo", LogType.NORMAL)
            } catch (e: Exception) {
                addLog("Falha ao limpar histórico: ${e.message}", LogType.ERROR)
            }
        }
    }

    suspend fun requestPairingCode(phone: String): Result<String> = runCatching {
        val res = call("POST", "/pair-code", JSONObject().put("phone", phone), timeoutMs = 25_000)
        val code = res.optString("code")
        if (code.isBlank()) throw Exception("O servidor não devolveu o código")
        _pairingCode.value = code
        addLog("Código de pareamento gerado: $code", LogType.OK)
        code
    }.onFailure { addLog("Falha ao gerar código: ${it.message}", LogType.ERROR) }

    suspend fun testConnection(): Result<String> = runCatching {
        val res = call("POST", "/test", timeoutMs = 8_000)
        val msg = res.optString("message").ifBlank { "Servidor online" }
        addLog(
            "Teste de conexão: $msg",
            if (res.optString("whatsapp") == "open") LogType.OK else LogType.NORMAL
        )
        msg
    }.onFailure { addLog("Teste de conexão falhou: ${it.message}", LogType.ERROR) }

    fun resetSession() {
        _waStatus.value = WhatsAppStatus.NEEDS_PAIRING
        _pairingCode.value = ""
        coroutineScope.launch(Dispatchers.IO) {
            try {
                call("POST", "/reset-session")
                addLog("Sessão do WhatsApp reiniciada. Aguardando novo pareamento.", LogType.NORMAL)
            } catch (e: Exception) {
                addLog("Falha ao reiniciar sessão: ${e.message}", LogType.ERROR)
            }
        }
    }

    fun updateApiToken(newToken: String) {
        val token = newToken.trim()
        if (token.isEmpty()) return
        coroutineScope.launch(Dispatchers.IO) {
            try {
                // Troca a senha no servidor (usa a senha atual para autenticar).
                call("POST", "/config", JSONObject().put("token", token))
                addLog("Token atualizado no servidor", LogType.OK)
            } catch (e: Exception) {
                // Se falhou (ex.: 401), usa o token só para falar com o servidor.
                addLog("Token salvo no app (servidor respondeu: ${e.message})", LogType.NORMAL)
            }
            _apiToken.value = token
        }
    }

    fun updateServerUrl(newUrl: String) {
        _serverUrl.value = newUrl.trim()
        addLog("URL base do servidor configurada para ${newUrl.trim()}", LogType.NORMAL)
    }

    fun setBackgroundPriorityEnabled(enabled: Boolean) {
        _backgroundPriorityEnabled.value = enabled
        addLog(
            "Prioridade em 2º plano: ${if (enabled) "Ativada (Foreground Service)" else "Padrão"}",
            LogType.NORMAL
        )
    }

    fun setWhatsAppStatus(status: WhatsAppStatus) {
        _waStatus.value = status
        addLog(
            "Status do WhatsApp: $status",
            if (status == WhatsAppStatus.OPEN) LogType.OK else LogType.NORMAL
        )
    }

    fun addLog(msg: String, type: LogType = LogType.NORMAL) {
        val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val entry = LogEntry(id = UUID.randomUUID().toString(), time = timeStr, message = msg, type = type)
        _logs.value = (listOf(entry) + _logs.value).take(60)
    }

    fun fillCaption(template: String): String {
        val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        return template.replace("{data}", dateStr).replace("{hora}", timeStr)
    }

    // ------------------------------------------------------------------ conversão JSON <-> modelos

    private fun JSONArray.objects(): List<JSONObject> =
        (0 until length()).mapNotNull { optJSONObject(it) }

    private fun JSONObject.strOrNull(key: String): String? =
        if (isNull(key)) null else optString(key)

    private fun formatIso(iso: String, withSeconds: Boolean = false): String {
        if (iso.isBlank()) return ""
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
                .apply { timeZone = TimeZone.getTimeZone("UTC") }
            val date = parser.parse(iso) ?: return iso
            val pattern = if (withSeconds) "dd/MM/yyyy HH:mm:ss" else "dd/MM/yyyy HH:mm"
            SimpleDateFormat(pattern, Locale.getDefault()).format(date)
        } catch (e: Exception) {
            iso
        }
    }

    private fun parseDispatch(o: JSONObject): Dispatch {
        val groups = (o.optJSONArray("targetGroups") ?: JSONArray()).objects().map {
            WhatsAppGroup(id = it.optString("id"), name = it.optString("name").ifBlank { it.optString("id") })
        }
        val file = o.optJSONObject("lastMessage")?.let {
            AttachedFile(
                fileName = it.optString("fileName", "arquivo"),
                fileSize = it.optLong("fileSize", 0L),
                mimetype = it.optString("mimetype", "application/octet-stream"),
                fileHash = it.optString("fileHash"),
                updatedAt = formatIso(it.optString("updatedAt"))
            )
        }
        val sched = when (o.optString("schedType").lowercase()) {
            "fixed" -> SchedType.FIXED
            "interval" -> SchedType.INTERVAL
            else -> SchedType.NONE
        }
        return Dispatch(
            slug = o.optString("slug"),
            name = o.optString("name"),
            groupJid = o.strOrNull("groupJid").orEmpty(),
            groupName = o.strOrNull("groupName").orEmpty(),
            targetGroups = groups,
            caption = o.optString("caption"),
            schedType = sched,
            schedValue = o.optString("schedValue"),
            active = o.optBoolean("active", true),
            preventDuplicates = o.optBoolean("preventDuplicates", true),
            autoReplaceSameName = o.optBoolean("autoReplaceSameName", true),
            duplicateWindowMinutes = o.optInt("duplicateWindowMinutes", 0),
            attachedFile = file,
            lastSentHash = o.strOrNull("lastSentHash"),
            lastSentName = o.strOrNull("lastSentName"),
            lastSentAt = o.strOrNull("lastSentAt")?.let { formatIso(it) },
            lastRunHM = o.strOrNull("lastRunHM"),
            lastRunTs = o.optLong("lastRunTs", 0L)
        )
    }

    private fun parseHistory(o: JSONObject): HistoryEntry {
        val note = o.strOrNull("note")
        return HistoryEntry(
            id = o.optString("id"),
            whenTime = formatIso(o.optString("when"), withSeconds = true),
            name = o.optString("name"),
            slug = o.optString("slug"),
            group = o.optString("group"),
            ok = o.optBoolean("ok", true),
            auto = o.optBoolean("auto", false),
            fileName = o.strOrNull("fileName"),
            note = note,
            error = o.strOrNull("error"),
            isDeduplicated = note?.startsWith("Duplicidade evitada") == true,
            groupsCount = o.optInt("groupsCount", 1)
        )
    }

    private fun toJson(d: Dispatch): JSONObject {
        val o = JSONObject()
        val groupsArr = JSONArray()
        d.effectiveGroups.forEach { groupsArr.put(JSONObject().put("id", it.id).put("name", it.name)) }
        o.put("slug", d.slug)
        o.put("name", d.name)
        o.put("groupJid", d.groupJid)
        o.put("groupName", d.groupName)
        o.put("targetGroups", groupsArr)
        o.put("caption", d.caption)
        o.put("schedType", d.schedType.name.lowercase())
        o.put("schedValue", d.schedValue)
        o.put("active", d.active)
        o.put("preventDuplicates", d.preventDuplicates)
        o.put("autoReplaceSameName", d.autoReplaceSameName)
        o.put("duplicateWindowMinutes", d.duplicateWindowMinutes)
        val f = d.attachedFile
        if (f == null) {
            o.put("removeFile", true)
        } else if (f.contentBase64 != null) {
            o.put("fileBase64", f.contentBase64)
            o.put("fileName", f.fileName)
            o.put("mimetype", f.mimetype)
        } // arquivo já está no servidor e não mudou: não reenvia
        return o
    }

    companion object {
        fun computeSha256(content: String): String {
            val bytes = content.toByteArray()
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(bytes)
            return digest.fold("") { str, it -> str + "%02x".format(it) }
        }

        fun formatBytes(bytes: Long): String {
            if (bytes <= 0) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB")
            val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
            val value = bytes / Math.pow(1024.0, digitGroups.toDouble())
            return String.format(Locale.getDefault(), "%.1f %s", value, units[digitGroups])
        }
    }
}
