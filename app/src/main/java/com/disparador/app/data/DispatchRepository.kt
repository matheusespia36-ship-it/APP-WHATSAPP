package com.disparador.app.data

import com.disparador.app.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Repository local do aplicativo.
 *
 * Importante:
 * - Não cria grupos, disparos ou histórico fictícios.
 * - Não presume que o WhatsApp está conectado.
 * - Não gera códigos de pareamento localmente.
 * - Não declara um envio como entregue sem integração/confirmacão do backend.
 *
 * O backend Baileys deve atualizar o status real usando setWhatsAppStatus()
 * e executar os envios reais. Até essa integração existir, os envios falham
 * explicitamente em vez de aparecerem como bem-sucedidos.
 */
class DispatchRepository(private val coroutineScope: CoroutineScope) {

    private val _dispatches = MutableStateFlow<List<Dispatch>>(emptyList())
    val dispatches: StateFlow<List<Dispatch>> = _dispatches.asStateFlow()

    private val _history = MutableStateFlow<List<HistoryEntry>>(emptyList())
    val history: StateFlow<List<HistoryEntry>> = _history.asStateFlow()

    private val _groups = MutableStateFlow<List<WhatsAppGroup>>(emptyList())
    val groups: StateFlow<List<WhatsAppGroup>> = _groups.asStateFlow()

    // Uma instalação nova começa desconectada.
    private val _waStatus =
        MutableStateFlow(WhatsAppStatus.NEEDS_PAIRING)
    val waStatus: StateFlow<WhatsAppStatus> = _waStatus.asStateFlow()

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    private val _serverUrl = MutableStateFlow("http://127.0.0.1:3000")
    val serverUrl: StateFlow<String> = _serverUrl.asStateFlow()

    private val _apiToken = MutableStateFlow("minhasenha")
    val apiToken: StateFlow<String> = _apiToken.asStateFlow()

    private val _pairingCode = MutableStateFlow("")
    val pairingCode: StateFlow<String> = _pairingCode.asStateFlow()

    // O serviço em segundo plano só deve ser ativado por escolha do usuário.
    private val _backgroundPriorityEnabled = MutableStateFlow(false)
    val backgroundPriorityEnabled: StateFlow<Boolean> =
        _backgroundPriorityEnabled.asStateFlow()

    init {
        // Não carregar dados de demonstração numa instalação nova.
        startScheduler()
        addLog("Aplicativo iniciado. Aguardando pareamento real do WhatsApp.", LogType.NORMAL)
    }

