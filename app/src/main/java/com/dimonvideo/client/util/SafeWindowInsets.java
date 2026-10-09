package com.dimonvideo.client.util;

import android.view.View;
import android.view.Window;

import androidx.core.graphics.ColorUtils;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.R;
import com.google.android.material.color.MaterialColors;

/** Keeps content inside system bars, cutouts and the keyboard when Android enforces edge-to-edge. */
public final class SafeWindowInsets {
    /** Prevents constructing the shared window policy. */
    private SafeWindowInsets() { }

    /** Enables edge-to-edge while keeping themed content and controls inside the usable window bounds. */
    public static void apply(Window window, View root) {
        WindowCompat.setDecorFitsSystemWindows(window, false);
        int surface = MaterialColors.getColor(root, R.attr.colorSurface);
        boolean lightSurface = ColorUtils.calculateLuminance(surface) > 0.5;
        WindowCompat.getInsetsController(window, root).setAppearanceLightStatusBars(lightSurface);
        WindowCompat.getInsetsController(window, root).setAppearanceLightNavigationBars(lightSurface);
        final int left = root.getPaddingLeft();
        final int top = root.getPaddingTop();
        final int right = root.getPaddingRight();
        final int bottom = root.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets safe = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout() | WindowInsetsCompat.Type.ime());
            view.setPadding(left + safe.left, top + safe.top, right + safe.right, bottom + safe.bottom);
            // Children occupy the padded area already; passing reduced insets avoids duplicate spacing.
            return insets.inset(safe.left, safe.top, safe.right, safe.bottom);
        });
        ViewCompat.requestApplyInsets(root);
    }
}
