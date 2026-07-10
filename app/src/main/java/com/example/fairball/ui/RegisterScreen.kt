package com.example.fairball.ui

import android.widget.Toast
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
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

    AuthScreenLayout(
        primaryButtonText = "Registrati con Google",
        isLoading = isLoading,
        onPrimaryClick = { startGoogleRegistration() },
        secondaryText = "Hai già un account? Accedi",
        onSecondaryClick = onNavigateToLogin
    )
}