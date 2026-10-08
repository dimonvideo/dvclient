package com.dimonvideo.client.ui;

import android.app.Application;
import android.content.res.ColorStateList;
import android.content.res.XmlResourceParser;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.InsetDrawable;
import android.util.Xml;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.graphics.ColorUtils;

import com.dimonvideo.client.R;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.shape.MaterialShapeDrawable;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.xmlpull.v1.XmlPullParser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** Checks the contrast of the real drawer resources, including the selected destination and its header. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, application = Application.class)
public class DrawerContrastTest {
    private static final int[] ENABLED = {android.R.attr.state_enabled};
    private static final int[] CHECKED = {android.R.attr.state_enabled, android.R.attr.state_checked};
    private static final int[] DISABLED_CHECKED = {-android.R.attr.state_enabled, android.R.attr.state_checked};

    /** Selected and ordinary destinations remain readable on the light drawer surface. */
    @Test
    @Config(qualifiers = "notnight")
    public void lightDrawerHasReadableItemsAndHeader() throws Exception {
        assertDrawerContrast();
    }

    /** A selected destination must use dark content on its light pill in the night palette. */
    @Test
    @Config(qualifiers = "night")
    public void darkDrawerHasReadableSelectedDestination() throws Exception {
        assertDrawerContrast();
    }

    /** Resolves the actual NavigationView attributes without starting the main activity's services. */
    private NavigationView inflateDrawer() throws Exception {
        ContextThemeWrapper context = new ContextThemeWrapper(RuntimeEnvironment.getApplication(), R.style.AppTheme);
        try (XmlResourceParser parser = context.getResources().getLayout(R.layout.activity_main)) {
            while (parser.next() != XmlPullParser.END_DOCUMENT) {
                if (parser.getEventType() == XmlPullParser.START_TAG
                        && NavigationView.class.getName().equals(parser.getName())) {
                    return new NavigationView(context, Xml.asAttributeSet(parser));
                }
            }
        }
        throw new AssertionError("Main layout must contain a navigation drawer");
    }

    /** Uses WCAG text/icon thresholds and verifies that disabled items do not inherit selected styling. */
    private void assertDrawerContrast() throws Exception {
        NavigationView drawer = inflateDrawer();
        int surface = MaterialColors.getColor(drawer, com.google.android.material.R.attr.colorSurface);
        ColorStateList text = drawer.getItemTextColor();
        ColorStateList icons = drawer.getItemIconTintList();
        assertNotNull(text);
        assertNotNull(icons);
        InsetDrawable itemInsets = (InsetDrawable) drawer.getItemBackground();
        MaterialShapeDrawable itemBackground = (MaterialShapeDrawable) itemInsets.getDrawable();
        ColorStateList fills = itemBackground.getFillColor();
        assertNotNull(fills);
        int selectedSurface = fills.getColorForState(CHECKED, Color.TRANSPARENT);
        assertContrast("ordinary text", text.getColorForState(ENABLED, 0), surface, 4.5);
        assertContrast("ordinary icon", icons.getColorForState(ENABLED, 0), surface, 3.0);
        assertContrast("selected text", text.getColorForState(CHECKED, 0), selectedSurface, 4.5);
        assertContrast("selected icon", icons.getColorForState(CHECKED, 0), selectedSurface, 3.0);
        assertContrast("selected indicator", selectedSurface, surface, 3.0);
        assertTrue(Color.alpha(text.getColorForState(DISABLED_CHECKED, 0))
                < Color.alpha(text.getColorForState(ENABLED, 0)));
        assertEquals(Color.TRANSPARENT, fills.getColorForState(DISABLED_CHECKED, Color.BLACK));

        View header = drawer.getHeaderView(0);
        int headerSurface = ((ColorDrawable) header.getBackground()).getColor();
        assertContrast("login", ((TextView) header.findViewById(R.id.login_string)).getCurrentTextColor(), headerSurface, 4.5);
        assertContrast("version", ((TextView) header.findViewById(R.id.app_version)).getCurrentTextColor(), headerSurface, 4.5);
        for (int id : new int[]{R.id.theme_icon, R.id.settings_icon, R.id.exit_icon}) {
            ColorStateList tint = ((ImageView) header.findViewById(id)).getImageTintList();
            assertNotNull(tint);
            assertContrast("header action", tint.getDefaultColor(), headerSurface, 3.0);
        }
    }

    /** Reports the contrast ratio so an unintended palette or checked-state change is easy to diagnose. */
    private void assertContrast(String role, int foreground, int background, double minimum) {
        double contrast = ColorUtils.calculateContrast(foreground, background);
        assertTrue(role + " contrast=" + contrast + ", minimum=" + minimum, contrast >= minimum);
    }
}
