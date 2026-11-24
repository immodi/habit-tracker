package com.modi.habittracker.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import com.google.firebase.Timestamp
import com.modi.habittracker.logic.ScheduleType
import java.time.format.DateTimeFormatter
import java.util.Date

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
) {
    val currentUser: FirebaseUser? get() = auth.currentUser

    /** Emits the current user on sign-in and sign-out. */
    val authState: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    suspend fun signIn(email: String, password: String) {
        auth.signInWithEmailAndPassword(email.trim(), password).await()
    }

    suspend fun signUp(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email.trim(), password).await()
    }

    suspend fun signInWithGoogle(idToken: String) {
        auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await()
    }

    fun signOut() {
        auth.signOut()
    }
}

class HabitRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    private fun habits(uid: String) =
        firestore.collection("users").document(uid).collection("habits")

    private fun logs(uid: String) =
        firestore.collection("users").document(uid).collection("habitLogs")

    private fun preferences(uid: String) =
        firestore.collection("users").document(uid).collection("settings").document("preferences")

    fun observeHabits(uid: String): Flow<List<Habit>> = callbackFlow {
        val registration = habits(uid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val list = snapshot?.documents
                ?.mapNotNull { it.toObject(Habit::class.java) }
                .orEmpty()
                .sortedWith(
                    compareBy<Habit> { it.createdAt?.time ?: Long.MAX_VALUE }
                        .thenBy { it.name },
                )
            trySend(list)
        }
        awaitClose { registration.remove() }
    }

    fun observeLogs(uid: String, startDate: String): Flow<Map<String, Set<String>>> =
        callbackFlow {
            val registration = logs(uid)
                .whereGreaterThanOrEqualTo("date", startDate)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }
                    val byHabit = mutableMapOf<String, MutableSet<String>>()
                    snapshot?.documents?.forEach { document ->
                        val habitId = document.getString("habitId") ?: return@forEach
                        val date = document.getString("date") ?: return@forEach
                        byHabit.getOrPut(habitId) { mutableSetOf() }.add(date)
                    }
                    trySend(byHabit.mapValues { (_, dates) -> dates.toSet() })
                }
            awaitClose { registration.remove() }
        }

    /** null means the reminder is off. */
    fun observeReminderTime(uid: String): Flow<String?> = callbackFlow {
        val registration = preferences(uid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            trySend(snapshot?.getString("reminderTime"))
        }
        awaitClose { registration.remove() }
    }

    suspend fun addHabit(uid: String, draft: HabitDraft) {
        habits(uid).add(
            draftFields(draft) + mapOf(
                "archived" to false,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            ),
        ).await()
    }

    suspend fun updateHabit(uid: String, habitId: String, draft: HabitDraft) {
        habits(uid).document(habitId).update(
            draftFields(draft) + mapOf(
                "updatedAt" to FieldValue.serverTimestamp(),
            ),
        ).await()
    }

    private fun draftFields(draft: HabitDraft): Map<String, Any> {
        val weekdaySchedule = draft.scheduleType == ScheduleType.WEEKDAYS
        return mapOf(
            "name" to draft.name.trim(),
            "scheduleType" to draft.scheduleType.storedValue,
            "weekdays" to if (weekdaySchedule) draft.weekdays.sorted() else emptyList<Int>(),
            "targetCount" to if (weekdaySchedule) {
                1
            } else {
                draft.targetCount.coerceIn(1, draft.scheduleType.maxTarget())
            },
        )
    }

    suspend fun setArchived(uid: String, habitId: String, archived: Boolean) {
        habits(uid).document(habitId).update(
            mapOf(
                "archived" to archived,
                "updatedAt" to FieldValue.serverTimestamp(),
            ),
        ).await()
    }

    suspend fun setCheckIn(uid: String, habitId: String, dateKey: String, done: Boolean) {
        // The deterministic id means two devices marking the same day write one document.
        val reference = logs(uid).document("${habitId}_$dateKey")
        if (done) {
            reference.set(
                mapOf(
                    "habitId" to habitId,
                    "date" to dateKey,
                    "createdAt" to FieldValue.serverTimestamp(),
                ),
            ).await()
        } else {
            reference.delete().await()
        }
    }

    suspend fun setReminderTime(uid: String, time: String?) {
        preferences(uid).set(
            mapOf("reminderTime" to time),
            SetOptions.merge(),
        ).await()
    }

    suspend fun loadReminderTime(uid: String): String? =
        preferences(uid).get().await().getString("reminderTime")

    /** Every habit and every check-in, not just the loaded 90-day window. */
    suspend fun exportJson(uid: String): String {
        val habitDocuments = habits(uid).get().await().documents
        val logDocuments = logs(uid).get().await().documents

        val root = JSONObject()
            .put("exportedAt", DateTimeFormatter.ISO_INSTANT.format(java.time.Instant.now()))
            .put("app", "habit-tracker")

        root.put("habits", JSONArray().apply {
            habitDocuments.forEach { document ->
                put(documentToJson(document.id, document.data))
            }
        })
        root.put("habitLogs", JSONArray().apply {
            logDocuments.forEach { document ->
                put(documentToJson(document.id, document.data))
            }
        })
        return root.toString(2)
    }

    private fun documentToJson(id: String, data: Map<String, Any?>?): JSONObject {
        val json = JSONObject()
        json.put("id", id)
        data?.forEach { (key, value) -> json.put(key, jsonValue(value)) }
        return json
    }

    private fun jsonValue(value: Any?): Any? = when (value) {
        null -> JSONObject.NULL
        is Timestamp -> DateTimeFormatter.ISO_INSTANT.format(value.toDate().toInstant())
        is Date -> DateTimeFormatter.ISO_INSTANT.format(value.toInstant())
        is Map<*, *> -> JSONObject().apply {
            value.forEach { (key, entry) -> put(key.toString(), jsonValue(entry)) }
        }
        is Iterable<*> -> JSONArray().apply { value.forEach { put(jsonValue(it)) } }
        is Array<*> -> JSONArray().apply { value.forEach { put(jsonValue(it)) } }
        else -> value
    }
}
