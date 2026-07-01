package com.example.fairball.ui

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

// UID fisso per l'account admin di debug: usarlo permette di ri-entrare
// sempre sullo stesso profilo admin senza doversi autenticare con Google.
private const val DEBUG_ADMIN_UID = "admin_test_id"

@Composable
fun LoginScreen(onLoginSuccess: (String, String?) -> Unit) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                auth.signInWithCredential(credential).addOnCompleteListener { authResult ->
                    if (authResult.isSuccessful) {
                        val firebaseUser = auth.currentUser
                        if (firebaseUser == null) {
                            Toast.makeText(context, "Errore: utente non disponibile dopo il login", Toast.LENGTH_SHORT).show()
                            return@addOnCompleteListener
                        }

                        db.collection("users").document(firebaseUser.uid).get()
                            .addOnSuccessListener { doc ->
                                if (doc.exists()) {
                                    // Caso normale: utente già presente su Firestore con questo uid.
                                    val role = doc.getString("role") ?: "referee"
                                    onLoginSuccess(role, firebaseUser.uid)
                                } else {
                                    // Il documento non esiste con questo uid: potrebbe essere un utente
                                    // già registrato in passato con un uid diverso (stessa email).
                                    db.collection("users").whereEqualTo("email", firebaseUser.email).get()
                                        .addOnSuccessListener { query ->
                                            if (!query.isEmpty) {
                                                // Migrazione: il profilo esiste già ma sotto un altro uid.
                                                // Lo ricreiamo sotto l'uid corretto e poi cancelliamo il vecchio.
                                                val existingDoc = query.documents[0]
                                                val role = existingDoc.getString("role") ?: "referee"
                                                val data = existingDoc.data?.toMutableMap() ?: mutableMapOf()
                                                data["uid"] = firebaseUser.uid

                                                db.collection("users").document(firebaseUser.uid).set(data)
                                                    .addOnSuccessListener {
                                                        if (existingDoc.id != firebaseUser.uid) {
                                                            db.collection("users").document(existingDoc.id).delete()
                                                        }
                                                        onLoginSuccess(role, firebaseUser.uid)
                                                    }
                                                    .addOnFailureListener {
                                                        Toast.makeText(context, "Errore durante la migrazione del profilo", Toast.LENGTH_SHORT).show()
                                                    }
                                            } else {
                                                // Primo accesso in assoluto: nuovo utente, ruolo di default "referee".
                                                // Gli account admin non vengono creati qui: vanno impostati a mano
                                                // su Firestore (campo role = "admin") oppure tramite il pulsante debug.
                                                val userData = mapOf(
                                                    "uid" to firebaseUser.uid,
                                                    "email" to firebaseUser.email,
                                                    "displayName" to firebaseUser.displayName,
                                                    "photoUrl" to (firebaseUser.photoUrl?.toString() ?: ""),
                                                    "role" to "referee"
                                                )
                                                db.collection("users").document(firebaseUser.uid).set(userData)
                                                    .addOnSuccessListener {
                                                        onLoginSuccess("referee", firebaseUser.uid)
                                                    }
                                                    .addOnFailureListener {
                                                        Toast.makeText(context, "Errore durante la creazione del profilo", Toast.LENGTH_SHORT).show()
                                                    }
                                            }
                                        }
                                        .addOnFailureListener {
                                            Toast.makeText(context, "Errore durante la ricerca del profilo", Toast.LENGTH_SHORT).show()
                                        }
                                }
                            }
                            .addOnFailureListener {
                                Toast.makeText(context, "Errore durante il caricamento del profilo", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        Toast.makeText(context, "Autenticazione fallita", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: ApiException) {
                Toast.makeText(context, "Errore Google Sign-In", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "FairBall", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken("1008501694911-4grfprp1gt93nbrajp8mg9hbu5d1gejb.apps.googleusercontent.com")
                    .requestEmail()
                    .build()
                val googleSignInClient = GoogleSignIn.getClient(context, gso)
                launcher.launch(googleSignInClient.signInIntent)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Accedi con Google")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Pulsante di debug: crea (o aggiorna) l'account admin fisso e ci entra
        // direttamente, senza passare da Firebase Auth. Utile solo in fase di test:
        // funziona perché le Firestore Rules sono ancora in modalità aperta.
        OutlinedButton(
            onClick = {
                val adminData = mapOf(
                    "uid" to DEBUG_ADMIN_UID,
                    "displayName" to "Admin FairBall",
                    "email" to "admin@fairball.com",
                    "photoUrl" to "",
                    "role" to "admin"
                )
                db.collection("users").document(DEBUG_ADMIN_UID).set(adminData)
                    .addOnSuccessListener {
                        onLoginSuccess("admin", DEBUG_ADMIN_UID)
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Errore creazione admin di debug", Toast.LENGTH_SHORT).show()
                    }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Entra come Admin (Debug)")
        }
    }
}