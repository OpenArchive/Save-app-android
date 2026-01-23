package net.opendasharchive.openarchive.db.sugar


import net.opendasharchive.openarchive.db.sugar.Space
import java.util.* // Needed for Date objects

// --- 1. Spaces (Container) ---

val dummySpaceList = listOf(
    Space(
        type = Space.Type.WEBDAV.id,
        username = "test01",
        password = "test01",
        name = "Elelan Server",
        host = "https://nx27277.your-storageshare.de/remote.php/webdav/",
        licenseUrl = "https://creativecommons.org/licenses/by-sa/4.0/"
    ).apply { id = 1L },
    Space(
        type = Space.Type.INTERNET_ARCHIVE.id,
        username = "test02",
        password = "test02",
        name = "IA Server",
        host = "https://nx27277.your-storageshare.de/remote.php/webdav/",
        licenseUrl = "https://creativecommons.org/licenses/by-sa/4.0/"
    ).apply { id = 2L },
    Space(
        type = Space.Type.RAVEN.id,
        username = "test03",
        password = "test03",
        name = "SaveDweb",
        host = "https://nx27277.your-storageshare.de/remote.php/webdav/",
        licenseUrl = "https://creativecommons.org/licenses/by-sa/4.0/"
    ).apply { id = 3L },
    Space(
        type = Space.Type.WEBDAV.id,
        username = "test04",
        password = "test04",
        name = "NextCloud",
        host = "https://nx27277.your-storageshare.de/remote.php/webdav/",
        licenseUrl = "https://creativecommons.org/licenses/by-sa/4.0/"
    ).apply { id = 4L },
)

// ----------------------------------------

// --- 2. Projects (Folder) ---
// Linked to dummySpaceList: 1L, 2L, 4L

val dummyProjectList = listOf(
    // Linked to Space 1L (WEBDAV)
    Project(
        spaceId = 1L,
        description = "Recent Vacation"
    ).apply { id = 1L; created = Date(System.currentTimeMillis() - 86400000) }, // 1 day ago
    Project(
        spaceId = 1L,
        description = "Archived Content",
        archived = true // For testing archived projects
    ).apply { id = 2L; created = Date(System.currentTimeMillis() - 2 * 86400000) }, // 2 days ago
    Project(
        spaceId = 1L,
        description = "Work Documents"
    ).apply { id = 5L; created = Date(System.currentTimeMillis() - 5 * 86400000) }, // 5 days ago
    Project(
        spaceId = 1L,
        description = "Family Photos"
    ).apply { id = 6L; created = Date(System.currentTimeMillis() - 7 * 86400000) }, // 7 days ago

    // Linked to Space 2L (Internet Archive)
    Project(
        spaceId = 2L,
        description = "Archive Test"
    ).apply { id = 3L; created = Date(System.currentTimeMillis() - 3 * 86400000) }, // 3 days ago

    // Linked to Space 4L (NextCloud)
    Project(
        spaceId = 4L,
        description = "Media Uploads"
    ).apply { id = 4L; created = Date() }, // Now
)

// ----------------------------------------

// --- 3. Collections (Upload Batches) ---
// Linked to dummyProjectList: 1L, 2L, 3L, 4L.
// Includes 'open' (uploadDate = null) and 'uploaded' collections.

