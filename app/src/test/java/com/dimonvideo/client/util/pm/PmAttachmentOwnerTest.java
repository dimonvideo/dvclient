package com.dimonvideo.client.util.pm;

import android.app.Application;
import android.os.Bundle;
import android.os.Parcel;

import androidx.lifecycle.SavedStateHandle;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Covers independently completing uploads without a live dialog, while keeping request storage bounded. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, application = Application.class)
public class PmAttachmentOwnerTest {
    /** Picking for another message must not replace an in-flight or already completed original attachment. */
    @Test
    public void overlappingUploadsCompleteIndependentlyAndConsumeOnlyTheirOwnResults() {
        PmAttachmentOwner owner = new PmAttachmentOwner(new SavedStateHandle());
        assertTrue(owner.beginPicker("first", "account"));
        owner.beginUpload("first", "account");
        assertTrue(owner.beginPicker("second", "account"));
        owner.beginUpload("second", "account");
        owner.complete("second", "account", "second-file.png", PmAttachmentEvent.Outcome.READY, "account");
        owner.complete("first", "account", "first-file.png", PmAttachmentEvent.Outcome.READY, "account");
        assertEquals("first-file.png", owner.result("first", "account").filename);
        assertEquals("second-file.png", owner.result("second", "account").filename);
        owner.consume("second", "account");
        assertNull(owner.result("second", "account"));
        assertEquals("first-file.png", owner.result("first", "account").filename);
        assertTrue(owner.beginPicker("third", "account"));
        assertEquals("first-file.png", owner.result("first", "account").filename);
    }

    /** Old completed uploads no longer permanently block the picker after their composers are abandoned. */
    @Test
    public void capacityReclaimsOldestCompletedResultAndPreservesRecentFiles() {
        PmAttachmentOwner owner = new PmAttachmentOwner(new SavedStateHandle());
        for (int index = 0; index < 32; index++) {
            String request = "request-" + index;
            assertTrue(owner.beginPicker(request, "account"));
            owner.beginUpload(request, "account");
            owner.complete(request, "account", index + ".png", PmAttachmentEvent.Outcome.READY, "account");
        }
        assertTrue(owner.beginPicker("extra", "account"));
        assertNull(owner.result("request-0", "account"));
        assertEquals("31.png", owner.result("request-31", "account").filename);
        owner.beginUpload("extra", "account");
        assertTrue(owner.beginPicker("another", "account"));
        assertNull(owner.result("request-1", "account"));
        assertTrue(owner.isPending("extra", "account"));
        owner.complete("request-0", "account", "late-evicted.png", PmAttachmentEvent.Outcome.READY, "account");
        assertNull(owner.result("request-0", "account"));
        assertEquals("31.png", owner.result("request-31", "account").filename);
    }

    /** The capacity bound still protects in-flight uploads instead of losing their eventual results. */
    @Test
    public void activeUploadsStayProtectedUntilOneCompletes() {
        PmAttachmentOwner owner = new PmAttachmentOwner(new SavedStateHandle());
        for (int index = 0; index < 32; index++) {
            String request = "active-" + index;
            assertTrue(owner.beginPicker(request, "account"));
            owner.beginUpload(request, "account");
        }
        assertFalse(owner.beginPicker("extra", "account"));
        for (int index = 0; index < 32; index++) assertTrue(owner.isPending("active-" + index, "account"));
        owner.complete("active-31", "account", null, PmAttachmentEvent.Outcome.FAILED, "account");
        assertTrue(owner.beginPicker("extra", "account"));
        assertTrue(owner.isPending("active-0", "account"));
        assertTrue(owner.isPending("active-30", "account"));
        assertNull(owner.result("active-31", "account"));
        owner.complete("active-0", "account", "still-routed.png", PmAttachmentEvent.Outcome.READY, "account");
        assertEquals("still-routed.png", owner.result("active-0", "account").filename);
    }

    /** Repeated abandoned completions remain bounded without exhausting image selection for the activity. */
    @Test
    public void repeatedAbandonedUploadsKeepAllowingNewPickers() {
        PmAttachmentOwner owner = new PmAttachmentOwner(new SavedStateHandle());
        for (int index = 0; index < 96; index++) {
            String request = "abandoned-" + index;
            assertTrue(owner.beginPicker(request, "account"));
            owner.beginUpload(request, "account");
            owner.complete(request, "account", index + ".png", PmAttachmentEvent.Outcome.READY, "account");
            assertTrue(owner.changes().getValue().size() <= 32);
        }
        assertNull(owner.result("abandoned-0", "account"));
        assertEquals("95.png", owner.result("abandoned-95", "account").filename);
        assertTrue(owner.beginPicker("next", "account"));
    }

    /** Serialized request ordinals preserve FIFO reclamation and advance correctly after process restoration. */
    @Test
    public void restoredOwnerReclaimsByCreationOrderRatherThanBundleKeyOrder() {
        PmAttachmentOwner owner = new PmAttachmentOwner(new SavedStateHandle());
        for (int index = 0; index < 32; index++) {
            String request = index == 0 ? "z-oldest" : "a-" + index;
            assertTrue(owner.beginPicker(request, "account"));
            owner.beginUpload(request, "account");
            owner.complete(request, "account", index + ".png", PmAttachmentEvent.Outcome.READY, "account");
        }
        Parcel parcel = Parcel.obtain();
        Bundle restoredRecords;
        try {
            parcel.writeBundle(owner.changes().getValue());
            parcel.setDataPosition(0);
            restoredRecords = parcel.readBundle(getClass().getClassLoader());
        } finally {
            parcel.recycle();
        }
        PmAttachmentOwner restored = new PmAttachmentOwner(
                new SavedStateHandle(Collections.singletonMap("pm_attachment", restoredRecords)));
        assertTrue(restored.beginPicker("extra", "account"));
        assertNull(restored.result("z-oldest", "account"));
        assertEquals("1.png", restored.result("a-1", "account").filename);
        restored.beginUpload("extra", "account");
        restored.complete("extra", "account", "new.png", PmAttachmentEvent.Outcome.READY, "account");
        assertTrue(restored.beginPicker("another", "account"));
        assertNull(restored.result("a-1", "account"));
        assertEquals("new.png", restored.result("extra", "account").filename);
    }

    /** Starting a new-account picker invalidates old private files and prevents late callbacks changing its routing. */
    @Test
    public void accountReplacementRejectsOldUploadWithoutCorruptingNewPicker() {
        PmAttachmentOwner owner = new PmAttachmentOwner(new SavedStateHandle());
        assertTrue(owner.beginPicker("request", "old-account"));
        owner.beginUpload("request", "old-account");
        assertTrue(owner.beginPicker("request", "new-account"));
        owner.complete("request", "old-account", "private-old.png", PmAttachmentEvent.Outcome.READY, "new-account");
        assertNull(owner.result("request", "old-account"));
        assertTrue(owner.isPending("request", "new-account"));
        assertEquals("new-account", owner.pendingPicker().getString("account"));
    }
}
