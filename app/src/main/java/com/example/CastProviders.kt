package com.example

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.*

interface CastDevice {
    val id: String
    val name: String
    val ipAddress: String
    val port: Int
    val deviceType: String // "CHROMECAST" or "LG_WEBOS"
}

data class GenericCastDevice(
    override val id: String,
    override val name: String,
    override val ipAddress: String,
    override val port: Int,
    override val deviceType: String
) : CastDevice

interface CastProvider {
    val isScanning: StateFlow<Boolean>
    val discoveredDevices: StateFlow<List<CastDevice>>
    val connectedDevice: StateFlow<CastDevice?>

    fun startDiscovery()
    fun stopDiscovery()
    fun connect(device: CastDevice, onResult: (Boolean) -> Unit)
    fun disconnect()
    fun beamCurrentMedia(
        videoUrl: String,
        title: String,
        subtitle: String,
        logoUrl: String?,
        isLive: Boolean,
        onResult: (Boolean) -> Unit
    )
}

class LgWebOsProvider(private val context: Context) : CastProvider {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var scanJob: Job? = null

    private val _isScanning = MutableStateFlow(false)
    override val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<CastDevice>>(emptyList())
    override val discoveredDevices: StateFlow<List<CastDevice>> = _discoveredDevices.asStateFlow()

    private val _connectedDevice = MutableStateFlow<CastDevice?>(null)
    override val connectedDevice: StateFlow<CastDevice?> = _connectedDevice.asStateFlow()

