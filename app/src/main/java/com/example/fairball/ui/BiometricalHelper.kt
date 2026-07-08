package com.example.fairball.ui

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Enumerazione degli stati di autenticazione con l'impronta.
 */
sealed class BiometricAvailability {
    object Available : BiometricAvailability()
    object NoHardware : BiometricAvailability()
    object HardwareUnavailable : BiometricAvailability()
    object NoneEnrolled : BiometricAvailability()
    object Unsupported : BiometricAvailability()
}

/**
 * Enumerazione dei risultati dell'autenticazione con l'impronta.
 */
sealed class BiometricAuthOutcome {
    object Success : BiometricAuthOutcome()
    object Cancelled : BiometricAuthOutcome()
    data class Error(val message: String) : BiometricAuthOutcome()
}

/**
 * Verifica se il dispositivo supporta l'autenticazione biometrica (impronta/volto)
 * e se l'utente ne ha configurata almeno una nelle impostazioni di sistema.
 */
fun checkBiometricAvailability(activity: FragmentActivity): BiometricAvailability {
    val biometricManager = BiometricManager.from(activity)
    return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
        BiometricManager.BIOMETRIC_SUCCESS ->
            BiometricAvailability.Available
        BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ->
            BiometricAvailability.NoHardware
        BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE ->
            BiometricAvailability.HardwareUnavailable
        BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ->
            BiometricAvailability.NoneEnrolled
        else ->
            BiometricAvailability.Unsupported
    }
}

/**
 * Mostra una finestra di dialogo per l'autenticazione con l'impronta.
 */
suspend fun showBiometricPrompt(
    activity: FragmentActivity,
    title: String = "Accedi con l'impronta",
    subtitle: String = "Usa il sensore biometrico per accedere a FairBall"
): BiometricAuthOutcome = suspendCancellableCoroutine { continuation ->

    val executor = androidx.core.content.ContextCompat.getMainExecutor(activity)

    val callback = object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            if (continuation.isActive) continuation.resume(BiometricAuthOutcome.Success)
        }

        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            val outcome = when (errorCode) {
                BiometricPrompt.ERROR_USER_CANCELED,
                BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                BiometricPrompt.ERROR_CANCELED -> BiometricAuthOutcome.Cancelled
                else -> BiometricAuthOutcome.Error(errString.toString())
            }
            if (continuation.isActive) continuation.resume(outcome)
        }

        override fun onAuthenticationFailed() {
            // Impronta non riconosciuta
        }
    }

    val biometricPrompt = BiometricPrompt(activity, executor, callback)

    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle(title)
        .setSubtitle(subtitle)
        .setNegativeButtonText("Annulla")
        .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
        .build()

    biometricPrompt.authenticate(promptInfo)

    continuation.invokeOnCancellation {
        biometricPrompt.cancelAuthentication()
    }
}