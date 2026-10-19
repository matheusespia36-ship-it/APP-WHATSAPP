package com.disparador.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.disparador.app.model.AppScreen
import com.disparador.app.node.NodeBridge
import com.disparador.app.service.SchedulerForegroundService
import com.disparador.app.ui.screens.*
import com.disparador.app.ui.theme.*
import com.disparador.app.ui.viewmodel.MainViewModel
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Inicializa o motor Node.js nativo na abertura do aplicativo[span_0](start_span)[span_0](end_span)
        NodeBridge.start(this)

        setContent {
            DisparadorTheme {
                MainAppContent(viewModel = viewModel)
            }
        }
    }
}


@Composable
fun MainAppContent(viewModel: MainViewModel) {
    val context = LocalContext.current
    val currentScreen by viewModel.currentScreen.collectAsState()
    val dispatches by viewModel.dispatches.collectAsState()
    val history by viewModel.history.collectAsState()
    val groups by viewModel.groups.collectAsState()
    val waStatus by viewModel.waStatus.collectAsState()
    val logs by viewModel.logs.collectAsState()
    val serverUrl by viewModel.serverUrl.collectAsState()
    val apiToken by viewModel.apiToken.collectAsState()
    val pairingCode by viewModel.pairingCode.collectAsState()
    val backgroundPriorityEnabled by viewModel.backgroundPriorityEnabled.collectAsState()
    val draftDispatch by viewModel.draftDispatch.collectAsState()
    val isEditing by viewModel.isEditing.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()

    // Request Notification permission for Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.showToast("Permissão de notificações concedida para 2º plano")
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        // Auto-start foreground service if priority is enabled
        if (backgroundPriorityEnabled) {
            SchedulerForegroundService.start(context)
        }
    }

    // Handle system back navigation
    BackHandler(enabled = currentScreen != AppScreen.HOME) {
        when (currentScreen) {
            AppScreen.PICK_GROUP -> viewModel.navigateTo(AppScreen.EDIT_DISPATCH)
            AppScreen.EDIT_DISPATCH -> viewModel.navigateTo(AppScreen.HOME)
            AppScreen.HISTORY, AppScreen.CONNECTION -> viewModel.navigateTo(AppScreen.HOME)
            AppScreen.HOME -> { /* system handles exiting */ }
        }
    }

    // Auto-dismiss toast
    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            delay(2800L)
            viewModel.clearToast()
        }
    }

    val showBottomNav = currentScreen in listOf(AppScreen.HOME, AppScreen.HISTORY, AppScreen.CONNECTION)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = BgDark,
        bottomBar = {
            if (showBottomNav) {
                NavigationBar(
                    containerColor = BgDark,
                    tonalElevation = 0.dp,
                    modifier = Modifier.border(width = 1.dp, color = LineBorder, shape = RoundedCornerShape(0.dp))
                ) {
                    NavigationBarItem(
                        selected = currentScreen == AppScreen.HOME,
                        onClick = { viewModel.navigateTo(AppScreen.HOME) },
                        icon = { Icon(imageVector = Icons.Default.Send, contentDescription = "Disparos") },
                        label = { Text("Disparos", fontWeight = if (currentScreen == AppScreen.HOME) FontWeight.Bold else FontWeight.Normal) },
                        modifier = Modifier.testTag("nav_disparos"),
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AccentInk,
                            selectedTextColor = AccentGreen,
                            indicatorColor = AccentGreen,
                            unselectedIconColor = MutedText,
                            unselectedTextColor = MutedText
                        )
                    )

                    NavigationBarItem(
                        selected = currentScreen == AppScreen.HISTORY,
                        onClick = { viewModel.navigateTo(AppScreen.HISTORY) },
                        icon = { Icon(imageVector = Icons.Default.List, contentDescription = "Histórico") },
                        label = { Text("Histórico", fontWeight = if (currentScreen == AppScreen.HISTORY) FontWeight.Bold else FontWeight.Normal) },
                        modifier = Modifier.testTag("nav_historico"),
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AccentInk,
                            selectedTextColor = AccentGreen,
                            indicatorColor = AccentGreen,
                            unselectedIconColor = MutedText,
                            unselectedTextColor = MutedText
                        )
                    )

                    NavigationBarItem(
                        selected = currentScreen == AppScreen.CONNECTION,
                        onClick = { viewModel.navigateTo(AppScreen.CONNECTION) },
                        icon = { Icon(imageVector = Icons.Default.Wifi, contentDescription = "Conexão") },
                        label = { Text("Conexão", fontWeight = if (currentScreen == AppScreen.CONNECTION) FontWeight.Bold else FontWeight.Normal) },
                        modifier = Modifier.testTag("nav_conexao"),
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AccentInk,
                            selectedTextColor = AccentGreen,
                            indicatorColor = AccentGreen,
                            unselectedIconColor = MutedText,
                            unselectedTextColor = MutedText
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                AppScreen.HOME -> {
                    HomeScreen(
                        dispatches = dispatches,
                        waStatus = waStatus,
                        onNewDispatch = { viewModel.startNewDispatch() },
                        onEditDispatch = { slug -> viewModel.editDispatch(slug) },
                        onToggleActive = { slug -> viewModel.toggleDispatchActive(slug) },
                        onTestDispatch = { slug -> viewModel.testDispatch(slug) },
                        onRefresh = { viewModel.showToast("Lista atualizada") }
                    )
                }
                AppScreen.HISTORY -> {
                    HistoryScreen(
                        history = history,
                        onClearHistory = { viewModel.clearHistory() }
                    )
                }
                AppScreen.CONNECTION -> {
                    ConnectionScreen(
                        waStatus = waStatus,
                        pairingCode = pairingCode,
                        serverUrl = serverUrl,
                        apiToken = apiToken,
                        logs = logs,
                        backgroundPriorityEnabled = backgroundPriorityEnabled,
                        onToggleBackgroundPriority = { enabled ->
                            viewModel.toggleBackgroundPriority(enabled, context)
                        },
                        onRequestBatteryOptimization = {
                            try {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                                    if (!pm.isIgnoringBatteryOptimizations(context.packageName)) {
                                        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                            data = Uri.parse("package:${context.packageName}")
                                        }
                                        context.startActivity(intent)
                                    } else {
                                        viewModel.showToast("Isenção de bateria já concedida! Prioridade total ativa.")
                                    }
                                } else {
                                    viewModel.showToast("Prioridade total já habilitada pelo sistema.")
                                }
                            } catch (e: Exception) {
                                try {
                                    val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                    context.startActivity(intent)
                                } catch (ex: Exception) {
                                    viewModel.showToast("Acesse Configurações > Bateria > Sem restrições")
                                }
                            }
                        },
                        onRequestPairCode = { phone -> viewModel.requestPairCode(phone) },
                        onResetSession = { viewModel.resetSession() },
                        onUpdateToken = { token -> viewModel.updateToken(token) },
                        onUpdateServerUrl = { url -> viewModel.updateServerUrl(url) },
                        onTestConnection = { viewModel.testConnection() }
                    )
                }
                AppScreen.EDIT_DISPATCH -> {
                    draftDispatch?.let { draft ->
                        DispatchEditScreen(
                            dispatch = draft,
                            isEditing = isEditing,
                            onUpdateDraft = { updater -> viewModel.updateDraft(updater) },
                            onSave = { viewModel.saveDraft() },
                            onDelete = { slug -> viewModel.deleteDispatch(slug) },
                            onBack = { viewModel.navigateTo(AppScreen.HOME) },
                            onPickGroup = { viewModel.navigateTo(AppScreen.PICK_GROUP) },
                            onAttachSample = { type -> viewModel.attachSampleReport(type) },
                            onRemoveFile = { viewModel.removeDraftFile() },
                            onTestNow = { viewModel.testDispatch(draft.slug, force = true) }
                        )
                    }
                }
                AppScreen.PICK_GROUP -> {
                    GroupPickerScreen(
                        groups = groups,
                        initialSelected = draftDispatch?.effectiveGroups ?: emptyList(),
                        onConfirmGroups = { selected ->
                            viewModel.setDraftTargetGroups(selected)
                        },
                        onManualJid = { jid -> viewModel.addManualGroup(name = "Grupo Personalizado", jid = jid) },
                        onBack = { viewModel.navigateTo(AppScreen.EDIT_DISPATCH) }
                    )
                }
            }

            // In-app Floating Toast
            AnimatedVisibility(
                visible = toastMessage != null,
                enter = fadeIn() + slideInVertically(initialOffsetY = { 40 }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { 40 }),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 20.dp, start = 20.dp, end = 20.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = ForegroundLight,
                    shadowElevation = 8.dp
                ) {
                    Text(
                        text = toastMessage ?: "",
                        color = BgOuter,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)
                    )
                }
            }
        }
    }
}
