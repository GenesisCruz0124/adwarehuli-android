package com.genesiscruz.adwarehuli.data.net

/**
 * Just enough DNS message parsing to read the queried hostname out of a
 * UDP/53 query, and to synthesize an NXDOMAIN reply for blocked domains.
 * We never need to parse real answer records — successful lookups are
 * forwarded to the device byte-for-byte from the upstream resolver.
 */
object DnsMessage {

    /** Extracts the first question's hostname from a raw DNS query payload, or null if unparsable. */
    fun parseQuestionHostname(query: ByteArray): String? {
        if (query.size < 12) return null
        val questionCount = u16(query, 4)
        if (questionCount < 1) return null

        val labels = StringBuilder()
        var offset = 12
        while (offset < query.size) {
            val labelLength = query[offset].toInt() and 0xFF
            if (labelLength == 0) break
            offset += 1
            if (offset + labelLength > query.size) return null
            if (labels.isNotEmpty()) labels.append('.')
            labels.append(String(query, offset, labelLength, Charsets.US_ASCII))
            offset += labelLength
        }
        return labels.toString().takeIf { it.isNotEmpty() }
    }

    /**
     * Builds a synthetic NXDOMAIN response that mirrors the original query's
     * ID and question section, used only when the user has opted into the
     * blocking toggle for a flagged domain.
     */
    fun buildNxDomainResponse(query: ByteArray): ByteArray {
        val response = query.copyOf()
        // Flags: QR=1 (response), Opcode=0, AA=0, TC=0, RD copied, RA=1, RCODE=3 (NXDOMAIN)
        response[2] = 0x81.toByte()
        response[3] = 0x83.toByte()
        // ANCOUNT = NSCOUNT = ARCOUNT = 0 (QDCOUNT is left untouched, mirroring the query)
        response[6] = 0; response[7] = 0
        response[8] = 0; response[9] = 0
        response[10] = 0; response[11] = 0
        return response
    }

    private fun u16(buffer: ByteArray, at: Int): Int =
        ((buffer[at].toInt() and 0xFF) shl 8) or (buffer[at + 1].toInt() and 0xFF)
}
