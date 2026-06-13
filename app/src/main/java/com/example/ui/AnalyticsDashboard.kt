package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TaskEntity
import com.example.viewmodel.TaskViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AnalyticsDashboardScreen(viewModel: TaskViewModel) {
    val tasks by viewModel.tasks.collectAsState()
    val logs by viewModel.activityLogs.collectAsState()

    var selectedStatusFilter by remember { mutableStateOf<String?>(null) }
    var selectedTrendPointIndex by remember { mutableStateOf<Int?>(null) }

    val scrollState = rememberScrollState()

    // Status aggregates
    val total = tasks.size
    val todoCount = tasks.count { it.status == "TODO" }
    val progressCount = tasks.count { it.status == "IN_PROGRESS" }
    val reviewCount = tasks.count { it.status == "REVIEW" }
    val completedCount = tasks.count { it.status == "DONE" || it.status == "COMPLETED" }

    // Aggregate trends over the last 7 days
    val calendarDays = remember(tasks, logs) {
        val sdfDate = SimpleDateFormat("MMM dd", Locale.getDefault())
        val daysList = mutableListOf<TrendDay>()
        val cal = Calendar.getInstance()

        // Generate past 7 days tracking list ending today
        for (i in 6 downTo 0) {
            val d = Calendar.getInstance()
            d.add(Calendar.DAY_OF_YEAR, -i)
            val dateLabel = sdfDate.format(d.time)
            
            // Format boundaries for midnight of day to midnight next day
            d.set(Calendar.HOUR_OF_DAY, 0)
            d.set(Calendar.MINUTE, 0)
            d.set(Calendar.SECOND, 0)
            d.set(Calendar.MILLISECOND, 0)
            val startTime = d.timeInMillis
            
            d.add(Calendar.DAY_OF_YEAR, 1)
            val endTime = d.timeInMillis

            // Filter real completions on this day: tasks marked "DONE" that were updated or logged completed
            val finishedTasksCount = tasks.count { task ->
                (task.status == "DONE" || task.status == "COMPLETED") && 
                        task.lastUpdated >= startTime && task.lastUpdated < endTime
            }

            // Cross-reference logs matching action 'Completed' or similar
            val finishedLogsCount = logs.count { logging ->
                (logging.action.contains("Completed", ignoreCase = true) || 
                 logging.action.contains("resolved", ignoreCase = true) ||
                 logging.action.contains("Finished", ignoreCase = true)) &&
                        logging.timestamp >= startTime && logging.timestamp < endTime
            }

            // Max count of events on this day to prevent double-counting but guarantee dynamic rendering
            val counts = maxOf(finishedTasksCount, finishedLogsCount)
            daysList.add(TrendDay(label = dateLabel, count = counts))
        }
        daysList
    }

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
                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f)
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
                        text = "Workflow Intelligence Analytics",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Continuous performance monitoring and trend visualizations",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                    contentDescription = "Analytics Icon",
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        // Summary Metric Highlight Widgets
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AnalyticsMiniMetric(
                title = "Pipeline Volatility",
                value = "$total",
                subtitle = "Total instances active",
                color = MaterialTheme.colorScheme.primary,
                icon = Icons.Default.Hub,
                modifier = Modifier.weight(1f)
            )
            AnalyticsMiniMetric(
                title = "Total Resolved",
                value = "$completedCount",
                subtitle = "${if (total > 0) (completedCount * 100) / total else 0}% completion scale",
                color = Color(0xFF00C853),
                icon = Icons.Default.TaskAlt,
                modifier = Modifier.weight(1f)
            )
        }

        // 1. Completion Trends Area Chart Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Daily Completion Density",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Completed tasks & resolves across the last 7 cycles",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "LAST 7 DAYS",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Render high-performance Custom Trend Line/Area Chart using Canvas
                CompletionTrendLineChart(
                    days = calendarDays,
                    selectedPointIndex = selectedTrendPointIndex,
                    onPointSelected = { selectedTrendPointIndex = it }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Dynamic Tooltip feedback container for Area Chart interactions
                selectedTrendPointIndex?.let { index ->
                    val day = calendarDays.getOrNull(index)
                    if (day != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Event,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Trace Endpoint: ${day.label}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = "${day.count} COMPLETED",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00C853)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Task Distribution Donut Chart Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Operational Segment Distribution",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Allocation ratio across lifecycle states (Tap to filter drilldown)",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (total == 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No metrics registered. Populate tasks list to review segments.",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // High fidelity custom donut ring segment chart
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(130.dp)
                        ) {
                            val colorTodo = MaterialTheme.colorScheme.secondary
                            val colorProgress = MaterialTheme.colorScheme.primary
                            val colorReview = MaterialTheme.colorScheme.tertiary
                            val colorDone = Color(0xFF00C853)

                            Canvas(modifier = Modifier.size(110.dp)) {
                                val totalF = total.toFloat()
                                val sweepTodo = (todoCount / totalF) * 360f
                                val sweepProgress = (progressCount / totalF) * 360f
                                val sweepReview = (reviewCount / totalF) * 360f
                                val sweepDone = (completedCount / totalF) * 360f

                                var baseAngle = -90f

                                if (sweepTodo > 0f) {
                                    drawArc(
                                        color = colorTodo,
                                        startAngle = baseAngle,
                                        sweepAngle = sweepTodo,
                                        useCenter = false,
                                        style = Stroke(width = 24f)
                                    )
                                    baseAngle += sweepTodo
                                }
                                if (sweepProgress > 0f) {
                                    drawArc(
                                        color = colorProgress,
                                        startAngle = baseAngle,
                                        sweepAngle = sweepProgress,
                                        useCenter = false,
                                        style = Stroke(width = 24f)
                                    )
                                    baseAngle += sweepProgress
                                }
                                if (sweepReview > 0f) {
                                    drawArc(
                                        color = colorReview,
                                        startAngle = baseAngle,
                                        sweepAngle = sweepReview,
                                        useCenter = false,
                                        style = Stroke(width = 24f)
                                    )
                                    baseAngle += sweepReview
                                }
                                if (sweepDone > 0f) {
                                    drawArc(
                                        color = colorDone,
                                        startAngle = baseAngle,
                                        sweepAngle = sweepDone,
                                        useCenter = false,
                                        style = Stroke(width = 24f)
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$total",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "TOTAL",
                                    fontSize = 8.sp,
                                    color = Color.Gray,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            }
                        }

                        // Chart Legend with Filter Triggers
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            InteractiveLegendRow(
                                label = "BACKLOG / TO-DO",
                                count = todoCount,
                                total = total,
                                color = MaterialTheme.colorScheme.secondary,
                                isSelected = selectedStatusFilter == "TODO",
                                onClick = {
                                    selectedStatusFilter = if (selectedStatusFilter == "TODO") null else "TODO"
                                }
                            )
                            InteractiveLegendRow(
                                label = "IN CYCLE",
                                count = progressCount,
                                total = total,
                                color = MaterialTheme.colorScheme.primary,
                                isSelected = selectedStatusFilter == "IN_PROGRESS",
                                onClick = {
                                    selectedStatusFilter = if (selectedStatusFilter == "IN_PROGRESS") null else "IN_PROGRESS"
                                }
                            )
                            InteractiveLegendRow(
                                label = "UNDER REVIEWS",
                                count = reviewCount,
                                total = total,
                                color = MaterialTheme.colorScheme.tertiary,
                                isSelected = selectedStatusFilter == "REVIEW",
                                onClick = {
                                    selectedStatusFilter = if (selectedStatusFilter == "REVIEW") null else "REVIEW"
                                }
                            )
                            InteractiveLegendRow(
                                label = "VERIFIED / DONE",
                                count = completedCount,
                                total = total,
                                color = Color(0xFF00C853),
                                isSelected = selectedStatusFilter == "DONE",
                                onClick = {
                                    selectedStatusFilter = if (selectedStatusFilter == "DONE") null else "DONE"
                                }
                            )
                        }
                    }
                }
            }
        }

        // 3. Drill-down task list interactive breakdown
        AnimatedVisibility(
            visible = total > 0,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            val filterName = when (selectedStatusFilter) {
                "TODO" -> "TODO"
                "IN_PROGRESS" -> "IN SYSTEM CYCLE"
                "REVIEW" -> "UNDER REVIEW"
                "DONE" -> "VERIFIED / DONE"
                else -> "ALL ACTIVE WORKSPACE ENTITIES"
            }

            val drilldownList = remember(tasks, selectedStatusFilter) {
                if (selectedStatusFilter != null) {
                    tasks.filter { 
                        if (selectedStatusFilter == "DONE") {
                            it.status == "DONE" || it.status == "COMPLETED"
                        } else {
                            it.status == selectedStatusFilter
                        }
                    }
                } else {
                    tasks
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 280.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = filterName,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.5.sp
                        )
                        if (selectedStatusFilter != null) {
                            TextButton(onClick = { selectedStatusFilter = null }) {
                                Text("Clear Filter", fontSize = 11.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (drilldownList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No items match specified filter segment.", fontSize = 11.sp, color = Color.Gray)
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(drilldownList) { task ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when (task.priority) {
                                                    "HIGH" -> Color(0xFFFF5252)
                                                    "MEDIUM" -> Color(0xFFFFAB40)
                                                    else -> Color(0xFF448AFF)
                                                }
                                            )
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = task.title,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Assignee: ${task.assignedName} • Category: ${task.category}",
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = task.status,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun CompletionTrendLineChart(
    days: List<TrendDay>,
    selectedPointIndex: Int?,
    onPointSelected: (Int?) -> Unit
) {
    val density = LocalDensity.current
    val strokeWidthPx = with(density) { 3.dp.toPx() }
    val pointRadiusPx = with(density) { 5.dp.toPx() }
    val pointTouchThresholdPx = with(density) { 24.dp.toPx() }

    // Colors
    val primaryColor = MaterialTheme.colorScheme.primary
    val outlineVariantColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    val gridTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)

    val maxCount = remember(days) {
        val max = days.maxOfOrNull { it.count } ?: 0
        // Cap bottom value at 4 to make the scale beautiful if numbers are quiet
        maxOf(max, 4)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(days) {
                    detectTapGestures { offset ->
                        // Calculate matching X-coordinate checkpoint indices
                        val sectionWidth = size.width / (days.size - 1)
                        var closestIndex: Int? = null
                        var minDistance = Float.MAX_VALUE

                        for (i in days.indices) {
                            val ptX = i * sectionWidth
                            val dist = kotlin.math.abs(offset.x - ptX)
                            if (dist < minDistance && dist <= pointTouchThresholdPx) {
                                minDistance = dist
                                closestIndex = i
                            }
                        }
                        onPointSelected(closestIndex)
                    }
                }
        ) {
            val chartWidth = size.width
            val chartHeight = size.height

            val usableHeight = chartHeight - 40f
            val sectionWidth = chartWidth / (days.size - 1)

            // 1. Draw horizontal dashed background grid reference guides
            val gridStep = maxCount / 4f
            val pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)

            for (i in 0..4) {
                val valLevel = (i * gridStep).toInt()
                val y = usableHeight - (i * (usableHeight / 4f)) + 15f
                
                // Horizontal guideline
                drawLine(
                    color = outlineVariantColor,
                    start = Offset(0f, y),
                    end = Offset(chartWidth, y),
                    pathEffect = pathEffect,
                    strokeWidth = 2f
                )
            }

            // 2. Map coordinates points
            val points = days.mapIndexed { idx, day ->
                val x = idx * sectionWidth
                val scaleRatio = day.count.toFloat() / maxCount.toFloat()
                val y = usableHeight - (scaleRatio * usableHeight) + 15f
                Offset(x, y)
            }

            // 3. Draw gradient closed trend area shape
            if (points.isNotEmpty()) {
                val areaPath = Path().apply {
                    moveTo(points[0].x, usableHeight + 15f)
                    points.forEach { pt ->
                        lineTo(pt.x, pt.y)
                    }
                    lineTo(points.last().x, usableHeight + 15f)
                    close()
                }

                drawPath(
                    path = areaPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.35f),
                            Color.Transparent
                        ),
                        startY = 15f,
                        endY = usableHeight + 15f
                    )
                )
            }

            // 4. Draw bold primary path trace line
            if (points.size > 1) {
                val linePath = Path().apply {
                    moveTo(points[0].x, points[0].y)
                    for (i in 1 until points.size) {
                        lineTo(points[i].x, points[i].y)
                    }
                }
                drawPath(
                    path = linePath,
                    color = primaryColor,
                    style = Stroke(
                        width = strokeWidthPx,
                        pathEffect = null
                    )
                )
            }

            // 5. Draw interactive active vertical guide ruler line
            selectedPointIndex?.let { index ->
                val activePt = points.getOrNull(index)
                if (activePt != null) {
                    drawLine(
                        color = primaryColor.copy(alpha = 0.6f),
                        start = Offset(activePt.x, 15f),
                        end = Offset(activePt.x, usableHeight + 15f),
                        strokeWidth = 3f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
                    )
                }
            }

            // 6. Draw highlighted data nodes/vertices
            points.forEachIndexed { i, pt ->
                // Outer ring ring glow
                val isSelected = i == selectedPointIndex
                drawCircle(
                    color = if (isSelected) primaryColor else primaryColor.copy(alpha = 0.25f),
                    radius = if (isSelected) pointRadiusPx * 2.2f else pointRadiusPx * 1.6f,
                    center = pt
                )
                // Core solid node
                drawCircle(
                    color = Color.White,
                    radius = pointRadiusPx,
                    center = pt
                )
                drawCircle(
                    color = primaryColor,
                    radius = pointRadiusPx * 0.6f,
                    center = pt
                )
            }
        }
    }

    // 7. Horizontal bottom date axis labels
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        days.forEachIndexed { index, day ->
            val isSelected = index == selectedPointIndex
            Text(
                text = day.label,
                fontSize = 9.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun InteractiveLegendRow(
    label: String,
    count: Int,
    total: Int,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val percentageValue = if (total > 0) (count * 100) / total else 0
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 8.dp),
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
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun AnalyticsMiniMetric(
    title: String,
    value: String,
    subtitle: String,
    color: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title.uppercase(),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column {
                Text(
                    text = value,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Light,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 9.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

data class TrendDay(
    val label: String,
    val count: Int
)
