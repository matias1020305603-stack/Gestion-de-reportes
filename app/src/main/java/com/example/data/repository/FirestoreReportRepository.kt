package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.ReportDocument
import com.example.data.model.UserProfileDocument
import com.example.util.OperationType
import com.example.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FirestoreReportRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeUserReports(): Flow<List<ReportDocument>> = flow {
        val uid = requireUserId()
        val path = "reports"
        emitAll(
            db.collection("reports")
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snapshot -> snapshot.toObjects(ReportDocument::class.java) }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun createReport(report: ReportDocument): Result<String> {
        val uid = requireUserId()
        val reportsRef = db.collection("reports")
        return try {
            val docRef = reportsRef.document()
            val payload = hashMapOf<String, Any>(
                "userId" to uid,
                "code" to report.code.ifBlank { "REP-" + System.currentTimeMillis().toString().takeLast(4) },
                "title" to report.title,
                "description" to report.description,
                "location" to report.location,
                "category" to report.category,
                "severity" to report.severity,
                "status" to report.status,
                "authorName" to report.authorName,
                "authorRole" to report.authorRole,
                "authorEmail" to report.authorEmail,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!report.assignedTo.isNullOrBlank()) {
                payload["assignedTo"] = report.assignedTo
            }
            if (!report.resolutionNotes.isNullOrBlank()) {
                payload["resolutionNotes"] = report.resolutionNotes
            }

            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, reportsRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateReportStatus(reportId: String, status: String, notes: String): Result<Unit> {
        require(reportId.isNotBlank()) { "Report ID must not be blank" }
        val docRef = db.collection("reports").document(reportId)
        return try {
            val updates = hashMapOf<String, Any>(
                "status" to status,
                "resolutionNotes" to notes,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun saveUserProfile(profile: UserProfileDocument): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        return try {
            val payload = hashMapOf<String, Any>(
                "userId" to uid,
                "username" to profile.username,
                "fullName" to profile.fullName,
                "email" to profile.email,
                "role" to profile.role,
                "avatarId" to profile.avatarId,
                "avatarColorHex" to profile.avatarColorHex,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun getUserProfile(userId: String): Result<UserProfileDocument?> {
        val docRef = db.collection("users").document(userId)
        return try {
            val snapshot = docRef.get().await()
            Result.success(snapshot.toObject(UserProfileDocument::class.java))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }
}
