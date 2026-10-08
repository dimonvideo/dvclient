package com.dimonvideo.client.util.pm;

import android.app.Application;

import androidx.lifecycle.SavedStateHandle;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

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

    /** Reaching the retained-draft bound rejects a new picker instead of silently evicting an unapplied file. */
    @Test
    public void capacityRejectsNewRequestUntilAResultIsAcknowledged() {
        PmAttachmentOwner owner = new PmAttachmentOwner(new SavedStateHandle());
        for (int index = 0; index < 32; index++) {
            String request = "request-" + index;
            assertTrue(owner.beginPicker(request, "account"));
            owner.beginUpload(request, "account");
            owner.complete(request, "account", index + ".png", PmAttachmentEvent.Outcome.READY, "account");
        }
        assertFalse(owner.beginPicker("extra", "account"));
        assertEquals("0.png", owner.result("request-0", "account").filename);
        assertEquals("31.png", owner.result("request-31", "account").filename);
        owner.consume("request-0", "account");
        assertTrue(owner.beginPicker("extra", "account"));
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
