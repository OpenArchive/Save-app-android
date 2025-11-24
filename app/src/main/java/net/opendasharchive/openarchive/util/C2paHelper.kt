package net.opendasharchive.openarchive.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.location.Location
import android.security.keystore.UserNotAuthenticatedException
import androidx.fragment.app.FragmentActivity
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.opendasharchive.openarchive.core.logger.AppLogger
import net.opendasharchive.openarchive.features.main.MainActivity
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.BasicConstraints
import org.bouncycastle.asn1.x509.Extension
import org.bouncycastle.asn1.x509.ExtendedKeyUsage
import org.bouncycastle.asn1.x509.KeyPurposeId
import org.bouncycastle.asn1.x509.KeyUsage
import org.bouncycastle.cert.X509v3CertificateBuilder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openssl.jcajce.JcaPEMWriter
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.contentauth.c2pa.C2PA
import org.contentauth.c2pa.SignerInfo
import org.contentauth.c2pa.SigningAlgorithm
import timber.log.Timber
import java.io.File
import java.io.StringWriter
import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.Security
import java.security.cert.X509Certificate
import java.security.spec.ECGenParameterSpec
import java.util.Calendar
import java.util.Date

object C2paHelper {
    private const val CERT_FILE = "c2pa_cert.pem"
    private const val KEY_FILE = "c2pa_key.pem"
    private const val CA_CERT_FILE = "c2pa_ca_cert.pem"

    private var initialized = false
    private var signerInfo: SignerInfo? = null

    init {
        Security.addProvider(BouncyCastleProvider())
    }

    fun init(
        activity: FragmentActivity,
        completed: () -> Unit,
    ) {
        if (initialized) return completed()

        val encryptedKey = Prefs.c2paEncryptedPrivateKey

        if (encryptedKey?.isNotEmpty() == true) {
            // Key is encrypted with biometrics
            Prefs.useC2paKeyEncryption = true

            val key = Hbks.loadKey()

            if (key != null) {
                Hbks.decrypt(encryptedKey, key, activity) { plaintext, e ->
                    if (e is UserNotAuthenticatedException) {
                        Runtime.getRuntime().exit(0)
                    } else {
                        finishInit(activity, completed, plaintext)
                    }
                }
            } else {
                // User removed device lock
                Prefs.c2paEncryptedPrivateKey = null
                Prefs.useC2paKeyEncryption = false
                removeCertificates(activity)
                finishInit(activity, completed)
            }
        } else {
            Prefs.useC2paKeyEncryption = false
            finishInit(activity, completed)
        }
    }

    private fun finishInit(
        context: Context,
        completed: () -> Unit,
        decryptedKey: String? = null,
    ) {
        try {
            // Ensure certificates exist
            if (!certificatesExist(context)) {
                generateCertificates(context, decryptedKey)
            }

            // Load signer info
            signerInfo = loadSignerInfo(context, decryptedKey)

            initialized = true
            AppLogger.i("C2PA initialized successfully")
        } catch (e: Exception) {
            AppLogger.e("Failed to initialize C2PA", e)
            Timber.e(e, "C2PA initialization failed")
        }

        completed()
    }

    private fun certificatesExist(context: Context): Boolean {
        val certFile = File(context.filesDir, CERT_FILE)
        val keyFile = File(context.filesDir, KEY_FILE)
        return certFile.exists() && keyFile.exists()
    }

    private fun generateCertificates(
        context: Context,
        passphrase: String? = null,
    ) {
        AppLogger.d("Generating C2PA certificates")

        // Generate CA key pair
        val caKeyPair = generateKeyPair()
        val caCert = generateCACertificate(caKeyPair)

        // Generate signing key pair
        val signingKeyPair = generateKeyPair()
        val signingCert = generateSigningCertificate(signingKeyPair, caKeyPair, caCert)

        // Convert to PEM format
        val certChainPem = certificateToPem(signingCert) + certificateToPem(caCert)
        val privateKeyPem = privateKeyToPem(signingKeyPair.private)
        val caCertPem = certificateToPem(caCert)

        // Save certificates
        File(context.filesDir, CERT_FILE).writeText(certChainPem)
        File(context.filesDir, CA_CERT_FILE).writeText(caCertPem)

        // Save private key (encrypt if passphrase provided)
        if (passphrase != null) {
            // Store encrypted key in preferences
            Hbks.encrypt(privateKeyPem, Hbks.loadKey()!!, null) { encryptedKey, _ ->
                if (encryptedKey != null) {
                    Prefs.c2paEncryptedPrivateKey = encryptedKey
                }
            }
            // Don't save unencrypted key to file
        } else {
            File(context.filesDir, KEY_FILE).writeText(privateKeyPem)
        }

        AppLogger.i("C2PA certificates generated successfully")
    }

