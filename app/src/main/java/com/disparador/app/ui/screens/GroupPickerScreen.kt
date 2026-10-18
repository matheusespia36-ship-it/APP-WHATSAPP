package com.disparador.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.disparador.app.model.WhatsAppGroup
import com.disparador.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupPickerScreen(
    groups: List<WhatsAppGroup>,
    initialSelected: List<WhatsAppGroup>,
    onConfirmGroups: (List<WhatsAppGroup>) -> Unit,
    onManualJid: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var manualJidInput by remember { mutableStateOf("") }
    val selectedIds = remember { mutableStateMapOf<String, Boolean>().apply {
        initialSelected.forEach { this[it.id] = true }
    } }

    val filtered = remember(groups, searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) groups
        else groups.filter { it.name.lowercase().contains(q) || it.id.lowercase().contains(q) }
    }

    val selectedCount = selectedIds.count { it.value }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = BgDark,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Escolher Grupos ($selectedCount)",
                        color = ForegroundLight,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("group_picker_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = ForegroundLight
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val selectedList = groups.filter { selectedIds[it.id] == true }
                            onConfirmGroups(selectedList)
                        },
                        modifier = Modifier.testTag("confirm_groups_selection")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Confirmar",
                            tint = AccentGreen
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgDark)
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = BgDark,
                border = androidx.compose.foundation.BorderStroke(1.dp, LineBorder)
            ) {
                Box(modifier = Modifier.padding(16.dp)) {
                    Button(
                        onClick = {
                            val selectedList = groups.filter { selectedIds[it.id] == true }
                            onConfirmGroups(selectedList)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("confirm_groups_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentGreen,
                            contentColor = AccentInk
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (selectedCount == 0) "Concluir (nenhum grupo)" else "Confirmar Seleção ($selectedCount grupo${if (selectedCount > 1) "s" else ""})",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 18.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().testTag("search_groups_input"),
                placeholder = { Text("Buscar grupo por nome ou JID...", color = MutedText, fontSize = 13.sp) },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = MutedText) },
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

            // Select All / Clear Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$selectedCount de ${groups.size} selecionado(s)",
                    color = MutedText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = { groups.forEach { selectedIds[it.id] = true } },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("Selecionar Todos", color = AccentGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    TextButton(
                        onClick = { selectedIds.clear() },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("Limpar", color = MutedText, fontSize = 12.sp)
                    }
                }
            }

            // Manual JID Box
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = SurfaceDark,
                border = androidx.compose.foundation.BorderStroke(1.dp, LineBorder)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Ou adicionar JID manualmente à seleção:",
                        color = MutedText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = manualJidInput,
                            onValueChange = { manualJidInput = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Ex: 120363024891100@g.us", color = MutedText, fontSize = 12.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
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
                            onClick = {
                                if (manualJidInput.isNotBlank()) {
                                    onManualJid(manualJidInput)
                                    manualJidInput = ""
                                }
                            },
                            modifier = Modifier.height(44.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SurfaceVariantDark,
                                contentColor = AccentGreen
                            )
                        ) {
                            Text("Adicionar", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            // List of groups with Checkbox
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                items(filtered, key = { it.id }) { group ->
                    val isChecked = selectedIds[group.id] == true
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                if (isChecked) selectedIds.remove(group.id)
                                else selectedIds[group.id] = true
                            },
                        color = if (isChecked) SurfaceVariantDark else SurfaceDark,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isChecked) AccentBorder else LineBorder
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        if (checked) selectedIds[group.id] = true
                                        else selectedIds.remove(group.id)
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = AccentGreen,
                                        checkmarkColor = AccentInk,
                                        uncheckedColor = MutedText
                                    )
                                )
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(if (isChecked) AccentGreen.copy(alpha = 0.25f) else AccentSoft),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = group.name.firstOrNull()?.uppercase() ?: "#",
                                        color = AccentGreen,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp
                                    )
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = group.name,
                                        color = ForegroundLight,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${group.participants} membros · ${group.id}",
                                        color = MutedText,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
