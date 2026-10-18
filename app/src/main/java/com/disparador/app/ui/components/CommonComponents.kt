package com.disparador.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.disparador.app.model.WhatsAppStatus
import com.disparador.app.ui.theme.*

@Composable
fun StatusBadge(status: WhatsAppStatus, modifier: Modifier = Modifier) {
    val (text, color, bg) = when (status) {
        WhatsAppStatus.OPEN -> Triple("Conectado", AccentGreen, AccentSoft)
        WhatsAppStatus.NEEDS_PAIRING -> Triple("Aguardando QR", WarningYellow, WarningSoft)
        WhatsAppStatus.CONNECTING -> Triple("Conectando...", MutedText, SurfaceVariantDark)
        WhatsAppStatus.OFFLINE -> Triple("Offline", DangerRed, DangerSoft)
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = text,
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun StatCard(
    activeCount: Int,
    totalCount: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = SurfaceDark,
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
                Text(
                    text = "⚡",
                    fontSize = 20.sp
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "$activeCount disparos automáticos ativos",
                    color = ForegroundLight,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = "$totalCount configurados no total",
                    color = MutedText,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun QRCodeVisual(
    modifier: Modifier = Modifier,
    sizeDp: Int = 180
) {
    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val cells = 15
            val cellW = canvasWidth / cells
            val cellH = canvasHeight / cells

            // Seeded pattern resembling WhatsApp QR Code
            val darkCells = setOf(
                Pair(0,0), Pair(0,1), Pair(0,2), Pair(0,3), Pair(0,4),
                Pair(4,0), Pair(4,4), Pair(1,0), Pair(2,0), Pair(3,0),
                Pair(1,4), Pair(2,4), Pair(3,4), Pair(2,2),
                Pair(10,0), Pair(11,0), Pair(12,0), Pair(13,0), Pair(14,0),
                Pair(10,4), Pair(14,4), Pair(10,1), Pair(10,2), Pair(10,3),
                Pair(14,1), Pair(14,2), Pair(14,3), Pair(12,2),
                Pair(0,10), Pair(1,10), Pair(2,10), Pair(3,10), Pair(4,10),
                Pair(0,14), Pair(4,14), Pair(0,11), Pair(0,12), Pair(0,13),
                Pair(4,11), Pair(4,12), Pair(4,13), Pair(2,12),
                Pair(6,2), Pair(7,2), Pair(8,2), Pair(6,4), Pair(7,5), Pair(8,6),
                Pair(6,7), Pair(7,7), Pair(8,7), Pair(6,9), Pair(7,10), Pair(8,11),
                Pair(10,7), Pair(11,8), Pair(12,9), Pair(13,10), Pair(14,11),
                Pair(2,7), Pair(3,8), Pair(4,9), Pair(10,13), Pair(11,13), Pair(12,13)
            )

            for (r in 0 until cells) {
                for (c in 0 until cells) {
                    if (darkCells.contains(Pair(r, c)) || ((r * 7 + c * 11) % 3 == 0 && (r in 5..9 || c in 5..9))) {
                        drawRect(
                            color = Color(0xFF0C1511),
                            topLeft = Offset(c * cellW, r * cellH),
                            size = Size(cellW - 0.5f, cellH - 0.5f)
                        )
                    }
                }
            }
        }
    }
}
