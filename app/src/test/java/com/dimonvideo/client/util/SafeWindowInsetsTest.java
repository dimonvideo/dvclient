package com.dimonvideo.client.util;

import android.app.Application;
import android.os.Bundle;
import android.widget.FrameLayout;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentActivity;

import com.dimonvideo.client.R;
import com.dimonvideo.client.databinding.ActivityMainBinding;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Verifies usable content bounds on Android 15/16 and the minimum supported Android version. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = {35, 36}, application = Application.class)
public class SafeWindowInsetsTest {
    private ActivityController<TestActivity> activity;
    private FrameLayout root;

    /** Attaches real window content with existing padding before applying the shared inset policy. */
    @Before
    public void setUp() {
        activity = Robolectric.buildActivity(TestActivity.class).setup();
        root = new FrameLayout(activity.get());
        root.setPadding(3, 5, 7, 11);
        activity.get().setContentView(root);
        SafeWindowInsets.apply(activity.get().getWindow(), root);
    }

    /** Releases the real host window after each framework-version scenario. */
    @After
    public void tearDown() { activity.pause().stop().destroy(); }

    /** A landscape cutout and side navigation bar must both leave controls inside the usable rectangle. */
    @Test
    public void landscapeInsetsProtectAllEdgesWithoutPassingDuplicateSpacingToChildren() {
        WindowInsetsCompat remaining = ViewCompat.dispatchApplyWindowInsets(root,
                insets(Insets.of(0, 24, 48, 0), Insets.of(36, 0, 0, 0), 0));
        assertPadding(39, 29, 55, 11);
        assertEquals(Insets.NONE, remaining.getInsets(WindowInsetsCompat.Type.systemBars()
                | WindowInsetsCompat.Type.displayCutout()));
    }

    /** Showing and hiding the keyboard restores navigation-bar padding instead of accumulating it. */
    @Test
    public void keyboardAvoidsCoveredInputsAndPaddingReturnsAfterItCloses() {
        ViewCompat.dispatchApplyWindowInsets(root,
                insets(Insets.of(0, 24, 0, 32), Insets.NONE, 320));
        assertPadding(3, 29, 7, 331);
        ViewCompat.dispatchApplyWindowInsets(root,
                insets(Insets.of(0, 24, 0, 32), Insets.NONE, 0));
        assertPadding(3, 29, 7, 43);
        ViewCompat.dispatchApplyWindowInsets(root,
                insets(Insets.of(0, 24, 0, 32), Insets.NONE, 0));
        assertPadding(3, 29, 7, 43);
    }

    /** Resizing or rotating changes the safe edges without retaining obsolete portrait offsets. */
    @Test
    public void resizedWindowReplacesOldSafeBounds() {
        ViewCompat.dispatchApplyWindowInsets(root,
                insets(Insets.of(0, 30, 0, 20), Insets.NONE, 0));
        assertPadding(3, 35, 7, 31);
        ViewCompat.dispatchApplyWindowInsets(root,
                insets(Insets.of(40, 0, 0, 0), Insets.NONE, 0));
        assertPadding(43, 5, 7, 11);
    }

    /** The actual drawer and its content must shrink inside the outer safe frame, not ignore root padding. */
    @Test
    public void productionDrawerContentStaysInsideSafeWindowBounds() {
        ActivityMainBinding binding = ActivityMainBinding.inflate(activity.get().getLayoutInflater());
        assertTrue(binding.getRoot() instanceof FrameLayout);
        root = binding.getRoot();
        activity.get().setContentView(root);
        SafeWindowInsets.apply(activity.get().getWindow(), root);
        ViewCompat.dispatchApplyWindowInsets(root,
                insets(Insets.of(0, 24, 8, 30), Insets.of(36, 0, 0, 0), 0));
        root.measure(android.view.View.MeasureSpec.makeMeasureSpec(800, android.view.View.MeasureSpec.EXACTLY),
                android.view.View.MeasureSpec.makeMeasureSpec(600, android.view.View.MeasureSpec.EXACTLY));
        root.layout(0, 0, 800, 600);
        assertEquals(36, binding.drawerLayout.getLeft());
        assertEquals(24, binding.drawerLayout.getTop());
        assertEquals(756, binding.drawerLayout.getWidth());
        assertEquals(546, binding.drawerLayout.getHeight());
        assertEquals(756, binding.appBarMain.getRoot().getWidth());
        assertEquals(546, binding.navView.getHeight());
    }

    /** The same policy accepts actual legacy windows without requiring Android 15-only APIs. */
    @Test
    @Config(sdk = 27)
    public void minimumAndroidVersionKeepsInitialContentPadding() {
        assertPadding(3, 5, 7, 11);
    }

    /** Builds independent system-bar, display-cutout and keyboard geometry supplied by Android. */
    private static WindowInsetsCompat insets(Insets bars, Insets cutout, int keyboardBottom) {
        return new WindowInsetsCompat.Builder()
                .setInsets(WindowInsetsCompat.Type.systemBars(), bars)
                .setInsets(WindowInsetsCompat.Type.displayCutout(), cutout)
                .setInsets(WindowInsetsCompat.Type.ime(), Insets.of(0, 0, 0, keyboardBottom))
                .build();
    }

    /** Checks the actual rendered content padding rather than a copied geometry calculation. */
    private void assertPadding(int left, int top, int right, int bottom) {
        assertEquals(left, root.getPaddingLeft());
        assertEquals(top, root.getPaddingTop());
        assertEquals(right, root.getPaddingRight());
        assertEquals(bottom, root.getPaddingBottom());
    }

    /** Supplies Material theme attributes without application services, network requests or navigation. */
    public static class TestActivity extends FragmentActivity {
        /** Installs the production theme before any view or window policy is created. */
        @Override protected void onCreate(Bundle savedInstanceState) {
            setTheme(R.style.AppTheme);
            super.onCreate(savedInstanceState);
        }
    }
}
