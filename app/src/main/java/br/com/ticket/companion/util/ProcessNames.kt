package br.com.ticket.companion.util

import android.app.Application
import android.os.Build
import java.io.File

object ProcessNames {
    /** Sem nome de processo, assume o principal: melhor iniciar o Companion duas vezes do que nunca. */
    fun isMain(processName: String?, packageName: String): Boolean = processName == null || processName == packageName

    fun fromCmdline(bytes: ByteArray): String? {
        val end = bytes.indexOf(0.toByte()).let { if (it < 0) bytes.size else it }
        return if (end == 0) null else String(bytes, 0, end, Charsets.UTF_8)
    }

    /** `Application.getProcessName()` só existe no Android 9+; antes lê `/proc/self/cmdline`. */
    fun current(): String? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) Application.getProcessName()
        else runCatching { File("/proc/self/cmdline").readBytes() }.getOrNull()?.let(::fromCmdline)
}
