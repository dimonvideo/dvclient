package com.dimonvideo.client.ui.pm;

/** Tracks the API's ten-row pages without advancing on a failed or duplicate request. */
public final class PmPagination {
    public static final int PAGE_SIZE = 10;
    private int nextPage = 1;
    private int activePage;
    private int removedRows;
    private boolean endReached;

    /** Resets pagination when refreshing or changing the authenticated mailbox. */
    public void reset() {
        nextPage = 1;
        activePage = 0;
        removedRows = 0;
        endReached = false;
    }

    /** Returns the next page and acquires the loading guard, or zero when no request is needed. */
    public int begin() {
        if (activePage != 0 || endReached) return 0;
        // Server-side deletions shift LIMIT offsets. Re-read overlapping pages and deduplicate IDs.
        nextPage = Math.max(1, nextPage - (removedRows + PAGE_SIZE - 1) / PAGE_SIZE);
        removedRows = 0;
        activePage = nextPage;
        return activePage;
    }

    /** Advances only after a successful response; the raw row count determines the terminal page. */
    public void complete(int rowCount) {
        if (activePage == 0) throw new IllegalStateException("No PM page is loading");
        nextPage = activePage + 1;
        activePage = 0;
        endReached = rowCount < PAGE_SIZE && removedRows == 0;
    }

    /** Releases the loading guard while preserving the page for an explicit retry. */
    public void fail() {
        activePage = 0;
    }

    /** Reopens the end marker and overlaps the next request after a confirmed server mutation. */
    public void onRemoteRemoval() {
        removedRows++;
        endReached = false;
    }

    /** Returns whether a network page is currently in flight. */
    public boolean isLoading() {
        return activePage != 0;
    }

    /** Returns whether the server has supplied a short or empty final page. */
    public boolean isEndReached() {
        return endReached;
    }
}
