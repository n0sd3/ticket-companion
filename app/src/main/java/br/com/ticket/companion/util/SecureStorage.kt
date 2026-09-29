package br.com.ticket.companion.util

import android.content.SharedPreferences
import androidx.core.content.edit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/** Preferências do app (as credenciais ficam com o ConnectionManager). Só o processo principal usa. */
@Singleton
class SecureStorage @Inject constructor(private val prefs: SharedPreferences) {
    var lastSyncTime: Long by stored("last_sync_time", 0L)
    var notifySyncSuccess: Boolean by stored("notify_sync_success", true)
    var notifySyncError: Boolean by stored("notify_sync_error", true)

    /** Endereço do atendimento web quando não há empresa vinculada ao Companion. */
    var webUrl: String? by optionalText("web_url")

    private fun <T : Any> stored(key: String, fallback: T) = object : ReadWriteProperty<Any?, T> {
        @Suppress("UNCHECKED_CAST")
        override fun getValue(thisRef: Any?, property: KProperty<*>): T = when (fallback) {
            is Long -> prefs.getLong(key, fallback) as T
            is Boolean -> prefs.getBoolean(key, fallback) as T
            else -> error("Tipo não suportado: ${fallback::class}")
        }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) = prefs.edit {
            when (value) {
                is Long -> putLong(key, value)
                is Boolean -> putBoolean(key, value)
                else -> error("Tipo não suportado: ${value::class}")
            }
        }
    }

    private fun optionalText(key: String) = object : ReadWriteProperty<Any?, String?> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): String? = prefs.getString(key, null)

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: String?) = prefs.edit {
            if (value == null) remove(key) else putString(key, value)
        }
    }
}
