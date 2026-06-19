package com.genesiscruz.adwarehuli.data.net

import java.net.Inet4Address
import java.nio.ByteBuffer

/** Minimal read-only view over a captured IPv4 packet, just enough to find UDP/53 traffic. */
class IpV4PacketView(private val buffer: ByteArray, private val length: Int) {

    val version: Int get() = (buffer[0].toInt() shr 4) and 0x0F
    private val headerLength: Int get() = (buffer[0].toInt() and 0x0F) * 4
    val protocol: Int get() = buffer[9].toInt() and 0xFF

    val sourceAddress: Inet4Address get() = addressAt(12)
    val destinationAddress: Inet4Address get() = addressAt(16)

    val isValid: Boolean get() = length >= 20 && version == 4 && headerLength in 20..length

    fun udpView(): UdpDatagramView? {
        if (protocol != PROTOCOL_UDP) return null
        val offset = headerLength
        if (length - offset < 8) return null
        return UdpDatagramView(buffer, offset, length)
    }

    private fun addressAt(offset: Int): Inet4Address =
        Inet4Address.getByAddress(buffer.copyOfRange(offset, offset + 4)) as Inet4Address

    companion object {
        const val PROTOCOL_UDP = 17
        const val PROTOCOL_TCP = 6
    }
}

/** Read-only view over the UDP segment that follows an IPv4 header. */
class UdpDatagramView(private val buffer: ByteArray, private val offset: Int, private val totalLength: Int) {
    val sourcePort: Int get() = u16(offset)
    val destinationPort: Int get() = u16(offset + 2)
    private val udpLength: Int get() = u16(offset + 4)
    val payloadOffset: Int get() = offset + 8
    val payloadLength: Int get() = (udpLength - 8).coerceAtLeast(0).coerceAtMost(totalLength - payloadOffset)

    fun payload(): ByteArray = buffer.copyOfRange(payloadOffset, payloadOffset + payloadLength)

    private fun u16(at: Int): Int = ((buffer[at].toInt() and 0xFF) shl 8) or (buffer[at + 1].toInt() and 0xFF)
}

/**
 * Builds a synthetic IPv4/UDP reply packet to inject back into the tun fd —
 * used to deliver a real (or, if blocking, spoofed) DNS response to the app
 * that issued the original query.
 */
object UdpReplyBuilder {

    fun build(
        sourceAddress: Inet4Address,
        sourcePort: Int,
        destinationAddress: Inet4Address,
        destinationPort: Int,
        payload: ByteArray
    ): ByteArray {
        val totalLength = 20 + 8 + payload.size
        val buffer = ByteBuffer.allocate(totalLength)

        // IPv4 header
        buffer.put(0x45.toByte()) // version 4, header length 5 words
        buffer.put(0) // DSCP/ECN
        buffer.putShort(totalLength.toShort())
        buffer.putShort(0) // identification
        buffer.putShort(0x4000.toShort()) // flags: don't fragment
        buffer.put(64) // TTL
        buffer.put(IpV4PacketView.PROTOCOL_UDP.toByte())
        buffer.putShort(0) // checksum placeholder
        buffer.put(sourceAddress.address)
        buffer.put(destinationAddress.address)

        val ipHeader = buffer.array().copyOfRange(0, 20)
        val ipChecksum = checksum(ipHeader)
        buffer.putShort(10, ipChecksum.toShort())

        // UDP header (checksum left as 0 — optional for IPv4, explicitly allowed by RFC 768)
        buffer.putShort(20, sourcePort.toShort())
        buffer.putShort(22, destinationPort.toShort())
        buffer.putShort(24, (8 + payload.size).toShort())
        buffer.putShort(26, 0)

        buffer.position(28)
        buffer.put(payload)

        return buffer.array()
    }

    private fun checksum(header: ByteArray): Int {
        var sum = 0
        var i = 0
        while (i < header.size) {
            val word = ((header[i].toInt() and 0xFF) shl 8) or (header[i + 1].toInt() and 0xFF)
            sum += word
            i += 2
        }
        while (sum shr 16 != 0) {
            sum = (sum and 0xFFFF) + (sum shr 16)
        }
        return sum.inv() and 0xFFFF
    }
}
