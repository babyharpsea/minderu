package com.minderu.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minderu.data.TaskUiModel

private val CoralAccent = Color(0xFFFF5252)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskBottomSheet(
    onDismissRequest: () -> Unit,
    taskToEdit: TaskUiModel? = null,
    onSubmit: (title: String, subhead: String, sparks: Int, taskId: String?) -> Unit
) {
    val isEdit = taskToEdit != null
    var taskTitle by remember(taskToEdit) { mutableStateOf(taskToEdit?.title ?: "") }
    var subhead by remember(taskToEdit) {
        mutableStateOf(taskToEdit?.subtitle ?: "One tiny step")
    }
    var sparksReward by remember(taskToEdit) {
        mutableStateOf((taskToEdit?.sparks ?: 2).toString())
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (isEdit) "Edit micro-task" else "New Micro-task",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = taskTitle,
                onValueChange = { taskTitle = it },
                label = { Text("What needs doing?") },
                placeholder = { Text("e.g., Clear one surface") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )

            OutlinedTextField(
                value = subhead,
                onValueChange = { subhead = it },
                label = { Text("Subhead") },
                placeholder = { Text("One tiny step") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = sparksReward,
                    onValueChange = { if (it.all { char -> char.isDigit() }) sparksReward = it },
                    label = { Text("Sparks reward") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp)
                )

                Button(
                    onClick = {
                        if (taskTitle.isNotBlank()) {
                            onSubmit(
                                taskTitle.trim(),
                                subhead.trim().ifBlank { "One tiny step" },
                                sparksReward.toIntOrNull()?.coerceAtLeast(0) ?: 2,
                                taskToEdit?.id
                            )
                        }
                    },
                    modifier = Modifier
                        .height(56.dp)
                        .weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CoralAccent,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = if (isEdit) "Save changes" else "Create Task",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
