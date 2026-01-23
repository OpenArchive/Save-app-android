package net.opendasharchive.openarchive.db.sugar


import android.content.Context
import com.orm.SugarRecord
import net.opendasharchive.openarchive.BuildConfig
import net.opendasharchive.openarchive.db.sugar.Space

object DatabaseSeeder {

    /**
     * Clears all tables and saves initial dummy data in a transaction.
     * This is useful for resetting the database in debug/testing builds.
     */
    fun seed(context: Context) {
        // Run this only for debug builds or specific test environments
        if (BuildConfig.DEBUG) {
            // Optional: Clear existing data before seeding
            clearDatabase()

            // Save all data collections in a single transaction for efficiency
            // Combine all lists into one collection for saveInTx
            val allEntities = dummySpaceList + dummyProjectList// + dummyCollectionList + dummyMediaList
            SugarRecord.saveInTx(allEntities)
        }
    }

    /**
     * Deletes all records from all relevant tables.
     */
    private fun clearDatabase() {
        SugarRecord.deleteAll(Media::class.java)
        SugarRecord.deleteAll(Collection::class.java)
        SugarRecord.deleteAll(Project::class.java)
        SugarRecord.deleteAll(Space::class.java)
    }
}