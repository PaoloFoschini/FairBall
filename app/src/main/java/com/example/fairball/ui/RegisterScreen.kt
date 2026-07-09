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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.fairball.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

/**
 * Schermata di registrazione di un nuovo account.
 */
@Composable
fun RegisterScreen(
    onRegisterSuccess: (String, String?) -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    var isLoading by remember { mutableStateOf(false) }

    fun startGoogleRegistration() {
        scope.launch {
            isLoading = true
            when (val outcome = signInWithGoogle(context, filterByAuthorizedAccounts = false)) {
                is GoogleSignInOutcome.Success -> {
                    when (lookupUser(db, outcome.firebaseUser)) {
                        is UserLookupResult.Found -> {
                            auth.signOut()
                            Toast.makeText(
                                context,
                                "Esiste già un account con questo indirizzo Google. Effettua il login.",
                                Toast.LENGTH_LONG
                            ).show()
                            onNavigateToLogin()
                        }
                        UserLookupResult.NotFound -> {
                            val role = createUserProfile(db, outcome.firebaseUser)
                            onRegisterSuccess(role, outcome.firebaseUser.uid)
                        }
                    }
                }
                GoogleSignInOutcome.NoCredential -> {
                    Toast.makeText(
                        context,
                        "Nessun account Google disponibile sul dispositivo.",
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
            onClick = { startGoogleRegistration() },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text("Registrati con Google")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(
            onClick = onNavigateToLogin,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Hai già un account? Accedi")
        }
    }
}