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
                        val user = auth.currentUser
                        user?.let { firebaseUser ->
                            db.collection("users").document(firebaseUser.uid).get()
                                .addOnSuccessListener { doc ->
                                    if (doc.exists()) {
                                        onLoginSuccess(doc.getString("role") ?: "referee", null)
                                    } else {
                                        db.collection("users")
                                            .whereEqualTo("email", firebaseUser.email)
                                            .get()
                                            .addOnSuccessListener { query ->
                                                if (!query.isEmpty) {
                                                    val existingDoc = query.documents[0]
                                                    val role = existingDoc.getString("role") ?: "referee"
                                                    val data = existingDoc.data?.toMutableMap() ?: mutableMapOf()
                                                    data["uid"] = firebaseUser.uid
                                                    db.collection("users").document(firebaseUser.uid).set(data)
                                                    if (existingDoc.id != firebaseUser.uid) {
                                                        db.collection("users").document(existingDoc.id).delete()
                                                    }
                                                    onLoginSuccess(role, null)
                                                } else {
                                                    val userData = mapOf(
                                                        "uid" to firebaseUser.uid,
                                                        "email" to firebaseUser.email,
                                                        "displayName" to firebaseUser.displayName,
                                                        "role" to "referee"
                                                    )
                                                    db.collection("users").document(firebaseUser.uid).set(userData)
                                                    onLoginSuccess("referee", null)
                                                }
                                            }
                                    }
                                }
                        }
                    } else {
                        Toast.makeText(context, "Auth Fallita", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: ApiException) {
                Toast.makeText(context, "Errore Google Sign-In", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
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

        OutlinedButton(
            onClick = { onLoginSuccess("referee", "mario_rossi_test_id") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Debug: Entra come Mario Rossi")
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(onClick = { onLoginSuccess("admin", null) }) {
            Text("Entra come Admin (Debug)")
        }
    }
}
