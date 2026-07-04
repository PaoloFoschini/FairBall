package com.example.fairball.ui

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.fairball.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onLoginSuccess: (String, String?) -> Unit,
    onNavigateToRegister: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    var isLoading by remember { mutableStateOf(false) }

    fun startGoogleSignIn() {
        scope.launch {
            isLoading = true
            when (val outcome = signInWithGoogle(context, filterByAuthorizedAccounts = true)) {
                is GoogleSignInOutcome.Success -> {
                    when (val result = lookupUser(db, outcome.firebaseUser)) {
                        is UserLookupResult.Found -> {
                            onLoginSuccess(result.role, outcome.firebaseUser.uid)
                        }
                        UserLookupResult.NotFound -> {
                            auth.signOut()
                            Toast.makeText(
                                context,
                                "Nessun account trovato. Registrati per continuare.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
                GoogleSignInOutcome.NoCredential -> {
                    Toast.makeText(
                        context,
                        "Nessun account Google salvato su questo dispositivo. Registrati per continuare.",
                        Toast.LENGTH_LONG
                    ).show()
                }
                is GoogleSignInOutcome.Error -> {
                    Toast.makeText(context, outcome.message, Toast.LENGTH_SHORT).show()
                }
            }
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.fairball),
            contentDescription = "Logo FairBall",
            modifier = Modifier
                .size(220.dp)
                .padding(bottom = 24.dp)
        )

        Text(
            text = "FairBall",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { startGoogleSignIn() },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text("Accedi con Google")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(
            onClick = onNavigateToRegister,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Non hai un account? Registrati")
        }
    }
}