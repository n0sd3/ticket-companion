package br.com.ticket.companion.service

/**
 * Memória do que o listener viu, só para a tela de Ajustes explicar por que uma notificação não virou
 * evento. Fica na memória do processo (o listener e os Ajustes vivem no mesmo); guarda pacote e horário,
 * nunca o texto da notificação.
 */
object CaptureDiagnostics {
    data class Snapshot(val listenerConnected: Boolean, val lastPackage: String?, val lastAt: Long, val lastOutcome: String?, val seen: Int)

    @Volatile private var state = Snapshot(false, null, 0L, null, 0)

    fun connected(value: Boolean) { state = state.copy(listenerConnected = value) }
    fun seen(packageName: String, at: Long) { state = state.copy(lastPackage = packageName, lastAt = at, lastOutcome = null, seen = state.seen + 1) }
    fun outcome(text: String) { state = state.copy(lastOutcome = text) }
    fun snapshot(): Snapshot = state
    fun reset() { state = Snapshot(false, null, 0L, null, 0) }
}
