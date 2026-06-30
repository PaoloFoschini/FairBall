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
import androidx.compose.material.icons.filled.*
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

    var showPickerFor by remember { mutableStateOf<String?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            when (showPickerFor) {
                "A" -> photoDistintaA = it
                "B" -> photoDistintaB = it
                "Referto" -> photoReferto = it
            }
        }
        showPickerFor = null
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        // In a real app, you'd save this bitmap to a file and get a Uri
        // For this prototype, we'll assume we get a Uri or just show the bitmap (not possible directly with AsyncImage easily without Uri)
        // Let's simplify and just use the Gallery for now but keep the UI choice
        showPickerFor = null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Caricamento Documenti") },
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
                text = "Allega i documenti ufficiali",
                style = MaterialTheme.typography.titleLarge
            )

            PhotoUploadSection(
                label = "Distinta Casa",
                imageUri = photoDistintaA,
                onUploadClick = { showPickerFor = "A" }
            )

            PhotoUploadSection(
                label = "Distinta Ospiti",
                imageUri = photoDistintaB,
                onUploadClick = { showPickerFor = "B" }
            )

            PhotoUploadSection(
                label = "Referto Gara",
                imageUri = photoReferto,
                onUploadClick = { showPickerFor = "Referto" }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onFinish,
                modifier = Modifier.fillMaxWidth(),
                enabled = photoDistintaA != null && photoDistintaB != null && photoReferto != null
            ) {
                Text("CONFERMA E CHIUDI")
            }
        }
    }

    if (showPickerFor != null) {
        ModalBottomSheet(onDismissRequest = { showPickerFor = null }) {
            Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                Text("Seleziona Sorgente", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { galleryLauncher.launch("image/*") }) {
                        Icon(Icons.Default.PhotoLibrary, null, modifier = Modifier.size(48.dp))
                        Text("Galleria")
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { /* cameraLauncher.launch(null) */ galleryLauncher.launch("image/*") }) {
                        Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(48.dp))
                        Text("Fotocamera")
                    }
                }
                Spacer(Modifier.height(32.dp))
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
                        .height(80.dp)
                        .border(1.dp, Color.Gray, shape = MaterialTheme.shapes.medium),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AddAPhoto, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Seleziona o Scatta", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}
