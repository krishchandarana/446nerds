package com.example.savr.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

private const val USERS_COLLECTION = "users"

private val firestore: FirebaseFirestore
    get() = FirebaseFirestore.getInstance()

private val auth: FirebaseAuth
    get() = FirebaseAuth.getInstance()

/**
 * Path for the current user's document. Document ID = Firebase Auth UID.
 */
fun currentUserDocPath(): String? {
    val uid = auth.currentUser?.uid ?: return null
    return "$USERS_COLLECTION/$uid"
}

/**
 * Gets the current user's profile from Firestore (users/{uid}).
 * Returns null if not logged in or document doesn't exist.
 */
suspend fun getUserProfile(): UserProfile? {
    val uid = auth.currentUser?.uid ?: return null
    val doc = firestore.collection(USERS_COLLECTION).document(uid).get().await()
    return if (doc.exists()) doc.toObject(UserProfile::class.java) else null
}

/**
 * Listens to the current user's profile document in real time.
 * Emits [UserProfile] when the document exists; emits null if not logged in,
 * doc doesn't exist, or read failed (e.g. PERMISSION_DENIED). Check Logcat on error.
 */
fun userProfileFlow(): Flow<UserProfile?> = callbackFlow {
    val uid = auth.currentUser?.uid
    if (uid == null) {
        trySend(null)
        close()
        return@callbackFlow
    }
    val ref = firestore.collection(USERS_COLLECTION).document(uid)
    val listener = ref.addSnapshotListener { snapshot, e ->
        if (e != null) {
            Log.e("UserRepo", "userProfileFlow read error for users/$uid", e)
            trySend(null)
            close(e)
            return@addSnapshotListener
        }
        if (snapshot == null || !snapshot.exists()) {
            trySend(null) // doc truly doesn't exist
        } else {
            trySend(snapshot.toObject(UserProfile::class.java))
        }
    }
    awaitClose { listener.remove() }
}

/**
 * Creates or overwrites the user document with [profile].
 * Uses the current user's UID as the document ID.
 */
suspend fun setUserProfile(profile: UserProfile) {
    val uid = auth.currentUser?.uid ?: throw IllegalStateException("Not logged in")
    val data = hashMapOf<String, Any>(
        "displayName" to (profile.displayName.ifBlank { "" }),
        "username" to (profile.username.ifBlank { "" }),
        "dietaryPreferences" to profile.dietaryPreferences,
        "groceryList" to profile.groceryList
    )
    firestore.collection(USERS_COLLECTION).document(uid).set(data).await()
}

/**
 * Creates the initial user document at users/{uid} with the given fields.
 * Call this after creating the Auth user (e.g. in CreateAccountScreen).
 * Uses an explicit Map so Firestore reliably creates the document.
 */
suspend fun createUserDocument(uid: String, displayName: String, username: String = "") {
    val data = hashMapOf<String, Any>(
        "displayName" to (displayName.ifBlank { "" }),
        "username" to (username.ifBlank { "" }),
        "dietaryPreferences" to emptyList<String>(),
        "groceryList" to emptyList<String>()
    )
    firestore.collection(USERS_COLLECTION).document(uid).set(data).await()
}

/**
 * Same as createUserDocument but uses callbacks so the UI can wait for the write
 * to finish before navigating. Use this from CreateAccountScreen to avoid
 * coroutine cancellation issues.
 */
fun createUserDocumentWithCallbacks(
    uid: String,
    displayName: String,
    username: String = "",
    onSuccess: () -> Unit,
    onFailure: (Exception) -> Unit
) {
    val data = hashMapOf<String, Any>(
        "displayName" to (displayName.ifBlank { "" }),
        "username" to (username.ifBlank { "" }),
        "dietaryPreferences" to emptyList<String>(),
        "groceryList" to emptyList<String>()
    )
    firestore.collection(USERS_COLLECTION).document(uid).set(data)
        .addOnSuccessListener {
            Log.d("UserRepo", "Created users/$uid")
            onSuccess()
        }
        .addOnFailureListener { e ->
            Log.e("UserRepo", "Failed creating users/$uid", e)
            onFailure(e)
        }
}

/**
 * Deletes the user document from Firestore (users/{uid}).
 * Uses callbacks so the UI can wait for the delete to finish.
 */
fun deleteUserDocumentWithCallbacks(
    uid: String,
    onSuccess: () -> Unit,
    onFailure: (Exception) -> Unit
) {
    firestore.collection(USERS_COLLECTION).document(uid).delete()
        .addOnSuccessListener {
            Log.d("UserRepo", "Deleted users/$uid")
            onSuccess()
        }
        .addOnFailureListener { e ->
            Log.e("UserRepo", "Failed deleting users/$uid", e)
            onFailure(e)
        }
}
