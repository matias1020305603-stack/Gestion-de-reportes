package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.TaskEntity
import com.example.data.model.TaskPriority
import com.example.ui.components.TaskCard
import com.example.ui.theme.SeverityAlta
import com.example.ui.theme.SeverityBaja
import com.example.ui.theme.SeverityMedia
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.TaskFilterStatus

@Composable
fun TasksScreen(
    viewModel: MainViewModel,
    onEditTask: (TaskEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val tasks by viewModel.filteredTasks.collectAsStateWithLifecycle()
    val allTasks by viewModel.allTasks.collectAsStateWithLifecycle()
    val searchQuery by viewModel.taskSearchQuery.collectAsStateWithLifecycle()
    val filterStatus by viewModel.taskFilterStatus.collectAsStateWithLifecycle()
    val filterPriority by viewModel.taskFilterPriority.collectAsStateWithLifecycle()

    val completedCount = allTasks.count { it.isCompleted }
    val pendingCount = allTasks.size - completedCount

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Quick Metrics Counters Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TaskMetricChip(
                label = "Total",
                count = allTasks.size,
                isSelected = filterStatus == TaskFilterStatus.ALL,
                activeColor = MaterialTheme.colorScheme.primary,
                onClick = { viewModel.setTaskFilterStatus(TaskFilterStatus.ALL) },
                modifier = Modifier.weight(1f)
            )

            TaskMetricChip(
                label = "Pendientes",
                count = pendingCount,
                isSelected = filterStatus == TaskFilterStatus.PENDING,
                activeColor = SeverityMedia,
                onClick = { viewModel.setTaskFilterStatus(TaskFilterStatus.PENDING) },
                modifier = Modifier.weight(1f)
            )

            TaskMetricChip(
                label = "Listas",
                count = completedCount,
                isSelected = filterStatus == TaskFilterStatus.COMPLETED,
                activeColor = SeverityBaja,
                onClick = { viewModel.setTaskFilterStatus(TaskFilterStatus.COMPLETED) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setTaskSearchQuery(it) },
            placeholder = { Text("Buscar tarea por título o categoría...") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Buscar", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            },
            trailingIcon = if (searchQuery.isNotBlank()) {
                {
                    IconButton(onClick = { viewModel.setTaskSearchQuery("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Limpiar búsqueda")
                    }
                }
            } else null,
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("task_search_field")
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Priority Filter Chips Row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                FilterChip(
                    selected = filterPriority == null,
                    onClick = { viewModel.setTaskFilterPriority(null) },
                    label = { Text("Todas") }
                )
            }
            item {
                FilterChip(
                    selected = filterPriority == TaskPriority.ALTA.name,
                    onClick = { viewModel.setTaskFilterPriority(TaskPriority.ALTA.name) },
                    label = { Text("Alta") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SeverityAlta.copy(alpha = 0.2f),
                        selectedLabelColor = SeverityAlta
                    )
                )
            }
            item {
                FilterChip(
                    selected = filterPriority == TaskPriority.MEDIA.name,
                    onClick = { viewModel.setTaskFilterPriority(TaskPriority.MEDIA.name) },
                    label = { Text("Media") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SeverityMedia.copy(alpha = 0.2f),
                        selectedLabelColor = SeverityMedia
                    )
                )
            }
            item {
                FilterChip(
                    selected = filterPriority == TaskPriority.BAJA.name,
                    onClick = { viewModel.setTaskFilterPriority(TaskPriority.BAJA.name) },
                    label = { Text("Baja") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SeverityBaja.copy(alpha = 0.2f),
                        selectedLabelColor = SeverityBaja
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Task List or Empty State
        if (tasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AssignmentTurnedIn,
                        contentDescription = null,
                        modifier = Modifier.size(54.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                    )
                    Text(
                        text = if (searchQuery.isNotBlank()) "No se encontraron tareas con ese criterio"
                               else "No hay tareas en esta categoría",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Toca el botón '+' para crear una nueva tarea",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 90.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(tasks, key = { it.id }) { task ->
                    TaskCard(
                        task = task,
                        onToggle = { completed -> viewModel.toggleTask(task.id, completed) },
                        onEdit = { onEditTask(task) },
                        onDelete = { viewModel.deleteTask(task) }
                    )
                }
            }
        }
    }
}

@Composable
fun TaskMetricChip(
    label: String,
    count: Int,
    isSelected: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) activeColor.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.surfaceContainer
        ),
        border = if (isSelected) CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(activeColor)
        ) else null
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
