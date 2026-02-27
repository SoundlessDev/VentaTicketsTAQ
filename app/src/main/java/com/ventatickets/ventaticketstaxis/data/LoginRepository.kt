package com.ventatickets.ventaticketstaxis.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.os.Build
import java.io.IOException
import java.util.*
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast

class LoginRepository(private val context: Context) {
    private fun getApiService() = ServiceLocator.getApiService(context)
    private val prefs: SharedPreferences = context.getSharedPreferences("login_prefs", Context.MODE_PRIVATE)
    
    init {
        // Inicializar EmpresaGlobals con el contexto
        EmpresaGlobals.initialize(context)
    }

    suspend fun login(usuario: String, contrasena: String): Result<LoginResponse> = withContext(Dispatchers.IO) {
        try {
            val response = getApiService().login(LoginRequest(usuario, contrasena))
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.user != null && body.nombre != null) {
                    if (body.user != "Incorrecto" && body.nombre != "Incorrecto") {
                        // Ambos datos válidos: login correcto
                        saveSession(body)
                        return@withContext Result.success(body)
                    } else if (body.user != "Incorrecto" && body.nombre == "Incorrecto") {
                        // Usuario existe, contraseña incorrecta
                        return@withContext Result.failure(Exception("Contraseña incorrecta: Revise su contraseña."))
                    } else if (body.user == "Incorrecto" && body.nombre == "Incorrecto") {
                        // Usuario no existe
                        return@withContext Result.failure(Exception("Usuario no existe: Revise su usuario."))
                    } else if (body.user == "Incorrecto" && body.nombre != "Incorrecto") {
                        // Usuario incorrecto
                        return@withContext Result.failure(Exception("Usuario incorrecto: Revise su usuario."))
                    } else {
                        // Caso genérico
                        return@withContext Result.failure(Exception("Usuario o contraseña incorrectos."))
                    }
                } else {
                    return@withContext Result.failure(Exception("Usuario o contraseña incorrectos."))
                }
            } else {
                return@withContext Result.failure(Exception("Usuario incorrecto: Revise su usuario o contraseña."))
            }
        } catch (e: HttpException) {
            return@withContext Result.failure(Exception("No se puede establecer conexion al servidor"))
        } catch (e: java.net.UnknownHostException) {
            return@withContext Result.failure(Exception("No se puede establecer conexion al servidor"))
        } catch (e: java.net.SocketTimeoutException) {
            return@withContext Result.failure(Exception("No se puede establecer conexion al servidor"))
        } catch (e: Exception) {
            return@withContext Result.failure(Exception("Error inesperado: Revisa la configuración de la aplicación."))
        }
    }

    suspend fun getEmpresaData(): Result<EmpresaData> = getEmpresaData(forceRefresh = false)
    
    suspend fun getEmpresaData(forceRefresh: Boolean): Result<EmpresaData> = withContext(Dispatchers.IO) {
        try {
            // Si no se fuerza la recarga y ya tenemos datos locales, retornarlos
            if (!forceRefresh && EmpresaGlobals.isDataLoaded()) {
                android.util.Log.d("LoginRepository", "Datos de empresa ya disponibles localmente: ${EmpresaGlobals.descrip}")
                return@withContext Result.success(
                    EmpresaData(
                        cla_empre = EmpresaGlobals.cla_empre ?: "",
                        descrip = EmpresaGlobals.descrip ?: "",
                        rfc = EmpresaGlobals.rfc ?: "",
                        direccion = EmpresaGlobals.direccion ?: "",
                        ini_nocturno = EmpresaGlobals.ini_nocturno ?: "",
                        fin_nocturno = EmpresaGlobals.fin_nocturno ?: ""
                    )
                )
            }
            
            android.util.Log.d("LoginRepository", "Cargando datos de empresa desde webservice... (forceRefresh=$forceRefresh)")
            android.util.Log.d("LoginRepository", "URL base: [oculto por seguridad]")
            val dataList = getApiService().getEmpresaData()
            android.util.Log.d("LoginRepository", "Datos recibidos: $dataList")
            
            if (dataList.isNotEmpty()) {
                val data = dataList[0] // Tomar el primer elemento del array
                android.util.Log.d("LoginRepository", "Datos de empresa cargados: ${data.descrip}")
                android.util.Log.d("LoginRepository", "Horarios recibidos: ${data.ini_nocturno} - ${data.fin_nocturno}")
                EmpresaGlobals.setFromData(data)
                android.util.Log.d("LoginRepository", "Datos guardados en EmpresaGlobals: ${EmpresaGlobals.descrip}")
                android.util.Log.d("LoginRepository", "Horarios guardados en EmpresaGlobals: ${EmpresaGlobals.ini_nocturno} - ${EmpresaGlobals.fin_nocturno}")
                Result.success(data)
            } else {
                android.util.Log.e("LoginRepository", "Lista de datos de empresa vacía")
                Result.failure(Exception("No se encontraron datos de empresa"))
            }
        } catch (e: Exception) {
            android.util.Log.e("LoginRepository", "Error cargando datos de empresa: ${e.message}", e)
            Result.failure(Exception("Error al cargar los datos de la empresa. Verifique la configuración."))
        }
    }

    fun saveSession(login: LoginResponse) {
        prefs.edit()
            .putString("user", login.user)
            .putString("nombre", login.nombre)
            .apply()
    }

    fun isLoggedIn(): Boolean {
        return prefs.getString("user", null) != null
    }

    fun getSavedUser(): LoginResponse? {
        val user = prefs.getString("user", null)
        val nombre = prefs.getString("nombre", null)
        return if (user != null && nombre != null) LoginResponse(user, nombre) else null
    }

    fun logout() {
        prefs.edit().clear().apply()
        EmpresaGlobals.clear()
    }
}

