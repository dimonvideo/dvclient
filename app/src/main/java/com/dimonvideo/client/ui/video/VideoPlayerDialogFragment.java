package com.dimonvideo.client.ui.video;

import android.app.Dialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.activity.ComponentDialog;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.AspectRatioFrameLayout;
import androidx.media3.ui.PlayerView;

import com.dimonvideo.client.R;

/** Restorable video window with user-controlled immersive playback and temporary landscape orientation. */
@UnstableApi
public class VideoPlayerDialogFragment extends DialogFragment {
    private static final String TAG = "video-player";
    private static final String URL = "url";
    private static final String CROP = "crop";
    private static final String POSITION = "position";
    private static final String PLAY = "play";
    private static final String FULLSCREEN = "fullscreen";
    private static final String ORIGINAL_ORIENTATION = "original-orientation";
    private static final String ORIENTATION_CAPTURED = "orientation-captured";
    private Player player;
    private PlayerView playerView;
    private View errorPanel;
    private long resumePosition;
    private boolean resumePlaying = true;
    private boolean fullscreen;
    private boolean orientationCaptured;
    private int originalOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
    private OnBackPressedCallback fullscreenBack;

    /** Allows FragmentManager to restore a video window after activity or process recreation. */
    public VideoPlayerDialogFragment() { }

    /** Opens a single restorable player when the host can safely accept a fragment transaction. */
    public static boolean open(Context context, String url, boolean cropVideo) {
        FragmentActivity activity = activity(context);
        if (activity == null || activity.isFinishing() || activity.isDestroyed() || url == null || url.isEmpty()) {
            return false;
        }
        FragmentManager fragments = activity.getSupportFragmentManager();
        if (fragments.isStateSaved()) return false;
        DialogFragment previous = (DialogFragment) fragments.findFragmentByTag(TAG);
        if (previous != null) previous.dismissNow();
        newInstance(url, cropVideo).showNow(fragments, TAG);
        return true;
    }

    /** Stores only the source and resize preference; player and activity objects never enter saved state. */
    static VideoPlayerDialogFragment newInstance(String url, boolean cropVideo) {
        VideoPlayerDialogFragment fragment = new VideoPlayerDialogFragment();
        Bundle arguments = new Bundle();
        arguments.putString(URL, url);
        arguments.putBoolean(CROP, cropVideo);
        fragment.setArguments(arguments);
        return fragment;
    }

    /** Resolves themed adapter contexts to their fragment host without retaining wrapper chains. */
    private static FragmentActivity activity(Context context) {
        Context current = context;
        while (current instanceof ContextWrapper && !(current instanceof FragmentActivity)) {
            Context next = ((ContextWrapper) current).getBaseContext();
            if (next == current) break;
            current = next;
        }
        return current instanceof FragmentActivity ? (FragmentActivity) current : null;
    }

