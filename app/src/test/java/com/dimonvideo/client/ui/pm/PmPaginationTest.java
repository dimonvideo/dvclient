package com.dimonvideo.client.ui.pm;

import org.junit.Test;
import static org.junit.Assert.*;

/** Regression checks for failed requests, overlapping deletion pages, and end-of-feed guards. */
public class PmPaginationTest {
    /** Fast scroll callbacks cannot issue duplicate requests or skip a failed page. */
    @Test public void failedPageIsRetriedWithoutSkipping() {
        PmPagination pager = new PmPagination();
        assertEquals(1, pager.begin());
        assertEquals(0, pager.begin());
        pager.complete(10);
        assertEquals(2, pager.begin());
        pager.fail();
        assertEquals(2, pager.begin());
    }

    /** A short server response stops fetching even when some returned rows were locally hidden. */
    @Test public void shortPageStopsUntilRefresh() {
        PmPagination pager = new PmPagination();
        assertEquals(1, pager.begin());
        pager.complete(3);
        assertTrue(pager.isEndReached());
        assertEquals(0, pager.begin());
        pager.reset();
        assertEquals(1, pager.begin());
    }

    /** A deletion overlapping an in-flight fetch causes the shifted boundary to be re-read. */
    @Test public void deletionDuringRequestRechecksBoundary() {
        PmPagination pager = new PmPagination();
        pager.begin();
        pager.complete(10);
        assertEquals(2, pager.begin());
        pager.onRemoteRemoval();
        pager.complete(10);
        assertEquals(2, pager.begin());
        pager.complete(10);
        assertEquals(3, pager.begin());
    }

    /** Bulk deletion can shift offsets by more than one complete page. */
    @Test public void bulkDeletionRewindsEnoughPages() {
        PmPagination pager = new PmPagination();
        for (int i = 1; i <= 4; i++) {
            assertEquals(i, pager.begin());
            pager.complete(10);
        }
        for (int i = 0; i < 11; i++) pager.onRemoteRemoval();
        assertEquals(3, pager.begin());
    }

    /** A mutation after an empty response permits reconciliation rather than keeping a stale end marker. */
    @Test public void deletionReopensAnEndedFeed() {
        PmPagination pager = new PmPagination();
        pager.begin();
        pager.complete(0);
        pager.onRemoteRemoval();
        assertFalse(pager.isEndReached());
        assertEquals(1, pager.begin());
    }
}