fun sendZplToZebra(context: Context, macAddress: String, zpl: String, onResult: (Boolean, String?) -> Unit) {
    Thread {
        try {
            // Usar BluetoothManager (método moderno) en lugar de getDefaultAdapter() (deprecado)
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val bluetoothAdapter = bluetoothManager?.adapter
            if (bluetoothAdapter == null) {
                onResult(false, "Bluetooth no soportado")
                return@Thread
            }

            val device: BluetoothDevice? = bluetoothAdapter.getRemoteDevice(macAddress)
            if (device == null) {
                onResult(false, "No se encontró el dispositivo")
                return@Thread
            }

            val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
            var socket: BluetoothSocket? = null
            try {
                socket = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    device.createRfcommSocketToServiceRecord(uuid)
                } else {
                    device.createRfcommSocketToServiceRecord(uuid)
                }
                socket.connect()
                val outputStream = socket.outputStream
                Log.d("ZPL_DEBUG", zpl)
                outputStream.write(zpl.toByteArray(Charsets.UTF_8))
                outputStream.flush()
                Thread.sleep(300)
                onResult(true, null)
            } catch (e: Exception) {
                onResult(false, "Error de impresion revise la impresora")
            } finally {
                try { socket?.close() } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            onResult(false, "Error de impresion revise la impresora")
        }
    }.start()
}

fun sendEscPosToPrinter(
    context: Context,
    escPosCommand: String,
    onResult: (Boolean, String?) -> Unit
) {
    val macAddress = AppConfig.getPrinterMac(context)
    if (macAddress.isEmpty()) {
        onResult(false, "MAC address de impresora no configurada")
        return
    }
    
    Thread {
        try {
            // Usar BluetoothManager (método moderno) en lugar de getDefaultAdapter() (deprecado)
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val bluetoothAdapter = bluetoothManager?.adapter
            if (bluetoothAdapter == null) {
                onResult(false, "Bluetooth no soportado")
                return@Thread
            }

            val device: BluetoothDevice? = bluetoothAdapter.getRemoteDevice(macAddress)
            if (device == null) {
                onResult(false, "No se encontró el dispositivo")
                return@Thread
            }

            val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
            var socket: BluetoothSocket? = null
            try {
                socket = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    device.createRfcommSocketToServiceRecord(uuid)
                } else {
                    device.createRfcommSocketToServiceRecord(uuid)
                }
                socket.connect()
                val outputStream = socket.outputStream
                outputStream.write(escPosCommand.toByteArray(Charsets.UTF_8))
                outputStream.flush()
                onResult(true, null)
            } catch (e: Exception) {
                onResult(false, "Error de impresion revise la impresora")
            } finally {
                try { socket?.close() } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            onResult(false, "Error de impresion revise la impresora")
        }
    }.start()
}

