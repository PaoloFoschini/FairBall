package com.example.fairball.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight

/**
 * TopAppBar con freccia indietro, riusata da tutte le schermate secondarie.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackTopBar(
    title: String,
    onBack: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
            }
        },
        actions = actions
    )
}

/** Spinner centrato a tutto schermo, usato per gli stati di caricamento. */
@Composable
fun LoadingBox(modifier: Modifier = Modifier.fillMaxSize()) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

/** Messaggio centrato per liste/risultati vuoti. */
@Composable
fun EmptyStateBox(
    message: String,
    modifier: Modifier = Modifier.fillMaxSize(),
    textColor: Color = Color.Gray
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text(message, color = textColor)
    }
}

/** Dialog di conferma per operazioni distruttive (eliminazione). */
@Composable
fun ConfirmDeleteDialog(
    title: String,
    message: String,
    confirmLabel: String = "ELIMINA",
    confirmBold: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    confirmLabel,
                    color = Color.Red,
                    fontWeight = if (confirmBold) FontWeight.Bold else null
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("ANNULLA") }
        }
    )
}
