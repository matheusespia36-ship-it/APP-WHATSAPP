package com.disparador.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.disparador.app.model.LogEntry
import com.disparador.app.model.LogType
import com.disparador.app.model.WhatsAppStatus
import com.disparador.app.ui.components.QRCodeVisual
import com.disparador.app.ui.components.StatusBadge
import com.disparador.app.ui.theme.*

@Composable
fun ConnectionScreen(
    waStatus: WhatsAppStatus,
    pairingCode: String,
    serverUrl: String,
    apiToken: String,
    logs: List<LogEntry>,
    backgroundPriorityEnabled: Boolean,
    onToggleBackgroundPriority: (Boolean) -> Unit,
    onRequestBatteryOptimization: () -> Unit,
    onRequestPairCode: (String) -> Unit,
    onResetSession: () -> Unit,
    onUpdateToken: (String) -> Unit,
    onUpdateServerUrl: (String) -> Unit,
    onTestConnection: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    var phoneInput by remember { mutableStateOf("") }
    var tokenVisible by remember { mutableStateOf(false) }
    var showEditTokenDialog by remember { mutableStateOf(false) }
    var showEditUrlDialog by remember { mutableStateOf(false) }
    var tokenEditValue by remember { mutableStateOf(apiToken) }
    var urlEditValue by remember { mutableStateOf(serverUrl) }

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
                    text = "Conexão WhatsApp",
                    color = ForegroundLight,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp
                )
                StatusBadge(status = waStatus)
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Main Status Banner
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SurfaceDark,
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, LineBorder)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(AccentSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Wifi,
                            contentDescription = null,
                            tint = AccentGreen,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = if (waStatus == WhatsAppStatus.OPEN) "Conectado e Operacional" else "Aguardando Pareamento Baileys",
                            color = ForegroundLight,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Motor Baileys com suporte a QR Code e Código de Telefone",
                            color = MutedText,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Background Priority & Android Protection Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SurfaceDark,
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (backgroundPriorityEnabled) AccentBorder else LineBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (backgroundPriorityEnabled) AccentSoft else SurfaceVariantDark),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = if (backgroundPriorityEnabled) AccentGreen else MutedText,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Prioridade Total em Segundo Plano",
                                    color = ForegroundLight,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = if (backgroundPriorityEnabled) "Serviço ativo: Android não fechará o app" else "Padrão: Android pode suspender o app",
                                    color = if (backgroundPriorityEnabled) AccentGreen else MutedText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        Switch(
                            checked = backgroundPriorityEnabled,
                            onCheckedChange = { onToggleBackgroundPriority(it) },
                            modifier = Modifier.testTag("toggle_background_priority"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AccentInk,
                                checkedTrackColor = AccentGreen,
                                uncheckedThumbColor = MutedText,
                                uncheckedTrackColor = LineBorder
                            )
                        )
                    }

                    Text(
                        text = "Mantém o motor Baileys, o verificador de agendamentos e o anti-duplicado executando sem interrupções 24h por dia, mesmo com a tela apagada ou celular em repouso.",
                        color = MutedText,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )

                    Button(
                        onClick = onRequestBatteryOptimization,
                        modifier = Modifier.fillMaxWidth().height(38.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SurfaceVariantDark,
                            contentColor = ForegroundLight
                        )
                    ) {
                        Icon(imageVector = Icons.Default.BatteryChargingFull, contentDescription = null, modifier = Modifier.size(16.dp), tint = AccentGreen)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Solicitar Isenção de Otimização de Bateria",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // If not connected: QR Code card & phone pairing
            if (waStatus != WhatsAppStatus.OPEN) {
                // QR Box
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = SurfaceDark,
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, LineBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Pareamento via QR Code",
                            color = ForegroundLight,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        QRCodeVisual(sizeDp = 180)
                        Text(
                            text = "Abra o WhatsApp > Dispositivos Conectados > Conectar aparelho",
                            color = MutedText,
                            fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = onTestConnection,
                                modifier = Modifier.height(36.dp),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, LineBorderStrong)
                            ) {
                                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Recarregar QR", fontSize = 12.sp, color = ForegroundLight)
                            }
                            Button(
                                onClick = onResetSession,
                                modifier = Modifier.height(36.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = DangerSoft, contentColor = DangerRed)
                            ) {
                                Text("Resetar Sessão", fontSize = 12.sp)
                            }
                        }
                    }
                }

                // Phone Pairing Code Box
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = SurfaceDark,
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, LineBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Ou Conectar por Código de Telefone",
                            color = ForegroundLight,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = phoneInput,
                                onValueChange = { phoneInput = it },
                                modifier = Modifier.weight(1f).testTag("phone_pairing_input"),
                                placeholder = { Text("Ex: 5511999998888", color = MutedText, fontSize = 13.sp) },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = ForegroundLight,
                                    unfocusedTextColor = ForegroundLight,
                                    focusedBorderColor = AccentGreen,
                                    unfocusedBorderColor = LineBorder,
                                    focusedContainerColor = SurfaceVariantDark,
                                    unfocusedContainerColor = SurfaceVariantDark
                                )
                            )
                            Button(
                                onClick = { onRequestPairCode(phoneInput) },
                                modifier = Modifier.height(48.dp).testTag("generate_pair_code_button"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SurfaceVariantDark, contentColor = AccentGreen)
                            ) {
                                Icon(imageVector = Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Gerar Código", fontWeight = FontWeight.Bold)
                            }
                        }

                        if (pairingCode.isNotBlank()) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = AccentSoft,
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, AccentBorder)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Código de Pareamento:", color = MutedText, fontSize = 11.sp)
                                        Text(
                                            text = pairingCode,
                                            color = AccentGreen,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 20.sp,
                                            fontFamily = FontFamily.Monospace,
                                            letterSpacing = 2.sp
                                        )
                                    }
                                    IconButton(onClick = { clipboardManager.setText(AnnotatedString(pairingCode)) }) {
                                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copiar", tint = AccentGreen)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Active session box
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = SurfaceDark,
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, LineBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Sessão Ativa",
                            color = ForegroundLight,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "O WhatsApp está autenticado e pronto para despachar mensagens agendadas e relatórios.",
                            color = MutedText,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = onResetSession,
                            modifier = Modifier.height(38.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DangerSoft, contentColor = DangerRed)
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Desconectar e Ler Novo QR Code", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Server Base URL Box
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SurfaceDark,
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, LineBorder)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(text = "URL Base do Servidor", color = MutedText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = serverUrl,
                            color = ForegroundLight,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.weight(1f)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            OutlinedButton(
                                onClick = {
                                    urlEditValue = serverUrl
                                    showEditUrlDialog = true
                                },
                                modifier = Modifier.height(32.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, LineBorderStrong)
                            ) {
                                Text("Editar", fontSize = 11.sp, color = ForegroundLight)
                            }
                            IconButton(
                                onClick = { clipboardManager.setText(AnnotatedString(serverUrl)) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copiar", tint = MutedText, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }

            // API Token Box
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SurfaceDark,
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, LineBorder)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(text = "Token de Acesso da API (Header: x-token)", color = MutedText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (tokenVisible) apiToken else "••••••••••••",
                            color = ForegroundLight,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.weight(1f)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            OutlinedButton(
                                onClick = { tokenVisible = !tokenVisible },
                                modifier = Modifier.height(32.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, LineBorderStrong)
                            ) {
                                Text(if (tokenVisible) "Ocultar" else "Mostrar", fontSize = 11.sp, color = ForegroundLight)
                            }
                            OutlinedButton(
                                onClick = {
                                    tokenEditValue = apiToken
                                    showEditTokenDialog = true
                                },
                                modifier = Modifier.height(32.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, LineBorderStrong)
                            ) {
                                Text("Editar", fontSize = 11.sp, color = ForegroundLight)
                            }
                            IconButton(
                                onClick = { clipboardManager.setText(AnnotatedString(apiToken)) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copiar", tint = MutedText, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }

            // Live Event Logger
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SurfaceDark,
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, LineBorder)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Registrador de Eventos / Logs Recentes",
                        color = ForegroundLight,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(BgOuter)
                            .border(1.dp, LineBorder, RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        if (logs.isEmpty()) {
                            Text("Aguardando atividades...", color = MutedText, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                items(logs, key = { it.id }) { log ->
                                    val textColor = when (log.type) {
                                        LogType.OK -> AccentGreen
                                        LogType.ERROR -> DangerRed
                                        LogType.NORMAL -> ForegroundLight
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("[${log.time}]", color = MutedText, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                        Text(log.message, color = textColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Quick Manual Connection Test
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SurfaceDark,
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, LineBorder)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(text = "Disparo de Teste Manual Rápido", color = ForegroundLight, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(text = "Verifique a comunicação com o servidor e WhatsApp.", color = MutedText, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = onTestConnection,
                        modifier = Modifier.fillMaxWidth().height(42.dp).testTag("test_backend_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceVariantDark, contentColor = AccentGreen)
                    ) {
                        Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Executar Teste de Conexão", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }

    // Edit Token Dialog
    if (showEditTokenDialog) {
        AlertDialog(
            onDismissRequest = { showEditTokenDialog = false },
            title = { Text("Editar Token de Acesso", color = ForegroundLight, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = tokenEditValue,
                    onValueChange = { tokenEditValue = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = ForegroundLight,
                        unfocusedTextColor = ForegroundLight,
                        focusedBorderColor = AccentGreen,
                        unfocusedBorderColor = LineBorder
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateToken(tokenEditValue)
                        showEditTokenDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen, contentColor = AccentInk)
                ) {
                    Text("Salvar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditTokenDialog = false }) {
                    Text("Cancelar", color = MutedText)
                }
            },
            containerColor = SurfaceDark
        )
    }

    // Edit URL Dialog
    if (showEditUrlDialog) {
        AlertDialog(
            onDismissRequest = { showEditUrlDialog = false },
            title = { Text("Editar URL Base", color = ForegroundLight, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = urlEditValue,
                    onValueChange = { urlEditValue = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = ForegroundLight,
                        unfocusedTextColor = ForegroundLight,
                        focusedBorderColor = AccentGreen,
                        unfocusedBorderColor = LineBorder
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateServerUrl(urlEditValue)
                        showEditUrlDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen, contentColor = AccentInk)
                ) {
                    Text("Salvar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditUrlDialog = false }) {
                    Text("Cancelar", color = MutedText)
                }
            },
            containerColor = SurfaceDark
        )
    }
}