    override fun startDiscovery() {
        if (_isScanning.value) return
        _isScanning.value = true
        _discoveredDevices.value = emptyList()

        scanJob = scope.launch {
            var multicastLock: android.net.wifi.WifiManager.MulticastLock? = null
            try {
                // Safely grab WifiManager MulticastLock to make sure Android doesn't drop incoming UDP SSDP packets
                try {
                    val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? android.net.wifi.WifiManager
                    multicastLock = wifiManager?.createMulticastLock("LgTvDiscovery")
                    multicastLock?.setReferenceCounted(true)
                    multicastLock?.acquire()
                } catch (e: Exception) {
                    Log.w("LgWebOsProvider", "Unable to acquire MulticastLock: ${e.localizedMessage}")
                }

                // Send standard UPnP M-SEARCH multicast message for MediaRenderer (smart TVs/DLNA/LG play receivers)
                val request = "M-SEARCH * HTTP/1.1\r\n" +
                        "HOST: 239.255.255.250:1900\r\n" +
                        "MAN: \"ssdp:discover\"\r\n" +
                        "MX: 3\r\n" +
                        "ST: urn:schemas-upnp-org:device:MediaRenderer:1\r\n\r\n"

                val bytes = request.toByteArray()
                val packet = DatagramPacket(bytes, bytes.size, InetAddress.getByName("239.255.255.250"), 1900)

                MulticastSocket(null).use { socket ->
                    socket.reuseAddress = true
                    socket.soTimeout = 3000
                    try {
                        socket.bind(InetSocketAddress(0))
                    } catch (ex: Exception) {
                        Log.w("LgWebOsProvider", "Fallback socket binding setup", ex)
                        // fallback binding if bind fails
                    }

                    // Send UDP query times to prevent packet loss
                    repeat(3) {
                        try {
                            socket.send(packet)
                        } catch (e: Exception) {
                            Log.e("LgWebOsProvider", "Failed to send M-SEARCH packet", e)
                        }
                        delay(250)
                    }

                    val rxBuffer = ByteArray(2048)
                    val rxPacket = DatagramPacket(rxBuffer, rxBuffer.size)
                    val startTime = System.currentTimeMillis()

                    while (isActive && (System.currentTimeMillis() - startTime < 6000)) {
                        try {
                            socket.receive(rxPacket)
                            val response = String(rxPacket.data, 0, rxPacket.length)
                            val ip = rxPacket.address.hostAddress ?: continue
                            
                            // Check if already in list
                            if (_discoveredDevices.value.none { it.ipAddress == ip }) {
                                parseSsdpResponse(response, ip)
                            }
                        } catch (timeout: SocketTimeoutException) {
                            // keep checking until loop ends
                        } catch (io: Exception) {
                            Log.e("LgWebOsProvider", "SSDP packet read failure", io)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("LgWebOsProvider", "Error scanning for LG/DLNA TVs", e)
            } finally {
                try {
                    if (multicastLock?.isHeld == true) {
                        multicastLock.release()
                    }
                } catch (ex: Exception) {
                    // ignore
                }
                _isScanning.value = false
            }
        }
    }

    override fun stopDiscovery() {
        scanJob?.cancel()
        _isScanning.value = false
    }

    override fun connect(device: CastDevice, onResult: (Boolean) -> Unit) {
        scope.launch {
            try {
                Log.i("LgWebOsProvider", "Connecting to ${device.name} at ${device.ipAddress}:${device.port}")
                _connectedDevice.value = device
                withContext(Dispatchers.Main) {
                    onResult(true)
                }
            } catch (e: Exception) {
                Log.e("LgWebOsProvider", "Connect failed", e)
                withContext(Dispatchers.Main) {
                    onResult(false)
                }
            }
        }
    }

    override fun disconnect() {
        Log.i("LgWebOsProvider", "Disconnecting current LG TV device: ${_connectedDevice.value?.name}")
        _connectedDevice.value = null
    }

    fun addManualIpDevice(ipStr: String) {
        val cleanIp = ipStr.trim()
        if (cleanIp.isNotBlank()) {
            addDeviceToList(cleanIp, 1301, "LG TV (Manual: $cleanIp)")
        }
    }

    override fun beamCurrentMedia(
        videoUrl: String,
        title: String,
        subtitle: String,
        logoUrl: String?,
        isLive: Boolean,
        onResult: (Boolean) -> Unit
    ) {
        val device = _connectedDevice.value
        if (device == null) {
            Log.e("LgWebOsProvider", "LG device is NULL. Beam aborted.")
            onResult(false)
            return
        }

        scope.launch {
            val success = tryBeamMedia(device, videoUrl, title, subtitle, logoUrl)
            withContext(Dispatchers.Main) {
                onResult(success)
            }
        }
    }

    private fun parseSsdpResponse(response: String, ip: String) {
        var port = 1301 // Generic AVTransport XML listening fallback port
        
        // Extract LOCATION: url link from SSDP header response
        val locationRegex = Regex("LOCATION:\\s*(https?://[^\\s]+)", RegexOption.IGNORE_CASE)
        val matchResult = locationRegex.find(response)
        if (matchResult != null) {
            val locationUrlStr = matchResult.groupValues[1]
            try {
                val url = URL(locationUrlStr)
                port = url.port.let { if (it == -1) url.defaultPort else it }
                
                // Query XML resource descriptor for exact display friendlyName
                scope.launch {
                    val resolvedName = fetchDeviceFriendlyName(locationUrlStr) ?: "LG Media TV ($ip)"
                    addDeviceToList(ip, port, resolvedName)
                }
                return
            } catch (e: Exception) {
                Log.e("LgWebOsProvider", "SSDP location parsing failed", e)
            }
        }
        
        addDeviceToList(ip, port, "LG Media TV ($ip)")
    }

    private fun addDeviceToList(ip: String, port: Int, name: String) {
        val list = _discoveredDevices.value.toMutableList()
        val deviceId = "lg_webos_${ip.replace(".", "_")}"
        if (list.none { it.id == deviceId }) {
            list.add(GenericCastDevice(
                id = deviceId,
                name = name,
                ipAddress = ip,
                port = port,
                deviceType = "LG_WEBOS"
            ))
            _discoveredDevices.value = list
            Log.i("LgWebOsProvider", "Registered LG TV candidate: $name at $ip:$port")
        }
    }

    private suspend fun fetchDeviceFriendlyName(urlStr: String): String? = withContext(Dispatchers.IO) {
        try {
            val url = URL(urlStr)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 1200
            conn.readTimeout = 1200
            conn.requestMethod = "GET"
            
            if (conn.responseCode == 200) {
                val XMLReader = BufferedReader(InputStreamReader(conn.inputStream))
                val content = StringBuilder()
                var line: String?
                while (XMLReader.readLine().also { line = it } != null) {
                    content.append(line)
                }
                XMLReader.close()
                val xml = content.toString()
                
                // Match friendlyName element
                val match = Regex("<friendlyName>(.*?)</friendlyName>", RegexOption.IGNORE_CASE).find(xml)
                if (match != null) {
                    return@withContext match.groupValues[1].trim()
                }
            }
        } catch (e: Exception) {
            Log.d("LgWebOsProvider", "XML metadata search failed for $urlStr: ${e.localizedMessage}")
        }
        return@withContext null
    }

    private suspend fun tryBeamMedia(
        device: CastDevice,
        videoUrl: String,
        title: String,
        subtitle: String,
        logoUrl: String?
    ): Boolean = withContext(Dispatchers.IO) {
        // Ports of UPnP renderers typically include 1301 (webOS renderer), 1600 (DLNA), 2870, 8058, 49152
        val portsToTry = listOf(device.port, 1301, 49152, 2870, 8058, 1600)
        
        // Common endpoint directories for SetAVTransportURI
        val pathsToTry = listOf(
            "/AVTransport/control",
            "/upnp/control/AVTransport1",
            "/AVTransport/1/control",
            "/udap/api/data?target=AVTransport",
            "/"
        )

        val cleanLogo = logoUrl ?: "https://images.unsplash.com/photo-1594909122845-11baa439b7bf?w=400"
        val escapedTitle = title.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        val escapedUrl = videoUrl.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

        val envelopeSetUri = """
            <?xml version="1.0" encoding="utf-8"?>
            <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
              <s:Body>
                <u:SetAVTransportURI xmlns:u="urn:schemas-upnp-org:service:AVTransport:1">
                  <InstanceID>0</InstanceID>
                  <CurrentURI>$escapedUrl</CurrentURI>
                  <CurrentURIMetaData><![CDATA[<DIDL-Lite xmlns="urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:upnp="urn:schemas-upnp-org:metadata-1-0/upnp/" xmlns:sec="http://www.sec.co.kr/"><item id="0" parentID="-1" restricted="1"><upnp:class>object.item.videoItem</upnp:class><dc:title>$escapedTitle</dc:title><upnp:albumArtURI>$cleanLogo</upnp:albumArtURI></item></DIDL-Lite>]]></CurrentURIMetaData>
                </u:SetAVTransportURI>
              </s:Body>
            </s:Envelope>
        """.trimIndent()

        val envelopePlay = """
            <?xml version="1.0" encoding="utf-8"?>
            <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
              <s:Body>
                <u:Play xmlns:u="urn:schemas-upnp-org:service:AVTransport:1">
                  <InstanceID>0</InstanceID>
                  <Speed>1</Speed>
                </u:Play>
              </s:Body>
            </s:Envelope>
        """.trimIndent()

        var beamSucceeded = false

        for (port in portsToTry) {
            for (path in pathsToTry) {
                try {
                    val endpoint = "http://${device.ipAddress}:$port$path"
                    val url = URL(endpoint)
                    
                    // Step 1: Send SetAVTransportURI
                    val conn = url.openConnection() as HttpURLConnection
                    conn.connectTimeout = 1000
                    conn.readTimeout = 1000
                    conn.requestMethod = "POST"
                    conn.setRequestProperty("Content-Type", "text/xml; charset=\"utf-8\"")
                    conn.setRequestProperty("SOAPACTION", "\"urn:schemas-upnp-org:service:AVTransport:1#SetAVTransportURI\"")
                    conn.doOutput = true
                    
                    conn.outputStream.use { os ->
                        OutputStreamWriter(os, "UTF-8").use { w ->
                            w.write(envelopeSetUri)
                            w.flush()
                        }
                    }

                    val code = conn.responseCode
                    if (code in 200..299) {
                        // Step 2: Trigger Play command
                        val playConn = url.openConnection() as HttpURLConnection
                        playConn.connectTimeout = 1000
                        playConn.readTimeout = 1000
                        playConn.requestMethod = "POST"
                        playConn.setRequestProperty("Content-Type", "text/xml; charset=\"utf-8\"")
                        playConn.setRequestProperty("SOAPACTION", "\"urn:schemas-upnp-org:service:AVTransport:1#Play\"")
                        playConn.doOutput = true
                        
                        playConn.outputStream.use { os ->
                            OutputStreamWriter(os, "UTF-8").use { w ->
                                w.write(envelopePlay)
                                w.flush()
                            }
                        }
                        
                        val playCode = playConn.responseCode
                        if (playCode in 200..299) {
                            beamSucceeded = true
                            Log.i("LgWebOsProvider", "Success beaming playback to LG TV at $endpoint")
                            break
                        }
                    }
                } catch (ex: Exception) {
                    // silent fallback to try next path/port profile
                }
            }
            if (beamSucceeded) break
        }

        return@withContext beamSucceeded
    }
}
