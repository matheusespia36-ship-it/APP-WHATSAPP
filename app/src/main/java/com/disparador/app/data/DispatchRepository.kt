package com.disparador.app.data

import com.disparador.app.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*

class DispatchRepository(private val coroutineScope: CoroutineScope) {

    private val _dispatches = MutableStateFlow<List<Dispatch>>(emptyList())
    val dispatches: StateFlow<List<Dispatch>> = _dispatches.asStateFlow()

    private val _history = MutableStateFlow<List<HistoryEntry>>(emptyList())
    val history: StateFlow<List<HistoryEntry>> = _history.asStateFlow()

    private val _groups = MutableStateFlow<List<WhatsAppGroup>>(emptyList())
    val groups: StateFlow<List<WhatsAppGroup>> = _groups.asStateFlow()

    private val _waStatus = MutableStateFlow(WhatsAppStatus.OPEN)
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

    init {
        initDefaultData()
        startScheduler()
    }

    private fun initDefaultData() {
        val g1 = WhatsAppGroup(id = "120363024891100@g.us", name = "Equipe Manutenção", participants = 24)
        val g2 = WhatsAppGroup(id = "120363045982200@g.us", name = "Diretoria & Gerência", participants = 8)
        val g3 = WhatsAppGroup(id = "120363078123300@g.us", name = "Operações Logísticas", participants = 42)
        val g4 = WhatsAppGroup(id = "120363099234400@g.us", name = "Plantão TI & Suporte", participants = 16)
        val g5 = WhatsAppGroup(id = "120363110345500@g.us", name = "Vendas & Faturamento", participants = 31)

        val groupsList = listOf(g1, g2, g3, g4, g5)
        _groups.value = groupsList

        val sampleExcelHash = computeSha256("EXCEL_SAMPLE_REPORT_V1_OCTOBER")
        val samplePdfHash = computeSha256("PDF_FINANCIAL_SUMMARY_V1")

        val initialDispatches = listOf(
            Dispatch(
                slug = "relatorio-manutencao",
                name = "Relatório Manutenção",
                groupJid = g1.id,
                groupName = g1.name,
                targetGroups = listOf(g1),
                caption = "Relatório de Manutenção Preventiva · {data} às {hora}\nEquipamentos vistoriados e prontos.",
                schedType = SchedType.FIXED,
                schedValue = "08:00, 18:00",
                active = true,
                preventDuplicates = true,
                autoReplaceSameName = true,
                duplicateWindowMinutes = 60,
                attachedFile = AttachedFile(
                    fileName = "relatorio_manutencao.xlsx",
                    fileSize = 148520L,
                    mimetype = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    fileHash = sampleExcelHash,
                    updatedAt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date()),
                    sampleContent = "Relatório de Manutenção com 48 itens"
                ),
                lastRunHM = "08:00",
                lastSentHash = sampleExcelHash,
                lastSentName = "relatorio_manutencao.xlsx",
                lastSentAt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
            ),
            Dispatch(
                slug = "fechamento-vendas",
                name = "Fechamento Diário de Vendas",
                groupJid = g2.id,
                groupName = g2.name,
                targetGroups = listOf(g2, g5), // Configured with MULTIPLE GROUPS!
                caption = "Fechamento Consolidado · {data} {hora}\nMetas superadas nas filiais!",
                schedType = SchedType.FIXED,
                schedValue = "22:00",
                active = true,
                preventDuplicates = true,
                autoReplaceSameName = true,
                duplicateWindowMinutes = 1440,
                attachedFile = AttachedFile(
                    fileName = "fechamento_vendas.pdf",
                    fileSize = 412890L,
                    mimetype = "application/pdf",
                    fileHash = samplePdfHash,
                    updatedAt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date()),
                    sampleContent = "Fechamento com dados de vendas por filial"
                ),
                lastRunHM = "22:00",
                lastSentHash = null,
                lastSentName = null,
                lastSentAt = null
            ),
            Dispatch(
                slug = "status-operacoes",
                name = "Status Operações Logísticas",
                groupJid = g3.id,
                groupName = g3.name,
                targetGroups = listOf(g3),
                caption = "Checklist Logística · {data} {hora}",
                schedType = SchedType.INTERVAL,
                schedValue = "120",
                active = true,
                preventDuplicates = true,
                autoReplaceSameName = true,
                duplicateWindowMinutes = 60,
                attachedFile = AttachedFile(
                    fileName = "status_operacoes.csv",
                    fileSize = 64200L,
                    mimetype = "text/csv",
                    fileHash = computeSha256("CSV_LOGISTICS_OPERATIONS_CHECK"),
                    updatedAt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date()),
                    sampleContent = "Linhas de rotas e frotas ativas"
                )
            )
        )
        _dispatches.value = initialDispatches

        val initialHistory = listOf(
            HistoryEntry(
                id = UUID.randomUUID().toString(),
                whenTime = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date(System.currentTimeMillis() - 1000 * 60 * 35)),
                name = "Relatório Manutenção",
                slug = "relatorio-manutencao",
                group = "Equipe Manutenção",
                ok = true,
                auto = true,
                fileName = "relatorio_manutencao.xlsx",
                note = "Entregue com sucesso no grupo"
            ),
            HistoryEntry(
                id = UUID.randomUUID().toString(),
                whenTime = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date(System.currentTimeMillis() - 1000 * 60 * 15)),
                name = "Relatório Manutenção",
                slug = "relatorio-manutencao",
                group = "Equipe Manutenção",
                ok = true,
                auto = true,
                fileName = "relatorio_manutencao.xlsx",
                note = "Duplicidade evitada: arquivo com mesmo conteúdo já despachado anteriormente",
                isDeduplicated = true
            )
        )
        _history.value = initialHistory

        addLog("WhatsApp ligado com sucesso e conectado ao Baileys", LogType.OK)
        addLog("Agendador em segundo plano com prioridade total ativo", LogType.OK)
    }

    private fun startScheduler() {
        coroutineScope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(30000L) // check every 30 seconds
                if (_waStatus.value == WhatsAppStatus.OPEN) {
                    evaluateScheduledDispatches()
                }
            }
        }
    }

    private fun evaluateScheduledDispatches() {
        val now = Calendar.getInstance()
        val currentHM = String.format(Locale.getDefault(), "%02d:%02d", now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE))
        val currentDay = now.get(Calendar.DAY_OF_MONTH)
        val dayHMKey = "$currentDay-$currentHM"
        val timestamp = System.currentTimeMillis()

        val updatedList = _dispatches.value.map { d ->
            if (!d.active) return@map d

            var shouldRun = false
            var newLastRunHM = d.lastRunHM
            var newLastRunTs = d.lastRunTs

            if (d.schedType == SchedType.FIXED && d.schedValue.isNotBlank()) {
                val times = d.schedValue.split(",").map { it.trim() }
                if (times.contains(currentHM) && d.lastRunHM != dayHMKey) {
                    shouldRun = true
                    newLastRunHM = dayHMKey
                }
            } else if (d.schedType == SchedType.INTERVAL && d.schedValue.isNotBlank()) {
                val intervalMinutes = d.schedValue.toLongOrNull() ?: 120L
                val intervalMs = intervalMinutes * 60 * 1000
                if (intervalMs > 0 && (d.lastRunTs == 0L || (timestamp - d.lastRunTs) >= intervalMs)) {
                    shouldRun = true
                    newLastRunTs = timestamp
                }
            }

            if (shouldRun && d.attachedFile != null) {
                executeDispatchInternal(d, isAuto = true)
                d.copy(
                    lastRunHM = newLastRunHM,
                    lastRunTs = newLastRunTs,
                    lastSentAt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date()),
                    lastSentHash = d.attachedFile.fileHash,
                    lastSentName = d.attachedFile.fileName
                )
            } else {
                d
            }
        }
        _dispatches.value = updatedList
    }

    fun executeDispatch(slug: String, force: Boolean = false): Result<String> {
        val d = _dispatches.value.find { it.slug == slug }
            ?: return Result.failure(Exception("Disparo não encontrado"))

        if (_waStatus.value != WhatsAppStatus.OPEN) {
            addLog("Tentativa de disparo falhou: WhatsApp offline", LogType.ERROR)
            return Result.failure(Exception("WhatsApp não está conectado"))
        }

        return executeDispatchInternal(d, isAuto = false, force = force)
    }

    private fun executeDispatchInternal(d: Dispatch, isAuto: Boolean, force: Boolean = false): Result<String> {
        val groupsToSend = d.effectiveGroups
        if (groupsToSend.isEmpty()) {
            addLog("Disparo \"${d.name}\" cancelado: nenhum grupo de destino definido", LogType.ERROR)
            return Result.failure(Exception("Nenhum grupo de destino definido"))
        }

        val file = d.attachedFile
        val fileName = file?.fileName ?: "mensagem_direta"
        val fileHash = file?.fileHash ?: ""
        val groupNamesStr = d.displayGroupName

        // Anti-duplicação: Se preventDuplicates ativo e mesmo hash dentro da janela
        val windowMin = d.duplicateWindowMinutes
        val isWithinWindow = windowMin == 0 || (d.lastSentAt != null)

        if (d.preventDuplicates && d.lastSentHash == fileHash && isWithinWindow && !force && fileHash.isNotBlank()) {
            val noteMsg = "Duplicidade evitada: \"$fileName\" possui conteúdo idêntico ao já despachado anteriormente."
            addHistoryEntry(
                HistoryEntry(
                    id = UUID.randomUUID().toString(),
                    whenTime = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date()),
                    name = d.name,
                    slug = d.slug,
                    group = groupNamesStr,
                    ok = true,
                    auto = isAuto,
                    fileName = fileName,
                    note = noteMsg,
                    isDeduplicated = true,
                    groupsCount = groupsToSend.size
                )
            )
            addLog("[DEDUPLICAÇÃO] Envio de \"$fileName\" evitado para $groupNamesStr (mesmo conteúdo)", LogType.NORMAL)
            return Result.success("Duplicidade evitada: arquivo idêntico já foi despachado anteriormente para $groupNamesStr.")
        }

        // Expand caption tokens
        val caption = fillCaption(d.caption)

        // Dispatch to all target groups
        groupsToSend.forEach { g ->
            addLog("[WHATSAPP] Enviando \"$fileName\" para grupo: ${g.name} (${g.id})", LogType.NORMAL)
        }

        // Log entry to history
        addHistoryEntry(
            HistoryEntry(
                id = UUID.randomUUID().toString(),
                whenTime = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date()),
                name = d.name,
                slug = d.slug,
                group = groupNamesStr,
                ok = true,
                auto = isAuto,
                fileName = fileName,
                note = "Entregue com sucesso para ${groupsToSend.size} grupo(s)",
                groupsCount = groupsToSend.size
            )
        )

        // Update dispatch last sent
        _dispatches.value = _dispatches.value.map {
            if (it.slug == d.slug) {
                it.copy(
                    lastSentHash = fileHash,
                    lastSentName = fileName,
                    lastSentAt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
                )
            } else it
        }

        val logText = if (isAuto) {
            "[AGENDADOR] Disparo automático de \"$fileName\" entregue com sucesso para: $groupNamesStr"
        } else {
            "[MANUAL] Disparo de \"$fileName\" realizado com sucesso para: $groupNamesStr"
        }
        addLog(logText, LogType.OK)
        return Result.success("Disparado com sucesso para ${groupsToSend.size} grupo(s): $groupNamesStr")
    }

    fun saveDispatch(dispatch: Dispatch) {
        val current = _dispatches.value.toMutableList()
        val index = current.indexOfFirst { it.slug == dispatch.slug }
        if (index >= 0) {
            current[index] = dispatch
            addLog("Disparo \"${dispatch.name}\" atualizado (${dispatch.effectiveGroups.size} grupo(s))", LogType.NORMAL)
        } else {
            current.add(0, dispatch)
            addLog("Novo disparo \"${dispatch.name}\" criado para ${dispatch.displayGroupName}", LogType.OK)
        }
        _dispatches.value = current
    }

    fun deleteDispatch(slug: String) {
        val d = _dispatches.value.find { it.slug == slug }
        _dispatches.value = _dispatches.value.filter { it.slug != slug }
        addLog("Disparo \"${d?.name ?: slug}\" removido", LogType.NORMAL)
    }

    fun toggleDispatchActive(slug: String) {
        _dispatches.value = _dispatches.value.map {
            if (it.slug == slug) {
                val newState = !it.active
                addLog("Disparo \"${it.name}\" ${if (newState) "ativado" else "pausado"}", LogType.NORMAL)
                it.copy(active = newState)
            } else it
        }
    }

    fun addGroup(name: String, jid: String): WhatsAppGroup {
        val newGroup = WhatsAppGroup(id = jid, name = name, participants = 1)
        _groups.value = listOf(newGroup) + _groups.value.filter { it.id != jid }
        addLog("Grupo \"$name\" ($jid) adicionado", LogType.NORMAL)
        return newGroup
    }

    fun clearHistory() {
        _history.value = emptyList()
        addLog("Histórico de envios limpo", LogType.NORMAL)
    }

    private fun addHistoryEntry(entry: HistoryEntry) {
        _history.value = (listOf(entry) + _history.value).take(150)
    }

    fun addLog(msg: String, type: LogType = LogType.NORMAL) {
        val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val entry = LogEntry(id = UUID.randomUUID().toString(), time = timeStr, message = msg, type = type)
        _logs.value = (listOf(entry) + _logs.value).take(60)
    }

    fun setWhatsAppStatus(status: WhatsAppStatus) {
        _waStatus.value = status
        addLog("Status do WhatsApp alterado para: $status", if (status == WhatsAppStatus.OPEN) LogType.OK else LogType.NORMAL)
    }

    fun requestPairingCode(phone: String): String {
        val cleanPhone = phone.replace(Regex("\\D"), "")
        if (cleanPhone.length < 8) {
            addLog("Erro: número de telefone inválido para código de pareamento", LogType.ERROR)
            return ""
        }
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val codePart1 = (1..4).map { alphabet.random() }.joinToString("")
        val codePart2 = (1..4).map { alphabet.random() }.joinToString("")
        val fullCode = "$codePart1-$codePart2"
        _pairingCode.value = fullCode
        _waStatus.value = WhatsAppStatus.OPEN
        addLog("Código de pareamento gerado para +$cleanPhone: $fullCode", LogType.OK)
        return fullCode
    }

    fun resetSession() {
        _waStatus.value = WhatsAppStatus.NEEDS_PAIRING
        _pairingCode.value = ""
        addLog("Sessão Baileys reiniciada. Aguardando novo QR Code ou código.", LogType.NORMAL)
    }

    fun updateApiToken(newToken: String) {
        _apiToken.value = newToken.trim()
        addLog("Token de autenticação da API atualizado", LogType.OK)
    }

    fun updateServerUrl(newUrl: String) {
        _serverUrl.value = newUrl.trim()
        addLog("URL base do servidor configurada para $newUrl", LogType.NORMAL)
    }

    fun setBackgroundPriorityEnabled(enabled: Boolean) {
        _backgroundPriorityEnabled.value = enabled
        addLog("Prioridade de execução em 2º plano: ${if (enabled) "Ativada (Foreground Service)" else "Padrão"}", LogType.NORMAL)
    }

    fun fillCaption(template: String): String {
        val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        return template
            .replace("{data}", dateStr)
            .replace("{hora}", timeStr)
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
