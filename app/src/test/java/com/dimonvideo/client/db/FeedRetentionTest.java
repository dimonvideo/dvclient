package com.dimonvideo.client.db;

import android.database.Cursor;

import androidx.room.Room;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.List;

import static org.junit.Assert.assertEquals;

/** Exercises production Room retention so startup cleanup cannot erase fresh offline feeds. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, application = android.app.Application.class)
public class FeedRetentionTest {
    private AppDatabase database;

    /** Creates a real isolated Room database without the application's network or worker startup. */
    @Before
    public void createDatabase() {
        database = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), AppDatabase.class)
                .allowMainThreadQueries()
                .build();
    }

    /** Releases the SQLite connection after each independent retention scenario. */
    @After
    public void closeDatabase() {
        database.close();
    }

    /** Both startup cleanup calls preserve recent data in every section and expire old rows only. */
    @Test
    public void repeatedStartupCleanupPreservesRecentSectionFeeds() {
        long now = scalar("SELECT CAST(strftime('%s', 'now') AS INTEGER)");
        long day = 24L * 60 * 60;
        insert(1, "comments", now);
        insert(2, "comments", now - 29 * day);
        insert(3, "comments", now - 31 * day);
        insert(4, "vuploader", now);
        insert(5, "vuploader", now - 31 * day);

        database.feedDao().clearDBOld();
        database.feedDao().clearDBOld();

        List<FeedEntity> home = database.feedDao().getAllRows("comments", 20);
        assertEquals(2, home.size());
        assertEquals(1, home.get(0).lid);
        assertEquals(2, home.get(1).lid);
        List<FeedEntity> videos = database.feedDao().getAllRows("vuploader", 20);
        assertEquals(1, videos.size());
        assertEquals(4, videos.get(0).lid);
    }

    /** The numeric cutoff expires its boundary and leaves independently stored read markers intact. */
    @Test
    public void cutoffBoundaryUsesEpochSecondsWithoutDeletingReadMarkers() {
        long cutoff = scalar("SELECT CAST(strftime('%s', 'now', '-30 day') AS INTEGER)");
        insert(1, "comments", cutoff);
        insert(2, "comments", cutoff + 60);
        ReadMarkEntity mark = new ReadMarkEntity();
        mark.lid = 1;
        mark.razdel = "comments";
        mark.status = 1;
        database.readMarkDao().insert(mark);

        database.feedDao().clearDBOld();

        List<FeedEntity> feeds = database.feedDao().getAllRows("comments", 20);
        assertEquals(1, feeds.size());
        assertEquals(2, feeds.get(0).lid);
        assertEquals(1, database.readMarkDao().getStatus(1, "comments"));
    }

    /** Stores a section row with the same timestamp units used by the API. */
    private void insert(int id, String section, long timestamp) {
        FeedEntity feed = new FeedEntity();
        feed.lid = id;
        feed.razdel = section;
        feed.timestamp = timestamp;
        database.feedDao().insert(feed);
    }

    /** Reads SQLite's own clock to avoid differences between its wall time and the test framework. */
    private long scalar(String sql) {
        try (Cursor cursor = database.getOpenHelper().getWritableDatabase().query(sql)) {
            cursor.moveToFirst();
            return cursor.getLong(0);
        }
    }
}
