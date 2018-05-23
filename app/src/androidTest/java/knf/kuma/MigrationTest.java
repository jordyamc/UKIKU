package knf.kuma;

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.db.framework.FrameworkSQLiteOpenHelperFactory;
import android.arch.persistence.room.testing.MigrationTestHelper;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import knf.kuma.database.CacheDB;

@RunWith(AndroidJUnit4.class)
public class MigrationTest {

    private static final String TEST_DB_NAME = "test-cache-db";
    @Rule
    public MigrationTestHelper migrationTestHelper =
            new MigrationTestHelper(
                    InstrumentationRegistry.getInstrumentation(),
                    CacheDB.class.getCanonicalName(),
                    new FrameworkSQLiteOpenHelperFactory());

    @Test
    public void testMigration() throws Exception {
        SupportSQLiteDatabase db = migrationTestHelper.createDatabase(TEST_DB_NAME, 2);
        db.close();
        migrationTestHelper.runMigrationsAndValidate(TEST_DB_NAME, 3, true, CacheDB.MIGRATION_2_3);
    }
}
