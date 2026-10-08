package com.dimonvideo.client.util.pm;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Ensures persisted work cannot be reused under a different authenticated account. */
public class PmDeletionAccountTest {
    /** Canonicalization matches the server's case-insensitive login comparison. */
    @Test
    public void caseAndWhitespaceDoNotChangeTheAccountIdentity() {
        assertEquals(PmDeletionAccount.key("  ТестUser  ", 123),
                PmDeletionAccount.key("тестuser", 123));
    }

    /** Both the server user ID and login participate in the non-secret job identity. */
    @Test
    public void differentAccountsCannotShareAQueueKey() {
        assertNotEquals(PmDeletionAccount.key("User", 1), PmDeletionAccount.key("User", 2));
        assertNotEquals(PmDeletionAccount.key("User", 1), PmDeletionAccount.key("Other", 1));
        assertTrue(PmDeletionAccount.key("User", 1).matches("[0-9a-f]{64}"));
    }

    /** Incomplete or logged-out accounts cannot enqueue authenticated work. */
    @Test
    public void incompleteIdentityCannotQueueWork() {
        assertNull(PmDeletionAccount.key(null, 123));
        assertNull(PmDeletionAccount.key("User", 0));
        assertNull(PmDeletionAccount.key("  ", 123));
    }
}
