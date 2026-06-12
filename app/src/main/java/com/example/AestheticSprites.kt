package com.example

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

object AestheticSprites {

    // Beautiful procedural galaxy constellation canvas for TV backgrounds/heros
    @Composable
    fun ConstellationMesh(
        modifier: Modifier = Modifier,
        isDarkTheme: Boolean = true
    ) {
        val transition = rememberInfiniteTransition(label = "ConstellationTransition")
        
        // Simulates continuous system heartbeat & client handshakes
        val animPhase1 by transition.animateFloat(
            initialValue = 0f,
            targetValue = (2 * Math.PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(14000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "Phase1"
        )
        
        val animPhase2 by transition.animateFloat(
            initialValue = 0f,
            targetValue = (2 * Math.PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(18000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "Phase2"
        )

        val starColor = if (isDarkTheme) Color(0xFF007AFF).copy(alpha = 0.5f) else Color(0xFF0051FF).copy(alpha = 0.35f)
        val lineColor = if (isDarkTheme) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)
        val activeGlowColor = if (isDarkTheme) Color(0xFF00F260).copy(alpha = 0.7f) else Color(0xFF30D158).copy(alpha = 0.61f)

        Canvas(modifier = modifier) {
            val width = size.width
            val height = size.height

            if (width < 10f || height < 10f) return@Canvas

            // Custom constellation anchors
            val baseNodes = listOf(
                Offset(width * 0.12f, height * 0.28f),
                Offset(width * 0.32f, height * 0.14f),
                Offset(width * 0.52f, height * 0.35f),
                Offset(width * 0.78f, height * 0.18f),
                Offset(width * 0.24f, height * 0.62f),
                Offset(width * 0.45f, height * 0.82f),
                Offset(width * 0.68f, height * 0.52f),
                Offset(width * 0.86f, height * 0.75f),
                Offset(width * 0.94f, height * 0.28f),
                Offset(width * 0.06f, height * 0.78f),
                Offset(width * 0.58f, height * 0.10f),
                Offset(width * 0.35f, height * 0.50f)
            )

            // Dynamic sine displacement for elegant organic liquid tracking
            val nodes = baseNodes.mapIndexed { idx, point ->
                val phase = if (idx % 2 == 0) animPhase1 else animPhase2
                val displacementX = 14f * cos(phase + idx * 1.3)
                val displacementY = 12f * sin(phase + idx * 1.7)
                Offset(point.x + displacementX.toFloat(), point.y + displacementY.toFloat())
            }

            // Draw line webs
            for (i in nodes.indices) {
                for (j in i + 1 until nodes.size) {
                    val distSq = (nodes[i].x - nodes[j].x) * (nodes[i].x - nodes[j].x) + 
                                 (nodes[i].y - nodes[j].y) * (nodes[i].y - nodes[j].y)
                    val maxDist = width * 0.32f
                    if (distSq < maxDist * maxDist) {
                        val dist = kotlin.math.sqrt(distSq)
                        val opacityGamma = (1f - dist / maxDist).coerceIn(0f, 1f)
                        drawLine(
                            color = lineColor.copy(alpha = lineColor.alpha * opacityGamma),
                            start = nodes[i],
                            end = nodes[j],
                            strokeWidth = 1.2.dp.toPx()
                        )
                    }
                }
            }

            // Render node clusters
            nodes.forEachIndexed { index, node ->
                val isGlowNode = index % 4 == 0
                val color = if (isGlowNode) activeGlowColor else starColor
                val radius = if (isGlowNode) 5.dp.toPx() else 3.5.dp.toPx()

                if (isGlowNode) {
                    drawCircle(
                        color = color.copy(alpha = 0.15f),
                        radius = radius * 3.5f,
                        center = node
                    )
                    drawCircle(
                        color = color.copy(alpha = 0.40f),
                        radius = radius * 1.8f,
                        center = node
                    )
                }

                drawCircle(
                    color = color,
                    radius = radius,
                    center = node
                )
            }
        }
    }

    // Gorgeous abstract cyber category card texture drawing behind icons
    @Composable
    fun CyberCategoryDecoration(
        modifier: Modifier = Modifier,
        tintColor: Color,
        isDarkTheme: Boolean
    ) {
        val strokeColor = tintColor.copy(alpha = if (isDarkTheme) 0.08f else 0.05f)
        Canvas(modifier = modifier) {
            val w = size.width
            val h = size.height

            // Abstract parallel tech vectors
            val strokePx = 1.dp.toPx()
            
            // Background mesh horizontal lines
            for (y in 0..h.toInt() step (h / 4).toInt().coerceAtLeast(10)) {
                drawLine(
                    color = strokeColor,
                    start = Offset(0f, y.toFloat()),
                    end = Offset(w, y.toFloat()),
                    strokeWidth = strokePx
                )
            }
            
            // Abstract futuristic circular arcs
            drawCircle(
                color = tintColor.copy(alpha = 0.03f),
                radius = w * 0.45f,
                center = Offset(w * 0.85f, h * 0.85f),
                style = Stroke(width = 2.dp.toPx())
            )
            
            drawCircle(
                color = tintColor.copy(alpha = 0.015f),
                radius = w * 0.70f,
                center = Offset(w * 0.85f, h * 0.85f),
                style = Stroke(width = 1.dp.toPx())
            )

            // Draw clean diagnostic corner indicators
            val markerLen = w * 0.08f
            // Top-left corner bracket
            drawLine(strokeColor, Offset(4.dp.toPx(), 4.dp.toPx()), Offset(4.dp.toPx() + markerLen, 4.dp.toPx()), strokePx)
            drawLine(strokeColor, Offset(4.dp.toPx(), 4.dp.toPx()), Offset(4.dp.toPx(), 4.dp.toPx() + markerLen), strokePx)

            // Bottom-right corner bracket
            drawLine(strokeColor, Offset(w - 4.dp.toPx(), h - 4.dp.toPx()), Offset(w - 4.dp.toPx() - markerLen, h - 4.dp.toPx()), strokePx)
            drawLine(strokeColor, Offset(w - 4.dp.toPx(), h - 4.dp.toPx()), Offset(w - 4.dp.toPx(), h - 4.dp.toPx() - markerLen), strokePx)
        }
    }

    // Procedural TV Dashboard Banner Component (replacing flat colors with a high-end dynamic visual)
    @Composable
    fun TvDashboardHeroBanner(
        isDarkTheme: Boolean = true,
        modifier: Modifier = Modifier
    ) {
        val primaryBrush = Brush.linearGradient(
            colors = if (isDarkTheme) {
                listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364))
            } else {
                listOf(Color(0xFFE2E8F0), Color(0xFFCBD5E1), Color(0xFF94A3B8))
            }
        )

        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(primaryBrush)
                .border(
                    width = 1.5.dp,
                    color = if (isDarkTheme) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f),
                    shape = RoundedCornerShape(20.dp)
                )
        ) {
            // Live animated server background
            ConstellationMesh(
                modifier = Modifier.fillMaxSize(),
                isDarkTheme = isDarkTheme
            )

            // Cyber decoration overlay
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 28.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(0.65f)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isDarkTheme) Color(0xFF0A84FF).copy(alpha = 0.2f) else Color(0xFF0A84FF).copy(alpha = 0.15f))
                            .border(1.dp, Color(0xFF0A84FF), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "NEXUS GLASS ENGINE v1.2.0",
                            color = Color(0xFF0A84FF),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Gerenciador de Arquivos Premium",
                        color = if (isDarkTheme) Color.White else Color(0xFF1E293B),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Compartilhe mídias, transfira APKs, transmita streams e monitore a telemetria do sistema em alta fidelidade.",
                        color = if (isDarkTheme) Color.LightGray.copy(alpha = 0.85f) else Color(0xFF475569),
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        maxLines = 2
                    )
                }

                // Glassmorphic side cluster displaying system metrics beautifully
                Box(
                    modifier = Modifier
                        .weight(0.35f)
                        .fillMaxHeight(0.78f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isDarkTheme) Color.White.copy(alpha = 0.04f) else Color.Black.copy(alpha = 0.03f))
                        .border(1.dp, if (isDarkTheme) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Active",
                                tint = Color(0xFF30D158),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "TV ATIVA",
                                color = if (isDarkTheme) Color.White else Color(0xFF1E293B),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                        
                        Divider(color = if (isDarkTheme) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.05f))

                        Text(
                            text = "Navegue pelo Explorer central ou emparelhe via aplicativo de QR Code compartilhando seu endereço Wi-Fi.",
                            color = if (isDarkTheme) Color.Gray else Color.DarkGray,
                            fontSize = 9.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 12.sp
                        )
                    }
                }
            }
        }
    }

    // High fidelity glassmorphic folder rendering
    @Composable
    fun ProceduralFolderIcon(
        modifier: Modifier = Modifier,
        isDarkTheme: Boolean = true
    ) {
        Canvas(modifier = modifier) {
            val w = size.width
            val h = size.height

            // Gorgeous neon dual-tone folder palette
            val folderColor = Color(0xFF0056D2)
            val frontLipColor = Color(0xFF007AFF)
            val tabColor = Color(0xFF5AC8FA)
            val shadowColor = Color(0x3C000000)

            // 1. Back Tab
            drawRoundRect(
                color = tabColor,
                topLeft = Offset(w * 0.12f, h * 0.12f),
                size = Size(w * 0.36f, h * 0.20f),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )

            // 2. Main Folder Background
            drawRoundRect(
                color = folderColor,
                topLeft = Offset(w * 0.05f, h * 0.26f),
                size = Size(w * 0.90f, h * 0.64f),
                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
            )

            // 3. Inner shadow line
            drawRoundRect(
                color = shadowColor,
                topLeft = Offset(w * 0.05f, h * 0.32f),
                size = Size(w * 0.90f, h * 0.08f),
                cornerRadius = CornerRadius(0f)
            )

            // 4. Front glass lip overlay structure (creates visual depth)
            drawRoundRect(
                color = frontLipColor,
                topLeft = Offset(w * 0.05f, h * 0.38f),
                size = Size(w * 0.90f, h * 0.52f),
                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
            )

            // 5. High light reflective edge
            drawLine(
                color = Color.White.copy(alpha = 0.25f),
                start = Offset(w * 0.05f, h * 0.40f),
                end = Offset(w * 0.95f, h * 0.40f),
                strokeWidth = 1.5.dp.toPx()
            )
        }
    }

    // Procedural multi-category file icon system (creates rich file avatars with fine detail)
    @Composable
    fun ProceduralFileIcon(
        modifier: Modifier = Modifier,
        extension: String,
        isDarkTheme: Boolean = true
    ) {
        val ext = extension.lowercase()

        // Detailed premium visual pairing for file categories
        val (themeColor, iconVector) = when (ext) {
            "mp4", "mkv", "avi", "mov", "m3u8", "webm", "flv", "3gp", "ts" -> Pair(Color(0xFFFE2C55), Icons.Default.PlayArrow) // Hot Coral Red
            "mp3", "wav", "flac", "ogg", "m4a", "aac", "mid" -> Pair(Color(0xFF9F4FFF), Icons.Default.Star) // Electric Purple
            "apk", "jar" -> Pair(Color(0xFF00FF87), Icons.Default.Build) // Hyper Green
            "zip", "rar", "tar", "gz", "7z" -> Pair(Color(0xFFFF9F0A), Icons.Default.Menu) // Vivid Orange
            "pdf", "epub", "txt", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "xml", "json" -> Pair(Color(0xFF0A84FF), Icons.Default.List) // Deep Ocean Blue
            else -> Pair(if (isDarkTheme) Color(0xFFC7C7CC) else Color(0xFF8E8E93), Icons.Default.Info) // Dynamic Gray
        }

        Box(
            modifier = modifier
                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 16.dp, bottomEnd = 6.dp, bottomStart = 6.dp))
                .background(themeColor.copy(alpha = if (isDarkTheme) 0.12f else 0.08f))
                .border(
                    width = 1.5.dp,
                    color = themeColor.copy(alpha = if (isDarkTheme) 0.70f else 0.85f),
                    shape = RoundedCornerShape(topStart = 6.dp, topEnd = 16.dp, bottomEnd = 6.dp, bottomStart = 6.dp)
                )
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            // Elegant background organic waves in Canvas
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Draw technical cut corner fold
                val path = Path().apply {
                    moveTo(w * 0.75f, 0f)
                    lineTo(w, h * 0.25f)
                    lineTo(w * 0.75f, h * 0.25f)
                    close()
                }
                
                drawPath(
                    path = path,
                    color = themeColor.copy(alpha = 0.4f)
                )
                
                // Draw decorative aesthetic lines representing text inside document
                if (ext != "apk" && ext != "mp4" && ext != "mp3") {
                    drawLine(
                        color = themeColor.copy(alpha = 0.15f),
                        start = Offset(w * 0.20f, h * 0.65f),
                        end = Offset(w * 0.80f, h * 0.65f),
                        strokeWidth = 2f
                    )
                    drawLine(
                        color = themeColor.copy(alpha = 0.15f),
                        start = Offset(w * 0.20f, h * 0.78f),
                        end = Offset(w * 0.70f, h * 0.78f),
                        strokeWidth = 2f
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = iconVector,
                    contentDescription = null,
                    tint = themeColor,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = ext.uppercase().take(4),
                    color = if (isDarkTheme) Color.LightGray else Color(0xFF475569),
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
    }
}
