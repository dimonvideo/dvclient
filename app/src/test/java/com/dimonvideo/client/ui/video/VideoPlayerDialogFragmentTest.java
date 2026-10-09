package com.dimonvideo.client.ui.video;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.app.Application;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;

import androidx.activity.ComponentDialog;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.media3.common.C;
import androidx.media3.common.DeviceInfo;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.Player;
import androidx.media3.common.Timeline;
import androidx.media3.common.Tracks;
import androidx.media3.common.VideoSize;
import androidx.media3.common.text.CueGroup;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.ui.AspectRatioFrameLayout;
import androidx.media3.ui.PlayerView;

import com.dimonvideo.client.R;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowLooper;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

/** Exercises the actual Media3 dialog and host lifecycle with deterministic players instead of network/codecs. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, application = Application.class)
@LooperMode(LooperMode.Mode.PAUSED)
@UnstableApi
public class VideoPlayerDialogFragmentTest {
    private static final String SOURCE = "https://dimonvideo.ru/files/example.mp4";
    private ActivityController<FragmentActivity> controller;
    private TestDialog fragment;

    /** Creates a real themed fragment host without starting application services or requesting orientation. */
    @Before
    public void setUp() {
        TestDialog.players.clear();
        controller = Robolectric.buildActivity(FragmentActivity.class);
        controller.get().setTheme(R.style.AppTheme);
        controller.setup();
    }

    /** Releases any visible test dialog and host resources before the next lifecycle scenario. */
    @After
    public void tearDown() {
        if (fragment != null && fragment.isAdded()) fragment.dismissNow();
        controller.pause().stop().destroy();
        ShadowLooper.shadowMainLooper().idle();
        TestDialog.players.clear();
    }

    /** Default playback keeps the whole frame, uses normal seek/pause controls and leaves rotation unrestricted. */
    @Test
    public void dialogOffersControlsAndFitsWholeFrameWithoutLockingOrientation() {
        open(false);
        assertEquals(AspectRatioFrameLayout.RESIZE_MODE_FIT, playerView().getResizeMode());
        assertTrue(playerView().getUseController());
        assertNotNull(dialog().findViewById(androidx.media3.ui.R.id.exo_play_pause));
        assertNotNull(dialog().findViewById(androidx.media3.ui.R.id.exo_progress));
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED, controller.get().getRequestedOrientation());
        assertEquals(SOURCE, latest().mediaItem.localConfiguration.uri.toString());
        assertTrue(latest().playing);
        assertEquals(1, latest().prepares);
        assertTrue(dialog().findViewById(R.id.video_player_close).isEnabled());
    }

    /** The existing aspect preference fills the display proportionally, rather than stretching arbitrary video. */
    @Test
    public void aspectPreferenceCropsProportionallyRatherThanForcingOrientation() {
        open(true);
        assertEquals(AspectRatioFrameLayout.RESIZE_MODE_ZOOM, playerView().getResizeMode());
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED, controller.get().getRequestedOrientation());
    }

    /** Backgrounding releases media and foregrounding resumes the same position while preserving a deliberate pause. */
    @Test
    public void backgroundReleasesAndForegroundRestoresPositionAndPauseChoice() {
        open(false);
        FakePlayer original = latest();
        original.position = 43_000;
        original.playing = false;
        PlayerView oldView = playerView();
        controller.pause().stop();
        assertEquals(1, original.releases);
        assertFalse(original.playing);
        assertNull(oldView.getPlayer());
        assertFalse(oldView.getKeepScreenOn());
        controller.start().resume().visible();
        ShadowLooper.shadowMainLooper().idle();
        FakePlayer resumed = latest();
        assertEquals(2, TestDialog.players.size());
        assertEquals(43_000, resumed.position);
        assertFalse(resumed.playing);
        assertEquals(0, resumed.releases);
        assertSame(resumed.player, playerView().getPlayer());
    }

    /** A restored fragment recreates playback from its saved source, progress and user pause state. */
    @Test
    public void activityRecreationRestoresVideoAndProgressWithoutKeepingOldPlayer() {
        open(true);
        FakePlayer original = latest();
        original.position = 81_000;
        original.playing = false;
        Bundle saved = new Bundle();
        controller.saveInstanceState(saved).pause().stop().destroy();
        assertEquals(1, original.releases);
        controller = Robolectric.buildActivity(FragmentActivity.class);
        controller.get().setTheme(R.style.AppTheme);
        controller.create(saved).start().resume().visible();
        fragment = (TestDialog) controller.get().getSupportFragmentManager().findFragmentByTag("test-video");
        ShadowLooper.shadowMainLooper().idle();
        assertNotNull(fragment);
        assertEquals(SOURCE, latest().mediaItem.localConfiguration.uri.toString());
        assertEquals(81_000, latest().position);
        assertFalse(latest().playing);
        assertEquals(AspectRatioFrameLayout.RESIZE_MODE_ZOOM, playerView().getResizeMode());
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED, controller.get().getRequestedOrientation());
    }

    /** Closing the dedicated button stops media immediately and overlapping stop/destroy callbacks release only once. */
    @Test
    public void closeButtonDetachesPlayerAndReleasesExactlyOnce() {
        open(false);
        FakePlayer original = latest();
        PlayerView oldView = playerView();
        dialog().findViewById(R.id.video_player_close).performClick();
        ShadowLooper.shadowMainLooper().idle();
        controller.get().getSupportFragmentManager().executePendingTransactions();
        assertEquals(1, original.releases);
        assertNull(oldView.getPlayer());
        assertFalse(oldView.getKeepScreenOn());
        assertFalse(fragment.isAdded());
    }

    /** Cancellation follows the same resource cleanup as the system back button and never leaves audio behind. */
    @Test
    public void systemCancellationStopsAndReleasesMedia() {
        open(false);
        FakePlayer original = latest();
        dialog().cancel();
        ShadowLooper.shadowMainLooper().idle();
        controller.get().getSupportFragmentManager().executePendingTransactions();
        assertEquals(1, original.releases);
        assertFalse(original.playing);
        assertFalse(fragment.isAdded());
    }

    /** A generic failure keeps a working retry action without leaking server response text or losing progress. */
    @Test
    public void playbackErrorOffersRetryAndPreservesPosition() {
        open(false);
        FakePlayer player = latest();
        player.position = 17_000;
        player.fail();
        assertEquals(View.VISIBLE, dialog().findViewById(R.id.video_player_error_panel).getVisibility());
        dialog().findViewById(R.id.video_player_retry).performClick();
        assertEquals(View.GONE, dialog().findViewById(R.id.video_player_error_panel).getVisibility());
        assertEquals(2, player.prepares);
        assertEquals(17_000, player.position);
        assertTrue(player.playing);
    }

    /** Bar and cutout insets protect controls on every side without accumulating padding on redispatch. */
    @Test
    public void windowInsetsProtectSystemBarsAndLandscapeCutout() {
        open(false);
        View root = dialog().findViewById(R.id.video_player_root);
        WindowInsetsCompat insets = new WindowInsetsCompat.Builder()
                .setInsets(WindowInsetsCompat.Type.systemBars(), Insets.of(8, 24, 9, 30))
                .setInsets(WindowInsetsCompat.Type.displayCutout(), Insets.of(32, 4, 0, 0)).build();
        ViewCompat.dispatchApplyWindowInsets(root, insets);
        ViewCompat.dispatchApplyWindowInsets(root, insets);
        assertEquals(32, root.getPaddingLeft());
        assertEquals(24, root.getPaddingTop());
        assertEquals(9, root.getPaddingRight());
        assertEquals(30, root.getPaddingBottom());
    }

    /** The visible fullscreen control rotates only on request and keeps paused playback and its exact position. */
    @Test
    public void fullscreenControlReusesPlayerAndRestoresExactHostOrientation() {
        controller.get().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
        open(false);
        FakePlayer original = latest();
        original.position = 29_000;
        original.playing = false;
        View control = fullscreenControl();
        assertEquals(controller.get().getString(androidx.media3.ui.R.string.exo_controls_fullscreen_enter_description),
                control.getContentDescription().toString());
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT, controller.get().getRequestedOrientation());

        assertTrue(control.performClick());
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE, controller.get().getRequestedOrientation());
        assertEquals(View.GONE, dialog().findViewById(R.id.video_player_close).getVisibility());
        assertEquals(controller.get().getString(androidx.media3.ui.R.string.exo_controls_fullscreen_exit_description),
                control.getContentDescription().toString());
        assertSame(original.player, playerView().getPlayer());
        assertEquals(29_000, original.position);
        assertFalse(original.playing);
        assertEquals(0, original.releases);

        assertTrue(control.performClick());
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT, controller.get().getRequestedOrientation());
        assertEquals(View.VISIBLE, dialog().findViewById(R.id.video_player_close).getVisibility());
        assertEquals(controller.get().getString(androidx.media3.ui.R.string.exo_controls_fullscreen_enter_description),
                control.getContentDescription().toString());
        assertSame(original.player, playerView().getPlayer());
        assertEquals(1, TestDialog.players.size());
        assertEquals(1, original.prepares);
        assertEquals(29_000, original.position);
        assertFalse(original.playing);
    }

    /** Media3's compact controls share the fullscreen handler and synchronize both accessible toggle icons. */
    @Test
    public void compactFullscreenControlUsesSameImmersiveAction() {
        open(false);
        View standard = fullscreenControl();
        View compact = dialog().findViewById(androidx.media3.ui.R.id.exo_minimal_fullscreen);
        assertNotNull(compact);
        assertEquals(View.VISIBLE, compact.getVisibility());
        assertTrue(compact.performClick());
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE, controller.get().getRequestedOrientation());
        assertEquals(standard.getContentDescription(), compact.getContentDescription());
        assertEquals(View.GONE, dialog().findViewById(R.id.video_player_close).getVisibility());

        assertTrue(compact.performClick());
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED, controller.get().getRequestedOrientation());
        assertEquals(standard.getContentDescription(), compact.getContentDescription());
        assertEquals(View.VISIBLE, dialog().findViewById(R.id.video_player_close).getVisibility());
        assertEquals(0, latest().releases);
    }

    /** Back first restores the original window and rotation, then a second Back releases and closes playback. */
    @Test
    public void firstBackLeavesFullscreenAndSecondBackClosesVideo() {
        controller.get().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
        open(false);
        FakePlayer original = latest();
        fullscreenControl().performClick();
        ComponentDialog window = (ComponentDialog) dialog();
        window.getOnBackPressedDispatcher().onBackPressed();
        ShadowLooper.shadowMainLooper().idle();
        assertTrue(fragment.isAdded());
        assertTrue(window.isShowing());
        assertEquals(View.VISIBLE, window.findViewById(R.id.video_player_close).getVisibility());
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT, controller.get().getRequestedOrientation());
        assertSame(original.player, playerView().getPlayer());
        assertEquals(0, original.releases);

        window.getOnBackPressedDispatcher().onBackPressed();
        ShadowLooper.shadowMainLooper().idle();
        controller.get().getSupportFragmentManager().executePendingTransactions();
        assertFalse(fragment.isAdded());
        assertEquals(1, original.releases);
        assertFalse(original.playing);
    }

    /** Direct cancellation closes immersive playback immediately and restores a non-default host rotation. */
    @Test
    public void cancellationFromFullscreenRestoresOrientationAndReleasesOnce() {
        controller.get().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
        open(false);
        FakePlayer original = latest();
        PlayerView surface = playerView();
        fullscreenControl().performClick();
        dialog().cancel();
        ShadowLooper.shadowMainLooper().idle();
        controller.get().getSupportFragmentManager().executePendingTransactions();
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR, controller.get().getRequestedOrientation());
        assertFalse(fragment.isAdded());
        assertEquals(1, original.releases);
        assertNull(surface.getPlayer());
        assertFalse(surface.getKeepScreenOn());
    }

    /** Programmatic dismissal also relinquishes landscape ownership rather than leaving the activity locked. */
    @Test
    public void dismissalFromFullscreenRestoresOrientationAndReleasesOnce() {
        controller.get().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);
        open(false);
        FakePlayer original = latest();
        fullscreenControl().performClick();
        fragment.dismissNow();
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_USER, controller.get().getRequestedOrientation());
        assertFalse(fragment.isAdded());
        assertEquals(1, original.releases);
        assertFalse(original.playing);
    }

    /** Rotation recreation restores immersive presentation, paused progress and the orientation to return to. */
    @Test
    public void activityRecreationKeepsFullscreenProgressAndOriginalOrientation() {
        controller.get().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
        open(true);
        FakePlayer original = latest();
        original.position = 63_000;
        original.playing = false;
        fullscreenControl().performClick();
        controller.recreate();
        fragment = (TestDialog) controller.get().getSupportFragmentManager().findFragmentByTag("test-video");
        ShadowLooper.shadowMainLooper().idle();
        assertNotNull(fragment);
        assertEquals(1, original.releases);
        assertEquals(2, TestDialog.players.size());
        assertEquals(63_000, latest().position);
        assertFalse(latest().playing);
        assertEquals(AspectRatioFrameLayout.RESIZE_MODE_ZOOM, playerView().getResizeMode());
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE, controller.get().getRequestedOrientation());
        assertEquals(View.GONE, dialog().findViewById(R.id.video_player_close).getVisibility());
        View control = fullscreenControl();
        assertEquals(controller.get().getString(androidx.media3.ui.R.string.exo_controls_fullscreen_exit_description),
                control.getContentDescription().toString());
        control.performClick();
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT, controller.get().getRequestedOrientation());
        assertEquals(63_000, latest().position);
        assertFalse(latest().playing);
        assertEquals(0, latest().releases);
    }

    /** Returning from the background recreates paused media while retaining a working fullscreen Back callback. */
    @Test
    public void backgroundedFullscreenRestoresPausedPlaybackAndFirstBackBehavior() {
        controller.get().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT);
        open(false);
        FakePlayer original = latest();
        original.position = 37_000;
        original.playing = false;
        fullscreenControl().performClick();
        controller.pause().stop();
        assertEquals(1, original.releases);
        controller.start().resume().visible();
        ShadowLooper.shadowMainLooper().idle();
        assertEquals(2, TestDialog.players.size());
        assertEquals(37_000, latest().position);
        assertFalse(latest().playing);
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE, controller.get().getRequestedOrientation());
        assertEquals(View.GONE, dialog().findViewById(R.id.video_player_close).getVisibility());

        ((ComponentDialog) dialog()).getOnBackPressedDispatcher().onBackPressed();
        ShadowLooper.shadowMainLooper().idle();
        assertTrue(fragment.isAdded());
        assertTrue(dialog().isShowing());
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT, controller.get().getRequestedOrientation());
        assertEquals(View.VISIBLE, dialog().findViewById(R.id.video_player_close).getVisibility());
        assertEquals(0, latest().releases);
        assertEquals(37_000, latest().position);
        assertFalse(latest().playing);
    }

    /** Immersive playback protects a landscape camera cutout without reserving space for transient system bars. */
    @Test
    public void fullscreenInsetsProtectCutoutWithoutLeavingBarsGap() {
        open(false);
        View root = dialog().findViewById(R.id.video_player_root);
        WindowInsetsCompat insets = new WindowInsetsCompat.Builder()
                .setInsets(WindowInsetsCompat.Type.systemBars(), Insets.of(8, 24, 9, 30))
                .setVisible(WindowInsetsCompat.Type.systemBars(), true)
                .setInsets(WindowInsetsCompat.Type.displayCutout(), Insets.of(32, 4, 0, 0)).build();
        ViewCompat.dispatchApplyWindowInsets(root, insets);
        assertEquals(24, root.getPaddingTop());
        fullscreenControl().performClick();
        ViewCompat.dispatchApplyWindowInsets(root, insets);
        ViewCompat.dispatchApplyWindowInsets(root, insets);
        assertEquals(32, root.getPaddingLeft());
        assertEquals(4, root.getPaddingTop());
        assertEquals(0, root.getPaddingRight());
        assertEquals(0, root.getPaddingBottom());

        fullscreenControl().performClick();
        ViewCompat.dispatchApplyWindowInsets(root, insets);
        assertEquals(32, root.getPaddingLeft());
        assertEquals(24, root.getPaddingTop());
        assertEquals(9, root.getPaddingRight());
        assertEquals(30, root.getPaddingBottom());
    }

    /** Saved fragment state and non-activity contexts are rejected instead of leaking an unmanaged video window. */
    @Test
    public void openingAfterStateSaveDoesNotCreateAnUnmanagedPlayer() {
        Bundle state = new Bundle();
        controller.saveInstanceState(state);
        assertTrue(controller.get().getSupportFragmentManager().isStateSaved());
        assertFalse(VideoPlayerDialogFragment.open(controller.get(), SOURCE, false));
        assertFalse(VideoPlayerDialogFragment.open(controller.get().getApplicationContext(), SOURCE, false));
        assertTrue(TestDialog.players.isEmpty());
    }

    /** Shows the real dialog with an injectable player and production argument serialization. */
    private void open(boolean crop) {
        fragment = new TestDialog();
        fragment.setArguments(VideoPlayerDialogFragment.newInstance(SOURCE, crop).getArguments());
        fragment.showNow(controller.get().getSupportFragmentManager(), "test-video");
        ShadowLooper.shadowMainLooper().idle();
    }

    /** Returns the actual displayed window instead of testing a mirrored controller implementation. */
    private Dialog dialog() { return fragment.requireDialog(); }

    /** Returns the real Media3 surface/controller view in the production layout. */
    private PlayerView playerView() { return dialog().findViewById(R.id.video_player); }

    /** Returns the real, visible Media3 action after ensuring the controls have not auto-hidden. */
    private View fullscreenControl() {
        playerView().showController();
        View control = dialog().findViewById(androidx.media3.ui.R.id.exo_fullscreen);
        assertNotNull(control);
        assertEquals(View.VISIBLE, control.getVisibility());
        assertTrue(control.isEnabled());
        return control;
    }

    /** Returns the newest decoder-free player created by the fragment lifecycle. */
    private FakePlayer latest() { return TestDialog.players.get(TestDialog.players.size() - 1); }

    /** Public restorable subclass supplies media deterministically while retaining all production dialog behavior. */
    public static class TestDialog extends VideoPlayerDialogFragment {
        static final List<FakePlayer> players = new ArrayList<>();

        /** Lets FragmentManager reconstruct the same test fragment during activity recreation. */
        public TestDialog() { }

        /** Replaces codec/network resources only; the real PlayerView still receives the normal Player interface. */
        @Override
        protected Player createPlayer(Context context) {
            FakePlayer fake = new FakePlayer();
            players.add(fake);
            return fake.player;
        }
    }

    /** Implements the Player boundary without a codec thread, so release/pause/seek assertions are deterministic. */
    private static final class FakePlayer implements InvocationHandler {
        final List<Player.Listener> listeners = new ArrayList<>();
        final Player.Commands commands = new Player.Commands.Builder().addAllCommands().build();
        final Player player = (Player) Proxy.newProxyInstance(Player.class.getClassLoader(),
                new Class<?>[]{Player.class}, this);
        MediaItem mediaItem;
        long position;
        boolean playing;
        int prepares;
        int releases;

        /** Emulates controller queries and media lifecycle calls used by the real Media3 PlayerView. */
        @Override
        public Object invoke(Object proxy, Method method, Object[] arguments) {
            switch (method.getName()) {
                case "getApplicationLooper": return Looper.getMainLooper();
                case "getAvailableCommands": return commands;
                case "isCommandAvailable": return true;
                case "getCurrentTimeline": return Timeline.EMPTY;
                case "getCurrentTracks": return Tracks.EMPTY;
                case "getVideoSize": return VideoSize.UNKNOWN;
                case "getMediaMetadata":
                case "getPlaylistMetadata": return MediaMetadata.EMPTY;
                case "getPlaybackParameters": return PlaybackParameters.DEFAULT;
                case "getCurrentCues": return CueGroup.EMPTY_TIME_ZERO;
                case "getDeviceInfo": return DeviceInfo.UNKNOWN;
                case "getPlaybackState": return Player.STATE_READY;
                case "getCurrentPosition":
                case "getContentPosition": return position;
                case "getDuration":
                case "getContentDuration": return C.TIME_UNSET;
                case "getPlayWhenReady":
                case "isPlaying": return playing;
                case "getCurrentMediaItem": return mediaItem;
                case "setMediaItem": mediaItem = (MediaItem) arguments[0]; return null;
                case "seekTo": position = (Long) arguments[arguments.length - 1]; return null;
                case "setPlayWhenReady": playing = (Boolean) arguments[0]; return null;
                case "play": playing = true; return null;
                case "pause": playing = false; return null;
                case "prepare": prepares++; return null;
                case "release": releases++; listeners.clear(); return null;
                case "addListener": listeners.add((Player.Listener) arguments[0]); return null;
                case "removeListener": listeners.remove(arguments[0]); return null;
                case "equals": return proxy == arguments[0];
                case "hashCode": return System.identityHashCode(proxy);
                case "toString": return "FakePlayer";
                default:
                    Class<?> type = method.getReturnType();
                    if (type == boolean.class) return false;
                    if (type == int.class) return 0;
                    if (type == long.class) return 0L;
                    if (type == float.class) return 0F;
                    return null;
            }
        }

        /** Sends an error through the actual fragment and Media3 listeners without contacting a server. */
        void fail() {
            PlaybackException error = new PlaybackException("private server text", null,
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED);
            for (Player.Listener listener : new ArrayList<>(listeners)) listener.onPlayerError(error);
        }
    }
}