    private fun startScheduler() {
        coroutineScope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(30_000L)

                // O agendador só avalia disparos quando o status veio da
                // integração real com o backend.
                if (_waStatus.value == WhatsAppStatus.OPEN) {
                    evaluateScheduledDispatches()
                }
            }
        }
    }

    private fun evaluateScheduledDispatches() {
        val now = Calendar.getInstance()
        val currentHM = String.format(
            Locale.getDefault(),
            "%02d:%02d",
            now.get(Calendar.HOUR_OF_DAY),
            now.get(Calendar.MINUTE)
        )
        val dayKey = "${now.get(Calendar.YEAR)}-${now.get(Calendar.DAY_OF_YEAR)}"
        val dayHMKey = "$dayKey-$currentHM"
        val timestamp = System.currentTimeMillis()

        val updatedList = _dispatches.value.map { dispatch ->
            if (!dispatch.active) return@map dispatch

            var shouldRun = false

            when {
                dispatch.schedType == SchedType.FIXED &&
                    dispatch.schedValue.isNotBlank() -> {
                    val times = dispatch.schedValue
                        .split(",")
                        .map { it.trim() }

                    if (times.contains(currentHM) && dispatch.lastRunHM != dayHMKey) {
                        shouldRun = true
                    }
                }

                dispatch.schedType == SchedType.INTERVAL &&
                    dispatch.schedValue.isNotBlank() -> {
                    val intervalMinutes = dispatch.schedValue.toLongOrNull()
                    if (intervalMinutes != null && intervalMinutes > 0) {
                        val intervalMs = intervalMinutes * 60_000L
                        if (
                            dispatch.lastRunTs == 0L ||
                            timestamp - dispatch.lastRunTs >= intervalMs
                        ) {
                            shouldRun = true
                        }
                    }
                }
            }

            if (!shouldRun) return@map dispatch

            // Nunca atualizar o último envio se não houver arquivo configurado.
            if (dispatch.attachedFile == null) {
                addLog(
                    "Agendamento de \"${dispatch.name}\" ignorado: nenhum arquivo anexado.",
                    LogType.ERROR
                )
                return@map dispatch
            }

            val result = executeDispatchInternal(dispatch, isAuto = true)

            // Só marcar a execução se o método realmente retornar sucesso.
            // Atualmente o envio real precisa ser conectado ao backend Baileys.
            if (result.isSuccess) {
                dispatch.copy(
                    lastRunHM = if (dispatch.schedType == SchedType.FIXED) dayHMKey
                    else dispatch.lastRunHM,
                    lastRunTs = if (dispatch.schedType == SchedType.INTERVAL) timestamp
                    else dispatch.lastRunTs,
                    lastSentAt = nowString("dd/MM/yyyy HH:mm"),
                    lastSentHash = dispatch.attachedFile.fileHash,
                    lastSentName = dispatch.attachedFile.fileName
                )
            } else {
                dispatch
            }
        }

        _dispatches.value = updatedList
    }

    fun executeDispatch(
        slug: String,
        force: Boolean = false
    ): Result<String> {
        val dispatch = _dispatches.value.find { it.slug == slug }
            ?: return Result.failure(Exception("Disparo não encontrado"))

        if (_waStatus.value != WhatsAppStatus.OPEN) {
            addLog("Disparo não executado: WhatsApp não está conectado.", LogType.ERROR)
            return Result.failure(Exception("WhatsApp não está conectado"))
        }

        return executeDispatchInternal(dispatch, isAuto = false, force = force)
    }

    /**
     * Esta versão não simula envios.
     *
     * Substitua o corpo pela chamada real ao backend Baileys, aguardando a
     * resposta da API e só então registrando sucesso/histórico.
     */
    private fun executeDispatchInternal(
        dispatch: Dispatch,
        isAuto: Boolean,
        force: Boolean = false
    ): Result<String> {
        if (_waStatus.value != WhatsAppStatus.OPEN) {
            addLog("Envio cancelado: WhatsApp não está conectado.", LogType.ERROR)
            return Result.failure(Exception("WhatsApp não está conectado"))
        }

        if (dispatch.effectiveGroups.isEmpty()) {
            addLog(
                "Disparo \"${dispatch.name}\" cancelado: nenhum grupo de destino definido.",
                LogType.ERROR
            )
            return Result.failure(Exception("Nenhum grupo de destino definido"))
        }

        // Evita afirmar que foi enviado quando não há chamada ao backend.
        val message =
            "Envio não realizado: integração de envio real com o backend Baileys ainda não foi chamada."
        addLog(message, LogType.ERROR)
        return Result.failure(IllegalStateException(message))
    }

    fun saveDispatch(dispatch: Dispatch) {
        val current = _dispatches.value.toMutableList()
        val index = current.indexOfFirst { it.slug == dispatch.slug }

        if (index >= 0) {
            current[index] = dispatch
            addLog(
                "Disparo \"${dispatch.name}\" atualizado (${dispatch.effectiveGroups.size} grupo(s)).",
                LogType.NORMAL
            )
        } else {
            current.add(0, dispatch)
            addLog("Novo disparo \"${dispatch.name}\" criado.", LogType.NORMAL)
        }

        _dispatches.value = current
    }

    fun deleteDispatch(slug: String) {
        val dispatch = _dispatches.value.find { it.slug == slug }
        _dispatches.value = _dispatches.value.filterNot { it.slug == slug }
        addLog("Disparo \"${dispatch?.name ?: slug}\" removido.", LogType.NORMAL)
    }

    fun toggleDispatchActive(slug: String) {
        _dispatches.value = _dispatches.value.map { dispatch ->
            if (dispatch.slug == slug) {
                val newState = !dispatch.active
                addLog(
                    "Disparo \"${dispatch.name}\" ${if (newState) "ativado" else "pausado"}.",
                    LogType.NORMAL
                )
                dispatch.copy(active = newState)
            } else {
                dispatch
            }
        }
    }

    fun addGroup(name: String, jid: String): WhatsAppGroup {
        val newGroup = WhatsAppGroup(
            id = jid,
            name = name,
            participants = 1
        )
        _groups.value = listOf(newGroup) + _groups.value.filterNot { it.id == jid }
        addLog("Grupo \"$name\" ($jid) adicionado.", LogType.NORMAL)
        return newGroup
    }

    /**
     * Substitui os grupos locais pelos grupos reais recebidos do backend.
     * Chame este método depois de consultar a rota real /groups.
     */
    fun setGroupsFromServer(groupsFromServer: List<WhatsAppGroup>) {
        _groups.value = groupsFromServer
    }

    fun clearHistory() {
        _history.value = emptyList()
        addLog("Histórico local limpo.", LogType.NORMAL)
    }

    private fun addHistoryEntry(entry: HistoryEntry) {
        _history.value = (listOf(entry) + _history.value).take(150)
    }

    fun addLog(msg: String, type: LogType = LogType.NORMAL) {
        val timeStr = nowString("HH:mm:ss")
        val entry = LogEntry(
            id = UUID.randomUUID().toString(),
            time = timeStr,
            message = msg,
            type = type
        )
        _logs.value = (listOf(entry) + _logs.value).take(60)
    }

    /**
     * Este método deve ser chamado com o status confirmado pelo backend,
     * não por um botão de teste que apenas presume conexão.
     */
    fun setWhatsAppStatus(status: WhatsAppStatus) {
        _waStatus.value = status
        if (status != WhatsAppStatus.OPEN) {
            _pairingCode.value = ""
        }
        addLog(
            "Status do WhatsApp atualizado: $status",
            if (status == WhatsAppStatus.OPEN) LogType.OK else LogType.NORMAL
        )
    }

    /**
     * Não cria códigos aleatórios. A solicitação real deve ser feita à rota
     * /pair-code do backend e o código retornado deve ser apresentado pela UI.
     */
    fun requestPairingCode(phone: String): String {
        val cleanPhone = phone.replace(Regex("\\D"), "")

        if (cleanPhone.length < 10) {
            addLog("Número de telefone inválido para pareamento.", LogType.ERROR)
            return ""
        }

        _pairingCode.value = ""
        _waStatus.value = WhatsAppStatus.NEEDS_PAIRING
        addLog(
            "Nenhum código fictício foi gerado. Solicite o código ao backend Baileys.",
            LogType.NORMAL
        )
        return ""
    }

    fun setPairingCodeFromServer(code: String) {
        _pairingCode.value = code.trim()
        if (code.isBlank()) {
            addLog("O servidor não retornou código de pareamento.", LogType.ERROR)
        } else {
            addLog("Código de pareamento recebido do servidor.", LogType.NORMAL)
        }
    }

    fun resetSession() {
        _waStatus.value = WhatsAppStatus.NEEDS_PAIRING
        _pairingCode.value = ""
        addLog(
            "Estado local redefinido. A sessão real também precisa ser reiniciada no backend.",
            LogType.NORMAL
        )
    }

    fun updateApiToken(newToken: String) {
        _apiToken.value = newToken.trim()
        addLog("Token de autenticação da API atualizado.", LogType.NORMAL)
    }

    fun updateServerUrl(newUrl: String) {
        _serverUrl.value = newUrl.trim().trimEnd('/')
        addLog("URL base do servidor atualizada.", LogType.NORMAL)
    }

    fun setBackgroundPriorityEnabled(enabled: Boolean) {
        _backgroundPriorityEnabled.value = enabled
        addLog(
            "Execução em segundo plano: ${if (enabled) "ativada" else "desativada"}.",
            LogType.NORMAL
        )
    }

    fun fillCaption(template: String): String {
        val dateStr = nowString("dd/MM/yyyy")
        val timeStr = nowString("HH:mm")
        return template
            .replace("{data}", dateStr)
            .replace("{hora}", timeStr)
    }

    private fun nowString(pattern: String): String =
        SimpleDateFormat(pattern, Locale.getDefault()).format(Date())

    companion object {
        fun computeSha256(content: String): String {
            val bytes = content.toByteArray()
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(bytes)
            return digest.joinToString("") { byte ->
                "%02x".format(byte)
            }
        }

        fun formatBytes(bytes: Long): String {
            if (bytes <= 0) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB")
            val digitGroups =
                (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
                    .coerceIn(0, units.lastIndex)
            val value = bytes / Math.pow(1024.0, digitGroups.toDouble())
            return String.format(
                Locale.getDefault(),
                "%.1f %s",
                value,
                units[digitGroups]
            )
        }
    }
}