val dummyCollectionList = listOf(
    // Collections for Project 1 (Recent Vacation)
    Collection(
        projectId = 1L,
        uploadDate = null, // Open Collection (has uploading media)
    ).apply { id = 1L },
    Collection(
        projectId = 1L,
        uploadDate = Date(System.currentTimeMillis() - 3600000), // Uploaded an hour ago
        serverUrl = "server/url/p1c2"
    ).apply { id = 2L },

    // Collections for Project 2 (Archived Content)
    Collection(
        projectId = 2L,
        uploadDate = Date(System.currentTimeMillis() - 7200000), // Uploaded two hours ago
        serverUrl = "server/url/p2c3"
    ).apply { id = 3L },

    // Collections for Project 3 (Public Archive Test)
    Collection(
        projectId = 3L,
        uploadDate = null, // Open Collection (has error media)
    ).apply { id = 4L },
    Collection(
        projectId = 3L,
        uploadDate = Date(System.currentTimeMillis() - 10800000), // Uploaded three hours ago
        serverUrl = "server/url/p3c5"
    ).apply { id = 5L },

    // Collections for Project 4 (Mixed Media Uploads)
    Collection(
        projectId = 4L,
        uploadDate = null, // Open Collection (has queued media)
    ).apply { id = 6L },

    // Collections for Project 5 (Work Documents) - 4 collections with multiple media
    Collection(
        projectId = 5L,
        uploadDate = null, // Open Collection (has uploading media)
    ).apply { id = 7L },
    Collection(
        projectId = 5L,
        uploadDate = Date(System.currentTimeMillis() - 14400000), // Uploaded 4 hours ago
        serverUrl = "server/url/p5c8"
    ).apply { id = 8L },
    Collection(
        projectId = 5L,
        uploadDate = Date(System.currentTimeMillis() - 21600000), // Uploaded 6 hours ago
        serverUrl = "server/url/p5c9"
    ).apply { id = 9L },
    Collection(
        projectId = 5L,
        uploadDate = Date(System.currentTimeMillis() - 28800000), // Uploaded 8 hours ago
        serverUrl = "server/url/p5c10"
    ).apply { id = 10L },

    // Collections for Project 6 (Family Photos)
    Collection(
        projectId = 6L,
        uploadDate = null, // Open Collection
    ).apply { id = 11L },
    Collection(
        projectId = 6L,
        uploadDate = Date(System.currentTimeMillis() - 172800000), // Uploaded 2 days ago
        serverUrl = "server/url/p6c12"
    ).apply { id = 12L },
)

// ----------------------------------------

// --- 4. Media (Files) ---
// Linked to dummyProjectList and dummyCollectionList