    /** Restores progress, pause choice and fullscreen ownership before creating any media resources. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            resumePosition = savedInstanceState.getLong(POSITION);
            resumePlaying = savedInstanceState.getBoolean(PLAY, true);
            fullscreen = savedInstanceState.getBoolean(FULLSCREEN);
            originalOrientation = savedInstanceState.getInt(ORIGINAL_ORIENTATION,
                    ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
            orientationCaptured = savedInstanceState.getBoolean(ORIENTATION_CAPTURED, fullscreen);
        }
    }

    /** Builds accessible playback controls, an explicit close action and a recoverable network error. */
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        ComponentDialog dialog = new ComponentDialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.video);
        View root = dialog.findViewById(R.id.video_player_root);
        playerView = dialog.findViewById(R.id.video_player);
        playerView.setResizeMode(requireArguments().getBoolean(CROP)
                ? AspectRatioFrameLayout.RESIZE_MODE_ZOOM : AspectRatioFrameLayout.RESIZE_MODE_FIT);
        playerView.setShowNextButton(false);
        playerView.setShowPreviousButton(false);
        playerView.setFullscreenButtonClickListener(this::setFullscreen);
        fullscreenBack = new OnBackPressedCallback(fullscreen) {
            /** Leaves immersive playback before allowing a subsequent system Back to close the video. */
            @Override
            public void handleOnBackPressed() { setFullscreen(false); }
        };
        // Fragment ownership survives dialog stop/start; remove it when this particular view is destroyed.
        dialog.getOnBackPressedDispatcher().addCallback(this, fullscreenBack);
        errorPanel = dialog.findViewById(R.id.video_player_error_panel);
        dialog.findViewById(R.id.video_player_retry).setOnClickListener(view -> retry());
        dialog.findViewById(R.id.video_player_close).setOnClickListener(view -> dismiss());
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.BLACK));
            WindowCompat.setDecorFitsSystemWindows(window, false);
            WindowCompat.getInsetsController(window, root).setAppearanceLightStatusBars(false);
            WindowCompat.getInsetsController(window, root).setAppearanceLightNavigationBars(false);
            ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
                int protectedTypes = WindowInsetsCompat.Type.displayCutout();
                if (!fullscreen) protectedTypes |= WindowInsetsCompat.Type.systemBars();
                Insets safe = insets.getInsets(protectedTypes);
                view.setPadding(safe.left, safe.top, safe.right, safe.bottom);
                return WindowInsetsCompat.CONSUMED;
            });
        }
        return dialog;
    }

    /** Occupies the current window size and attaches a fresh player only while this window is visible. */
    @Override
    public void onStart() {
        super.onStart();
        Window window = requireDialog().getWindow();
        if (window != null) window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        applyFullscreen();
        if (player == null) {
            player = createPlayer(requireContext());
            Player attached = player;
            errorPanel.setVisibility(View.GONE);
            player.addListener(new Player.Listener() {
                /** Exposes a retry without displaying server text or source URLs to the user. */
                @Override
                public void onPlayerError(@NonNull PlaybackException error) {
                    if (player == attached && errorPanel != null) errorPanel.setVisibility(View.VISIBLE);
                }

                /** Keeps the screen awake only during active playback, rather than while paused or buffering. */
                @Override
                public void onIsPlayingChanged(boolean isPlaying) {
                    if (player == attached && playerView != null) playerView.setKeepScreenOn(isPlaying);
                }

                /** Removes a previous failure message after successful playback recovery. */
                @Override
                public void onPlaybackStateChanged(int state) {
                    if (player == attached && errorPanel != null && state == Player.STATE_READY) {
                        errorPanel.setVisibility(View.GONE);
                    }
                }
            });
            playerView.setPlayer(player);
            player.setMediaItem(MediaItem.fromUri(requireArguments().getString(URL, "")));
            player.seekTo(resumePosition);
            player.setPlayWhenReady(resumePlaying);
            player.prepare();
        }
    }

    /** Creates the application-context player; tests can provide deterministic media without codecs or a server. */
    protected Player createPlayer(Context context) {
        AudioAttributes audio = new AudioAttributes.Builder().setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build();
        return new ExoPlayer.Builder(context.getApplicationContext())
                .setAudioAttributes(audio, true).setHandleAudioBecomingNoisy(true).build();
    }

    /** Changes presentation once; Media3 may call this listener again while synchronizing its icon. */
    private void setFullscreen(boolean enabled) {
        if (fullscreen == enabled) return;
        fullscreen = enabled;
        applyFullscreen();
        if (!enabled) restoreOrientation();
    }

    /** Applies immersive controls independently of whether the device honors the landscape request. */
    private void applyFullscreen() {
        if (playerView == null || getDialog() == null) return;
        playerView.setFullscreenButtonState(fullscreen);
        requireDialog().findViewById(R.id.video_player_close).setVisibility(fullscreen ? View.GONE : View.VISIBLE);
        if (fullscreenBack != null) fullscreenBack.setEnabled(fullscreen);
        View root = requireDialog().findViewById(R.id.video_player_root);
        Window window = requireDialog().getWindow();
        if (window != null) {
            WindowInsetsControllerCompat insets = WindowCompat.getInsetsController(window, root);
            insets.setSystemBarsBehavior(fullscreen
                    ? WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    : WindowInsetsControllerCompat.BEHAVIOR_DEFAULT);
            if (fullscreen) insets.hide(WindowInsetsCompat.Type.systemBars());
            else insets.show(WindowInsetsCompat.Type.systemBars());
        }
        ViewCompat.requestApplyInsets(root);
        if (fullscreen) {
            FragmentActivity host = requireActivity();
            if (!orientationCaptured) {
                originalOrientation = host.getRequestedOrientation();
                orientationCaptured = true;
            }
            if (host.getRequestedOrientation() != ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE) {
                host.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
            }
        }
    }

    /** Restores the exact host rotation policy rather than imposing portrait on exit or dismissal. */
    private void restoreOrientation() {
        FragmentActivity host = getActivity();
        if (!orientationCaptured || host == null || host.isDestroyed()) return;
        orientationCaptured = false;
        host.setRequestedOrientation(originalOrientation);
    }

    /** Retries the current source without losing position after a temporary connection failure. */
    private void retry() {
        if (player == null) return;
        errorPanel.setVisibility(View.GONE);
        player.prepare();
        player.play();
    }

    /** Saves playback, fullscreen state and original orientation for activity and process recreation. */
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        checkpoint();
        outState.putLong(POSITION, resumePosition);
        outState.putBoolean(PLAY, resumePlaying);
        outState.putBoolean(FULLSCREEN, fullscreen);
        outState.putInt(ORIGINAL_ORIENTATION, originalOrientation);
        outState.putBoolean(ORIENTATION_CAPTURED, orientationCaptured);
        super.onSaveInstanceState(outState);
    }

    /** Stops background playback and releases decoder/surface resources until the window becomes visible again. */
    @Override
    public void onStop() {
        releasePlayer();
        super.onStop();
    }

    /** Releases media immediately for close, system back and outside cancellation as well as lifecycle stop. */
    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        releasePlayer();
        FragmentActivity host = getActivity();
        // DialogFragment also dismisses the old window during rotation; the restored window keeps ownership.
        if (host != null && (!host.isChangingConfigurations() || isRemoving())) restoreOrientation();
        super.onDismiss(dialog);
    }

    /** Drops references to the destroyed dialog so configuration changes do not retain old views. */
    @Override
    public void onDestroyView() {
        releasePlayer();
        if (fullscreenBack != null) fullscreenBack.remove();
        fullscreenBack = null;
        playerView = null;
        errorPanel = null;
        super.onDestroyView();
    }

    /** Captures playback state before pausing, so returning to the foreground preserves deliberate pauses. */
    private void checkpoint() {
        if (player == null) return;
        resumePosition = Math.max(0, player.getCurrentPosition());
        resumePlaying = player.getPlayWhenReady();
    }

    /** Detaches the surface and releases one player exactly once across overlapping close/stop callbacks. */
    private void releasePlayer() {
        if (player == null) return;
        checkpoint();
        Player releasing = player;
        player = null;
        releasing.pause();
        if (playerView != null) {
            playerView.setPlayer(null);
            playerView.setKeepScreenOn(false);
        }
        releasing.release();
    }
}
