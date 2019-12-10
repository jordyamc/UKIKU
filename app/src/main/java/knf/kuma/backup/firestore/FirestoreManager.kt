package knf.kuma.backup.firestore

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.afollestad.materialdialogs.MaterialDialog
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import knf.kuma.App
import knf.kuma.BuildConfig
import knf.kuma.R
import knf.kuma.ads.SubscriptionReceiver
import knf.kuma.backup.Backups
import knf.kuma.backup.firestore.data.*
import knf.kuma.commons.*
import knf.kuma.database.CacheDB
import knf.kuma.database.EADB
import knf.kuma.pojos.SeenObject
import kotlinx.coroutines.*
import org.jetbrains.anko.doAsync
import xdroid.toaster.Toaster.toast
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object FirestoreManager {
    enum class State { IDLE, UPLOAD, SYNC }

    val firestoreDB by lazy { Firebase.firestore }
    val user: FirebaseUser? get() = FirebaseAuth.getInstance().currentUser
    val uid: String? get() = user?.uid

    private val listeners = mutableListOf<ListenerRegistration>()

    val isLoggedIn: Boolean get() = uid != null

    val favsLiveData = MutableLiveData(State.IDLE)
    val seenLiveData = MutableLiveData(State.IDLE)
    val eaLiveData = MutableLiveData(State.IDLE)
    val achievementsLiveData = MutableLiveData(State.IDLE)
    val genresLiveData = MutableLiveData(State.IDLE)
    val historyLiveData = MutableLiveData(State.IDLE)
    val queueLiveData = MutableLiveData(State.IDLE)
    val seeingLiveData = MutableLiveData(State.IDLE)

    private var isUpdateBlocked = false

    @ExperimentalContracts
    @ExperimentalCoroutinesApi
    fun start() {
        if (isLoggedIn && ((PrefsUtil.isAdsEnabled && !Network.isAdsBlocked) || BuildConfig.DEBUG || admFile.exists() || PrefsUtil.isSubscriptionEnabled)) {
            QueueManager.open()
            doAsync {
                firestoreDB.document("users/$uid/backups/history").addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
                    doAsync {
                        if (documentSnapshot.needsUpdate() && !isUpdateBlocked) {
                            runBlocking(Dispatchers.Main) { historyLiveData.value = State.SYNC }
                            documentSnapshot.toObject<HistoryData>()?.list?.let {
                                CacheDB.INSTANCE.recordsDAO().apply {
                                    clear()
                                    addAll(it)
                                }
                                PrefsUtil.lsHistory = currentTime()
                                Log.e("Firestore", "History updated")
                            }
                            runBlocking(Dispatchers.Main) { historyLiveData.value = State.IDLE }
                        } else firebaseFirestoreException?.printStackTrace()
                    }
                }.also { listeners.add(it) }
                firestoreDB.document("users/$uid/backups/achievements").addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
                    doAsync {
                        if (documentSnapshot.needsUpdate() && !isUpdateBlocked) {
                            runBlocking(Dispatchers.Main) { achievementsLiveData.value = State.SYNC }
                            documentSnapshot.toObject<AchievementsData>()?.list?.let {
                                CacheDB.INSTANCE.achievementsDAO().apply {
                                    update(it)
                                }
                                PrefsUtil.lsAchievements = currentTime()
                                Log.e("Firestore", "Achievements updated")
                            }
                            runBlocking(Dispatchers.Main) { achievementsLiveData.value = State.IDLE }
                        } else firebaseFirestoreException?.printStackTrace()
                    }
                }.also { listeners.add(it) }
                firestoreDB.document("users/$uid/backups/ea").addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
                    doAsync {
                        if (documentSnapshot.needsUpdate() && !isUpdateBlocked) {
                            runBlocking(Dispatchers.Main) { eaLiveData.value = State.SYNC }
                            documentSnapshot.toObject<EAData>()?.list?.let {
                                EADB.INSTANCE.eaDAO().apply {
                                    unlock(it)
                                }
                                PrefsUtil.lsEa = currentTime()
                                Log.e("Firestore", "EA Updated")
                            }
                            runBlocking(Dispatchers.Main) { eaLiveData.value = State.IDLE }
                        } else firebaseFirestoreException?.printStackTrace()
                    }
                }.also { listeners.add(it) }
                firestoreDB.document("users/$uid/backups/favs").addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
                    doAsync {
                        if (documentSnapshot.needsUpdate() && !isUpdateBlocked) {
                            runBlocking(Dispatchers.Main) { favsLiveData.value = State.SYNC }
                            documentSnapshot.toObject<FavsData>()?.list?.let {
                                CacheDB.INSTANCE.favsDAO().apply {
                                    clear()
                                    addAll(it)
                                }
                                PrefsUtil.lsFavs = currentTime()
                                Log.e("Firestore", "Favs updated")
                            }
                            runBlocking(Dispatchers.Main) { favsLiveData.value = State.IDLE }
                        } else firebaseFirestoreException?.printStackTrace()
                    }
                }.also { listeners.add(it) }
                firestoreDB.document("users/$uid/backups/genres").addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
                    doAsync {
                        if (documentSnapshot.needsUpdate() && !isUpdateBlocked) {
                            runBlocking(Dispatchers.Main) { genresLiveData.value = State.SYNC }
                            documentSnapshot.toObject<GenresData>()?.list?.let {
                                CacheDB.INSTANCE.genresDAO().apply {
                                    reset()
                                    insertStatus(it)
                                }
                                PrefsUtil.lsGenres = currentTime()
                                Log.e("Firestore", "Genres updated")
                            }
                            runBlocking(Dispatchers.Main) { genresLiveData.value = State.IDLE }
                        } else firebaseFirestoreException?.printStackTrace()
                    }
                }.also { listeners.add(it) }
                firestoreDB.document("users/$uid/backups/queue").addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
                    doAsync {
                        if (documentSnapshot.needsUpdate() && !isUpdateBlocked) {
                            runBlocking(Dispatchers.Main) { queueLiveData.value = State.SYNC }
                            documentSnapshot.toObject<QueueData>()?.list?.let {
                                CacheDB.INSTANCE.queueDAO().apply {
                                    nuke()
                                    add(it)
                                }
                                PrefsUtil.lsQueue = currentTime()
                                Log.e("Firestore", "Queue updated")
                            }
                            runBlocking(Dispatchers.Main) { queueLiveData.value = State.IDLE }
                        } else firebaseFirestoreException?.printStackTrace()
                    }
                }.also { listeners.add(it) }
                firestoreDB.document("users/$uid/backups/seeing").addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
                    doAsync {
                        if (documentSnapshot.needsUpdate() && !isUpdateBlocked) {
                            runBlocking(Dispatchers.Main) { seeingLiveData.value = State.SYNC }
                            documentSnapshot.toObject<SeeingData>()?.list?.let {
                                CacheDB.INSTANCE.seeingDAO().apply {
                                    clear()
                                    addAll(it)
                                }
                                PrefsUtil.lsSeeing = currentTime()
                                Log.e("Firestore", "Seeing updated")
                            }
                            runBlocking(Dispatchers.Main) { seeingLiveData.value = State.IDLE }
                        } else firebaseFirestoreException?.printStackTrace()
                    }
                }.also { listeners.add(it) }
                firestoreDB.collection("users/$uid/backups/seen/data").addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                    doAsync {
                        if (querySnapshot.needsUpdate() && !isUpdateBlocked) {
                            runBlocking(Dispatchers.Main) { seenLiveData.value = State.SYNC }
                            val nList = mutableListOf<SeenObject>()
                            querySnapshot.documents.forEach {
                                it.toObject<SeenData>()?.list?.let { seenList ->
                                    nList.addAll(seenList)
                                }
                            }
                            CacheDB.INSTANCE.seenDAO().apply {
                                clear()
                                addAll(nList)
                            }
                            PrefsUtil.lsSeen = currentTime()
                            Log.e("Firestore", "Seen updated")
                            runBlocking(Dispatchers.Main) { seenLiveData.value = State.IDLE }
                        } else firebaseFirestoreException?.printStackTrace()
                    }
                }.also { listeners.add(it) }
                firestoreDB.document("subscriptions/$uid").get().addOnSuccessListener {
                    if (it.exists() && PrefsUtil.subscriptionToken == null) {
                        noCrash {
                            it.toObject<SubscriptionReceiver.SubscriptionInfo>()?.let {
                                GlobalScope.launch(Dispatchers.IO) {
                                    val info = SubscriptionReceiver.checkStatus(it.token)
                                    if (info.isVerified) {
                                        PrefsUtil.subscriptionToken = it.token
                                        toast("Suscripción restaurada")
                                    } else {
                                        firestoreDB.document("subscriptions/$uid").delete()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else if (isLoggedIn) {
            doSignOut(App.context)
            Backups.type = Backups.Type.NONE
            toast("Firestore deshabilitado")
        }
        doAsync {
            firestoreDB.document("top/${uid
                    ?: PrefsUtil.instanceUuid}").addSnapshotListener { documentSnapshot, _ ->
                doAsync {
                    if (documentSnapshot.needsUpdate() && !isUpdateBlocked) {
                        documentSnapshot.toObject<TopData>()?.let {
                            if (it.isForced) {
                                user?.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(it.name).build())
                                        ?: { PrefsUtil.instanceName = it.name }()
                            }
                            PrefsUtil.userRewardedVideoCount = it.number
                            Log.e("Firestore", "Top updated")
                        }
                    }
                }
            }.also { listeners.add(it) }
        }
    }

    fun stop() {
        QueueManager.close()
        listeners.forEach { it.remove() }
    }

    @ExperimentalCoroutinesApi
    @ExperimentalContracts
    private fun uploadAllData(checkForFiles: Boolean, activity: Activity) {
        if (checkForFiles)
            firestoreDB.collection("users/$uid/backups").get()
                    .addOnSuccessListener {
                        if (it.isEmpty) {
                            MaterialDialog(activity).safeShow {
                                title(text = "¿Nuevo usuario?")
                                message(text = "Este parece ser tu primer inicio de sesion, tus datos necesitan ser subidos a la nube, primero asegurate que éste sea tu dispositivo principal!")
                                cancelable(false)
                                positiveButton(text = "Subir") {
                                    setDefaultDevice()
                                    uploadAllData(false, activity)
                                }
                                negativeButton(text = "Cerrar sesion") {
                                    doSignOut(activity)
                                }
                            }
                            firestoreDB.document("top/${PrefsUtil.instanceUuid}").delete()
                        } else {
                            firestoreDB.document("users/$uid/backups/info")
                                    .get().addOnCompleteListener { document ->
                                        val data = document.result?.data
                                        if (data != null && data["uuid"] == PrefsUtil.instanceUuid) {
                                            MaterialDialog(activity).safeShow {
                                                title(text = "Bienvenido de nuevo")
                                                message(text = "Actualmente tienes datos en la nube y este es tu dispositivo principal, ¿que datos quieres usar?\n(Usar tus datos locales sobreescribirá lo que haya en la nube)")
                                                cancelable(false)
                                                positiveButton(text = "Datos locales") {
                                                    uploadAllData(false, activity)
                                                }
                                                negativeButton(text = "Descargar de la nube") {
                                                    start()
                                                }
                                            }
                                        } else {
                                            toast("Se descargarán tus datos de la nube")
                                            start()
                                        }
                                    }
                        }
                    }
        else {
            stop()
            isUpdateBlocked = true
            QueueManager.open()
            Log.e("Firestore", "On upload all data")
            syncData {
                history()
                seen()
                achievements()
                ea()
                favs()
                genres()
                queue()
                seeing()
                top()
            }
            GlobalScope.launch(Dispatchers.IO) {
                delay(10000)
                isUpdateBlocked = false
                start()
            }
        }
    }

    private fun setDefaultDevice() {
        firestoreDB.document("users/$uid/backups/info").set(mapOf("uuid" to PrefsUtil.instanceUuid))
    }

    fun updateHistory(collection: CollectionReference) =
            noCrash {
                doOnUI { historyLiveData.value = State.SYNC }
                collection.document("history").set(HistoryData.create()).addOnSuccessListener {
                    Log.e("Firestore", "History upload success")
                    PrefsUtil.lsHistory = currentTime()
                    doOnUI { historyLiveData.value = State.IDLE }
                }.addOnFailureListener {
                    Log.e("Firestore", "History upload error", it)
                    doOnUI { historyLiveData.value = State.IDLE }
                }
            }

    fun updateSeen(collection: CollectionReference) =
            noCrash {
                doOnUI { seenLiveData.value = State.SYNC }
                val data = SeenData.create()
                val segments = data.list.chunked(10000).map { SeenData(it) }
                collection.document("seen").set(mapOf("size" to segments.size))
                val subcollection = collection.document("seen").collection("data")
                segments.forEachIndexed { index, seenData ->
                    subcollection.document("seen_$index").set(seenData).addOnSuccessListener {
                        Log.e("Firestore", "Seen_$index upload success")
                    }.addOnFailureListener {
                        Log.e("Firestore", "Seen_$index upload error", it)
                    }
                }
                runBlocking {
                    var nextIndex = data.list.size
                    var needsNext = true
                    while (needsNext) {
                        needsNext = suspendCoroutine {
                            noCrashLet(false) {
                                val reference = subcollection.document("seen_$needsNext")
                                reference.get().addOnCompleteListener { subDocument ->
                                    noCrashExec(exec = { it.resume(false) }) {
                                        if (subDocument.result?.exists() == true) {
                                            reference.delete()
                                            it.resume(true)
                                        } else
                                            it.resume(false)
                                    }
                                }
                            }
                        }
                        nextIndex++
                    }
                }
                PrefsUtil.lsSeen = currentTime()
                doOnUI { seenLiveData.value = State.IDLE }
            }

    fun updateAchievements(collection: CollectionReference) =
            noCrash {
                doOnUI { achievementsLiveData.value = State.SYNC }
                collection.document("achievements").set(AchievementsData.create()).addOnSuccessListener {
                    Log.e("Firestore", "Achievements upload success")
                    PrefsUtil.lsAchievements = currentTime()
                    doOnUI { achievementsLiveData.value = State.IDLE }
                }.addOnFailureListener {
                    Log.e("Firestore", "Achievements upload error", it)
                    doOnUI { achievementsLiveData.value = State.IDLE }
                }
            }

    fun updateEA(collection: CollectionReference) =
            noCrash {
                doOnUI { eaLiveData.value = State.SYNC }
                collection.document("ea").set(EAData.create()).addOnSuccessListener {
                    Log.e("Firestore", "EA upload success")
                    PrefsUtil.lsEa = currentTime()
                    doOnUI { eaLiveData.value = State.IDLE }
                }.addOnFailureListener {
                    Log.e("Firestore", "EA upload error", it)
                    doOnUI { eaLiveData.value = State.IDLE }
                }
            }

    fun updateFavs(collection: CollectionReference) =
            noCrash {
                doOnUI { favsLiveData.value = State.SYNC }
                collection.document("favs").set(FavsData.create()).addOnSuccessListener {
                    Log.e("Firestore", "Favs upload success")
                    PrefsUtil.lsFavs = currentTime()
                    doOnUI { favsLiveData.value = State.IDLE }
                }.addOnFailureListener {
                    Log.e("Firestore", "Favs upload error", it)
                    doOnUI { favsLiveData.value = State.IDLE }
                }
            }

    fun updateGenres(collection: CollectionReference) =
            noCrash {
                doOnUI { genresLiveData.value = State.SYNC }
                collection.document("genres").set(GenresData.create()).addOnSuccessListener {
                    Log.e("Firestore", "Genres upload success")
                    PrefsUtil.lsGenres = currentTime()
                    doOnUI { genresLiveData.value = State.IDLE }
                }.addOnFailureListener {
                    Log.e("Firestore", "Genres upload error", it)
                    doOnUI { genresLiveData.value = State.IDLE }
                }
            }

    fun updateQueue(collection: CollectionReference) =
            noCrash {
                doOnUI { queueLiveData.value = State.SYNC }
                collection.document("queue").set(QueueData.create()).addOnSuccessListener {
                    Log.e("Firestore", "Queue upload success")
                    PrefsUtil.lsQueue = currentTime()
                    doOnUI { queueLiveData.value = State.IDLE }
                }.addOnFailureListener {
                    Log.e("Firestore", "Queue upload error", it)
                    doOnUI { queueLiveData.value = State.IDLE }
                }
            }

    fun updateSeeing(collection: CollectionReference) =
            noCrash {
                doOnUI { seeingLiveData.value = State.SYNC }
                collection.document("seeing").set(SeeingData.create()).addOnSuccessListener {
                    Log.e("Firestore", "Seeing upload success")
                    PrefsUtil.lsSeeing = currentTime()
                    doOnUI { seeingLiveData.value = State.IDLE }
                }.addOnFailureListener {
                    Log.e("Firestore", "Seeing upload error", it)
                    doOnUI { seeingLiveData.value = State.IDLE }
                }
            }

    fun updateTop() =
            doAsync {
                noCrash {
                    firestoreDB.document("top/${uid
                            ?: PrefsUtil.instanceUuid}").set(TopData.create())
                    Log.e("Firestore", "Top upload success")
                }
            }

    @ExperimentalContracts
    fun listenTop(callback: (list: List<TopData>) -> Unit) =
            firestoreDB.collection("top").addSnapshotListener { querySnapshot, exception ->
                exception?.let { Log.e("Firestore", "Top Query Error", it) }
                Log.e("Firestore", "On tops update")
                querySnapshot?.let { callback(it.documents.mapNotNull { document -> document.toObject<TopData>() }) }
            }

    fun doLogin(activity: Activity) {
        if (isLoggedIn) {
            MaterialDialog(activity).safeShow {
                message(text = "Deseas cerrar sesión?")
                positiveButton(text = "Cerrar sesion") {
                    doSignOut(activity)
                }
            }
        } else {
            val providers = arrayListOf(
                    AuthUI.IdpConfig.EmailBuilder().build(),
                    AuthUI.IdpConfig.GoogleBuilder().build(),
                    AuthUI.IdpConfig.TwitterBuilder().build()
            )
            activity.startActivityForResult(
                    AuthUI.getInstance()
                            .createSignInIntentBuilder()
                            .setAvailableProviders(providers)
                            .setLogo(R.drawable.ic_launcher_login)
                            .build()
                    , 5548
            )
        }
    }

    fun doSignOut(context: Context) = AuthUI.getInstance().signOut(context)

    @ExperimentalContracts
    @ExperimentalCoroutinesApi
    fun handleLogin(activity: Activity, requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 5548) {
            val response = IdpResponse.fromResultIntent(data)
            if (resultCode == Activity.RESULT_OK) {
                Backups.type = Backups.Type.FIRESTORE
                uploadAllData(true, activity)
            } else if (response != null) {
                val error = response.error
                error?.printStackTrace()
                toast("Error al iniciar sesion: ${error?.message}")
            }
        }
    }

}

@ExperimentalContracts
fun DocumentSnapshot?.needsUpdate(): Boolean {
    contract { returns(true) implies (this@needsUpdate != null) }
    return this != null && !this.metadata.hasPendingWrites() && this.exists()
}

@ExperimentalContracts
fun QuerySnapshot?.needsUpdate(): Boolean {
    contract { returns(true) implies (this@needsUpdate != null) }
    return this != null && !this.metadata.hasPendingWrites()
}

class SyncRequest(private val collection: CollectionReference) {
    private val syncList = mutableListOf<() -> Unit>()

    fun history() = syncList.add { runBlocking(Dispatchers.IO) { FirestoreManager.updateHistory(collection) } }
    fun seen() = syncList.add { runBlocking(Dispatchers.IO) { FirestoreManager.updateSeen(collection) } }
    fun achievements() = syncList.add { runBlocking(Dispatchers.IO) { FirestoreManager.updateAchievements(collection) } }
    fun ea() = syncList.add { runBlocking(Dispatchers.IO) { FirestoreManager.updateEA(collection) } }
    fun favs() = syncList.add { runBlocking(Dispatchers.IO) { FirestoreManager.updateFavs(collection) } }
    fun genres() = syncList.add { runBlocking(Dispatchers.IO) { FirestoreManager.updateGenres(collection) } }
    fun queue() = syncList.add { runBlocking(Dispatchers.IO) { FirestoreManager.updateQueue(collection) } }
    fun seeing() = syncList.add { runBlocking(Dispatchers.IO) { FirestoreManager.updateSeeing(collection) } }
    fun top() = syncList.add { runBlocking(Dispatchers.IO) { FirestoreManager.updateTop() } }

    fun sync() {
        if (FirestoreManager.isLoggedIn)
            QueueManager.add(syncList)
    }
}

fun syncData(uploads: SyncRequest.() -> Unit) {
    val syncRequest = SyncRequest(FirestoreManager.firestoreDB.collection("users/${FirestoreManager.uid}/backups"))
    uploads(syncRequest)
    syncRequest.sync()
}