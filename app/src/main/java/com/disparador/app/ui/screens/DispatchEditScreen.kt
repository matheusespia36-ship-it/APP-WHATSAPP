package com.disparador.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.disparador.app.data.DispatchRepository
import com.disparador.app.model.Dispatch
import com.disparador.app.model.SchedType
import com.disparador.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DispatchEditScreen(
    dispatch: Dispatch,
    isEditing: Boolean,
    onUpdateDraft: ((Dispatch) -> Dispatch) -> Unit,
    onSave: () -> Unit,
    onDelete: (String) -> Unit,
    onBack: () -> Unit,
    onPickGroup: () -> Unit,
    onAttachSample: (String) -> Unit,
    onRemoveFile: () -> Unit,
    onTestNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    var newTimeInput by remember { mutableStateOf("") }
    var showWindowDropdown by remember { mutableStateOf(false) }
    var showIntervalDropdown by remember { mutableStateOf(false) }
    var showSampleFileSheet by remember { mutableStateOf(false) }

    val timesList = remember(dispatch.schedValue) {
        if (dispatch.schedType == SchedType.FIXED) {
            dispatch.schedValue.split(",").map { it.trim() }.filter { it.isNotBlank() }
        } else emptyList()
    }

    val intervalOptions = listOf(
        Pair(15, "15 minutos"),
        Pair(30, "30 minutos"),
        Pair(60, "1 hora"),
        Pair(120, "2 horas"),
        Pair(240, "4 horas"),
        Pair(360, "6 horas"),
        Pair(720, "12 horas"),
        Pair(1440, "24 horas")
    )

    val windowOptions = listOf(
        Pair(0, "Sempre verificar (nunca reenviar arquivo idêntico)"),
        Pair(30, "Bloquear duplicados dentro de 30 minutos"),
        Pair(60, "Bloquear duplicados dentro de 1 hora"),
        Pair(120, "Bloquear duplicados dentro de 2 horas"),
        Pair(1440, "Bloquear duplicados no mesmo dia (24 horas)")
    )

    val currentSlug = dispatch.slug.ifEmpty { "meu-disparo" }
    val webhookUrl = "http://127.0.0.1:3000/send/$currentSlug"

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = BgDark,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEditing) "Editar Disparo" else "Novo Disparo",
                        color = ForegroundLight,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = ForegroundLight
                        )
                    }
                },
                actions = {
                    if (isEditing) {
                        IconButton(
                            onClick = { onDelete(dispatch.slug) },
                            modifier = Modifier.testTag("delete_dispatch_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Excluir Disparo",
                                tint = DangerRed
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgDark)
            )
        },
        bottomBar = {
            Surface(
                color = BgDark,
                border = androidx.compose.foundation.BorderStroke(1.dp, LineBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onTestNow,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, LineBorderStrong),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ForegroundLight)
                    ) {
                        Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Testar Envio", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onSave,
                        modifier = Modifier
                            .weight(1.3f)
                            .height(48.dp)
                            .testTag("save_dispatch_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentGreen,
                            contentColor = AccentInk
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isEditing) "Salvar Alterações" else "Criar Disparo", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Field: Name
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Nome do Disparo",
                    color = MutedText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                OutlinedTextField(
                    value = dispatch.name,
                    onValueChange = { newName ->
                        onUpdateDraft { it.copy(name = newName) }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("dispatch_name_input"),
                    placeholder = { Text("Ex: Relatório Manutenção", color = MutedText) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = ForegroundLight,
                        unfocusedTextColor = ForegroundLight,
                        focusedBorderColor = AccentGreen,
                        unfocusedBorderColor = LineBorder,
                        focusedContainerColor = SurfaceDark,
                        unfocusedContainerColor = SurfaceDark
                    )
                )
            }

            // Field: WhatsApp Destination Groups (Multiple)
            val selectedGroups = dispatch.effectiveGroups
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Grupos WhatsApp de Destino (${selectedGroups.size})",
                        color = MutedText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedButton(
                        onClick = onPickGroup,
                        modifier = Modifier.height(32.dp).testTag("pick_groups_button"),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AccentBorder)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = AccentGreen,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (selectedGroups.isEmpty()) "Escolher Grupos" else "Adicionar / Alterar",
                            color = AccentGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (selectedGroups.isEmpty()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onPickGroup() },
                        color = SurfaceDark,
                        border = androidx.compose.foundation.BorderStroke(1.dp, LineBorder),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(AccentSoft),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("#", color = AccentGreen, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Nenhum grupo selecionado", color = ForegroundLight, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Toque para escolher múltiplos grupos de destino", color = MutedText, fontSize = 12.sp)
                            }
                            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = MutedText)
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        selectedGroups.forEach { group ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = SurfaceDark,
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, LineBorder)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(AccentSoft),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = group.name.firstOrNull()?.uppercase() ?: "#",
                                                color = AccentGreen,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 14.sp
                                            )
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = group.name,
                                                color = ForegroundLight,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = group.id,
                                                color = MutedText,
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = {
                                            val remaining = selectedGroups.filter { it.id != group.id }
                                            onUpdateDraft {
                                                val firstG = remaining.firstOrNull()
                                                it.copy(
                                                    targetGroups = remaining,
                                                    groupJid = firstG?.id ?: "",
                                                    groupName = firstG?.name ?: ""
                                                )
                                            }
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remover grupo",
                                            tint = DangerRed,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Field: Attached File Section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Escolha de Arquivo (Relatório / Documento)",
                        color = MutedText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedButton(
                        onClick = { showSampleFileSheet = true },
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AccentBorder)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Upload,
                            contentDescription = null,
                            tint = AccentGreen,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (dispatch.attachedFile != null) "Trocar Arquivo" else "Escolher Arquivo",
                            color = AccentGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (dispatch.attachedFile != null) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = SurfaceDark,
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, LineBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(AccentSoft),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Description,
                                        contentDescription = null,
                                        tint = AccentGreen,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = dispatch.attachedFile.fileName,
                                        color = ForegroundLight,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${DispatchRepository.formatBytes(dispatch.attachedFile.fileSize)} · ${dispatch.attachedFile.mimetype}",
                                        color = MutedText,
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = "✓ Arquivo pronto · Substituição automática ativa sem duplicar",
                                        color = AccentGreen,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { showSampleFileSheet = true },
                                    modifier = Modifier.weight(1f).height(34.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceVariantDark, contentColor = ForegroundLight)
                                ) {
                                    Text("Substituir", fontSize = 12.sp)
                                }
                                Button(
                                    onClick = onRemoveFile,
                                    modifier = Modifier.height(34.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = DangerSoft, contentColor = DangerRed)
                                ) {
                                    Text("Remover", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                } else {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { showSampleFileSheet = true },
                        color = SurfaceDark,
                        border = androidx.compose.foundation.BorderStroke(1.dp, LineBorder),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(AccentSoft),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Upload,
                                    contentDescription = null,
                                    tint = AccentGreen,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Text(
                                text = "Clique para escolher o arquivo",
                                color = ForegroundLight,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Suporta relatórios Excel (.xlsx, .xls), PDF, Word (.docx) ou CSV.\nAo receber novo relatório com mesmo nome, ele substitui automaticamente.",
                                color = MutedText,
                                fontSize = 12.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Anti-duplication Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SurfaceDark,
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, LineBorder)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Shield, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(18.dp))
                        Text(
                            text = "Regras Anti-Duplicação & Arquivos com Mesmo Nome",
                            color = ForegroundLight,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    // Rule 1: Prevent identical files
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = "Evitar envio de arquivos idênticos",
                                color = ForegroundLight,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Se o arquivo recebido tiver exatamente o mesmo conteúdo do anterior, o envio é cancelado.",
                                color = MutedText,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = dispatch.preventDuplicates,
                            onCheckedChange = { checked ->
                                onUpdateDraft { it.copy(preventDuplicates = checked) }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AccentInk,
                                checkedTrackColor = AccentGreen,
                                uncheckedThumbColor = MutedText,
                                uncheckedTrackColor = LineBorder
                            )
                        )
                    }

                    HorizontalDivider(color = LineBorder, thickness = 1.dp)

                    // Rule 2: Auto-replace same name
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = "Substituição automática ao receber mesmo nome",
                                color = ForegroundLight,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Ex: Chega novo relatório com mesmo nome: substitui o antigo sem duplicar cópias.",
                                color = MutedText,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = dispatch.autoReplaceSameName,
                            onCheckedChange = { checked ->
                                onUpdateDraft { it.copy(autoReplaceSameName = checked) }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AccentInk,
                                checkedTrackColor = AccentGreen,
                                uncheckedThumbColor = MutedText,
                                uncheckedTrackColor = LineBorder
                            )
                        )
                    }

                    HorizontalDivider(color = LineBorder, thickness = 1.dp)

                    // Protection Window
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Janela de proteção contra relatórios repetidos:",
                            color = MutedText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Box {
                            val selectedWindowText = windowOptions.find { it.first == dispatch.duplicateWindowMinutes }?.second
                                ?: "Sempre verificar"
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { showWindowDropdown = true },
                                color = SurfaceVariantDark,
                                border = androidx.compose.foundation.BorderStroke(1.dp, LineBorder),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = selectedWindowText, color = ForegroundLight, fontSize = 12.sp)
                                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = MutedText)
                                }
                            }
                            DropdownMenu(
                                expanded = showWindowDropdown,
                                onDismissRequest = { showWindowDropdown = false },
                                modifier = Modifier.background(SurfaceDark)
                            ) {
                                windowOptions.forEach { opt ->
                                    DropdownMenuItem(
                                        text = { Text(opt.second, color = ForegroundLight, fontSize = 12.sp) },
                                        onClick = {
                                            onUpdateDraft { it.copy(duplicateWindowMinutes = opt.first) }
                                            showWindowDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Field: Schedule Type
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Tipo de Agendamento",
                    color = MutedText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                // Segmented buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceDark)
                        .border(1.dp, LineBorder, RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val isFixed = dispatch.schedType == SchedType.FIXED
                    Button(
                        onClick = { onUpdateDraft { it.copy(schedType = SchedType.FIXED) } },
                        modifier = Modifier.weight(1f).height(38.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isFixed) AccentGreen else Color.Transparent,
                            contentColor = if (isFixed) AccentInk else MutedText
                        )
                    ) {
                        Text("Horários Fixos", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    val isInterval = dispatch.schedType == SchedType.INTERVAL
                    Button(
                        onClick = { onUpdateDraft { it.copy(schedType = SchedType.INTERVAL) } },
                        modifier = Modifier.weight(1f).height(38.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isInterval) AccentGreen else Color.Transparent,
                            contentColor = if (isInterval) AccentInk else MutedText
                        )
                    ) {
                        Text("Intervalo Recorrente", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                if (dispatch.schedType == SchedType.FIXED) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = "Horários programados do dia:", color = MutedText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            timesList.forEach { time ->
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = AccentSoft,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, AccentBorder)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = time, color = AccentGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        IconButton(
                                            onClick = {
                                                val remaining = timesList.filter { it != time }
                                                onUpdateDraft { it.copy(schedValue = remaining.joinToString(", ")) }
                                            },
                                            modifier = Modifier.size(22.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.Close, contentDescription = "Remover", tint = AccentGreen, modifier = Modifier.size(12.dp))
                                        }
                                    }
                                }
                            }
                        }

                        // Add time input
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newTimeInput,
                                onValueChange = { newTimeInput = it },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("Ex: 14:30", color = MutedText, fontSize = 13.sp) },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = ForegroundLight,
                                    unfocusedTextColor = ForegroundLight,
                                    focusedBorderColor = AccentGreen,
                                    unfocusedBorderColor = LineBorder,
                                    focusedContainerColor = SurfaceDark,
                                    unfocusedContainerColor = SurfaceDark
                                )
                            )
                            Button(
                                onClick = {
                                    val trimmed = newTimeInput.trim()
                                    if (trimmed.isNotBlank() && !timesList.contains(trimmed)) {
                                        val updated = (timesList + trimmed).sorted()
                                        onUpdateDraft { it.copy(schedValue = updated.joinToString(", ")) }
                                        newTimeInput = ""
                                    }
                                },
                                modifier = Modifier.height(48.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SurfaceVariantDark, contentColor = AccentGreen)
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Adicionar")
                            }
                        }

                        // Presets
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Atalhos:", color = MutedText, fontSize = 11.sp)
                            listOf("08:00", "12:00", "14:00", "18:00", "22:00").forEach { preset ->
                                OutlinedButton(
                                    onClick = {
                                        if (!timesList.contains(preset)) {
                                            val updated = (timesList + preset).sorted()
                                            onUpdateDraft { it.copy(schedValue = updated.joinToString(", ")) }
                                        }
                                    },
                                    modifier = Modifier.height(28.dp),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, LineBorderStrong)
                                ) {
                                    Text("+$preset", fontSize = 10.sp, color = MutedText)
                                }
                            }
                        }
                    }
                } else {
                    // Interval dropdown
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(text = "Repetir envio a cada:", color = MutedText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Box {
                            val currentIntervalMin = dispatch.schedValue.toIntOrNull() ?: 120
                            val selectedIntervalText = intervalOptions.find { it.first == currentIntervalMin }?.second ?: "$currentIntervalMin minutos"
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { showIntervalDropdown = true },
                                color = SurfaceVariantDark,
                                border = androidx.compose.foundation.BorderStroke(1.dp, LineBorder),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = "A cada $selectedIntervalText", color = ForegroundLight, fontSize = 13.sp)
                                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = MutedText)
                                }
                            }
                            DropdownMenu(
                                expanded = showIntervalDropdown,
                                onDismissRequest = { showIntervalDropdown = false },
                                modifier = Modifier.background(SurfaceDark)
                            ) {
                                intervalOptions.forEach { opt ->
                                    DropdownMenuItem(
                                        text = { Text("A cada ${opt.second}", color = ForegroundLight, fontSize = 13.sp) },
                                        onClick = {
                                            onUpdateDraft { it.copy(schedValue = opt.first.toString()) }
                                            showIntervalDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Field: Caption
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Legenda da Mensagem (Opcional)",
                        color = MutedText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedButton(
                            onClick = {
                                onUpdateDraft { it.copy(caption = "${it.caption} {data}".trim()) }
                            },
                            modifier = Modifier.height(26.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, LineBorderStrong)
                        ) {
                            Text("+{data}", fontSize = 10.sp, color = AccentGreen)
                        }
                        OutlinedButton(
                            onClick = {
                                onUpdateDraft { it.copy(caption = "${it.caption} {hora}".trim()) }
                            },
                            modifier = Modifier.height(26.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, LineBorderStrong)
                        ) {
                            Text("+{hora}", fontSize = 10.sp, color = AccentGreen)
                        }
                    }
                }

                OutlinedTextField(
                    value = dispatch.caption,
                    onValueChange = { newCaption ->
                        onUpdateDraft { it.copy(caption = newCaption) }
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp),
                    placeholder = { Text("Ex: Relatório Manutenção · {data} às {hora}", color = MutedText, fontSize = 13.sp) },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = ForegroundLight,
                        unfocusedTextColor = ForegroundLight,
                        focusedBorderColor = AccentGreen,
                        unfocusedBorderColor = LineBorder,
                        focusedContainerColor = SurfaceDark,
                        unfocusedContainerColor = SurfaceDark
                    )
                )
                Text(
                    text = "Tags disponíveis: {data} para data (DD/MM/AAAA) e {hora} para horário (HH:MM).",
                    color = MutedText,
                    fontSize = 11.sp
                )
            }

            // Field: Active / Paused Segmented
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text = "Status deste disparo", color = MutedText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceDark)
                        .border(1.dp, LineBorder, RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Button(
                        onClick = { onUpdateDraft { it.copy(active = true) } },
                        modifier = Modifier.weight(1f).height(38.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (dispatch.active) AccentGreen else Color.Transparent,
                            contentColor = if (dispatch.active) AccentInk else MutedText
                        )
                    ) {
                        Text("✓ Ativo (executando)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Button(
                        onClick = { onUpdateDraft { it.copy(active = false) } },
                        modifier = Modifier.weight(1f).height(38.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!dispatch.active) DangerRed else Color.Transparent,
                            contentColor = if (!dispatch.active) AccentInk else MutedText
                        )
                    ) {
                        Text("✕ Pausado", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            // Webhook Info
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text = "Webhook HTTP para disparo remoto (Excel / n8n / API)", color = MutedText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = SurfaceDark,
                    border = androidx.compose.foundation.BorderStroke(1.dp, LineBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = webhookUrl,
                            color = AccentGreen,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { clipboardManager.setText(AnnotatedString(webhookUrl)) },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copiar", tint = MutedText, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }
    }

    // Sample file selector bottom sheet dialog
    if (showSampleFileSheet) {
        AlertDialog(
            onDismissRequest = { showSampleFileSheet = false },
            title = { Text("Escolher Arquivo de Relatório", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = ForegroundLight) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Selecione o formato de relatório a ser despachado no grupo:", color = MutedText, fontSize = 13.sp)
                    listOf(
                        Triple("excel", "Planilha Excel (.xlsx)", "Relatório com tabelas e fórmulas"),
                        Triple("pdf", "Documento PDF (.pdf)", "Relatório executivo diagramado"),
                        Triple("csv", "Arquivo CSV (.csv)", "Dados tabulares de faturamento"),
                        Triple("word", "Documento Word (.docx)", "Comunicado ou ata de reunião")
                    ).forEach { (type, label, desc) ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    onAttachSample(type)
                                    showSampleFileSheet = false
                                },
                            color = SurfaceVariantDark,
                            border = androidx.compose.foundation.BorderStroke(1.dp, LineBorder),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Description, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(20.dp))
                                Column {
                                    Text(label, color = ForegroundLight, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(desc, color = MutedText, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSampleFileSheet = false }) {
                    Text("Cancelar", color = AccentGreen)
                }
            },
            containerColor = SurfaceDark
        )
    }
}
