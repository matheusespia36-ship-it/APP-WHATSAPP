package com.disparador.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.disparador.app.data.DispatchRepository
import com.disparador.app.model.Dispatch
import com.disparador.app.model.SchedType
import com.disparador.app.model.WhatsAppStatus
import com.disparador.app.ui.components.StatCard
import com.disparador.app.ui.components.StatusBadge
import com.disparador.app.ui.theme.*

@Composable
fun HomeScreen(
    dispatches: List<Dispatch>,
    waStatus: WhatsAppStatus,
    onNewDispatch: () -> Unit,
    onEditDispatch: (String) -> Unit,
    onToggleActive: (String) -> Unit,
    onTestDispatch: (String) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeCount = dispatches.count { it.active }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = BgDark,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BgDark)
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Disparador WhatsApp",
                    color = ForegroundLight,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp
                )
                StatusBadge(status = waStatus)
            }
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BgDark)
                    .border(width = 1.dp, color = LineBorder, shape = RoundedCornerShape(0.dp))
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Button(
                    onClick = onNewDispatch,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("new_dispatch_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentGreen,
                        contentColor = AccentInk
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Novo Disparo",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Novo disparo",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 20.dp)
        ) {
            item {
                StatCard(
                    activeCount = activeCount,
                    totalCount = dispatches.size
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Meus Disparos",
                        color = ForegroundLight,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("refresh_dispatches_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Atualizar",
                            tint = MutedText
                        )
                    }
                }
            }

            if (dispatches.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, LineBorderStrong, RoundedCornerShape(16.dp))
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = null,
                                tint = MutedText,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "Nenhum disparo configurado ainda.",
                                color = ForegroundLight,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Toque em 'Novo disparo' abaixo para agendar relatórios automáticos.",
                                color = MutedText,
                                fontSize = 13.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(dispatches, key = { it.slug }) { dispatch ->
                    DispatchItemCard(
                        dispatch = dispatch,
                        onEdit = { onEditDispatch(dispatch.slug) },
                        onToggleActive = { onToggleActive(dispatch.slug) },
                        onTest = { onTestDispatch(dispatch.slug) }
                    )
                }
            }
        }
    }
}

@Composable
fun DispatchItemCard(
    dispatch: Dispatch,
    onEdit: () -> Unit,
    onToggleActive: () -> Unit,
    onTest: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, if (dispatch.active) LineBorderStrong else LineBorder, RoundedCornerShape(18.dp)),
        color = SurfaceDark,
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: Title & Active Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = dispatch.name.ifEmpty { dispatch.slug },
                    color = if (dispatch.active) ForegroundLight else MutedText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onEdit() }
                )
                Switch(
                    checked = dispatch.active,
                    onCheckedChange = { onToggleActive() },
                    modifier = Modifier.testTag("toggle_dispatch_${dispatch.slug}"),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = AccentInk,
                        checkedTrackColor = AccentGreen,
                        uncheckedThumbColor = MutedText,
                        uncheckedTrackColor = LineBorder
                    )
                )
            }

            // Recipient Groups
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Group,
                    contentDescription = null,
                    tint = if (dispatch.effectiveGroups.size > 1) AccentGreen else MutedText,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = dispatch.displayGroupName,
                    color = if (dispatch.effectiveGroups.size > 1) ForegroundLight else MutedText,
                    fontSize = 13.sp,
                    fontWeight = if (dispatch.effectiveGroups.size > 1) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Schedule info
            val schedDescription = when (dispatch.schedType) {
                SchedType.FIXED -> if (dispatch.schedValue.isNotBlank()) "Diariamente às ${dispatch.schedValue}" else "Sem horários definidos"
                SchedType.INTERVAL -> "A cada ${dispatch.schedValue} minutos"
                SchedType.NONE -> "Disparo sob demanda"
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = MutedText,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = schedDescription,
                    color = MutedText,
                    fontSize = 13.sp
                )
            }

            // Attached file
            if (dispatch.attachedFile != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = AccentGreen,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "Arquivo: ${dispatch.attachedFile.fileName} (${DispatchRepository.formatBytes(dispatch.attachedFile.fileSize)})",
                        color = AccentGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = MutedText,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "Sem arquivo anexado (defina ao editar)",
                        color = MutedText,
                        fontSize = 12.sp
                    )
                }
            }

            // Anti-duplication badge
            if (dispatch.preventDuplicates) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = AccentGreen,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Anti-duplicado ativo (substitui mesmo nome)",
                        color = AccentGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Caption preview
            if (dispatch.caption.isNotBlank()) {
                Text(
                    text = dispatch.caption,
                    color = MutedText,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Status message
            val statusMsg = if (!dispatch.active) {
                "Pausado"
            } else if (dispatch.lastRunHM != null) {
                "✓ Último envio: ${dispatch.lastRunHM}"
            } else {
                "Aguardando agendamento"
            }
            Text(
                text = statusMsg,
                color = if (dispatch.active) AccentGreen else MutedText,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )

            // Card Action Buttons
            HorizontalDivider(color = LineBorder, thickness = 1.dp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f).height(40.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = AccentGreen
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AccentBorder)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Editar",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Editar", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onTest,
                    modifier = Modifier.height(40.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SurfaceVariantDark,
                        contentColor = ForegroundLight
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Testar",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Testar", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
