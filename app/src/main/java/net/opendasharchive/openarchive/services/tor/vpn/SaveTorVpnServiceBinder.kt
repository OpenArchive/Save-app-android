package net.opendasharchive.openarchive.services.tor.vpn

import android.os.Binder
import android.os.IBinder
import android.os.Parcel
import org.torproject.onionmasq.ISocketProtect
import java.lang.ref.WeakReference

/**
 * Binder class for SaveTorVpnService.
 * Implements ISocketProtect to allow OnionMasq to protect sockets from VPN routing.
 */
class SaveTorVpnServiceBinder(
    private val serviceRef: WeakReference<SaveTorVpnService>
) : Binder(), ISocketProtect {

    override fun protect(socket: Int): Boolean {
        serviceRef.get()?.let {
            return it.protect(socket)
        }
        return false
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
        if (code == IBinder.LAST_CALL_TRANSACTION) {
            serviceRef.get()?.let {
                it.onRevoke()
                return true
            }
        }
        return false
    }

    val service: SaveTorVpnService?
        get() = serviceRef.get()
}
