package com.example.fairball.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.fairball.model.User

@Composable
fun RefereeEditDialog(
    referee: User,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var displayName by remember { mutableStateOf(referee.displayName) }
    var email by remember { mutableStateOf(referee.email) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modifica Profilo Arbitro") },
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
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(displayName, email) },
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