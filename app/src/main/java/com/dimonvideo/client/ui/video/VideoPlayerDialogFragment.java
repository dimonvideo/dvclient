package com.dimonvideo.client.ui.video;

import android.app.Dialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
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

/** Restorable video window that follows the available display size without locking device orientation. */
@UnstableApi
public class VideoPlayerDialogFragment extends DialogFragment {
    private static final String TAG = "video-player";
    private static final String URL = "url";
    private static final String CROP = "crop";
    private static final String POSITION = "position";
    private static final String PLAY = "play";
    private Player player;
    private PlayerView playerView;
    private View errorPanel;
    private long resumePosition;
    private boolean resumePlaying = true;

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

    /** Restores position and the user's pause choice before creating any media resources. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            resumePosition = savedInstanceState.getLong(POSITION);
            resumePlaying = savedInstanceState.getBoolean(PLAY, true);
        }
    }

    /** Builds accessible playback controls, an explicit close action and a recoverable network error. */
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.video);
        View root = dialog.findViewById(R.id.video_player_root);
        playerView = dialog.findViewById(R.id.video_player);
        playerView.setResizeMode(requireArguments().getBoolean(CROP)
                ? AspectRatioFrameLayout.RESIZE_MODE_ZOOM : AspectRatioFrameLayout.RESIZE_MODE_FIT);
        playerView.setShowNextButton(false);
        playerView.setShowPreviousButton(false);
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
                Insets safe = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                        | WindowInsetsCompat.Type.displayCutout());
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
        ViewCompat.requestApplyInsets(requireDialog().findViewById(R.id.video_player_root));
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

    /** Retries the current source without losing position after a temporary connection failure. */
    private void retry() {
        if (player == null) return;
        errorPanel.setVisibility(View.GONE);
        player.prepare();
        player.play();
    }

    /** Saves playback progress and pause state for normal activity and process recreation. */
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        checkpoint();
        outState.putLong(POSITION, resumePosition);
        outState.putBoolean(PLAY, resumePlaying);
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
        super.onDismiss(dialog);
    }

    /** Drops references to the destroyed dialog so configuration changes do not retain old views. */
    @Override
    public void onDestroyView() {
        releasePlayer();
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
