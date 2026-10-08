package com.dimonvideo.client.adater;

import android.app.Application;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Protects same-ID messages/members from draft, attachment and read-state leakage between accounts. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, application = Application.class)
public class PmAccountStateTest {
    /** A different account must receive an empty draft even when it opens the same global member/message ID. */
    @Test
    public void sameIdInNewAccountCannotReuseTextAttachmentReadOrHiddenState() {
        AtomicReference<String> current = new AtomicReference<>("account-a");
        PmAccountState state = new PmAccountState(current::get);
        PmMessageDialog.Draft previous = state.draft(42);
        previous.text = "private account-a draft";
        previous.attachment = "account-a.png";
        assertTrue(state.beginRead("account-a", 42));
        assertTrue(state.finishRead("account-a", 42, true));
        state.hide(42);

        current.set("account-b");
        assertTrue(state.synchronize());
        PmMessageDialog.Draft next = state.draft(42);
        assertNotSame(previous, next);
        assertEquals("", next.text);
        assertNull(next.attachment);
        assertFalse(state.isRead(42));
        assertFalse(state.isHidden(42));
        assertTrue(state.beginRead("account-b", 42));
    }

    /** Old row actions and callbacks are rejected immediately, even before a refresh synchronizes the adapter. */
    @Test
    public void oldAccountCallbacksCannotMarkNewSameIdReadOrRemoveItsDraft() {
        AtomicReference<String> current = new AtomicReference<>("account-a");
        PmAccountState state = new PmAccountState(current::get);
        assertTrue(state.beginRead("account-a", 42));
        current.set("account-b");
        assertFalse(state.isCurrent("account-a"));
        assertFalse(state.finishRead("account-a", 42, true));
        state.synchronize();
        PmMessageDialog.Draft next = state.draft(42);
        next.text = "account-b draft";
        state.removeDraft("account-a", 42);
        assertEquals("account-b draft", state.draft(42).text);
        assertFalse(state.isRead(42));
    }

    /** Guest member previews remain visible, while guest send/read actions never qualify as authenticated. */
    @Test
    public void publicLoggedOutRowsAreVisibleWithoutEnablingPrivateActions() {
        AtomicReference<String> current = new AtomicReference<>(null);
        PmAccountState state = new PmAccountState(current::get);
        assertTrue(state.isVisibleAccount());
        assertFalse(state.isCurrent(null));
        assertFalse(state.beginRead(null, 42));
        current.set("new-account");
        assertFalse(state.isVisibleAccount());
        assertTrue(state.synchronize());
        assertTrue(state.isVisibleAccount());
        assertTrue(state.isCurrent("new-account"));
    }

    /** Logout clears private transient state and forbids in-flight/read mutation until authentication returns. */
    @Test
    public void logoutClearsStateAndInvalidatesActions() {
        AtomicReference<String> current = new AtomicReference<>("account-a");
        PmAccountState state = new PmAccountState(current::get);
        state.draft(42).text = "private draft";
        current.set(null);
        assertTrue(state.synchronize());
        assertFalse(state.isCurrent("account-a"));
        assertFalse(state.beginRead(null, 42));
        current.set("account-a");
        assertTrue(state.synchronize());
        assertEquals("", state.draft(42).text);
    }
}
