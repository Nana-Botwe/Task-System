package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TaskEntity
import com.example.viewmodel.TaskViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(viewModel: TaskViewModel) {
    val tasks by viewModel.tasks.collectAsState()
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    val total = tasks.size
    val todo = tasks.count { it.status == "TODO" }
    val inProgress = tasks.count { it.status == "IN_PROGRESS" }
    val review = tasks.count { it.status == "REVIEW" }
    val done = tasks.count { it.status == "DONE" || it.status == "COMPLETED" }
    
    val high = tasks.count { it.priority == "HIGH" }
    val medium = tasks.count { it.priority == "MEDIUM" }
    val low = tasks.count { it.priority == "LOW" }

    val overdue = tasks.count { it.status != "DONE" && it.dueDate < System.currentTimeMillis() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome Header Banner
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            ),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Operations Analytics Console",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Real-time task synchronization across replication nodes",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.Default.Analytics,
                    contentDescription = "Analytics Icon",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        if (total == 0) {
            EmptyOnboardingScene(onAction = { viewModel.simulateActiveUserAction() })
        }

        // Metrics Grid Row (KPI Widgets)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            maxItemsInEachRow = 2
        ) {
            KpiMetricBox(
                title = "Total Scale",
                value = total.toString(),
                unit = "tasks",
                icon = Icons.Default.Inventory,
                color = MaterialTheme.colorScheme.secondary,
                useContainerBg = true,
                modifier = Modifier.weight(1f)
            )

            KpiMetricBox(
                title = "Pipeline Active",
                value = (todo + inProgress + review).toString(),
                unit = "pending",
                icon = Icons.Default.Cached,
                color = MaterialTheme.colorScheme.primary,
                useContainerBg = false,
                modifier = Modifier.weight(1f)
            )

            KpiMetricBox(
                title = "Completed Work",
                value = done.toString(),
                unit = "resolved",
                icon = Icons.Default.TaskAlt,
                color = Color(0xFF00C853),
                useContainerBg = true,
                modifier = Modifier.weight(1f)
            )

            KpiMetricBox(
                title = "Critical Latency",
                value = overdue.toString(),
                unit = "overdue",
                icon = Icons.Default.NotificationImportant,
                color = MaterialTheme.colorScheme.error,
                useContainerBg = false,
                modifier = Modifier.weight(1f)
            )
        }

        // Analytical custom graphs section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "System Priority Distribution",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))

                if (total == 0) {
                    Text("No tasks populated to chart distribution", modifier = Modifier.padding(24.dp))
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Custom Arc Chart Drawing using Jetpack Compose Canvas
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(120.dp)
                        ) {
                            val arcColorHigh = Color(0xFFFF5252)
                            val arcColorMedium = Color(0xFFFFAB40)
                            val arcColorLow = Color(0xFF448AFF)
                            Canvas(modifier = Modifier.size(100.dp)) {
                                val totalF = total.toFloat()
                                val angleHigh = (high / totalF) * 360f
                                val angleMedium = (medium / totalF) * 360f
                                val angleLow = (low / totalF) * 360f

                                var startAngle = -90f

                                // High priority ring segment
                                if (angleHigh > 0f) {
                                    drawArc(
                                        color = arcColorHigh,
                                        startAngle = startAngle,
                                        sweepAngle = angleHigh,
                                        useCenter = false,
                                        style = Stroke(width = 24f)
                                    )
                                    startAngle += angleHigh
                                }

                                // Medium priority ring segment
                                if (angleMedium > 0f) {
                                    drawArc(
                                        color = arcColorMedium,
                                        startAngle = startAngle,
                                        sweepAngle = angleMedium,
                                        useCenter = false,
                                        style = Stroke(width = 24f)
                                    )
                                    startAngle += angleMedium
                                }

                                // Low priority ring segment
                                if (angleLow > 0f) {
                                    drawArc(
                                        color = arcColorLow,
                                        startAngle = startAngle,
                                        sweepAngle = angleLow,
                                        useCenter = false,
                                        style = Stroke(width = 24f)
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "$total", fontSize = 18.sp, fontWeight = FontWeight.Black)
                                Text(text = "TOTAL", fontSize = 8.sp, color = Color.Gray, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            }
                        }

                        // Chart Legend
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            LegendRow(label = "CRITICAL / HIGH", count = high, percentageValue = (high * 100) / total, color = Color(0xFFFF5252))
                            LegendRow(label = "MEDIUM ALERT", count = medium, percentageValue = (medium * 100) / total, color = Color(0xFFFFAB40))
                            LegendRow(label = "NORMAL / LOW", count = low, percentageValue = (low * 100) / total, color = Color(0xFF448AFF))
                        }
                    }
                }
            }
        }

        // Custom Bar Chart representation of Pipeline Board Statuses
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Workflow Stage Densities",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))

                if (total == 0) {
                    Text("No task logs registered", modifier = Modifier.padding(16.dp))
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        DashboardBarRow(label = "TO-DO PILE", count = todo, total = total, color = MaterialTheme.colorScheme.secondary)
                        DashboardBarRow(label = "IN CURRENT CYCLE", count = inProgress, total = total, color = MaterialTheme.colorScheme.primary)
                        DashboardBarRow(label = "UNDER REVIEWS", count = review, total = total, color = MaterialTheme.colorScheme.tertiary)
                        DashboardBarRow(label = "VERIFIED / DONE", count = done, total = total, color = Color(0xFF00C853))
                    }
                }
            }
        }

        // Action widgets / Exports Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Automated Document Dispatch",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Generate local files for audits or dispatch client summaries",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp, top = 2.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { viewModel.exportToCSV(context) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Output, contentDescription = "CSV", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export CSV", fontSize = 12.sp)
                    }

                    Button(
                        onClick = { viewModel.exportToPDF(context) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Auditor PDF", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { viewModel.sendWeeklySummaryEmail(context) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Email, contentDescription = "Email", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Dispatch Weekly Summary Email", fontSize = 12.sp)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun KpiMetricBox(
    title: String,
    value: String,
    unit: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    useContainerBg: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(128.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (useContainerBg) MaterialTheme.colorScheme.primaryContainer 
                             else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(24.dp),
        border = if (useContainerBg) null 
                 else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.8f)),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top: Icon
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (useContainerBg && color == MaterialTheme.colorScheme.primary) MaterialTheme.colorScheme.primary else color,
                modifier = Modifier.size(24.dp)
            )

            // Bottom: Text details
            Column {
                Row(
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = value,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Light,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (unit.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = unit,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurfaceVariantBy(useContainerBg),
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = title.uppercase(),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariantBy(useContainerBg),
                    letterSpacing = 1.2.sp
                )
            }
        }
    }
}

