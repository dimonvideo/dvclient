package com.dimonvideo.client.adater;

import android.app.Application;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.widget.TextView;

import androidx.core.graphics.ColorUtils;

import com.dimonvideo.client.R;
import com.google.android.material.card.MaterialCardView;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertTrue;

/** Checks the actual shared message/member row colors in both application themes. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, application = Application.class)
public class PmRowContrastTest {
    /** Sender names must remain readable on white cards rather than use the toolbar accent. */
    @Test
    @Config(qualifiers = "notnight")
    public void senderNameMeetsNormalTextContrastInLightTheme() {
        assertSenderNameContrast();
    }

    /** The dark toolbar primary must not make sender names disappear into black cards. */
    @Test
    @Config(qualifiers = "night")
    public void senderNameMeetsNormalTextContrastInDarkTheme() {
        assertSenderNameContrast();
    }

    /** Inflates the production card and compares its drawn foreground/background against WCAG 4.5:1. */
    private static void assertSenderNameContrast() {
        ContextThemeWrapper context = new ContextThemeWrapper(
                RuntimeEnvironment.getApplication(), R.style.AppTheme);
        MaterialCardView card = (MaterialCardView) LayoutInflater.from(context)
                .inflate(R.layout.list_row_pm, null, false);
        TextView sender = card.findViewById(R.id.name);
        sender.setText("DimonVideo");
        int foreground = sender.getCurrentTextColor();
        int background = card.getCardBackgroundColor().getColorForState(card.getDrawableState(), 0);
        assertTrue("Sender/member name contrast must be at least 4.5:1",
                ColorUtils.calculateContrast(foreground, background) >= 4.5);
    }
}
