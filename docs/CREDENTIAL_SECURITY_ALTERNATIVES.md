# Authentication & Credential Security Alternatives

This document explores alternatives for securing credentials for Internet Archive (IA) and WebDAV (Nextcloud) services, focusing on reducing the risk of using main account passwords and restricting access to "upload-time only".

## Comparison of Alternatives

| Method | Description | Security Benefit | Complexity |
| :--- | :--- | :--- | :--- |
| **S3 Access Keys (IA)** | Use Access/Secret keys instead of email/pass. | Prevents leaking main account credentials. Revocable. | Low |
| **App Passwords (WebDAV)** | Unique passwords for "OpenArchive" device. | Revocable without changing main pass. Works with 2FA. | Low |
| **Encrypted Storage** | Store any secret in `EncryptedSharedPreferences`. | Standard baseline. Hardware-backed encryption. | Medium |
| **Biometric Step-up** | Prompt for Biometrics before upload session. | **Credentials only decrypted during upload.** | Medium |
| **OAuth2 / Tokens** | Token-based flow (mostly for Nextcloud). | No passwords stored. Theoretically scoped. | High |

---

## Recommended Strategy: "Secure Session" Approach

To fulfill the requirement of **"accessing them only while uploading"**, we can combine these methods:

### 1. Shift to Lower-Privilege Credentials
We should update the UI to encourage/guide users to enter:
- **IA**: S3 Access Key / Secret Key pairs.
- **Nextcloud**: App Passwords generated in Nextcloud Settings.

### 2. Lock Vaults with Biometrics
We can add a "Secure Vault" toggle. When enabled:
- The app stores the credential in `EncryptedSharedPreferences`.
- The credential is **never** retrieved in the background.
- When an upload starts, the user receives a **Biometric Prompt** (Fingerprint/FaceID).
- Upon success, the credential is decrypted into memory for that specific `Conduit` instance and cleared immediately after the upload task finishes.

### 3. Implementation Path
- **UI**: Add a "Guide me" link in the Login screens explaining how to get S3 keys or App passwords.
- **Domain**: Add `isBiometricLocked: Boolean` to `Vault`.
- **Logic**: Update `UploadWorker` to check for this flag and trigger a biometric prompt if needed before initializing the `Conduit`.

---

## Conclusion
The most realistic and secure path for this app is **App Passwords + Biometric Decryption**. This avoids the complexity of full OAuth2 (which lacks scoped support in many Nextcloud instances) while providing the "on-demand" security requested.