val dummyMediaList = listOf(
    // Media for Project 1 (Vacation), Open Collection 1
    Media(
        projectId = 1L,
        collectionId = 1L,
        mimeType = "image/jpeg",
        title = "IMG_001_Uploading.jpg",
        status = Media.Status.Uploading.id, // Actively uploading
        createDate = Date(System.currentTimeMillis() - 10000),
        contentLength = 5_000_000, // 5MB
        progress = 2_500_000 // 50% uploaded
    ).apply { id = 1L },
    Media(
        projectId = 1L,
        collectionId = 1L,
        mimeType = "video/mp4",
        title = "VID_002_New.mp4",
        status = Media.Status.New.id, // Ready to be queued
        createDate = Date(System.currentTimeMillis() - 20000),
        contentLength = 50_000_000 // 50MB
    ).apply { id = 2L },

    // Media for Project 1, Uploaded Collection 2
    Media(
        projectId = 1L,
        collectionId = 2L,
        mimeType = "image/png",
        title = "Uploaded_3_Completed.png",
        status = Media.Status.Uploaded.id, // Uploaded
        uploadDate = Date(System.currentTimeMillis() - 3600000),
        contentLength = 1_000_000
    ).apply { id = 3L },

    // Media for Project 3 (Public Archive), Open Collection 4
    Media(
        projectId = 3L,
        collectionId = 4L,
        mimeType = "audio/mp3",
        title = "Error_4_FailedUpload.mp3",
        status = Media.Status.Error.id, // Failed upload
        statusMessage = "Server connection lost.",
        contentLength = 8_000_000
    ).apply { id = 4L },

    // Media for Project 3, Uploaded Collection 5
    Media(
        projectId = 3L,
        collectionId = 5L,
        mimeType = "video/mov",
        title = "Uploaded_5_PublicVideo.mov",
        status = Media.Status.Uploaded.id, // Uploaded
        uploadDate = Date(System.currentTimeMillis() - 10800000),
        contentLength = 30_000_000
    ).apply { id = 5L },

    // Media for Project 4, Open Collection 6
    Media(
        projectId = 4L,
        collectionId = 6L,
        mimeType = "image/gif",
        title = "Queued_6_Animation.gif",
        status = Media.Status.Queued.id, // Waiting for upload
        contentLength = 2_000_000
    ).apply { id = 6L },

    // ========================================
    // Media for Project 5 (Work Documents) - Collection 7 (Open, has uploading items)
    // ========================================
    Media(
        projectId = 5L,
        collectionId = 7L,
        mimeType = "application/pdf",
        title = "Report_Q4_2024.pdf",
        status = Media.Status.Uploading.id,
        createDate = Date(System.currentTimeMillis() - 5000),
        contentLength = 12_000_000, // 12MB
        progress = 8_000_000 // 67% uploaded
    ).apply { id = 7L },
    Media(
        projectId = 5L,
        collectionId = 7L,
        mimeType = "application/vnd.ms-excel",
        title = "Budget_Analysis.xlsx",
        status = Media.Status.New.id,
        createDate = Date(System.currentTimeMillis() - 15000),
        contentLength = 3_500_000 // 3.5MB
    ).apply { id = 8L },
    Media(
        projectId = 5L,
        collectionId = 7L,
        mimeType = "application/vnd.ms-powerpoint",
        title = "Presentation_Slides.pptx",
        status = Media.Status.Queued.id,
        createDate = Date(System.currentTimeMillis() - 25000),
        contentLength = 18_000_000 // 18MB
    ).apply { id = 9L },
    Media(
        projectId = 5L,
        collectionId = 7L,
        mimeType = "image/jpeg",
        title = "Chart_Screenshot.jpg",
        status = Media.Status.New.id,
        createDate = Date(System.currentTimeMillis() - 30000),
        contentLength = 2_200_000 // 2.2MB
    ).apply { id = 10L },

    // Media for Project 5 - Collection 8 (Uploaded 4 hours ago)
    Media(
        projectId = 5L,
        collectionId = 8L,
        mimeType = "application/pdf",
        title = "Meeting_Notes_Jan.pdf",
        status = Media.Status.Uploaded.id,
        uploadDate = Date(System.currentTimeMillis() - 14400000),
        contentLength = 1_800_000
    ).apply { id = 11L },
    Media(
        projectId = 5L,
        collectionId = 8L,
        mimeType = "application/msword",
        title = "Project_Proposal.docx",
        status = Media.Status.Uploaded.id,
        uploadDate = Date(System.currentTimeMillis() - 14400000),
        contentLength = 4_200_000
    ).apply { id = 12L },
    Media(
        projectId = 5L,
        collectionId = 8L,
        mimeType = "image/png",
        title = "Logo_Design_v2.png",
        status = Media.Status.Uploaded.id,
        uploadDate = Date(System.currentTimeMillis() - 14400000),
        contentLength = 850_000
    ).apply { id = 13L },

    // Media for Project 5 - Collection 9 (Uploaded 6 hours ago)
    Media(
        projectId = 5L,
        collectionId = 9L,
        mimeType = "video/mp4",
        title = "Training_Video_Part1.mp4",
        status = Media.Status.Uploaded.id,
        uploadDate = Date(System.currentTimeMillis() - 21600000),
        contentLength = 85_000_000 // 85MB
    ).apply { id = 14L },
    Media(
        projectId = 5L,
        collectionId = 9L,
        mimeType = "video/mp4",
        title = "Training_Video_Part2.mp4",
        status = Media.Status.Uploaded.id,
        uploadDate = Date(System.currentTimeMillis() - 21600000),
        contentLength = 92_000_000 // 92MB
    ).apply { id = 15L },
    Media(
        projectId = 5L,
        collectionId = 9L,
        mimeType = "application/pdf",
        title = "Training_Materials.pdf",
        status = Media.Status.Uploaded.id,
        uploadDate = Date(System.currentTimeMillis() - 21600000),
        contentLength = 6_500_000
    ).apply { id = 16L },
    Media(
        projectId = 5L,
        collectionId = 9L,
        mimeType = "image/jpeg",
        title = "Certificate_Template.jpg",
        status = Media.Status.Uploaded.id,
        uploadDate = Date(System.currentTimeMillis() - 21600000),
        contentLength = 1_200_000
    ).apply { id = 17L },

    // Media for Project 5 - Collection 10 (Uploaded 8 hours ago)
    Media(
        projectId = 5L,
        collectionId = 10L,
        mimeType = "application/zip",
        title = "Source_Code_Backup.zip",
        status = Media.Status.Uploaded.id,
        uploadDate = Date(System.currentTimeMillis() - 28800000),
        contentLength = 145_000_000 // 145MB
    ).apply { id = 18L },
    Media(
        projectId = 5L,
        collectionId = 10L,
        mimeType = "text/plain",
        title = "README.txt",
        status = Media.Status.Uploaded.id,
        uploadDate = Date(System.currentTimeMillis() - 28800000),
        contentLength = 15_000
    ).apply { id = 19L },
    Media(
        projectId = 5L,
        collectionId = 10L,
        mimeType = "application/json",
        title = "config_backup.json",
        status = Media.Status.Uploaded.id,
        uploadDate = Date(System.currentTimeMillis() - 28800000),
        contentLength = 45_000
    ).apply { id = 20L },

    // ========================================
    // Media for Project 6 (Family Photos) - Collection 11 (Open)
    // ========================================
    Media(
        projectId = 6L,
        collectionId = 11L,
        mimeType = "image/jpeg",
        title = "Birthday_Party_2024_01.jpg",
        status = Media.Status.Uploading.id,
        createDate = Date(System.currentTimeMillis() - 8000),
        contentLength = 4_500_000,
        progress = 3_000_000 // 67% uploaded
    ).apply { id = 21L },
    Media(
        projectId = 6L,
        collectionId = 11L,
        mimeType = "image/jpeg",
        title = "Birthday_Party_2024_02.jpg",
        status = Media.Status.Queued.id,
        createDate = Date(System.currentTimeMillis() - 12000),
        contentLength = 4_800_000
    ).apply { id = 22L },
    Media(
        projectId = 6L,
        collectionId = 11L,
        mimeType = "video/mp4",
        title = "Birthday_Cake_Candles.mp4",
        status = Media.Status.New.id,
        createDate = Date(System.currentTimeMillis() - 18000),
        contentLength = 35_000_000
    ).apply { id = 23L },

    // Media for Project 6 - Collection 12 (Uploaded 2 days ago)
    Media(
        projectId = 6L,
        collectionId = 12L,
        mimeType = "image/jpeg",
        title = "Beach_Sunset_01.jpg",
        status = Media.Status.Uploaded.id,
        uploadDate = Date(System.currentTimeMillis() - 172800000),
        contentLength = 6_200_000
    ).apply { id = 24L },
    Media(
        projectId = 6L,
        collectionId = 12L,
        mimeType = "image/jpeg",
        title = "Beach_Sunset_02.jpg",
        status = Media.Status.Uploaded.id,
        uploadDate = Date(System.currentTimeMillis() - 172800000),
        contentLength = 5_800_000
    ).apply { id = 25L },
    Media(
        projectId = 6L,
        collectionId = 12L,
        mimeType = "image/jpeg",
        title = "Kids_Playing_Sand.jpg",
        status = Media.Status.Uploaded.id,
        uploadDate = Date(System.currentTimeMillis() - 172800000),
        contentLength = 4_100_000
    ).apply { id = 26L },
    Media(
        projectId = 6L,
        collectionId = 12L,
        mimeType = "video/mov",
        title = "Beach_Waves_Timelapse.mov",
        status = Media.Status.Uploaded.id,
        uploadDate = Date(System.currentTimeMillis() - 172800000),
        contentLength = 78_000_000
    ).apply { id = 27L },
)