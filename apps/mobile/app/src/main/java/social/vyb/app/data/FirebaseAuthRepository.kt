package social.vyb.app.data

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import social.vyb.app.R

class FirebaseAuthRepository {
    private val auth: FirebaseAuth = Firebase.auth

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    fun addAuthStateListener(listener: (FirebaseUser?) -> Unit): FirebaseAuth.AuthStateListener {
        val authListener = FirebaseAuth.AuthStateListener { listener(it.currentUser) }
        auth.addAuthStateListener(authListener)
        return authListener
    }

    fun removeAuthStateListener(listener: FirebaseAuth.AuthStateListener) {
        auth.removeAuthStateListener(listener)
    }

    fun signInWithEmail(
        email: String,
        password: String,
        onResult: (Result<FirebaseUser>) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email.trim(), password)
            .addOnSuccessListener { result ->
                result.user?.let { user ->
                    onResult(Result.success(user))
                } ?: onResult(Result.failure(IllegalStateException("No Firebase user returned.")))
            }
            .addOnFailureListener { onResult(Result.failure(it)) }
    }

    fun createAccount(
        email: String,
        password: String,
        onResult: (Result<FirebaseUser>) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email.trim(), password)
            .addOnSuccessListener { result ->
                result.user?.let { user ->
                    user.sendEmailVerification()
                        .addOnSuccessListener {
                            auth.signOut()
                            onResult(
                                Result.failure(
                                    VerificationEmailSentException(
                                        "Verification email sent to ${user.email}. Verify it, then sign in."
                                    )
                                )
                            )
                        }
                        .addOnFailureListener { onResult(Result.failure(it)) }
                } ?: onResult(Result.failure(IllegalStateException("No Firebase user returned.")))
            }
            .addOnFailureListener { onResult(Result.failure(it)) }
    }

    fun sendPasswordReset(
        email: String,
        onResult: (Result<String>) -> Unit
    ) {
        auth.sendPasswordResetEmail(email.trim())
            .addOnSuccessListener {
                onResult(Result.success("Password reset email sent to ${email.trim()}."))
            }
            .addOnFailureListener { onResult(Result.failure(it)) }
    }

    suspend fun signInWithGoogle(context: Context): Result<FirebaseUser> {
        return try {
            Result.success(
                signInWithGoogleCredential(context)
            )
        } catch (_: NoCredentialException) {
            Result.failure(
                IllegalStateException(
                    "No Google account is available. Add an account in Android Settings and try again."
                )
            )
        } catch (error: Throwable) {
            Result.failure(error)
        }
    }

    private suspend fun signInWithGoogleCredential(context: Context): FirebaseUser {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(context.getString(R.string.default_web_client_id))
                .setAutoSelectEnabled(false)
                .build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()
            val credential = CredentialManager.create(context)
                .getCredential(context, request)
                .credential

            require(
                credential is CustomCredential &&
                    credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) { "Google did not return a valid ID credential." }

            val token = GoogleIdTokenCredential.createFrom(credential.data).idToken
            val firebaseCredential = GoogleAuthProvider.getCredential(token, null)
            val task = auth.signInWithCredential(firebaseCredential)
            val user = kotlinx.coroutines.suspendCancellableCoroutine<FirebaseUser> { continuation ->
                task.addOnSuccessListener { result ->
                    val signedInUser = result.user
                    if (signedInUser != null) continuation.resume(signedInUser) { _, _, _ -> }
                    else continuation.resumeWith(
                        Result.failure(IllegalStateException("No Firebase user returned."))
                    )
                }
                task.addOnFailureListener { continuation.resumeWith(Result.failure(it)) }
            }
            return user
    }

    suspend fun signOut(context: Context) {
        auth.signOut()
        runCatching {
            CredentialManager.create(context)
                .clearCredentialState(ClearCredentialStateRequest())
        }
    }

}

class VerificationEmailSentException(message: String) : Exception(message)