    private fun generateKeyPair(): KeyPair {
        val keyPairGenerator = KeyPairGenerator.getInstance("EC")
        // Use P-256 curve explicitly (required for ES256)
        val ecSpec = ECGenParameterSpec("secp256r1")
        keyPairGenerator.initialize(ecSpec)
        return keyPairGenerator.generateKeyPair()
    }

    private fun generateCACertificate(keyPair: KeyPair): X509Certificate {
        val now = Date()
        val calendar = Calendar.getInstance()
        calendar.time = now
        calendar.add(Calendar.YEAR, 10)
        val expiry = calendar.time

        val issuer = X500Name("CN=Save App CA,O=OpenArchive,C=US")
        val serial = BigInteger.valueOf(System.currentTimeMillis())

        val certBuilder: X509v3CertificateBuilder =
            JcaX509v3CertificateBuilder(
                issuer,
                serial,
                now,
                expiry,
                issuer,
                keyPair.public,
            )

        val extensionUtils = JcaX509ExtensionUtils()

        // CA extensions
        certBuilder.addExtension(
            Extension.basicConstraints,
            true,
            BasicConstraints(true),
        )
        certBuilder.addExtension(
            Extension.keyUsage,
            true,
            KeyUsage(KeyUsage.keyCertSign or KeyUsage.cRLSign),
        )
        // Subject Key Identifier
        certBuilder.addExtension(
            Extension.subjectKeyIdentifier,
            false,
            extensionUtils.createSubjectKeyIdentifier(keyPair.public),
        )

        val signer =
            JcaContentSignerBuilder("SHA256withECDSA")
                .build(keyPair.private)

        return JcaX509CertificateConverter()
            .getCertificate(certBuilder.build(signer))
    }

    private fun generateSigningCertificate(
        signingKeyPair: KeyPair,
        caKeyPair: KeyPair,
        caCert: X509Certificate,
    ): X509Certificate {
        val now = Date()
        val calendar = Calendar.getInstance()
        calendar.time = now
        calendar.add(Calendar.YEAR, 5)
        val expiry = calendar.time

        val subject = X500Name("CN=Save App Signer,O=OpenArchive,C=US")
        val serial = BigInteger.valueOf(System.currentTimeMillis() + 1)

        val certBuilder: X509v3CertificateBuilder =
            JcaX509v3CertificateBuilder(
                caCert,
                serial,
                now,
                expiry,
                subject,
                signingKeyPair.public,
            )

        val extensionUtils = JcaX509ExtensionUtils()

        // Signing certificate extensions
        certBuilder.addExtension(
            Extension.basicConstraints,
            true,
            BasicConstraints(false),
        )
        certBuilder.addExtension(
            Extension.keyUsage,
            true,
            KeyUsage(KeyUsage.digitalSignature or KeyUsage.nonRepudiation),
        )
        // Subject Key Identifier
        certBuilder.addExtension(
            Extension.subjectKeyIdentifier,
            false,
            extensionUtils.createSubjectKeyIdentifier(signingKeyPair.public),
        )
        // Authority Key Identifier - links to the CA
        certBuilder.addExtension(
            Extension.authorityKeyIdentifier,
            false,
            extensionUtils.createAuthorityKeyIdentifier(caCert),
        )
        // Extended Key Usage - for code/document signing
        certBuilder.addExtension(
            Extension.extendedKeyUsage,
            false,
            ExtendedKeyUsage(
                arrayOf(
                    KeyPurposeId.id_kp_codeSigning,
                    KeyPurposeId.id_kp_emailProtection,
                ),
            ),
        )

        val signer =
            JcaContentSignerBuilder("SHA256withECDSA")
                .build(caKeyPair.private)

        return JcaX509CertificateConverter()
            .getCertificate(certBuilder.build(signer))
    }

    private fun certificateToPem(cert: X509Certificate): String {
        val writer = StringWriter()
        JcaPEMWriter(writer).use { pemWriter ->
            pemWriter.writeObject(cert)
        }
        return writer.toString()
    }

    private fun privateKeyToPem(privateKey: PrivateKey): String {
        // Output in PKCS#8 format (required by C2PA library)
        val base64 = android.util.Base64.encodeToString(
            privateKey.encoded,
            android.util.Base64.NO_WRAP
        )
        return "-----BEGIN PRIVATE KEY-----\n" +
                base64.chunked(64).joinToString("\n") +
                "\n-----END PRIVATE KEY-----\n"
    }

