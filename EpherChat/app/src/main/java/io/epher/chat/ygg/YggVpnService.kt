package io.epher.chat.ygg

import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.widget.Toast
import io.epher.chat.util.isVpnActive
import java.io.File

class YggVpnService : VpnService() {
    private var proc: Process? = null

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, YggVpnService::class.java)
            context.startService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, YggVpnService::class.java)
            context.stopService(intent)
        }

        fun isVpnActive(context: Context): Boolean {
            return isVpnActive(context)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (proc != null) return START_STICKY

        if (isVpnActive(this)) {
            Toast.makeText(this, "Another VPN is active. Please disable it before starting Yggdrasil.", Toast.LENGTH_LONG).show()
            stopSelf()
            return START_NOT_STICKY
        }

        val iface: ParcelFileDescriptor = Builder()
            .addAddress("200::1", 7)
            .addRoute("::", 0)
            .setSession("Epher-Ygg")
            .establish() ?: return START_NOT_STICKY

        val fdField = iface.fileDescriptor.javaClass.getDeclaredField("fd").apply {
            isAccessible = true
        }
        val fdInt = fdField.getInt(iface.fileDescriptor)

        try {
            val yggBin = File(filesDir, "ygg.android").apply {
                if (!exists()) {
                    assets.open("bin/ygg.android").copyTo(outputStream())
                    setExecutable(true, false)
                }
            }

            proc = ProcessBuilder(
                yggBin.absolutePath,
                "-tunfd", fdInt.toString(),
                "-socks", "9001",
                "-subnet", "200::/7"
            ).redirectErrorStream(true).start()
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to start Yggdrasil VPN: ${e.message}", Toast.LENGTH_LONG).show()
            stopSelf()
            return START_NOT_STICKY
        }

        return START_STICKY
    }

    override fun onDestroy() {
        proc?.destroy()
        proc = null
        super.onDestroy()
    }
}