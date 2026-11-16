package com.example.firebase

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.example.R
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class GoogleAuthManager(private val context: Context) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val credentialManager = CredentialManager.create(context)

    private val _currentUser = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        _currentUser.value = firebaseAuth.currentUser
    }

    init {
        auth.addAuthStateListener(authListener)
    }

    fun signInWithGoogle(
        activityContext: Context,
        scope: CoroutineScope,
        onResult: (Boolean, String?) -> Unit
    ) {
        scope.launch(Dispatchers.Main) {
            try {
                val clientId = activityContext.getString(R.string.default_web_client_id)
                val signInOption = GetSignInWithGoogleOption.Builder(clientId)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(signInOption)
                    .build()

                val response = credentialManager.getCredential(
                    context = activityContext,
                    request = request
                )

                val credential = response.credential
                if (credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val authCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                    
                    suspendCancellableCoroutine<Unit> { cont ->
                        auth.signInWithCredential(authCredential)
                            .addOnSuccessListener {
                                _currentUser.value = auth.currentUser
                                cont.resume(Unit)
                            }
                            .addOnFailureListener { exc ->
                                cont.resumeWithException(exc)
                            }
                    }
                    onResult(true, null)
                } else {
                    onResult(false, "Unsupported credential returned")
                }
            } catch (e: GetCredentialCancellationException) {
                Log.d(TAG, "User cancelled Google Sign-in flow")
                onResult(false, "Sign-in cancelled")
            } catch (e: GetCredentialException) {
                Log.e(TAG, "CredentialManager sign in failed: ${e.message}", e)
                onResult(false, e.localizedMessage ?: "Google Sign-in failed")
            } catch (e: Exception) {
                Log.e(TAG, "Authentication error: ${e.message}", e)
                onResult(false, e.localizedMessage ?: "Authentication error")
            }
        }
    }

    fun signOut() {
        auth.signOut()
        _currentUser.value = null
    }

    companion object {
        private const val TAG = "GoogleAuthManager"
    }
}
