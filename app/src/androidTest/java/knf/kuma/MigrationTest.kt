package knf.kuma

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import knf.kuma.database.CacheDB
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationTest {
    @Rule
    var migrationTestHelper = MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            CacheDB::class.java.canonicalName!!,
            FrameworkSQLiteOpenHelperFactory())

    @Test
    @Throws(Exception::class)
    fun testMigration() {
        val db = migrationTestHelper.createDatabase(TEST_DB_NAME, 11)
        db.close()
        migrationTestHelper.runMigrationsAndValidate(TEST_DB_NAME, 12, true, CacheDB.MIGRATION_11_12)
    }

    companion object {

        private val TEST_DB_NAME = "test-cache-db"
    }
}