fun sendStarToPrinter(
    context: Context,
    starCommand: String,
    onResult: (Boolean, String?) -> Unit
) {
    val macAddress = AppConfig.getPrinterMac(context)
    if (macAddress.isEmpty()) {
        onResult(false, "MAC address de impresora no configurada")
        return
    }
    
    Thread {
        try {
            // Usar BluetoothManager (método moderno) en lugar de getDefaultAdapter() (deprecado)
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val bluetoothAdapter = bluetoothManager?.adapter
            if (bluetoothAdapter == null) {
                onResult(false, "Bluetooth no soportado")
                return@Thread
            }

            val device: BluetoothDevice? = bluetoothAdapter.getRemoteDevice(macAddress)
            if (device == null) {
                onResult(false, "No se encontró el dispositivo")
                return@Thread
            }

            val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
            var socket: BluetoothSocket? = null
            try {
                socket = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    device.createRfcommSocketToServiceRecord(uuid)
                } else {
                    device.createRfcommSocketToServiceRecord(uuid)
                }
                socket.connect()
                val outputStream = socket.outputStream

                outputStream.write(starCommand.toByteArray(Charsets.UTF_8))
                outputStream.flush()
                onResult(true, null)
            } catch (e: Exception) {
                onResult(false, "Error de impresion revise la impresora")
            } finally {
                try { socket?.close() } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            onResult(false, "Error de impresion revise la impresora")
        }
    }.start()
}

fun showBluetoothPermissionsExplanation(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
    intent.data = Uri.fromParts("package", context.packageName, null)
    context.startActivity(intent)
}

// Función para construir comando ZPL con QR
fun buildZplQrCommand(
    empresa: String,
    rfc: String,
    direccion: String,
    fecha: String,
    folio: String,
    destino: String,
    costo: String
): String {
    return """
^XA
^PW352
^LL540

^FO0,40^A0N,25,25^FB352,3,5,C^FD$empresa^FS
^FO0,120^A0N,20,20^FB352,2,5,C^FDRFC: $rfc^FS

^FO30,170^A0N,25,25^FB290,2,5,L^FDDestino: $destino^FS

^FO30,240^A0N,25,25^FDCosto: ${'$'}$costo.00^FS
^FO30,280^A0N,25,25^FDFecha: $fecha^FS
^FO30,320^A0N,25,25^FDFolio: $folio^FS

^FO90,345^BQN,2,6^FDQA,$folio^FS

^FO0,500^A0N,22,22^FB352,1,0,C^FDGracias por su preferencia!^FS

^XZ
""".trimIndent()
}

// Función para construir comando ESC/POS con QR
fun buildEscPosQrCommand(
    empresa: String,
    rfc: String,
    direccion: String,
    fecha: String,
    folio: String,
    destino: String,
    costo: String
): ByteArray {
    val list = mutableListOf<Byte>()
    
    // Inicializar impresora
    list += 0x1B
    list += 0x40
    
    // Encabezado
    list += "\n".toByteArray(Charsets.US_ASCII).toList()
    list += (empresa + "\n").toByteArray(Charsets.US_ASCII).toList()
    list += ("RFC: $rfc\n").toByteArray(Charsets.US_ASCII).toList()
    list += "-".repeat(45).toByteArray(Charsets.US_ASCII).toList()
    list += "\n".toByteArray(Charsets.US_ASCII).toList()
    
    // Información del ticket
    list += ("Destino: $destino\n").toByteArray(Charsets.US_ASCII).toList()
    list += ("Costo: $$costo.00\n").toByteArray(Charsets.US_ASCII).toList()
    list += ("Fecha: $fecha\n").toByteArray(Charsets.US_ASCII).toList()
    list += ("Folio: $folio\n").toByteArray(Charsets.US_ASCII).toList()
    list += "-".repeat(45).toByteArray(Charsets.US_ASCII).toList()
    list += "\n".toByteArray(Charsets.US_ASCII).toList()
    
    // Centrar texto
    list += 0x1B
    list += 0x61
    list += 0x01
    
    // ========================
    //   Comandos QR ESC/POS
    // ========================
    
    // 1️⃣ Seleccionar modelo de QR
    list += 0x1D
    list += 0x28
    list += 0x6B
    list += 0x04
    list += 0x00
    list += 0x31
    list += 0x41
    list += 0x32
    list += 0x00
    
    // 2️⃣ Nivel de corrección
    list += 0x1D
    list += 0x28
    list += 0x6B
    list += 0x03
    list += 0x00
    list += 0x31
    list += 0x45
    list += 0x30
    
    // 3️⃣ Tamaño del módulo
    list += 0x1D
    list += 0x28
    list += 0x6B
    list += 0x03
    list += 0x00
    list += 0x31
    list += 0x43
    list += 0x08
    
    // 4️⃣ Enviar datos del QR
    val qrData = folio.toByteArray(Charsets.US_ASCII)
    val qrLen = qrData.size + 3
    val pL = qrLen and 0xFF
    val pH = (qrLen shr 8) and 0xFF
    list += 0x1D
    list += 0x28
    list += 0x6B
    list += pL.toByte()
    list += pH.toByte()
    list += 0x31
    list += 0x50
    list += 0x30
    list += qrData.toList()
    
    // 5️⃣ Imprimir QR
    list += 0x1D
    list += 0x28
    list += 0x6B
    list += 0x03
    list += 0x00
    list += 0x31
    list += 0x51
    list += 0x30
    
    // Saltos de línea
    repeat(3) { list += 0x0A }
    
    // Mensaje final
    list += "Gracias por su preferencia\n".toByteArray(Charsets.US_ASCII).toList()
    repeat(6) { list += 0x0A }
    
    // Corte de papel
    list += 0x1B
    list += 0x64
    list += 0x00
    
    return list.toByteArray()
}

