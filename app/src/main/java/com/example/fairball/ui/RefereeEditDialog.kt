package com.example.fairball.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.fairball.model.User

private data class RoleOption(val value: String, val label: String)

private val ROLE_OPTIONS = listOf(
    RoleOption("referee", "Arbitro"),
    RoleOption("admin", "Amministratore")
)

@Composable
fun RefereeEditDialog(
    referee: User,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var displayName by remember { mutableStateOf(referee.displayName) }
    var email by remember { mutableStateOf(referee.email) }
    var selectedRole by remember { mutableStateOf(referee.role) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modifica Profilo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Nome e Cognome") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("E-mail") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))
                Text("Ruolo", style = MaterialTheme.typography.labelLarge)

                Column(Modifier.selectableGroup()) {
                    ROLE_OPTIONS.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = (selectedRole == option.value),
                                    onClick = { selectedRole = option.value },
                                    role = Role.RadioButton
                                )
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (selectedRole == option.value),
                                onClick = { selectedRole = option.value }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(option.label)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(displayName, email, selectedRole) },
                enabled = displayName.isNotBlank() && email.isNotBlank()
            ) {
                Text("Salva")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla")
            }
        }
    )
}