package com.disparador.app.model

enum class SchedType {
    FIXED,
    INTERVAL,
    NONE
}

data class AttachedFile(
    val fileName: String,
    val fileSize: Long,
    val mimetype: String,
    val fileHash: String,
    val updatedAt: String,
    val sampleContent: String = ""
)

data class WhatsAppGroup(
    val id: String,
    val name: String,
    val participants: Int = 0
)

data class Dispatch(
    val slug: String,
    val name: String,
    val groupJid: String = "",
    val groupName: String = "",
    val targetGroups: List<WhatsAppGroup> = emptyList(),
    val caption: String = "",
    val schedType: SchedType = SchedType.FIXED,
    val schedValue: String = "08:00, 18:00",
    val active: Boolean = true,
    val preventDuplicates: Boolean = true,
    val autoReplaceSameName: Boolean = true,
    val duplicateWindowMinutes: Int = 60,
    val attachedFile: AttachedFile? = null,
    val lastSentHash: String? = null,
    val lastSentName: String? = null,
    val lastSentAt: String? = null,
    val lastRunHM: String? = null,
    val lastRunTs: Long = 0L
) {
    val effectiveGroups: List<WhatsAppGroup>
        get() {
            if (targetGroups.isNotEmpty()) return targetGroups
            if (groupJid.isNotBlank()) return listOf(WhatsAppGroup(id = groupJid, name = groupName.ifBlank { "Grupo" }))
            return emptyList()
        }

    val displayGroupName: String
        get() {
            val list = effectiveGroups
            return when {
                list.isEmpty() -> "Sem grupos definidos"
                list.size == 1 -> list[0].name
                list.size == 2 -> "${list[0].name} e ${list[1].name}"
                else -> "${list[0].name} +${list.size - 1} grupos (${list.size} no total)"
            }
        }
}

data class HistoryEntry(
    val id: String,
    val whenTime: String,
    val name: String,
    val slug: String,
    val group: String,
    val ok: Boolean,
    val auto: Boolean = false,
    val fileName: String? = null,
    val note: String? = null,
    val error: String? = null,
    val isDeduplicated: Boolean = false,
    val groupsCount: Int = 1
)

enum class WhatsAppStatus {
    OPEN,
    NEEDS_PAIRING,
    CONNECTING,
    OFFLINE
}

enum class LogType {
    NORMAL,
    OK,
    ERROR
}

data class LogEntry(
    val id: String,
    val time: String,
    val message: String,
    val type: LogType = LogType.NORMAL
)

enum class AppScreen {
    HOME,
    HISTORY,
    CONNECTION,
    EDIT_DISPATCH,
    PICK_GROUP
}
