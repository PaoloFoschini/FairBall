package com.example.fairball.ui

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

private const val WEB_CLIENT_ID = "1008501694911-4grfprp1gt93nbrajp8mg9hbu5d1gejb.apps.googleusercontent.com"

/**
 * Enumerazione dei risultati dell'autenticazione con Google.
 */
sealed class GoogleSignInOutcome {
    data class Success(val firebaseUser: FirebaseUser) : GoogleSignInOutcome()
    object NoCredential : GoogleSignInOutcome()
    data class Error(val message: String) : GoogleSignInOutcome()
}

sealed class UserLookupResult {
    data class Found(val role: String) : UserLookupResult()
    object NotFound : UserLookupResult()
}

/**
 * Avvia il flusso di Google Sign-In tramite Credential Manager e autentica su Firebase.
 *
 * @param filterByAuthorizedAccounts
 *   - true  -> mostra solo account Google già usati in precedenza con questa app
 *   - false -> mostra tutti gli account Google disponibili sul dispositivo
 */
suspend fun signInWithGoogle(
    context: Context,
    filterByAuthorizedAccounts: Boolean
): GoogleSignInOutcome {
    android.util.Log.d("GoogleAuthHelper", "signInWithGoogle chiamata, filterByAuthorizedAccounts=$filterByAuthorizedAccounts")
    val credentialManager = CredentialManager.create(context)

    val googleIdOption = GetGoogleIdOption.Builder()
        .setServerClientId(WEB_CLIENT_ID)
        .setFilterByAuthorizedAccounts(filterByAuthorizedAccounts)
        .build()

    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    return try {
        val response = credentialManager.getCredential(context, request)
        val credential = response.credential

        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val authCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
            val authResult = FirebaseAuth.getInstance().signInWithCredential(authCredential).await()
            val firebaseUser = authResult.user
                ?: return GoogleSignInOutcome.Error("Utente non disponibile dopo l'autenticazione")
            GoogleSignInOutcome.Success(firebaseUser)
        } else {
            GoogleSignInOutcome.Error("Tipo di credenziale non supportato")
        }
    } catch (e: GoogleIdTokenParsingException) {
        GoogleSignInOutcome.Error("Errore nel parsing del token Google")
    } catch (e: NoCredentialException) {
        /**
         * Nessuna credenziale Google trovata. Con filterByAuthorizedAccounts=true significa
         * "nessun account già usato con questa app"; con filterByAuthorizedAccounts=false
         * significa "nessun account Google presente sul dispositivo" (Impostazioni > Account)
         */
        android.util.Log.e("GoogleAuthHelper", "NoCredentialException: ${e.message}", e)
        GoogleSignInOutcome.NoCredential
    } catch (e: GetCredentialException) {
        android.util.Log.e("GoogleAuthHelper", "GetCredentialException (${e.type}): ${e.message}", e)
        GoogleSignInOutcome.Error(e.message ?: "Accesso Google annullato o non riuscito")
    }
}

/**
 * Cerca il profilo utente su Firestore a partire dal firebase uid.
 * Gestisce anche la migrazione di documenti "legacy" salvati con un id diverso
 * dal firebase uid, effettuando il match per email (comportamento ereditato dalla
 * versione precedente della LoginScreen).
 */
suspend fun lookupUser(db: FirebaseFirestore, firebaseUser: FirebaseUser): UserLookupResult {
    val docByUid = db.collection("users").document(firebaseUser.uid).get().await()
    if (docByUid.exists()) {
        val role = docByUid.getString("role") ?: "referee"
        return UserLookupResult.Found(role)
    }

    val query = db.collection("users").whereEqualTo("email", firebaseUser.email).get().await()
    if (!query.isEmpty) {
        val existingDoc = query.documents[0]
        val role = existingDoc.getString("role") ?: "referee"
        val data = existingDoc.data?.toMutableMap() ?: mutableMapOf()
        data["uid"] = firebaseUser.uid

        db.collection("users").document(firebaseUser.uid).set(data).await()
        if (existingDoc.id != firebaseUser.uid) {
            db.collection("users").document(existingDoc.id).delete().await()
        }
        return UserLookupResult.Found(role)
    }

    return UserLookupResult.NotFound
}

/** Crea un nuovo profilo utente su Firestore con ruolo di default "referee". Ritorna il ruolo assegnato. */
suspend fun createUserProfile(db: FirebaseFirestore, firebaseUser: FirebaseUser): String {
    val userData = mapOf(
        "uid" to firebaseUser.uid,
        "email" to firebaseUser.email,
        "displayName" to firebaseUser.displayName,
        "photoUrl" to (firebaseUser.photoUrl?.toString() ?: ""),
        "role" to "referee"
    )
    db.collection("users").document(firebaseUser.uid).set(userData).await()
    return "referee"
}