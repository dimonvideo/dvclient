package com.dimonvideo.client.adater;

import android.app.Application;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.core.widget.NestedScrollView;

import com.dimonvideo.client.R;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Measures the complete sheet so its fixed header cannot consume the scrollable reply viewport. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, application = Application.class, qualifiers = "w320dp-h640dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class PmHeaderLayoutTest {
    /** A long subject and sender retain usable reply controls with a large accessibility font. */
    @Test
    public void longFixedHeaderLeavesReplyActionsReachableAtLargeFont() {
        assertBoundedHeader(1.6f, 576);
    }

    /** Even a keyboard-reduced viewport leaves a scrollable body and the close button accessible. */
    @Test
    public void maximumFontHeaderLeavesViewportWhenAvailableHeightShrinks() {
        assertBoundedHeader(2f, 320);
    }

    /** Inflates and measures the production root, then scrolls each reply action into its actual viewport. */
    private static void assertBoundedHeader(float fontScale, int availableHeight) {
        ContextThemeWrapper appTheme = new ContextThemeWrapper(
                RuntimeEnvironment.getApplication(), R.style.AppTheme);
        Configuration configuration = new Configuration(
                RuntimeEnvironment.getApplication().getResources().getConfiguration());
        configuration.fontScale = fontScale;
        appTheme.applyOverrideConfiguration(configuration);
        ContextThemeWrapper context = new ContextThemeWrapper(appTheme, R.style.ThemeOverlay_DVClient_PmDialog);
        ViewGroup root = (ViewGroup) LayoutInflater.from(context).inflate(R.layout.dialog_pm_detail, null, false);
        TextView title = root.findViewById(R.id.pm_detail_title);
        String subject = "An extremely long private message subject ".repeat(100);
        title.setText(subject);
        title.setTextSize(28);
        TextView sender = root.findViewById(R.id.pm_detail_sender);
        sender.setText("An unusually long sender name and date ".repeat(100));
        TextView body = root.findViewById(R.id.pm_detail_body);
        body.setText("Complete message line\n".repeat(40));
        root.measure(View.MeasureSpec.makeMeasureSpec(320, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(availableHeight, View.MeasureSpec.AT_MOST));
        root.layout(0, 0, root.getMeasuredWidth(), root.getMeasuredHeight());

        assertTrue("The fixed subject must occupy at most two lines", title.getLineCount() <= 2);
        assertTrue("A long subject must visibly indicate truncation",
                title.getLayout().getEllipsisCount(title.getLineCount() - 1) > 0);
        assertEquals("The full subject remains available to accessibility", subject, title.getText().toString());
        assertEquals("Sender metadata must fit one fixed line", 1, sender.getLineCount());
        NestedScrollView scroll = root.findViewById(R.id.pm_detail_scroll);
        assertTrue("Measuring the entire root must preserve a usable scroll viewport", scroll.getHeight() >= 48);

        Rect closeBounds = boundsIn(root, root.findViewById(R.id.pm_detail_close));
        assertTrue("The close button must remain completely within the capped sheet",
                closeBounds.top >= 0 && closeBounds.bottom <= root.getHeight()
                        && closeBounds.left >= 0 && closeBounds.right <= root.getWidth());
        int[] reachableControls = {R.id.pm_reply_input, R.id.pm_attach_button,
                R.id.pm_reply_send, R.id.pm_reply_send_delete, R.id.pm_detail_delete};
        ViewGroup content = (ViewGroup) scroll.getChildAt(0);
        for (int id : reachableControls) {
            Rect bounds = boundsIn(content, root.findViewById(id));
            scroll.scrollTo(0, Math.max(0, bounds.bottom - scroll.getHeight()));
            assertTrue("Reply control " + id + " must be reachable within the remaining scroll viewport",
                    bounds.bottom > scroll.getScrollY()
                            && bounds.top < scroll.getScrollY() + scroll.getHeight());
        }
    }

    /** Converts a descendant's complete bounds into its containing layout's coordinate system. */
    private static Rect boundsIn(ViewGroup ancestor, View descendant) {
        Rect bounds = new Rect(0, 0, descendant.getWidth(), descendant.getHeight());
        ancestor.offsetDescendantRectToMyCoords(descendant, bounds);
        return bounds;
    }
}
