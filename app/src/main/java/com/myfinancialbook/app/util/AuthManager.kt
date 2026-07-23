package com.myfinancialbook.app.util

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object AuthManager {
    private val auth = FirebaseAuth.getInstance()

    fun isSignedIn(): Boolean = auth.currentUser != null
    fun getCurrentUser(): FirebaseUser? = auth.currentUser
    fun getUserId(): String = auth.currentUser?.uid ?: ""
    fun getUserEmail(): String = auth.currentUser?.email ?: ""
    fun signOut() = auth.signOut()

    suspend fun signInWithEmail(email: String, password: String): Result<FirebaseUser> =
        safeCall { auth.signInWithEmailAndPassword(email, password).await().user!! }

    suspend fun createUserWithEmail(email: String, password: String): Result<FirebaseUser> =
        safeCall { auth.createUserWithEmailAndPassword(email, password).await().user!! }

    suspend fun signInWithGoogle(idToken: String): Result<FirebaseUser> =
        safeCall {
            val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
            auth.signInWithCredential(credential).await().user!!
        }

    private suspend fun safeCall(block: suspend () -> FirebaseUser): Result<FirebaseUser> =
        try { Result.success(block()) } catch (e: Exception) { Result.failure(e) }
}

private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { r -> cont.resume(r) }
    addOnFailureListener { e -> cont.resumeWithException(e) }
    addOnCanceledListener { cont.cancel() }
}
