package com.example.fairball.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchSummaryScreen(
    matchId: String,
    onFinish: () -> Unit,
    onBack: () -> Unit
) {
    var photoDistintaA by remember { mutableStateOf<Uri?>(null) }
    var photoDistintaB by remember { mutableStateOf<Uri?>(null) }
    var photoReferto by remember { mutableStateOf<Uri?>(null) }

    val launcherA = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        photoDistintaA = uri
    }
    val launcherB = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        photoDistintaB = uri
    }
    val launcherReferto = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        photoReferto = uri
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Riepilogo e Documenti") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Indietro")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Carica i documenti della partita",
                style = MaterialTheme.typography.titleLarge
            )

            PhotoUploadSection(
                label = "Foto Distinta Squadra A",
                imageUri = photoDistintaA,
                onUploadClick = { launcherA.launch("image/*") }
            )

            PhotoUploadSection(
                label = "Foto Distinta Squadra B",
                imageUri = photoDistintaB,
                onUploadClick = { launcherB.launch("image/*") }
            )

            PhotoUploadSection(
                label = "Foto Referto",
                imageUri = photoReferto,
                onUploadClick = { launcherReferto.launch("image/*") }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onFinish,
                modifier = Modifier.fillMaxWidth(),
                enabled = photoDistintaA != null && photoDistintaB != null && photoReferto != null
            ) {
                Text("FINE")
            }
        }
    }
}

@Composable
fun PhotoUploadSection(
    label: String,
    imageUri: Uri?,
    onUploadClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onUploadClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
            
            if (imageUri != null) {
                Box(contentAlignment = Alignment.TopEnd) {
                    AsyncImage(
                        model = imageUri,
                        contentDescription = label,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .border(1.dp, Color.Gray),
                        contentScale = ContentScale.Crop
                    )
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Caricato",
                        tint = Color.Green,
                        modifier = Modifier.padding(4.dp)
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .border(1.dp, Color.Gray, shape = MaterialTheme.shapes.medium),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.AddAPhoto,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text("Tocca per caricare", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}
