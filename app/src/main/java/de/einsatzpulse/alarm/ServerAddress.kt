package de.einsatzpulse.alarm

import java.net.URI

/** Hard fail on HTTP, credentials, URLs with paths or ambiguous query/fragment. */
fun normalizeServerAddress(value: String): String {
    val raw = value.trim().trimEnd('/')
    val uri = try { URI(raw) } catch (_: Exception) { throw IllegalArgumentException("Ungültige Serveradresse") }
    require(uri.scheme?.lowercase() == "https" &&
        !uri.host.isNullOrBlank() && uri.userInfo == null &&
        (uri.path.isNullOrEmpty() || uri.path == "/") &&
        uri.query == null && uri.fragment == null &&
        uri.port in -1..65535 && uri.host.length <= 253) {
        "Nur vollständige HTTPS-Serveradressen ohne Unterpfad sind erlaubt."
    }
    return raw
}
