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

import java.util.ArrayList;
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

    @Test
    public void expiredArticlesAreDeletedInBatchesAcrossSections() {
        long now = scalar("SELECT CAST(strftime('%s', 'now') AS INTEGER)");
        long day = 24L * 60 * 60;
        long cutoff = now - 30 * day;
        insert(1, "comments", now);
        insert(2, "comments", cutoff + 1);
        insert(3, "comments", now - 31 * day);
        insert(4, "vuploader", now);
        insert(5, "vuploader", cutoff);
        insert(6, "articles", now - 32 * day);

        assertEquals(2, database.feedDao().deleteExpired(cutoff, 2));
        assertEquals(1, database.feedDao().deleteExpired(cutoff, 2));
        assertEquals(0, database.feedDao().deleteExpired(cutoff, 2));

        List<FeedEntity> home = database.feedDao().getRecentRows("comments", Long.MIN_VALUE, 20);
        assertEquals(2, home.size());
        assertEquals(1, home.get(0).lid);
        assertEquals(2, home.get(1).lid);
        List<FeedEntity> videos = database.feedDao().getRecentRows("vuploader", Long.MIN_VALUE, 20);
        assertEquals(1, videos.size());
        assertEquals(4, videos.get(0).lid);
        assertEquals(0, database.feedDao().getRecentRows("articles", Long.MIN_VALUE, 20).size());
    }

    @Test
    public void expiredReadMarksAreBatchedIndependentlyOfArticleDates() {
        long cutoff = scalar("SELECT CAST(strftime('%s', 'now', '-30 day') AS INTEGER)");
        insert(1, "comments", cutoff);
        insert(2, "comments", cutoff + 60);
        ReadMarkDao marks = database.readMarkDao();
        marks.insertAll(List.of(
                readMark(1, "comments", cutoff + 1),
                readMark(2, "comments", cutoff),
                readMark(1, "articles", cutoff - 1),
                readMark(2, "vuploader", cutoff - 2)));

        assertEquals(1, database.feedDao().deleteExpired(cutoff, 2));
        assertEquals(2, marks.deleteExpired(cutoff, 2));
        assertEquals(1, marks.deleteExpired(cutoff, 2));
        assertEquals(0, marks.deleteExpired(cutoff, 2));

        List<FeedEntity> feeds = database.feedDao().getRecentRows("comments", Long.MIN_VALUE, 20);
        assertEquals(1, feeds.size());
        assertEquals(2, feeds.get(0).lid);
        assertEquals(1, marks.getStatus(1, "comments"));
        assertEquals(0, marks.getStatus(2, "comments"));
        assertEquals(0, marks.getStatus(1, "articles"));
        assertEquals(0, marks.getStatus(2, "vuploader"));
    }

    @Test
    public void recentBulkMarksSurviveCleanupAfterMoreThanTwoHundredLaterMarks() {
        long cutoff = scalar("SELECT CAST(strftime('%s', 'now', '-30 day') AS INTEGER)");
        for (int id = 250; id >= 1; id--) {
            insert(id, "comments", cutoff + id);
        }
        insert(0, "comments", cutoff + 251);
        insert(250, "articles", cutoff + 250);
        insert(250, "vuploader", cutoff);
        ReadMarkDao marks = database.readMarkDao();

        marks.markRecentRead("comments", 20, cutoff);
        marks.markRecentRead("vuploader", 20, cutoff);
        List<ReadMarkEntity> laterMarks = new ArrayList<>();
        for (int id = 1; id <= 205; id++) {
            laterMarks.add(readMark(id, "articles", cutoff + 1));
        }
        marks.insertAll(laterMarks);
        assertEquals(0, marks.deleteExpired(cutoff, 20));

        for (int id = 231; id <= 250; id++) {
            assertEquals("Read mark for " + id, 1, marks.getStatus(id, "comments"));
        }
        assertEquals(0, marks.getStatus(230, "comments"));
        assertEquals(0, marks.getStatus(0, "comments"));
        assertEquals(0, marks.getStatus(250, "articles"));
        assertEquals(0, marks.getStatus(250, "vuploader"));
        assertEquals(225, scalar("SELECT COUNT(*) FROM read_marks"));
    }

    /** Stores a section row with the same timestamp units used by the API. */
    private void insert(int id, String section, long timestamp) {
        FeedEntity feed = new FeedEntity();
        feed.lid = id;
        feed.razdel = section;
        feed.timestamp = timestamp;
        database.feedDao().insert(feed);
    }

    private ReadMarkEntity readMark(int id, String section, long timestamp) {
        ReadMarkEntity mark = new ReadMarkEntity();
        mark.lid = id;
        mark.razdel = section;
        mark.status = 1;
        mark.markedAt = timestamp;
        return mark;
    }

    /** Reads SQLite's own clock to avoid differences between its wall time and the test framework. */
    private long scalar(String sql) {
        try (Cursor cursor = database.getOpenHelper().getWritableDatabase().query(sql)) {
            cursor.moveToFirst();
            return cursor.getLong(0);
        }
    }
}
