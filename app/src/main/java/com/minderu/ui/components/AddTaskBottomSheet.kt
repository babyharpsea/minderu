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

/**
 * Unified bottom sheet for creating and editing tasks.
 *
 * @param existingTask If non-null, enters **Edit mode** with pre-populated fields.
 *                     If null, enters **Create mode** with blank fields.
 * @param onSubmit Callback with (title, subhead, sparksReward).
 * @param onDismissRequest Called when the sheet is dismissed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskBottomSheet(
    existingTask: TaskUiModel? = null,
    onDismissRequest: () -> Unit,
    onSubmit: (title: String, subhead: String, sparksReward: Int) -> Unit
) {
    val isEditMode = existingTask != null

    var taskTitle by remember { mutableStateOf(existingTask?.title ?: "") }
    var taskSubhead by remember { mutableStateOf(existingTask?.subhead ?: "One tiny step") }
    var sparksReward by remember { mutableStateOf(existingTask?.sparks?.toString() ?: "2") }

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
                text = if (isEditMode) "Edit Task" else "New Micro-task",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = taskTitle,
                onValueChange = { taskTitle = it },
                label = { Text("What needs doing?") },
                placeholder = { Text("e.g., Clear one surface") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )

            OutlinedTextField(
                value = taskSubhead,
                onValueChange = { taskSubhead = it },
                label = { Text("Subtitle / motivation") },
                placeholder = { Text("e.g., One tiny step") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                singleLine = true
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
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true
                )

                Button(
                    onClick = {
                        if (taskTitle.isNotBlank()) {
                            onSubmit(
                                taskTitle.trim(),
                                taskSubhead.trim().ifBlank { "One tiny step" },
                                sparksReward.toIntOrNull() ?: 2
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
                        text = if (isEditMode) "Save Changes" else "Create Task",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