// Función para construir comando STAR con QR
fun buildStarQrCommand(
    empresa: String,
    rfc: String,
    direccion: String,
    fecha: String,
    folio: String,
    destino: String,
    costo: String
): ByteArray {
    val list = mutableListOf<Byte>()
    
    // Inicializar impresora
    list += 0x1B
    list += 0x40
    
    // Encabezado
    list += "\n".toByteArray(Charsets.US_ASCII).toList()
    list += (empresa + "\n").toByteArray(Charsets.US_ASCII).toList()
    list += ("RFC: $rfc\n").toByteArray(Charsets.US_ASCII).toList()
    list += "_______________________________________________\n\n".toByteArray(Charsets.US_ASCII).toList()
    
    // Información del ticket
    list += ("Destino: $destino\n").toByteArray(Charsets.US_ASCII).toList()
    list += ("Costo: $$costo.00\n").toByteArray(Charsets.US_ASCII).toList()
    list += ("Fecha: $fecha\n").toByteArray(Charsets.US_ASCII).toList()
    list += ("Folio: $folio\n").toByteArray(Charsets.US_ASCII).toList()
    list += "_______________________________________________\n\n".toByteArray(Charsets.US_ASCII).toList()
    
    // Justificación centrada
    list += 0x1B
    list += 0x1D
    list += 0x61
    list += 0x01
    
    // ====== QR STAR PRNT ======
    // 1. Set QR model (Modelo 2)
    list += 0x1B
    list += 0x1D
    list += 0x79
    list += 0x53
    list += 0x30
    list += 0x02
    
    // 2. Set error correction (M = 1)
    list += 0x1B
    list += 0x1D
    list += 0x79
    list += 0x53
    list += 0x31
    list += 0x01
    
    // 3. Set cell size (8)
    list += 0x1B
    list += 0x1D
    list += 0x79
    list += 0x53
    list += 0x32
    list += 0x08
    
    // 4. Set QR data
    val folioBuffer = folio.toByteArray(Charsets.US_ASCII)
    val dataLength = folioBuffer.size
    val nL = dataLength and 0xFF
    val nH = (dataLength shr 8) and 0xFF
    list += 0x1B
    list += 0x1D
    list += 0x79
    list += 0x44
    list += 0x31
    list += 0x00
    list += nL.toByte()
    list += nH.toByte()
    list += folioBuffer.toList()
    
    // 5. Print QR
    list += 0x1B
    list += 0x1D
    list += 0x79
    list += 0x50
    
    // Avance de línea
    repeat(3) { list += 0x0A }
    
    // Mensaje final
    list += "Gracias por su preferencia\n".toByteArray(Charsets.US_ASCII).toList()
    repeat(6) { list += 0x0A }
    
    // Corte de papel
    list += 0x1B
    list += 0x64
    list += 0x00
    
    return list.toByteArray()
}

