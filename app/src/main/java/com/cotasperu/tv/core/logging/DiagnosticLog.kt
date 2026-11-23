package com.cotasperu.tv.core.logging

class DiagnosticLog(
    private val enabled: Boolean,
    private val sink: (String) -> Unit,
) {
    fun event(operation: String, status: Int? = null, correlationId: String? = null) {
        if (!enabled) return
        val safeOperation = operation.takeIf { OPERATION.matches(it) } ?: "unknown"
        val safeCorrelation = correlationId?.takeIf { CORRELATION.matches(it) } ?: "-"
        sink("operation=$safeOperation status=${status ?: -1} correlation=$safeCorrelation")
    }

    companion object {
        private val OPERATION = Regex("^[a-z0-9_]{1,40}$")
        private val CORRELATION = Regex("^[A-Za-z0-9-]{8,80}$")

        fun silent(): DiagnosticLog = DiagnosticLog(enabled = false, sink = {})
    }
}
