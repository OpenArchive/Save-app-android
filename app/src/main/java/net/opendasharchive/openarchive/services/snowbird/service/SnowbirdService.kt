package net.opendasharchive.openarchive.services.snowbird.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.SaveApp
import net.opendasharchive.openarchive.extensions.RetryAttempt
import net.opendasharchive.openarchive.extensions.retryWithScope
import net.opendasharchive.openarchive.extensions.suspendToRetry
import net.opendasharchive.openarchive.features.main.MainActivity
import net.opendasharchive.openarchive.services.snowbird.SnowbirdBridge
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.time.Duration.Companion.seconds

class SnowbirdService : Service() {

    companion object {
        var DEFAULT_BACKEND_DIRECTORY = ""
            private set

        var DEFAULT_SOCKET_PATH = ""
            private set
    }

    private var serverJob: Job? = null
    private var pollingJob: Job? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _serviceStatus = MutableStateFlow<ServiceStatus>(ServiceStatus.Stopped)
    val serviceStatus = _serviceStatus.asStateFlow()

    override fun onCreate() {
        super.onCreate()

        val backendBaseDirectory = filesDir
        DEFAULT_BACKEND_DIRECTORY = backendBaseDirectory.absolutePath

        val serverSocketFile = File(filesDir, "rust_server.sock")
        DEFAULT_SOCKET_PATH = serverSocketFile.absolutePath

        val path = Path(serverSocketFile.absolutePath)

        try {
            Files.delete(path)
        } catch (e: Exception) {
            // ignore
            e.printStackTrace()
        } finally {
            Files.createFile(path)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(
            SaveApp.SNOWBIRD_SERVICE_ID,
            createNotification("Snowbird Server is starting up.")
        )
        startServer(DEFAULT_BACKEND_DIRECTORY, DEFAULT_SOCKET_PATH)
        startPolling()
        return START_STICKY
    }

    override fun onDestroy() {
        stopServer()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * Checks if a web server is available and responding with a 200 OK status.
     * Throws exceptions on failure for better integration with retry mechanisms.
     *
     * @param url The URL to check
     * @param timeout Optional timeout in milliseconds (default 5000ms)
     * @throws ConnectException if the server refuses connection
     * @throws SocketTimeoutException if the connection times out
     * @throws IOException for other network-related errors
     */
    private suspend fun checkServerAvailability(url: String, timeout: Int = 1000) {
        withContext(Dispatchers.IO) {
            var connection: HttpURLConnection? = null
            try {
                connection = (URL(url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = timeout
                    readTimeout = timeout
                    requestMethod = "GET"
                    instanceFollowRedirects = false
                }

                when (connection.responseCode) {
                    HttpURLConnection.HTTP_OK -> return@withContext
                    else -> throw IOException("Server returned ${connection.responseCode}")
                }
            } catch (e: Exception) {
                Timber.d("Server check failed: ${e.message}")
                throw e
            } finally {
                connection?.disconnect()
            }
        }
    }

    private fun createNotification(text: String, withSound: Boolean = false): Notification {
        val channelId =
            if (withSound) SaveApp.SNOWBIRD_SERVICE_CHANNEL_CHIME else SaveApp.SNOWBIRD_SERVICE_CHANNEL_SILENT

        val pendingIntent: PendingIntent = Intent(
            this,
            MainActivity::class.java
        ).let { notificationIntent ->
            PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Raven Service")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_app_notify)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
    }

    /**
     * Starts polling the server for availability
     */
    private fun startPolling() {
        Timber.d("Starting polling")
        pollingJob?.cancel() // Cancel any existing polling

        pollingJob = suspendToRetry { checkServerAvailability("http://localhost:8080/status") }
            .retryWithScope(
                scope = serviceScope,
                config = RetryConfig(
                    maxAttempts = null,
                    backoffStrategy = BackoffStrategy.Linear(
                        baseDelay = 2.seconds,
                    )
                ),
                shouldRetry = { error ->
                    when (error) {
                        is ConnectException,
                        is SocketTimeoutException -> true

                        else -> false
                    }
                }
            ) { attempt ->
                val attemptNumber = attempt.attempt
                when (attempt) {
                    is RetryAttempt.Success -> {
                        _serviceStatus.value = ServiceStatus.Connected
                        updateNotification("Service Connected", withSound = true)
                        Timber.d("Service is up after $attemptNumber attempt(s)")
                        stopPolling()
                    }

                    is RetryAttempt.Retry -> {
                        _serviceStatus.value = ServiceStatus.Connecting
                        updateNotification("Connecting... One moment please.")
                        Timber.d("Attempt $attemptNumber failed, retrying...")
                    }

                    is RetryAttempt.Failure -> {
                        val errorMessage = attempt.error.message ?: "Unknown error"
                        _serviceStatus.value = ServiceStatus.Failed(attempt.error)
                        updateNotification("Connection Failed: $errorMessage")
                        Timber.e(attempt.error)
                        stopPolling()
                    }
                }
            }
    }

    private fun startServer(baseDirectory: String, socketPath: String) {
        serverJob = serviceScope.launch {
            Timber.d("Starting Raven Service")
            val result = SnowbirdBridge.getInstance()
                .startServer(applicationContext, baseDirectory, socketPath)
            Timber.d("Raven Service: $result")
        }
    }

    private fun stopPolling() {
        Timber.d("Stopping polling")
        pollingJob?.cancel()
        pollingJob = null
    }

    private fun stopServer() {
        serverJob?.cancel()
    }

    private fun updateNotification(status: String, withSound: Boolean = false) {
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(
            SaveApp.SNOWBIRD_SERVICE_ID,
            createNotification(status, withSound)
        )
    }
}

/**
 * Represents the current status of the polling service
 */
sealed class ServiceStatus {
    data object Stopped : ServiceStatus()
    data object Connecting : ServiceStatus()
    data object Connected : ServiceStatus()
    data class Failed(val error: Throwable) : ServiceStatus()
}