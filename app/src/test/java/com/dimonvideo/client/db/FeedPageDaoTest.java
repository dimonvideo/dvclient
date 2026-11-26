package com.dimonvideo.client.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.room.Room;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.List;

/** Exercises real Room snapshots and the non-destructive upgrade from the released database. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, application = android.app.Application.class)
public class FeedPageDaoTest {
    private static final String DATABASE_NAME = "feed-page-test";
    private Context context;
    private AppDatabase database;

    /** Opens a separate on-disk cache so each test can verify durable storage without app startup. */
    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        context.deleteDatabase(DATABASE_NAME);
        database = openDatabase();
    }

    /** Releases SQLite and removes the isolated test database. */
    @After
    public void tearDown() {
        if (database != null) database.close();
        context.deleteDatabase(DATABASE_NAME);
    }

    /** Identical item IDs cannot mix sections, categories, searches or accounts, and HTML is unchanged. */
    @Test
    public void responsesRemainScopedAndPreserveRawHtml() {
        String comments = "[{\"id\":7,\"text\":\"<b>Ответ</b>&#x20;&amp;\"}]";
        String video = "[{\"id\":7,\"text\":\"Видео\"}]";
        database.feedPageDao().savePage("comments/category0/account1", 1, comments, 100);
        database.feedPageDao().savePage("video/category0/account1", 1, video, 101);
        database.feedPageDao().savePage("video/category2/account1", 1, "[]", 102);
        database.feedPageDao().savePage("video/category0/search=abc/account1", 1, "[]", 103);
        database.feedPageDao().savePage("video/category0/account2", 1, "[]", 104);

        assertEquals(comments, database.feedPageDao().loadPages("comments/category0/account1").get(0).payload);
        assertEquals(video, database.feedPageDao().loadPages("video/category0/account1").get(0).payload);
        assertTrue(database.feedPageDao().loadPages("comments/category0/account2").isEmpty());
    }

    /** Offline pages are ordered for display and later pagination cannot grow storage without a bound. */
    @Test
    public void onlyFirstFivePagesArePersistedInDisplayOrder() {
        FeedPageDao dao = database.feedPageDao();
        dao.savePage("comments", 1, "[1]", 100);
        dao.savePage("comments", 3, "[3]", 102);
        dao.savePage("comments", 2, "[2]", 101);
        for (int page = 4; page <= 8; page++) dao.savePage("comments", page, "[" + page + "]", 100 + page);

        List<FeedPageEntity> pages = dao.loadPages("comments");
        assertEquals(5, pages.size());
        for (int index = 0; index < pages.size(); index++) {
            assertEquals(index + 1, pages.get(index).page);
            assertEquals("[" + (index + 1) + "]", pages.get(index).payload);
        }
    }

    /** Refreshing one list cannot append outdated later pages or remove another section's snapshot. */
    @Test
    public void successfulFirstPageResetsOnlyItsOwnOlderPages() {
        FeedPageDao dao = database.feedPageDao();
        dao.savePage("comments", 1, "[1]", 100);
        dao.savePage("comments", 2, "[2]", 101);
        dao.savePage("video", 1, "[9]", 102);
        dao.savePage("comments", 1, "[3]", 103);

        assertEquals(1, dao.loadPages("comments").size());
        assertEquals("[3]", dao.loadPages("comments").get(0).payload);
        assertEquals("[9]", dao.loadPages("video").get(0).payload);
    }

    /** A successful empty server response replaces cached items instead of reviving them when offline. */
    @Test
    public void emptyFirstPageIsAnAuthoritativePersistedResponse() {
        FeedPageDao dao = database.feedPageDao();
        dao.savePage("comments", 1, "[1]", 100);
        dao.savePage("comments", 2, "[2]", 101);
        dao.savePage("comments", 1, "[]", 102);

        database.close();
        database = openDatabase();
        List<FeedPageEntity> pages = database.feedPageDao().loadPages("comments");
        assertEquals(1, pages.size());
        assertEquals("[]", pages.get(0).payload);
        assertEquals(102, pages.get(0).savedAt);
    }

    /** Closing the process's database connection does not lose the exact page contents or ordering. */
    @Test
    public void snapshotsSurviveDatabaseReopening() {
        database.feedPageDao().savePage("comments", 1, "[{\"id\":42}]", 100);
        database.feedPageDao().savePage("comments", 2, "[{\"id\":43}]", 101);
        database.close();
        database = openDatabase();

        List<FeedPageEntity> pages = database.feedPageDao().loadPages("comments");
        assertEquals(2, pages.size());
        assertEquals("[{\"id\":42}]", pages.get(0).payload);
        assertEquals("[{\"id\":43}]", pages.get(1).payload);
    }

    /** Scope eviction removes all stale pages together and retains a recently refreshed old section. */
    @Test
    public void scopeLimitRetainsTheFortyMostRecentlySavedSnapshots() {
        FeedPageDao dao = database.feedPageDao();
        for (int scope = 0; scope < 40; scope++) dao.savePage("scope" + scope, 1, "[]", scope);
        dao.savePage("scope0", 2, "[2]", 100);
        dao.savePage("scope1", 2, "[2]", 1);
        dao.savePage("scope40", 1, "[]", 101);

        assertTrue(dao.loadPages("scope1").isEmpty());
        assertEquals(2, dao.loadPages("scope0").size());
        assertEquals(1, dao.loadPages("scope40").size());
        int remaining = 0;
        for (int scope = 0; scope <= 40; scope++) if (!dao.loadPages("scope" + scope).isEmpty()) remaining++;
        assertEquals(40, remaining);
    }

    @Test
    public void expiredScopesAreRemovedTogetherAndRecentlySavedScopesStayComplete() {
        FeedPageDao dao = database.feedPageDao();
        long cutoffMillis = 1_700_000_000_000L;
        dao.savePage("stale", 1, "[1]", cutoffMillis - 10);
        dao.savePage("stale", 3, "[3]", cutoffMillis);
        dao.savePage("recent", 1, "[1]", cutoffMillis - 100);
        dao.savePage("recent", 2, "[2]", cutoffMillis + 1);

        dao.deleteExpiredScopes(cutoffMillis);

        assertTrue(dao.loadPages("stale").isEmpty());
        List<FeedPageEntity> pages = dao.loadPages("recent");
        assertEquals(2, pages.size());
        assertEquals(1, pages.get(0).page);
        assertEquals(2, pages.get(1).page);
    }

    /** User-requested snapshot clearing leaves downloaded content and read markers under their own policies. */
    @Test
    public void clearingSnapshotsDoesNotDeleteOtherStoredData() {
        FeedEntity feed = new FeedEntity();
        feed.lid = 7;
        feed.razdel = "comments";
        database.feedDao().insert(feed);
        ReadMarkEntity readMark = new ReadMarkEntity();
        readMark.lid = 7;
        readMark.razdel = "comments";
        readMark.status = 1;
        database.readMarkDao().insert(readMark);
        database.feedPageDao().savePage("comments", 1, "[]", 100);

        database.feedPageDao().clearAll();

        assertTrue(database.feedPageDao().loadPages("comments").isEmpty());
        assertEquals(1, database.feedDao().getRecentRows("comments", Long.MIN_VALUE, 10).size());
        assertEquals(1, database.readMarkDao().getStatus(7, "comments"));
    }

    /** Opening the released version-seven schema upgrades it while retaining downloaded items and read status. */
    @Test
    public void migrationFromVersionSevenPreservesExistingData() {
        recreateLegacyDatabase(7);

        database = openDatabase();
        List<FeedEntity> feeds = database.feedDao().getRecentRows("comments", Long.MIN_VALUE, 10);
        assertEquals(1, feeds.size());
        assertEquals("Сохранено", feeds.get(0).title);
        assertEquals("<b>HTML</b>", feeds.get(0).description);
        assertEquals("Полный текст", feeds.get(0).fullText);
        assertEquals(1, database.readMarkDao().getStatus(7, "comments"));
        try (Cursor mark = database.getOpenHelper().getReadableDatabase().query(
                "SELECT marked_at, CAST(strftime('%s', 'now') AS INTEGER) "
                        + "FROM read_marks WHERE lid = 7 AND razdel = 'comments'")) {
            assertTrue(mark.moveToFirst());
            long age = mark.getLong(1) - mark.getLong(0);
            assertTrue("The migrated mark starts a fresh retention period", age >= 0 && age <= 60);
        }
        database.feedPageDao().savePage("comments", 1, "[]", 100);
        assertEquals("[]", database.feedPageDao().loadPages("comments").get(0).payload);
        assertEquals(9, database.getOpenHelper().getReadableDatabase().getVersion());
    }

    @Test
    public void migrationFromVersionEightPreservesExistingSnapshots() {
        recreateLegacyDatabase(8);

        database = openDatabase();
        List<FeedPageEntity> pages = database.feedPageDao().loadPages("comments");
        assertEquals(1, pages.size());
        assertEquals("[7]", pages.get(0).payload);
        assertEquals(101, pages.get(0).savedAt);
        assertEquals(1, database.readMarkDao().getStatus(7, "comments"));
    }

    private void recreateLegacyDatabase(int version) {
        database.close();
        context.deleteDatabase(DATABASE_NAME);
        try (SQLiteDatabase legacy = context.openOrCreateDatabase(DATABASE_NAME, 0, null)) {
            legacy.execSQL("CREATE TABLE data (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + "lid INTEGER NOT NULL, title TEXT, text TEXT, full_text TEXT, date TEXT, "
                    + "time INTEGER NOT NULL, category TEXT, img TEXT, razdel TEXT, size TEXT, "
                    + "url TEXT, state INTEGER NOT NULL)");
            legacy.execSQL("CREATE INDEX index_data_lid ON data(lid)");
            legacy.execSQL("CREATE INDEX index_data_razdel ON data(razdel)");
            legacy.execSQL("CREATE UNIQUE INDEX index_data_lid_razdel ON data(lid, razdel)");
            legacy.execSQL("CREATE TABLE read_marks (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + "lid INTEGER NOT NULL, razdel TEXT, status INTEGER NOT NULL)");
            legacy.execSQL("CREATE UNIQUE INDEX index_read_marks_lid_razdel ON read_marks(lid, razdel)");
            legacy.execSQL("INSERT INTO data (lid,title,text,full_text,time,razdel,state) "
                    + "VALUES (7,'Сохранено','<b>HTML</b>','Полный текст',123,'comments',0)");
            legacy.execSQL("INSERT INTO read_marks (lid,razdel,status) VALUES (7,'comments',1)");
            if (version == 8) {
                legacy.execSQL("CREATE TABLE feed_pages (scope_key TEXT NOT NULL, page INTEGER NOT NULL, "
                        + "payload TEXT NOT NULL, saved_at INTEGER NOT NULL, PRIMARY KEY(scope_key, page))");
                legacy.execSQL("INSERT INTO feed_pages (scope_key,page,payload,saved_at) "
                        + "VALUES ('comments',1,'[7]',101)");
            }
            legacy.setVersion(version);
        }
    }

    /** Uses production entities and migration with synchronous access limited to this isolated test. */
    private AppDatabase openDatabase() {
        return Room.databaseBuilder(context, AppDatabase.class, DATABASE_NAME)
                .addMigrations(AppDatabase.MIGRATION_7_8, AppDatabase.MIGRATION_8_9)
                .allowMainThreadQueries()
                .build();
    }
}
