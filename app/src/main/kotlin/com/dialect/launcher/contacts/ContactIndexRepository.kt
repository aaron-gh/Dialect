package com.dialect.launcher.contacts

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import com.dialect.launcher.matching.T9Sequence
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Live contact index, gated entirely behind READ_CONTACTS being granted (only requested when the
 * user turns the "Contact search & dialing" setting on). Mirrors AppIndexRepository's shape: a
 * StateFlow rebuilt off the main thread, kept live via a ContentObserver instead of a callback.
 */
class ContactIndexRepository(context: Context) {
    private val appContext = context.applicationContext
    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _contacts = MutableStateFlow<List<ContactIndexEntry>>(emptyList())
    val contacts: StateFlow<List<ContactIndexEntry>> = _contacts.asStateFlow()

    private var started = false

    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            rebuild()
        }
    }

    fun start() {
        if (started) return
        started = true
        appContext.contentResolver.registerContentObserver(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            true,
            observer,
        )
        rebuild()
    }

    fun stop() {
        if (!started) return
        started = false
        appContext.contentResolver.unregisterContentObserver(observer)
        _contacts.value = emptyList()
    }

    fun rebuild() {
        if (!hasPermission()) {
            _contacts.value = emptyList()
            return
        }
        repoScope.launch {
            _contacts.value = queryContacts()
        }
    }

    private fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(appContext, Manifest.permission.READ_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun queryContacts(): List<ContactIndexEntry> {
        val results = mutableListOf<ContactIndexEntry>()
        val seenContactIds = mutableSetOf<Long>()
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
        )
        appContext.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            null,
            null,
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC",
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val nameIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (cursor.moveToNext()) {
                val contactId = cursor.getLong(idIndex)
                // A contact can have several numbers; the first one returned wins for MVP.
                if (!seenContactIds.add(contactId)) continue
                val name = cursor.getString(nameIndex) ?: continue
                val number = cursor.getString(numberIndex) ?: continue
                results += ContactIndexEntry(
                    contactId = contactId,
                    phoneNumber = number,
                    displayName = name,
                    fullPrefixDigits = T9Sequence.fullPrefixDigits(name),
                    wordInitialDigits = T9Sequence.wordInitialDigits(name),
                )
            }
        }
        return results
    }
}