@Composable
fun ColorScheme.onSurfaceVariantBy(useContainerBg: Boolean): Color {
    return if (useContainerBg) this.onSurfaceVariant.copy(alpha = 0.82f) else this.onSurfaceVariant
}


@Composable
fun LegendRow(label: String, count: Int, percentageValue: Int, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "$count ($percentageValue%)",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun DashboardBarRow(label: String, count: Int, total: Int, color: Color) {
    val percentage = if (total > 0) count.toFloat() / total.toFloat() else 0f
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("$count tasks", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.outlineVariant)
        ) {
            if (percentage > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(percentage)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(color)
                )
            }
        }
    }
}

@Composable
fun EmptyOnboardingScene(onAction: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(120.dp)
            ) {
                val primaryColor = MaterialTheme.colorScheme.primary
                val tertiaryColor = MaterialTheme.colorScheme.tertiary
                Canvas(modifier = Modifier.size(100.dp)) {
                    drawRoundRect(
                        color = primaryColor,
                        topLeft = Offset(10f, 10f),
                        size = Size(80f, 80f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f),
                        style = Stroke(width = 4f)
                    )
                    drawLine(
                        color = tertiaryColor,
                        start = Offset(50f, 40f),
                        end = Offset(90f, 80f),
                        strokeWidth = 6f
                    )
                    drawCircle(color = primaryColor, radius = 8f, center = Offset(30f, 30f))
                    drawLine(color = primaryColor, start = Offset(45f, 30f), end = Offset(70f, 30f), strokeWidth = 3f)
                    
                    drawCircle(color = primaryColor, radius = 8f, center = Offset(30f, 55f))
                    drawLine(color = primaryColor, start = Offset(45f, 55f), end = Offset(70f, 55f), strokeWidth = 3f)
                }
            }
            
            Text(
                text = "Set Up Your Workspace Pipelines",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = "There are no operational task pipelines seeded yet. Let's register users, assign tasks, or use simulated nodes to jumpstart onboarding!",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp,
                modifier = Modifier.widthIn(max = 280.dp)
            )
            
            Button(
                onClick = onAction,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.GroupAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Seed Simulated Collaboration Data", fontSize = 12.sp)
            }
        }
    }
}
