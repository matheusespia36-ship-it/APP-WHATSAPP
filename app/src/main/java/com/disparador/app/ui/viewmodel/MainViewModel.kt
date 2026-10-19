package com.disparador.app.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.disparador.app.data.DispatchRepository
import com.disparador.app.model.*
import com.disparador.app.service.SchedulerForegroundService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainViewModel(
    private val repository: DispatchRepository = DispatchRepository(kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default))
) : ViewModel() {

    val dispatches: StateFlow<List<Dispatch>> = repository.dispatches
    val history: StateFlow<List<HistoryEntry>> = repository.history
    val groups: StateFlow<List<WhatsAppGroup>> = repository.groups
    val waStatus: StateFlow<WhatsAppStatus> = repository.waStatus
    val logs: StateFlow<List<LogEntry>> = repository.logs
    val serverUrl: StateFlow<String> = repository.serverUrl
    val apiToken: StateFlow<String> = repository.apiToken
    val pairingCode: StateFlow<String> = repository.pairingCode
    val backgroundPriorityEnabled: StateFlow<Boolean> = repository.backgroundPriorityEnabled

    private val _currentScreen = MutableStateFlow(AppScreen.HOME)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _draftDispatch = MutableStateFlow<Dispatch?>(null)
    val draftDispatch: StateFlow<Dispatch?> = _draftDispatch.asStateFlow()

    private val _isEditing = MutableStateFlow(false)
    val isEditing: StateFlow<Boolean> = _isEditing.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _groupSearchQuery = MutableStateFlow("")
    val groupSearchQuery: StateFlow<String> = _groupSearchQuery.asStateFlow()

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun showToast(msg: String) {
        _toastMessage.value = msg
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun startNewDispatch() {
        val newSlug = "disparo-" + System.currentTimeMillis() % 10000
        val defaultGroup = groups.value.firstOrNull()
        val defaultList = if (defaultGroup != null) listOf(defaultGroup) else emptyList()
        _draftDispatch.value = Dispatch(
            slug = newSlug,
            name = "",
            groupJid = defaultGroup?.id ?: "",
            groupName = defaultGroup?.name ?: "",
            targetGroups = defaultList,
            caption = "Relatório · {data} às {hora}",
            schedType = SchedType.FIXED,
            schedValue = "08:00, 18:00",
            active = true,
            preventDuplicates = true,
            autoReplaceSameName = true,
            duplicateWindowMinutes = 60,
            attachedFile = null
        )
        _isEditing.value = false
        _currentScreen.value = AppScreen.EDIT_DISPATCH
    }

    fun editDispatch(slug: String) {
        val existing = dispatches.value.find { it.slug == slug }
        if (existing != null) {
            val targets = existing.effectiveGroups
            _draftDispatch.value = existing.copy(targetGroups = targets)
            _isEditing.value = true
            _currentScreen.value = AppScreen.EDIT_DISPATCH
        }
    }

    fun updateDraft(updater: (Dispatch) -> Dispatch) {
        _draftDispatch.value?.let {
            _draftDispatch.value = updater(it)
        }
    }

    fun saveDraft() {
        val draft = _draftDispatch.value ?: return
        if (draft.name.isBlank()) {
            showToast("Informe o nome do disparo")
            return
        }
        val targetList = draft.effectiveGroups
        if (targetList.isEmpty()) {
            showToast("Selecione ao menos um grupo WhatsApp de destino")
            return
        }
        val finalSlug = if (draft.slug.isBlank()) {
            draft.name.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').ifEmpty { "disparo" }
        } else draft.slug

        val firstG = targetList.first()
        val prepared = draft.copy(
            slug = finalSlug,
            groupJid = firstG.id,
            groupName = firstG.name,
            targetGroups = targetList
        )

        repository.saveDispatch(prepared)
        showToast(if (_isEditing.value) "Disparo atualizado com sucesso" else "Disparo criado com sucesso para ${targetList.size} grupo(s)")
        _currentScreen.value = AppScreen.HOME
    }

    fun deleteDispatch(slug: String) {
        repository.deleteDispatch(slug)
        showToast("Disparo excluído")
        _currentScreen.value = AppScreen.HOME
    }

    fun toggleDispatchActive(slug: String) {
        repository.toggleDispatchActive(slug)
    }

    fun testDispatch(slug: String, force: Boolean = false) {
        viewModelScope.launch {
            val res = repository.executeDispatch(slug, force = force)
            res.fold(
                onSuccess = { msg -> showToast(msg) },
                onFailure = { err -> showToast("Falha: ${err.message}") }
            )
        }
    }

    fun setDraftTargetGroups(selected: List<WhatsAppGroup>) {
        updateDraft { current ->
            val first = selected.firstOrNull()
            current.copy(
                targetGroups = selected,
                groupJid = first?.id ?: "",
                groupName = first?.name ?: ""
            )
        }
        _currentScreen.value = AppScreen.EDIT_DISPATCH
    }

    fun removeGroupFromDraft(group: WhatsAppGroup) {
        updateDraft { current ->
            val remaining = current.effectiveGroups.filter { it.id != group.id }
            val first = remaining.firstOrNull()
            current.copy(
                targetGroups = remaining,
                groupJid = first?.id ?: "",
                groupName = first?.name ?: ""
            )
        }
        showToast("Grupo removido do disparo")
    }

    fun addManualGroup(name: String, jid: String) {
        val trimmed = jid.trim()
        if (trimmed.isBlank()) {
            showToast("Informe um JID válido")
            return
        }
        val group = repository.addGroup(name = name.ifBlank { "Grupo Personalizado" }, jid = trimmed)
        updateDraft { current ->
            val updated = (current.effectiveGroups.filter { it.id != group.id } + group)
            current.copy(
                targetGroups = updated,
                groupJid = updated.first().id,
                groupName = updated.first().name
            )
        }
        showToast("Grupo adicionado!")
        _currentScreen.value = AppScreen.EDIT_DISPATCH
    }

    fun clearHistory() {
        repository.clearHistory()
        showToast("Histórico limpo")
    }

    fun requestPairCode(phone: String) {
        viewModelScope.launch {
            repository.requestPairingCode(phone).fold(
                onSuccess = { showToast("Código gerado: $it") },
                onFailure = { showToast("Falha: ${it.message}") }
            )
        }
    }

    fun resetSession() {
        repository.resetSession()
        showToast("Sessão reiniciada. Aguardando novo QR Code.")
    }

    fun updateToken(token: String) {
        repository.updateApiToken(token)
        showToast("Token atualizado")
    }

    fun updateServerUrl(url: String) {
        repository.updateServerUrl(url)
        showToast("URL do servidor salva")
    }

    fun testConnection() {
        viewModelScope.launch {
            repository.testConnection().fold(
                onSuccess = { showToast(it) },
                onFailure = { showToast("Falha: ${it.message}") }
            )
        }
    }

    fun toggleBackgroundPriority(enabled: Boolean, context: Context? = null) {
        repository.setBackgroundPriorityEnabled(enabled)
        if (context != null) {
            if (enabled) {
                SchedulerForegroundService.start(context)
                showToast("Prioridade total em segundo plano ATIVADA")
            } else {
                SchedulerForegroundService.stop(context)
                showToast("Serviço em segundo plano desativado")
            }
        } else {
            showToast(if (enabled) "Prioridade em segundo plano ATIVADA" else "Serviço em segundo plano desativado")
        }
    }

    fun attachSampleReport(type: String) {
        val nowStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
        val (fileName, mime, size, sample) = when (type) {
            "excel" -> SampleData(
                "relatorio_financeiro.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                256000L,
                "Planilha de fechamento mensal"
            )
            "pdf" -> SampleData(
                "resumo_executivo.pdf",
                "application/pdf",
                512000L,
                "Documento PDF executivo consolidado"
            )
            "csv" -> SampleData(
                "extrato_pedidos.csv",
                "text/csv",
                98400L,
                "Registros de pedidos por hora"
            )
            else -> SampleData(
                "comunicado_geral.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                142000L,
                "Memorando corporativo de rotina"
            )
        }
        val fileHash = DispatchRepository.computeSha256(sample + nowStr)
        val file = AttachedFile(
            fileName = fileName,
            fileSize = size,
            mimetype = mime,
            fileHash = fileHash,
            updatedAt = nowStr,
            sampleContent = sample
        )
        updateDraft { it.copy(attachedFile = file) }
        showToast("Arquivo \"$fileName\" anexado com sucesso")
    }

    fun attachRealFile(fileName: String, base64Content: String) {
        val nowStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
        val fileHash = DispatchRepository.computeSha256(base64Content)
        val file = AttachedFile(
            fileName = fileName,
            fileSize = base64Content.length.toLong(),
            mimetype = "application/octet-stream",
            fileHash = fileHash,
            updatedAt = nowStr,
            sampleContent = "",
            contentBase64 = base64Content
        )
        updateDraft { it.copy(attachedFile = file) }
        showToast("Arquivo \"$fileName\" anexado com sucesso")
    }

    fun removeDraftFile() {
        updateDraft { it.copy(attachedFile = null) }
        showToast("Arquivo removido do disparo")
    }
}

private data class SampleData(
    val fileName: String,
    val mime: String,
    val size: Long,
    val sample: String
)
