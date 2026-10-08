package com.dimonvideo.client.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.Manifest;
import android.app.Application;
import android.app.DownloadManager;
import android.app.NotificationManager;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowDownloadManager;
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.shadows.ShadowNotificationManager;
import org.robolectric.util.ReflectionHelpers;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/** Verifies cancellation of real progress monitors through DownloadManager and notification shadows. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, manifest = Config.NONE, application = Application.class)
@LooperMode(LooperMode.Mode.PAUSED)
public class DownloadBroadcastReceiverTest {
    private Context context;
    private DownloadManager downloads;
    private ShadowNotificationManager notifications;
    private ShadowLooper background;

    /** Pauses the monitor's own looper so tests can advance polling without wall-clock sleeps. */
    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        downloads = context.getSystemService(DownloadManager.class);
        // Robolectric starts IDs at zero, whereas the public monitor requires real positive IDs.
        long zeroId = downloads.enqueue(new DownloadManager.Request(
                Uri.parse("https://example.org/reserved-zero.bin")));
        downloads.remove(zeroId);
        notifications = Shadows.shadowOf(context.getSystemService(NotificationManager.class));
        notifications.setNotificationsEnabled(true);
        Handler handler = ReflectionHelpers.getStaticField(DownloadBroadcastReceiver.class,
                "BACKGROUND_HANDLER");
        background = Shadows.shadowOf(handler.getLooper());
        background.pause();
    }

    /** Removes all monitors using their public cancellation API before Robolectric resets the looper. */
    @After
    public void tearDown() {
        for (Long id : new ArrayList<>(monitors().keySet())) {
            DownloadBroadcastReceiver.stopProgressMonitoring(id);
        }
        background.idle();
    }

    /** Removing a system download must remove its progress notification and stop future polling. */
    @Test
    public void deletedDownloadCancelsNotificationAndReleasesMonitor() {
        long id = startRunningDownload();
        assertNotNull(progressNotification(id));

        downloads.remove(id);
        background.idleFor(1, TimeUnit.SECONDS);

        assertStopped(id);
        background.idleFor(5, TimeUnit.SECONDS);
        assertStopped(id);
    }

    /** Successful and failed rows are terminal even if no completion broadcast reaches the receiver. */
    @Test
    public void terminalDownloadRowsCancelTheirProgressMonitors() {
        long successful = startRunningDownload();
        long failed = startRunningDownload();
        request(successful).setStatus(DownloadManager.STATUS_SUCCESSFUL);
        request(failed).setStatus(DownloadManager.STATUS_FAILED);

        background.idleFor(1, TimeUnit.SECONDS);

        assertStopped(successful);
        assertStopped(failed);
        background.idleFor(5, TimeUnit.SECONDS);
        assertStopped(successful);
        assertStopped(failed);
    }

    /** Cancelling one monitor must retain the other download and never cancel either system request. */
    @Test
    public void explicitStopCancelsOnlyItsNotificationAndDoesNotRemoveSystemDownload() {
        long first = startRunningDownload();
        long second = startRunningDownload();
        assertNotNull(progressNotification(first));
        assertNotNull(progressNotification(second));

        DownloadBroadcastReceiver.stopProgressMonitoring(first);
        background.idle();

        assertStopped(first);
        assertTrue(monitors().containsKey(second));
        assertNotNull(progressNotification(second));
        assertNotNull(Shadows.shadowOf(downloads).getRequest(first));
        assertNotNull(Shadows.shadowOf(downloads).getRequest(second));
        background.idleFor(5, TimeUnit.SECONDS);
        assertStopped(first);
        assertTrue(monitors().containsKey(second));
        assertNotNull(progressNotification(second));
    }

    /** Disabling notifications while a download is active must clear the previously posted progress. */
    @Test
    public void disabledNotificationsStopMonitoringWithoutKeepingAnInvisibleTask() {
        long id = startRunningDownload();
        assertNotNull(progressNotification(id));
        notifications.setNotificationsEnabled(false);

        background.idleFor(1, TimeUnit.SECONDS);

        assertStopped(id);
        notifications.setNotificationsEnabled(true);
        background.idleFor(5, TimeUnit.SECONDS);
        assertStopped(id);
    }

    /** Android 13 permission denial must release the task rather than polling without visible progress. */
    @Test
    @Config(sdk = 33)
    public void deniedRuntimeNotificationPermissionDoesNotRetainMonitor() {
        Shadows.shadowOf(RuntimeEnvironment.getApplication())
                .denyPermissions(Manifest.permission.POST_NOTIFICATIONS);
        long id = startRunningDownload();

        assertStopped(id);
        Shadows.shadowOf(RuntimeEnvironment.getApplication())
                .grantPermissions(Manifest.permission.POST_NOTIFICATIONS);
        background.idleFor(5, TimeUnit.SECONDS);
        assertStopped(id);
        assertNotNull(Shadows.shadowOf(downloads).getRequest(id));
    }

    /** Enqueues a shadow system request and runs the public monitor until its first progress update. */
    private long startRunningDownload() {
        long id = downloads.enqueue(new DownloadManager.Request(
                Uri.parse("https://example.org/download-monitor-test.bin")));
        ShadowDownloadManager.ShadowRequest request = request(id);
        request.setStatus(DownloadManager.STATUS_RUNNING);
        request.setBytesSoFar(25);
        request.setTotalSize(100);
        DownloadBroadcastReceiver.startProgressMonitoring(context, id);
        background.idle();
        return id;
    }

    /** Returns the supported DownloadManager shadow used to change the system download's state. */
    private ShadowDownloadManager.ShadowRequest request(long id) {
        return Shadows.shadowOf(Shadows.shadowOf(downloads).getRequest(id));
    }

    /** Reads the notification specific to one download, proving parallel downloads have distinct tags. */
    private android.app.Notification progressNotification(long id) {
        return notifications.getNotification("download:" + id, 1001);
    }

    /** Checks both visible cancellation and release of the receiver's otherwise inaccessible registry. */
    private void assertStopped(long id) {
        assertNull(progressNotification(id));
        assertFalse(monitors().containsKey(id));
    }

    /** Exposes registry membership only; tests exercise all mutations through the public monitor API. */
    private Map<Long, ?> monitors() {
        return ReflectionHelpers.getStaticField(DownloadBroadcastReceiver.class, "MONITORS");
    }
}
