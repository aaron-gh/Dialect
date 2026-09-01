package com.dialect.launcher.contacts

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager

/**
 * Resolves which communication services are actually usable right now, and builds the intent for
 * one. CALL routes through Android's Telecom framework: any app that registers a self-managed
 * ConnectionService (WhatsApp does, confirmed via `adb shell dumpsys telecom` against a real
 * device — `com.whatsapp/com.whatsapp.calling.telecom.SelfManagedConnectionService`, Schemes: tel)
 * can be dialed directly via ACTION_CALL + EXTRA_PHONE_ACCOUNT_HANDLE, the same mechanism the
 * system Contacts/Phone app uses for its own "Call via WhatsApp" option. This isn't
 * dialer-exclusive; any app holding CALL_PHONE can do it. MESSAGE has no such unified mechanism,
 * so each service gets its own deep link.
 */
class CommunicationServiceResolver(context: Context) {
    private val appContext = context.applicationContext
    private val telecomManager = appContext.getSystemService(Context.TELECOM_SERVICE) as TelecomManager

    fun availableServicesFor(actionType: ContactActionType): List<CommunicationService> {
        return when (actionType) {
            ContactActionType.CALL -> callCapableServices()
            ContactActionType.MESSAGE -> messageCapableServices()
        }
    }

    fun buildIntent(service: CommunicationService, actionType: ContactActionType, phoneNumber: String): Intent? {
        return when (actionType) {
            ContactActionType.CALL -> buildCallIntent(service, phoneNumber)
            ContactActionType.MESSAGE -> buildMessageIntent(service, phoneNumber)
        }
    }

    fun launch(service: CommunicationService, actionType: ContactActionType, phoneNumber: String): Boolean {
        val intent = buildIntent(service, actionType, phoneNumber) ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            appContext.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun callCapableServices(): List<CommunicationService> {
        val registeredPackages = callCapablePackageNames()
        return CommunicationService.entries.filter { service ->
            when (service) {
                CommunicationService.PHONE -> true // native telephony, assumed present on any phone
                CommunicationService.SMS -> false // SMS apps don't place calls
                else -> service.packageName != null && registeredPackages.contains(service.packageName)
            }
        }
    }

    private fun messageCapableServices(): List<CommunicationService> {
        return CommunicationService.entries.filter { service ->
            when (service) {
                CommunicationService.PHONE -> false // no messaging action for the native dialer
                CommunicationService.SMS -> true // default SMS app is always available
                else -> service.packageName != null && isPackageInstalled(service.packageName)
            }
        }
    }

    private fun buildCallIntent(service: CommunicationService, phoneNumber: String): Intent? {
        val uri = Uri.parse("tel:$phoneNumber")
        if (service == CommunicationService.PHONE) {
            return Intent(Intent.ACTION_CALL, uri)
        }
        val handle = findPhoneAccountHandle(service) ?: return null
        return Intent(Intent.ACTION_CALL, uri).putExtra(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, handle)
    }

    private fun buildMessageIntent(service: CommunicationService, phoneNumber: String): Intent? {
        val sanitized = phoneNumber.filter { it.isDigit() || it == '+' }
        return when (service) {
            CommunicationService.SMS -> Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$phoneNumber"))
            CommunicationService.WHATSAPP -> Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$sanitized"))
            CommunicationService.TELEGRAM -> Intent(Intent.ACTION_VIEW, Uri.parse("tg://resolve?phone=$sanitized"))
            CommunicationService.PHONE -> null
        }
    }

    private fun findPhoneAccountHandle(service: CommunicationService): PhoneAccountHandle? {
        val packageName = service.packageName ?: return null
        return callCapableHandles().firstOrNull { it.componentName?.packageName == packageName }
    }

    private fun callCapablePackageNames(): Set<String> {
        return callCapableHandles().mapNotNull { it.componentName?.packageName }.toSet()
    }

    private fun callCapableHandles(): List<PhoneAccountHandle> {
        // getCallCapablePhoneAccounts() alone does not return self-managed accounts (confirmed
        // against a real device: WhatsApp is genuinely registered per `dumpsys telecom`, but was
        // absent from that list) — getSelfManagedPhoneAccounts() is the separate call needed to
        // see apps like WhatsApp, and requires READ_PHONE_STATE rather than CALL_PHONE.
        val managed = try {
            telecomManager.callCapablePhoneAccounts
        } catch (e: SecurityException) {
            emptyList()
        }
        val selfManaged = try {
            telecomManager.selfManagedPhoneAccounts
        } catch (e: SecurityException) {
            emptyList()
        }
        return managed + selfManaged
    }

    private fun isPackageInstalled(packageName: String): Boolean {
        return try {
            appContext.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
}