// Función para construir comando STAR con Código de Barras
fun buildStarBarcodeCommand(
    empresa: String,
    rfc: String,
    direccion: String,
    fecha: String,
    folio: String,
    destino: String,
    costo: String
): ByteArray {
    val list = mutableListOf<Byte>()
    
    // Inicializar impresora
    list += 0x1B
    list += 0x40
    
    // Encabezado
    list += "\n".toByteArray(Charsets.US_ASCII).toList()
    list += (empresa + "\n").toByteArray(Charsets.US_ASCII).toList()
    list += ("RFC: $rfc\n").toByteArray(Charsets.US_ASCII).toList()
    list += "_______________________________________________\n\n".toByteArray(Charsets.US_ASCII).toList()
    
    // Información del ticket
    list += ("Destino: $destino\n").toByteArray(Charsets.US_ASCII).toList()
    list += ("Costo: $$costo.00\n").toByteArray(Charsets.US_ASCII).toList()
    list += ("Fecha: $fecha\n").toByteArray(Charsets.US_ASCII).toList()
    list += ("Folio: $folio\n").toByteArray(Charsets.US_ASCII).toList()
    list += "_______________________________________________\n\n".toByteArray(Charsets.US_ASCII).toList()
    
    // Avance de línea
    list += 0x0A
    
    // Justificación centrada
    list += 0x1B
    list += 0x1D
    list += 0x61
    list += 0x01
    
    // Código de barras usando el modo adecuado (Code128)
    // 0x1B 0x62 0x06 = código de barras Code128
    // 0x02 = ancho del módulo
    // 0x02 = altura del código de barras
    // 0x30 = formato numérico
    list += 0x1B
    list += 0x62
    list += 0x06
    list += 0x02
    list += 0x02
    list += 0x30
    
    // Datos del código de barras en formato numérico
    list += folio.toByteArray(Charsets.US_ASCII).toList()
    
    // RS (fin del código de barras)
    list += 0x1E
    
    // Avance de línea
    list += 0x0A
    list += 0x0A
    list += 0x0A
    
    // Mensaje final
    list += "Gracias por su preferencia\n".toByteArray(Charsets.US_ASCII).toList()
    repeat(6) { list += 0x0A }
    
    // Corte de papel
    list += 0x1B
    list += 0x64
    list += 0x00
    
    return list.toByteArray()
}

// Función para construir comando ESC/POS con Código de Barras
fun buildEscPosBarcodeCommand(
    empresa: String,
    rfc: String,
    direccion: String,
    fecha: String,
    folio: String,
    destino: String,
    costo: String
): ByteArray {
    val list = mutableListOf<Byte>()
    
    // Inicializar impresora
    list += 0x1B
    list += 0x40
    
    // Encabezado
    list += "\n".toByteArray(Charsets.US_ASCII).toList()
    list += (empresa + "\n").toByteArray(Charsets.US_ASCII).toList()
    list += ("RFC: $rfc\n").toByteArray(Charsets.US_ASCII).toList()
    list += "-".repeat(45).toByteArray(Charsets.US_ASCII).toList()
    list += "\n".toByteArray(Charsets.US_ASCII).toList()
    
    // Información del ticket
    list += ("Destino: $destino\n").toByteArray(Charsets.US_ASCII).toList()
    list += ("Costo: $$costo.00\n").toByteArray(Charsets.US_ASCII).toList()
    list += ("Fecha: $fecha\n").toByteArray(Charsets.US_ASCII).toList()
    list += ("Folio: $folio\n").toByteArray(Charsets.US_ASCII).toList()
    list += "-".repeat(45).toByteArray(Charsets.US_ASCII).toList()
    list += "\n".toByteArray(Charsets.US_ASCII).toList()
    
    // Centrar texto
    list += 0x1B
    list += 0x61
    list += 0x01
    
    // Código de barras Code128
    // \x1D\x77\x02 = Ancho del código de barras (2)
    list += 0x1D
    list += 0x77
    list += 0x02
    
    // \x1D\x68\x45 = Altura del código de barras (69)
    list += 0x1D
    list += 0x68
    list += 0x45
    
    // \x1D\x6B\x49 = Code128 con datos siguientes
    // String.fromCharCode(folio.length + 2) = longitud de datos + 2
    // \x7B\x42 = inicio de Code128 tipo B
    list += 0x1D
    list += 0x6B
    list += 0x49
    list += (folio.length + 2).toByte()
    list += 0x7B
    list += 0x42
    list += folio.toByteArray(Charsets.US_ASCII).toList()
    
    // Más espacio entre código de barras y número del folio
    list += "\n".toByteArray(Charsets.US_ASCII).toList()
    
    // Tamaño caracteres para el folio debajo del código
    list += 0x1B
    list += 0x21
    list += 0x06
    list += folio.toByteArray(Charsets.US_ASCII).toList()
    list += "\n".toByteArray(Charsets.US_ASCII).toList()
    
    // Reset tamaño
    list += 0x1B
    list += 0x21
    list += 0x08
    
    // Saltos de línea
    list += "\n".toByteArray(Charsets.US_ASCII).toList()
    list += "\n".toByteArray(Charsets.US_ASCII).toList()
    
    // Mensaje final
    list += "Gracias por su preferencia\n".toByteArray(Charsets.US_ASCII).toList()
    repeat(6) { list += 0x0A }
    
    // Corte de papel
    list += 0x1B
    list += 0x64
    list += 0x00
    
    return list.toByteArray()
}