    private fun loadSignerInfo(
        context: Context,
        decryptedKey: String? = null,
    ): SignerInfo {
        val certPem = File(context.filesDir, CERT_FILE).readText()

        val keyPem =
            decryptedKey ?: File(context.filesDir, KEY_FILE).readText()

        return SignerInfo(
            algorithm = SigningAlgorithm.ES256,
            certificatePEM = certPem,
            privateKeyPEM = keyPem,
            tsaURL = null, // Optional: add timestamp server URL for production
        )
    }

    fun getSignerInfo(): SignerInfo? = signerInfo

    fun getCertificatePem(context: Context): String? {
        val certFile = File(context.filesDir, CERT_FILE)
        return if (certFile.exists()) certFile.readText() else null
    }

    fun removeCertificates(context: Context) {
        listOf(CERT_FILE, KEY_FILE, CA_CERT_FILE).forEach { filename ->
            try {
                File(context.filesDir, filename).delete()
            } catch (e: Exception) {
                Timber.d(e)
            }
        }
        signerInfo = null
        initialized = false
    }

    fun signMedia(
        context: Context,
        sourcePath: String,
        destPath: String,
        location: Location? = null,
    ): Boolean {
        val signer =
            signerInfo ?: run {
                AppLogger.e("C2PA not initialized - cannot sign media")
                return false
            }

        try {
            val manifest = createManifest(context, location)

            C2PA.signFile(
                sourcePath = sourcePath,
                destPath = destPath,
                manifest = manifest,
                signerInfo = signer,
            )

            // Save manifest as separate file
            try {
                val manifestJson = C2PA.readFile(destPath)
                if (manifestJson != null) {
                    val manifestFile = File(destPath).let {
                        File(it.parent, "${it.nameWithoutExtension}.c2pa.json")
                    }
                    manifestFile.writeText(manifestJson)
                    AppLogger.i("C2PA manifest saved to: ${manifestFile.absolutePath}")
                }
            } catch (e: Exception) {
                AppLogger.w("Could not save separate manifest file: ${e.message}")
            }

            AppLogger.i("C2PA manifest embedded successfully")
            return true
        } catch (e: Exception) {
            AppLogger.e("Failed to sign media with C2PA", e)
            Timber.e(e, "C2PA signing failed")
            return false
        }
    }

    private fun createManifest(
        context: Context,
        location: Location? = null,
    ): String {
        val manifest = buildJsonObject {
            put("claim_generator", "Save by OpenArchive")
            put("title", "Media captured with Save")
            put("assertions", buildJsonArray {
                // Add creation info
                add(buildJsonObject {
                    put("label", "stds.schema-org.CreativeWork")
                    put("data", buildJsonObject {
                        put("@context", "https://schema.org")
                        put("@type", "CreativeWork")
                        put("author", buildJsonArray {
                            add(buildJsonObject {
                                put("@type", "Person")
                                put("name", "Save App User")
                            })
                        })
                    })
                })

                // Add actions
                add(buildJsonObject {
                    put("label", "c2pa.actions")
                    put("data", buildJsonObject {
                        put("actions", buildJsonArray {
                            add(buildJsonObject {
                                put("action", "c2pa.created")
                                put("softwareAgent", buildJsonObject {
                                    put("name", "Save by OpenArchive")
                                    put("version", getAppVersion(context))
                                })
                                put("when", java.time.Instant.now().toString())
                            })
                        })
                    })
                })

                // Add location if available and enabled
                if (location != null && Prefs.c2paLocation) {
                    add(buildJsonObject {
                        put("label", "stds.exif")
                        put("data", buildJsonObject {
                            put("EXIF:GPSLatitude", location.latitude)
                            put("EXIF:GPSLongitude", location.longitude)
                            put("EXIF:GPSAltitude", location.altitude)
                        })
                    })
                }
            })
        }

        return manifest.toString()
    }

    private fun getAppVersion(context: Context): String =
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }

    fun restartApp(activity: Activity) {
        val intent = Intent(activity, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        activity.startActivity(intent)
        activity.finish()
        Prefs.store()
        Runtime.getRuntime().exit(0)
    }

    fun readManifest(filePath: String): String? =
        try {
            C2PA.readFile(filePath)
        } catch (e: Exception) {
            AppLogger.e("Failed to read C2PA manifest", e)
            null
        }
}
