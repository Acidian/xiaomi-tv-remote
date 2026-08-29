package za.co.acidian.xiaomitvremote

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Handler
import android.os.Looper
import android.util.Base64
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.EOFException
import java.io.InputStream
import java.io.OutputStream
import java.math.BigInteger
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Date
import java.util.concurrent.Executors
import javax.net.ssl.KeyManager
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLException
import javax.net.ssl.SSLHandshakeException
import javax.net.ssl.SSLSocket
import javax.net.ssl.TrustManager
import javax.net.ssl.X509ExtendedKeyManager
import javax.net.ssl.X509TrustManager
import javax.security.auth.x500.X500Principal
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.BasicConstraints
import org.bouncycastle.asn1.x509.Extension
import org.bouncycastle.asn1.x509.GeneralName
import org.bouncycastle.asn1.x509.GeneralNames
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder

/**
 * Native Android TV Remote Service v2 client.
 * Pairing uses TLS on 6467; commands use TLS on 6466.
 *
 * The client identity intentionally uses an ordinary software RSA key rather than
 * AndroidKeyStore. Some Android/Conscrypt combinations fail TLS client-auth when an
 * AndroidKeyStore RSA key is presented to the Android TV pairing service.
 */
class AndroidTvRemoteClient(
    private val context: Context,
    private val listener: Listener,
) {
    data class TvDevice(val name: String, val host: String, val port: Int = 6466)

    interface Listener {
        fun onStatus(status: String)
        fun onDevices(devices: List<TvDevice>)
        fun onPairingCodeRequested(host: String)
        fun onConnected(host: String)
        fun onDisconnected(reason: String)
        fun onPowerChanged(on: Boolean) {}
    }

    companion object {
        const val KEY_HOME = 3
        const val KEY_BACK = 4
        const val KEY_DPAD_UP = 19
        const val KEY_DPAD_DOWN = 20
        const val KEY_DPAD_LEFT = 21
        const val KEY_DPAD_RIGHT = 22
        const val KEY_DPAD_CENTER = 23
        const val KEY_VOLUME_UP = 24
        const val KEY_VOLUME_DOWN = 25
        const val KEY_POWER = 26
        const val KEY_ENTER = 66
        const val KEY_DEL = 67
        const val KEY_MENU = 82
        const val KEY_SEARCH = 84
        const val KEY_MEDIA_PLAY_PAUSE = 85
        const val KEY_MEDIA_NEXT = 87
        const val KEY_MEDIA_PREVIOUS = 88
        const val KEY_MEDIA_REWIND = 89
        const val KEY_MEDIA_FAST_FORWARD = 90
        const val KEY_MUTE = 91
        const val KEY_SETTINGS = 176
        const val KEY_SLEEP = 223
        const val KEY_WAKEUP = 224

        private const val SERVICE_TYPE = "_androidtvremote2._tcp."
        private const val PAIR_PORT = 6467
        private const val REMOTE_PORT = 6466
        private const val CLIENT_ALIAS = "xiaomi_tv_remote_client"
        private const val PREF_PRIVATE_KEY = "client_private_key_v2"
        private const val PREF_CERTIFICATE = "client_certificate_v2"
        private const val FEATURE_FLAGS = 1 or 2 or 4 or 32 or 64 or 512
    }

    private data class ClientIdentity(
        val privateKey: PrivateKey,
        val certificate: X509Certificate,
    )

    private val main = Handler(Looper.getMainLooper())
    private val io = Executors.newCachedThreadPool()
    private val prefs = context.getSharedPreferences("remote", Context.MODE_PRIVATE)
    private val identity: ClientIdentity by lazy { loadOrCreateIdentity() }
    private val sslContext: SSLContext by lazy { createSslContext() }

    @Volatile private var remoteSocket: SSLSocket? = null
    @Volatile private var pairingSocket: SSLSocket? = null
    @Volatile private var pairingServerCert: X509Certificate? = null
    @Volatile private var pairingHost: String? = null
    @Volatile private var imeCounter: Int = 0
    @Volatile private var imeFieldCounter: Int = 0

    private val remoteWriteLock = Any()
    private val pairingWriteLock = Any()
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private val found = linkedMapOf<String, TvDevice>()

    fun lastHost(): String? = prefs.getString("last_host", null)

    fun discover() {
        stopDiscovery()
        found.clear()
        postStatus("Searching your Wi-Fi…")
        val nsd = context.getSystemService(Context.NSD_SERVICE) as NsdManager
        val dl = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) = Unit
            override fun onDiscoveryStopped(serviceType: String) = Unit

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                postStatus("Discovery failed ($errorCode). Enter the TV IP manually.")
                runCatching { nsd.stopServiceDiscovery(this) }
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) = Unit
            override fun onServiceLost(serviceInfo: NsdServiceInfo) = Unit

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                if (!serviceInfo.serviceType.contains("androidtvremote2", ignoreCase = true)) return
                @Suppress("DEPRECATION")
                nsd.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                    override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) = Unit

                    override fun onServiceResolved(info: NsdServiceInfo) {
                        val address = info.host?.hostAddress ?: return
                        val device = TvDevice(
                            name = info.serviceName.ifBlank { "Android TV" },
                            host = address,
                            port = info.port.takeIf { it > 0 } ?: REMOTE_PORT,
                        )
                        synchronized(found) { found[address] = device }
                        main.post {
                            listener.onDevices(synchronized(found) { found.values.toList() })
                        }
                    }
                })
            }
        }

        discoveryListener = dl
        runCatching { nsd.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, dl) }
            .onFailure { postStatus("Discovery unavailable. Enter the TV IP manually.") }

        main.postDelayed({
            stopDiscovery()
            if (found.isEmpty()) postStatus("No TV found automatically. Enter its IP address below.")
        }, 7000)
    }

    fun stopDiscovery() {
        val dl = discoveryListener ?: return
        val nsd = context.getSystemService(Context.NSD_SERVICE) as NsdManager
        runCatching { nsd.stopServiceDiscovery(dl) }
        discoveryListener = null
    }

    fun connect(host: String) {
        if (host.isBlank()) return
        io.execute {
            disconnectRemote(false)
            try {
                postStatus("Connecting to $host…")
                val socket = openTls(host, REMOTE_PORT)
                remoteSocket = socket
                prefs.edit().putString("last_host", host).apply()
                postStatus("Connected to $host")
                main.post { listener.onConnected(host) }
                Thread { remoteReadLoop(socket) }.start()
            } catch (e: Exception) {
                remoteSocket = null
                val reason = friendly(e)
                postStatus("Not paired or unavailable: $reason")
                main.post { listener.onDisconnected(reason) }
            }
        }
    }

    fun startPairing(host: String) {
        if (host.isBlank()) return
        io.execute {
            closePairing()
            try {
                postStatus("Opening secure pairing with $host…")
                val socket = openTls(host, PAIR_PORT)
                pairingSocket = socket
                pairingHost = host
                pairingServerCert = socket.session.peerCertificates.firstOrNull() as? X509Certificate
                    ?: error("TV certificate unavailable")

                postStatus("Pairing: negotiating…")
                writePair(pairingRequest())
                expectPairField(readFrame(socket.inputStream), 11, "request acknowledgement")

                writePair(pairingOption())
                expectPairField(readFrame(socket.inputStream), 20, "pairing options")

                writePair(pairingConfiguration())
                expectPairField(readFrame(socket.inputStream), 31, "pairing configuration")

                postStatus("Enter the 6-character code shown on the TV")
                main.post { listener.onPairingCodeRequested(host) }
            } catch (e: Exception) {
                closePairing()
                postStatus("Pairing could not start: ${friendly(e)}")
            }
        }
    }

    fun finishPairing(code: String) {
        io.execute {
            try {
                val socket = pairingSocket ?: error("Pairing session expired")
                val serverCert = pairingServerCert ?: error("TV certificate unavailable")
                val normalized = code.trim().uppercase()
                require(normalized.length == 6 && normalized.all { it in "0123456789ABCDEF" }) {
                    "Code must contain 6 hexadecimal characters"
                }

                postStatus("Pairing: checking code…")
                val secret = pairingSecret(normalized, serverCert)
                writePair(pairingSecretMessage(secret))
                expectPairField(readFrame(socket.inputStream), 41, "pairing confirmation")

                val host = pairingHost ?: error("Pairing host unavailable")
                prefs.edit().putString("last_host", host).apply()
                closePairing()
                postStatus("Paired. Connecting…")
                connect(host)
            } catch (e: Exception) {
                closePairing()
                postStatus("Pairing failed: ${friendly(e)}")
            }
        }
    }

    fun sendKey(keyCode: Int, direction: Int = 3) {
        val key = concat(
            fieldVarint(1, keyCode.toLong()),
            fieldVarint(2, direction.toLong()),
        )
        sendRemote(fieldMessage(10, key))
    }

    fun sendText(text: String) {
        if (text.isEmpty()) return
        val pos = (text.length - 1).coerceAtLeast(0)
        val imeObject = concat(
            fieldVarint(1, pos.toLong()),
            fieldVarint(2, pos.toLong()),
            fieldString(3, text),
        )
        val editInfo = concat(
            fieldVarint(1, 1),
            fieldMessage(2, imeObject),
        )
        val batch = concat(
            fieldVarint(1, imeCounter.toLong()),
            fieldVarint(2, imeFieldCounter.toLong()),
            fieldMessage(3, editInfo),
        )
        sendRemote(fieldMessage(21, batch))
    }

    fun launchAppLink(link: String) {
        if (link.isBlank()) return
        sendRemote(fieldMessage(90, fieldString(1, link.trim())))
    }

    fun disconnect() {
        stopDiscovery()
        closePairing()
        disconnectRemote(true)
        io.shutdownNow()
    }

    private fun remoteReadLoop(socket: SSLSocket) {
        try {
            while (!socket.isClosed) {
                val frame = readFrame(socket.inputStream)
                val top = parseFields(frame)
                when {
                    top.containsKey(1) -> {
                        val serverFeatures = parseFields(top.getValue(1).bytes ?: byteArrayOf())[1]
                            ?.varint?.toInt() ?: FEATURE_FLAGS
                        val active = FEATURE_FLAGS and serverFeatures
                        val deviceInfo = concat(
                            fieldVarint(3, 1),
                            fieldString(4, "1"),
                            fieldString(5, "atvremote"),
                            fieldString(6, "1.0.0"),
                        )
                        sendRemote(
                            fieldMessage(
                                1,
                                concat(
                                    fieldVarint(1, active.toLong()),
                                    fieldMessage(2, deviceInfo),
                                ),
                            ),
                        )
                    }

                    top.containsKey(2) -> {
                        sendRemote(fieldMessage(2, fieldVarint(1, FEATURE_FLAGS.toLong())))
                    }

                    top.containsKey(8) -> {
                        val ping = parseFields(top.getValue(8).bytes ?: byteArrayOf())[1]?.varint ?: 0
                        sendRemote(fieldMessage(9, fieldVarint(1, ping)))
                    }

                    top.containsKey(21) -> {
                        val fields = parseFields(top.getValue(21).bytes ?: byteArrayOf())
                        imeCounter = fields[1]?.varint?.toInt() ?: imeCounter
                        imeFieldCounter = fields[2]?.varint?.toInt() ?: imeFieldCounter
                    }

                    top.containsKey(40) -> {
                        val started = parseFields(top.getValue(40).bytes ?: byteArrayOf())[1]?.varint == 1L
                        main.post { listener.onPowerChanged(started) }
                    }
                }
            }
        } catch (e: Exception) {
            if (remoteSocket === socket) {
                remoteSocket = null
                val reason = friendly(e)
                postStatus("Disconnected: $reason")
                main.post { listener.onDisconnected(reason) }
            }
        } finally {
            runCatching { socket.close() }
        }
    }

    private fun sendRemote(body: ByteArray) {
        io.execute {
            val socket = remoteSocket
            if (socket == null || socket.isClosed) {
                postStatus("Not connected")
                return@execute
            }
            try {
                synchronized(remoteWriteLock) {
                    writeFrame(socket.outputStream, body)
                }
            } catch (e: Exception) {
                postStatus("Command failed: ${friendly(e)}")
            }
        }
    }

    private fun writePair(body: ByteArray) {
        val socket = pairingSocket ?: error("Pairing connection is closed")
        synchronized(pairingWriteLock) {
            writeFrame(socket.outputStream, body)
        }
    }

    private fun expectPairField(frame: ByteArray, expectedField: Int, stage: String) {
        val fields = parseFields(frame)
        val status = fields[2]?.varint?.toInt() ?: 200
        if (status != 200) error("TV rejected $stage (status $status)")
        if (!fields.containsKey(expectedField)) error("Unexpected response during $stage")
    }

    private fun pairingRequest(): ByteArray = pairingOuter(
        fieldMessage(
            10,
            concat(
                fieldString(1, "atvremote"),
                fieldString(2, "Xiaomi TV Remote"),
            ),
        ),
    )

    private fun pairingOption(): ByteArray {
        val encoding = concat(
            fieldVarint(1, 3),
            fieldVarint(2, 6),
        )
        val option = concat(
            fieldMessage(1, encoding),
            fieldVarint(3, 1),
        )
        return pairingOuter(fieldMessage(20, option))
    }

    private fun pairingConfiguration(): ByteArray {
        val encoding = concat(
            fieldVarint(1, 3),
            fieldVarint(2, 6),
        )
        val config = concat(
            fieldMessage(1, encoding),
            fieldVarint(2, 1),
        )
        return pairingOuter(fieldMessage(30, config))
    }

    private fun pairingSecretMessage(secret: ByteArray): ByteArray =
        pairingOuter(fieldMessage(40, fieldBytes(1, secret)))

    private fun pairingOuter(payload: ByteArray): ByteArray = concat(
        fieldVarint(1, 2),
        fieldVarint(2, 200),
        payload,
    )

    private fun pairingSecret(pin: String, serverCert: X509Certificate): ByteArray {
        val clientKey = identity.certificate.publicKey as RSAPublicKey
        val serverKey = serverCert.publicKey as RSAPublicKey
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(unsigned(clientKey.modulus))
        digest.update(exponentBytes(clientKey.publicExponent))
        digest.update(unsigned(serverKey.modulus))
        digest.update(exponentBytes(serverKey.publicExponent))
        digest.update(hexToBytes(pin.substring(2)))
        val result = digest.digest()
        require((result[0].toInt() and 0xff) == pin.substring(0, 2).toInt(16)) {
            "Code does not match this pairing session"
        }
        return result
    }

    private fun unsigned(value: BigInteger): ByteArray {
        val bytes = value.toByteArray()
        return if (bytes.size > 1 && bytes[0] == 0.toByte()) {
            bytes.copyOfRange(1, bytes.size)
        } else {
            bytes
        }
    }

    private fun exponentBytes(value: BigInteger): ByteArray {
        var hex = value.toString(16)
        hex = "0$hex"
        if (hex.length % 2 != 0) hex = "0$hex"
        return hexToBytes(hex)
    }

    private fun hexToBytes(hex: String): ByteArray = ByteArray(hex.length / 2) { i ->
        hex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
    }

    private fun openTls(host: String, port: Int): SSLSocket {
        val socket = sslContext.socketFactory.createSocket(host, port) as SSLSocket
        socket.soTimeout = 20_000
        val modern = socket.supportedProtocols.filter { it == "TLSv1.2" || it == "TLSv1.3" }
        if (modern.isNotEmpty()) socket.enabledProtocols = modern.toTypedArray()
        socket.startHandshake()
        socket.soTimeout = 0
        return socket
    }

    private fun createSslContext(): SSLContext {
        val clientIdentity = identity
        val keyManager = object : X509ExtendedKeyManager() {
            override fun chooseClientAlias(
                keyType: Array<out String>?,
                issuers: Array<out java.security.Principal>?,
                socket: java.net.Socket?,
            ): String? = if (keyType == null || keyType.any { it.equals("RSA", true) }) CLIENT_ALIAS else null

            override fun chooseServerAlias(
                keyType: String?,
                issuers: Array<out java.security.Principal>?,
                socket: java.net.Socket?,
            ): String? = null

            override fun getCertificateChain(alias: String?): Array<X509Certificate>? =
                if (alias == CLIENT_ALIAS) arrayOf(clientIdentity.certificate) else null

            override fun getClientAliases(
                keyType: String?,
                issuers: Array<out java.security.Principal>?,
            ): Array<String>? = if (keyType == null || keyType.equals("RSA", true)) arrayOf(CLIENT_ALIAS) else null

            override fun getPrivateKey(alias: String?): PrivateKey? =
                if (alias == CLIENT_ALIAS) clientIdentity.privateKey else null

            override fun getServerAliases(
                keyType: String?,
                issuers: Array<out java.security.Principal>?,
            ): Array<String>? = null
        }

        val trustAll = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
        }

        return SSLContext.getInstance("TLS").apply {
            init(
                arrayOf<KeyManager>(keyManager),
                arrayOf<TrustManager>(trustAll),
                SecureRandom(),
            )
        }
    }

    private fun loadOrCreateIdentity(): ClientIdentity {
        val storedKey = prefs.getString(PREF_PRIVATE_KEY, null)
        val storedCert = prefs.getString(PREF_CERTIFICATE, null)
        if (!storedKey.isNullOrBlank() && !storedCert.isNullOrBlank()) {
            runCatching {
                val keyBytes = Base64.decode(storedKey, Base64.NO_WRAP)
                val certBytes = Base64.decode(storedCert, Base64.NO_WRAP)
                val privateKey = KeyFactory.getInstance("RSA")
                    .generatePrivate(PKCS8EncodedKeySpec(keyBytes))
                val certificate = CertificateFactory.getInstance("X.509")
                    .generateCertificate(ByteArrayInputStream(certBytes)) as X509Certificate
                certificate.checkValidity(Date())
                return ClientIdentity(privateKey, certificate)
            }.onFailure {
                prefs.edit()
                    .remove(PREF_PRIVATE_KEY)
                    .remove(PREF_CERTIFICATE)
                    .apply()
            }
        }
        return generateAndSaveIdentity()
    }

    private fun generateAndSaveIdentity(): ClientIdentity {
        val generator = KeyPairGenerator.getInstance("RSA")
        generator.initialize(2048, SecureRandom())
        val pair = generator.generateKeyPair()

        val now = System.currentTimeMillis()
        val subject = X500Name(X500Principal("CN=Xiaomi TV Remote").name)
        val builder = JcaX509v3CertificateBuilder(
            subject,
            BigInteger.valueOf(1000),
            Date(now - 86_400_000L),
            Date(now + 315_360_000_000L),
            subject,
            pair.public,
        )
        builder.addExtension(Extension.basicConstraints, false, BasicConstraints(0))
        builder.addExtension(
            Extension.subjectAlternativeName,
            false,
            GeneralNames(GeneralName(GeneralName.dNSName, "xiaomi-tv-remote")),
        )

        val signer = JcaContentSignerBuilder("SHA256withRSA").build(pair.private)
        val certificate = JcaX509CertificateConverter().getCertificate(builder.build(signer))
        certificate.verify(pair.public)

        prefs.edit()
            .putString(PREF_PRIVATE_KEY, Base64.encodeToString(pair.private.encoded, Base64.NO_WRAP))
            .putString(PREF_CERTIFICATE, Base64.encodeToString(certificate.encoded, Base64.NO_WRAP))
            .apply()

        return ClientIdentity(pair.private, certificate)
    }

    private fun disconnectRemote(notify: Boolean) {
        val socket = remoteSocket
        remoteSocket = null
        runCatching { socket?.close() }
        if (notify) main.post { listener.onDisconnected("Disconnected") }
    }

    private fun closePairing() {
        val socket = pairingSocket
        pairingSocket = null
        pairingServerCert = null
        pairingHost = null
        runCatching { socket?.close() }
    }

    private fun postStatus(text: String) = main.post { listener.onStatus(text) }

    private fun friendly(error: Throwable): String {
        return when (error) {
            is SSLHandshakeException -> "TLS handshake failed"
            is SSLException -> "TLS error: ${error.message.orEmpty().lineSequence().firstOrNull().orEmpty().take(90)}"
            else -> error.message?.lineSequence()?.firstOrNull()?.take(120)
                ?: error.javaClass.simpleName
        }
    }

    private data class FieldValue(
        val wire: Int,
        val varint: Long? = null,
        val bytes: ByteArray? = null,
    )

    private fun parseFields(data: ByteArray): Map<Int, FieldValue> {
        val out = linkedMapOf<Int, FieldValue>()
        var p = 0
        while (p < data.size) {
            val (tag, nextPosition) = readVarint(data, p)
            p = nextPosition
            val field = (tag ushr 3).toInt()
            val wire = (tag and 7).toInt()
            when (wire) {
                0 -> {
                    val (value, next) = readVarint(data, p)
                    p = next
                    out[field] = FieldValue(wire, varint = value)
                }

                1 -> p += 8

                2 -> {
                    val (length, next) = readVarint(data, p)
                    p = next
                    val end = (p + length.toInt()).coerceAtMost(data.size)
                    out[field] = FieldValue(wire, bytes = data.copyOfRange(p, end))
                    p = end
                }

                5 -> p += 4
                else -> error("Unsupported protobuf wire type $wire")
            }
        }
        return out
    }

    private fun readVarint(data: ByteArray, offset: Int): Pair<Long, Int> {
        var result = 0L
        var shift = 0
        var p = offset
        while (p < data.size && shift < 64) {
            val b = data[p++].toInt() and 0xff
            result = result or ((b and 0x7f).toLong() shl shift)
            if (b and 0x80 == 0) return result to p
            shift += 7
        }
        error("Invalid varint")
    }

    private fun readFrame(input: InputStream): ByteArray {
        val length = readVarint(input).toInt()
        val data = ByteArray(length)
        var offset = 0
        while (offset < length) {
            val read = input.read(data, offset, length - offset)
            if (read < 0) throw EOFException("TV closed the connection")
            offset += read
        }
        return data
    }

    private fun readVarint(input: InputStream): Long {
        var result = 0L
        var shift = 0
        while (shift < 64) {
            val b = input.read()
            if (b < 0) throw EOFException("TV closed the connection")
            result = result or ((b and 0x7f).toLong() shl shift)
            if (b and 0x80 == 0) return result
            shift += 7
        }
        error("Invalid frame length")
    }

    private fun writeFrame(output: OutputStream, body: ByteArray) {
        output.write(varint(body.size.toLong()))
        output.write(body)
        output.flush()
    }

    private fun fieldVarint(field: Int, value: Long): ByteArray =
        concat(varint((field shl 3).toLong()), varint(value))

    private fun fieldString(field: Int, value: String): ByteArray =
        fieldBytes(field, value.toByteArray(Charsets.UTF_8))

    private fun fieldBytes(field: Int, value: ByteArray): ByteArray =
        concat(
            varint(((field shl 3) or 2).toLong()),
            varint(value.size.toLong()),
            value,
        )

    private fun fieldMessage(field: Int, body: ByteArray): ByteArray = fieldBytes(field, body)

    private fun varint(value: Long): ByteArray {
        var remaining = value
        val out = ByteArrayOutputStream()
        do {
            var byte = (remaining and 0x7f).toInt()
            remaining = remaining ushr 7
            if (remaining != 0L) byte = byte or 0x80
            out.write(byte)
        } while (remaining != 0L)
        return out.toByteArray()
    }

    private fun concat(vararg arrays: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        arrays.forEach { out.write(it) }
        return out.toByteArray()
    }
}
