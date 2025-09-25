//package net.opendasharchive.openarchive.util
//
//import com.google.api.client.extensions.android.http.AndroidHttp
//import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
//import com.google.api.client.http.FileContent
//import com.google.api.client.json.jackson2.JacksonFactory
//import com.google.api.services.drive.Drive
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.withContext
//import java.io.File
//
//object DriveServiceHelper {
//    const val APPLICATION_NAME = "save"
//
//    private fun getDriveService(credential: GoogleAccountCredential): Drive {
//        return Drive.Builder(
//            AndroidHttp.newCompatibleTransport(),
//            JacksonFactory.getDefaultInstance(),
//            credential
//        )
//            .setApplicationName(APPLICATION_NAME)
//            .build()
//    }
//
//    suspend fun createFolder(folderName: String, parentFolderId: String?, credential: GoogleAccountCredential) {
//        withContext(Dispatchers.IO) {
//            val driveService = getDriveService(credential)
//            val fileMetadata = com.google.api.services.drive.model.File()
//            fileMetadata.name = folderName
//            fileMetadata.mimeType = "application/vnd.google-apps.folder"
//
//            if (parentFolderId != null) {
//                fileMetadata.parents = listOf(parentFolderId)
//            }
//
//            driveService.files().create(fileMetadata).execute()
//        }
//    }
//
//    suspend fun uploadFile(filePath: String, mimeType: String, parentFolder: String?, credential: GoogleAccountCredential) {
//        withContext(Dispatchers.IO) {
//            val driveService = getDriveService(credential)
//            val file = File(filePath)
//            val fileMetadata = com.google.api.services.drive.model.File()
//            fileMetadata.name = file.name
//            fileMetadata.mimeType = mimeType
//
//            if (parentFolder != null) {
//                fileMetadata.parents = listOf(parentFolder)
//            }
//
//            val mediaContent = FileContent(mimeType, file)
//            driveService.files().create(fileMetadata, mediaContent).execute()
//        }
//    }
//}