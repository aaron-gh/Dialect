package com.dialect.launcher.homerole

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.provider.Settings

/**
 * T-7: uses RoleManager.createRequestRoleIntent so TalkBack users get the standard, accessible
 * system role-picker rather than a custom one. minSdk is 31 (RoleManager.ROLE_HOME exists since
 * API 29), so the Settings fallback branch is effectively unreachable on this app's supported
 * range, but is kept per T-7's literal wording in case minSdk ever drops.
 */
object HomeRoleManager {
    fun isDefaultHome(context: Context): Boolean {
        val roleManager = context.getSystemService(Context.ROLE_SERVICE) as? RoleManager
        return roleManager?.isRoleHeld(RoleManager.ROLE_HOME) ?: false
    }

    fun createRequestRoleIntent(context: Context): Intent {
        val roleManager = context.getSystemService(Context.ROLE_SERVICE) as? RoleManager
        return if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
            roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
        } else {
            Intent(Settings.ACTION_HOME_SETTINGS)
        }
    }
}