// Función para construir comando ZPL con Código de Barras
fun buildZplBarcodeCommand(
    empresa: String,
    rfc: String,
    direccion: String,
    fecha: String,
    folio: String,
    destino: String,
    costo: String
): String {
    return """
^XA
^PW352
^LL560

^FO0,40^A0N,28,28^FB352,3,5,C^FD$empresa^FS
^FO0,110^A0N,22,22^FB352,2,5,C^FDRFC: $rfc^FS

^FO30,160^A0N,25,25^FB290,2,5,L^FDDestino: $destino^FS

^FO30,230^A0N,25,25^FDCosto: ${'$'}$costo.00^FS
^FO30,270^A0N,25,25^FDFecha: $fecha^FS
^FO30,310^A0N,25,25^FDFolio: $folio^FS

^BY3,3,110
^FO40,350^BCN,110,Y,N,N
^FD$folio^FS

^FO0,510^A0N,22,22^FB352,1,0,C^FDGracias por su preferencia!^FS

^XZ
""".trimIndent()
}

fun buildStarPrintCommand(
    empresa: String,
    rfc: String,
    direccion: String,
    fecha: String,
    folio: String
): ByteArray {
    val list = mutableListOf<Byte>()

    // Inicializa la impresora
    list += 0x1B
    list += 0x40

    // Texto a imprimir
    list += "\n".toByteArray(Charsets.US_ASCII).toList()
    list += (empresa + "\n").toByteArray(Charsets.US_ASCII).toList()
    list += "\n".toByteArray(Charsets.US_ASCII).toList()
    list += ("RFC: $rfc\n").toByteArray(Charsets.US_ASCII).toList()
    list += (direccion + "\n").toByteArray(Charsets.US_ASCII).toList()
    list += "Estacionamiento Tipo B\n\n".toByteArray(Charsets.US_ASCII).toList()
    list += "Tarifa:\n".toByteArray(Charsets.US_ASCII).toList()
    list += "- 22 pesos primera hora.\n".toByteArray(Charsets.US_ASCII).toList()
    list += "- 3 pesos cada 15 minutos o fracciones posteriores.\n\n".toByteArray(Charsets.US_ASCII).toList()
    list += "Horario: 24 horas al dia\n\n".toByteArray(Charsets.US_ASCII).toList()
    list += ("Entrada: $fecha\n").toByteArray(Charsets.US_ASCII).toList()
    list += ("Folio: $folio\n").toByteArray(Charsets.US_ASCII).toList()
    list += "_______________________________________________\n\n".toByteArray(Charsets.US_ASCII).toList()
    list += 0x0A

    // Justificación centrada
    list += 0x1B
    list += 0x1D
    list += 0x61
    list += 0x01

    // Código de barras
    list += 0x1B
    list += 0x62
    list += 0x06
    list += 0x02
    list += 0x02
    list += 0x30
    list += folio.toByteArray(Charsets.US_ASCII).toList()
    list += 0x1E

    // Avance de línea
    repeat(3) { list += 0x0A }

    // Mensaje final
    list += "FAVOR DE PAGAR ANTES DE SALIR\n".toByteArray(Charsets.US_ASCII).toList()
    repeat(6) { list += 0x0A }

    // Corte de papel
    list += 0x1B
    list += 0x64
    list += 0x00

    return list.toByteArray()
} 