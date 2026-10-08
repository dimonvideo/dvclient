package com.dimonvideo.client.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import android.os.Looper;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.text.style.ImageSpan;
import android.text.style.URLSpan;
import android.widget.TextView;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowLooper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

/** Verifies the renderer's visible HTML and view lifecycle contract without a device or network. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, manifest = Config.NONE)
@LooperMode(LooperMode.Mode.PAUSED)
public class AsyncHtmlRendererTest {
    private AsyncHtmlRenderer renderer;

    /** Creates an isolated renderer whose queued work is released after each test. */
    @Before
    public void setUp() {
        renderer = new AsyncHtmlRenderer();
    }

    /** Prevents worker threads and cached results from escaping the test's lifecycle. */
    @After
    public void tearDown() {
        renderer.release();
        ShadowLooper.shadowMainLooper().idle();
    }

    /** HTML entities must be decoded while emphasis, links and list text remain rendered spans. */
    @Test
    public void rendersEntitiesFormattingLinksAndListsWithoutExposingTags() throws Exception {
        TextView view = newView();
        renderer.bind(view, "<b>A &amp; B</b><br><a href=\"https://dimonvideo.ru\">link</a>"
                + "<ul><li>item &#x20; one</li></ul>", false);
        assertFalse(view.getText().toString().contains("<b>"));
        await(() -> view.getText().toString().contains("item"));

        String text = view.getText().toString();
        assertTrue(text.contains("A & B"));
        assertTrue(text.replaceAll("\\s+", " ").contains("item one"));
        assertFalse(text.contains("&#x20;"));
        assertFalse(text.contains("<li>"));
        Spanned rendered = (Spanned) view.getText();
        assertTrue(rendered.getSpans(0, rendered.length(), StyleSpan.class).length > 0);
        assertEquals("https://dimonvideo.ru",
                rendered.getSpans(0, rendered.length(), URLSpan.class)[0].getURL());
    }

    /** Every view waiting on an identical parse must receive the result, including a second holder. */
    @Test
    public void deliversOneSourceToEveryWaitingView() throws Exception {
        TextView first = newView();
        TextView second = newView();
        renderer.bind(first, "<b>Shared &amp; decoded</b>", false);
        renderer.bind(second, "<b>Shared &amp; decoded</b>", false);
        await(() -> first.getText().length() > 0 && second.getText().length() > 0);
        assertEquals("Shared & decoded", first.getText().toString());
        assertEquals(first.getText().toString(), second.getText().toString());
    }

    /** A holder rebound during parsing must keep its new row, including colliding Java string hashes. */
    @Test
    public void recycledViewReceivesOnlyItsLatestSource() throws Exception {
        TextView reused = newView();
        assertEquals("<b>Aa</b>".hashCode(), "<b>BB</b>".hashCode());
        renderer.bind(reused, "<b>Aa</b>", false);
        renderer.bind(reused, "<b>BB</b>", false);
        await(() -> "BB".contentEquals(reused.getText()));
        ShadowLooper.shadowMainLooper().idle();
        assertEquals("BB", reused.getText().toString());
        assertNotEquals("Aa", reused.getText().toString());
    }

    /** Clearing a recycled target and releasing a list must invalidate outstanding result delivery. */
    @Test
    public void clearAndReleaseDoNotOverwriteDetachedTargets() throws Exception {
        TextView cleared = newView();
        TextView detached = newView();
        renderer.bind(cleared, "<b>Old cleared row</b>", false);
        renderer.clear(cleared);
        cleared.setText("recycled");
        renderer.bind(detached, "<b>Old detached row</b>", false);
        renderer.release();
        detached.setText("detached");
        renderer.activate();
        TextView attached = newView();
        renderer.bind(attached, "<b>New row &amp; ready</b>", false);
        await(() -> attached.getText().toString().contains("ready"));
        assertEquals("recycled", cleared.getText().toString());
        assertEquals("detached", detached.getText().toString());
        assertEquals("New row & ready", attached.getText().toString());
    }

    /** A full parse queue must defer excess visible rows and eventually render them without raw HTML. */
    @Test
    public void manyDistinctRowsEventuallyRenderAcrossQueuePressure() throws Exception {
        List<TextView> views = new ArrayList<>();
        for (int index = 0; index < 60; index++) {
            TextView view = newView();
            views.add(view);
            renderer.bind(view, "<b>Row " + index + " &amp; value</b>", false);
            assertFalse(view.getText().toString().contains("<b>"));
        }
        await(() -> views.stream().allMatch(view -> view.getText().length() > 0));
        for (int index = 0; index < views.size(); index++) {
            assertEquals("Row " + index + " & value", views.get(index).getText().toString());
        }
    }

    /** Inline images must use separate mutable drawables per view even when their HTML is cached. */
    @Test
    public void cachedHtmlDoesNotShareImageDrawablesBetweenViews() throws Exception {
        TextView first = newView();
        TextView second = newView();
        String source = "image <img src=\"file:///missing-dv-image.png\">";
        renderer.bind(first, source, true);
        await(() -> first.getText().toString().contains("image"));
        renderer.bind(second, source, true);
        await(() -> second.getText().toString().contains("image"));
        Spanned firstText = (Spanned) first.getText();
        Spanned secondText = (Spanned) second.getText();
        ImageSpan firstImage = firstText.getSpans(0, firstText.length(), ImageSpan.class)[0];
        ImageSpan secondImage = secondText.getSpans(0, secondText.length(), ImageSpan.class)[0];
        assertNotSame(firstImage.getDrawable(), secondImage.getDrawable());

        TextView preview = newView();
        renderer.bind(preview, source, false);
        Spanned previewText = (Spanned) preview.getText();
        assertEquals(0, previewText.getSpans(0, previewText.length(), ImageSpan.class).length);
        assertFalse(previewText.toString().contains("<img"));
    }

    /** Null API text should clear a previous row instead of crashing or leaking its content. */
    @Test
    public void nullHtmlRendersEmptyText() throws Exception {
        TextView view = newView();
        view.setText("previous");
        renderer.bind(view, null, false);
        TextView completion = newView();
        renderer.bind(completion, "completed", false);
        await(() -> "completed".contentEquals(completion.getText()));
        assertEquals("", view.getText().toString());
    }

    /** Creates a widget using Robolectric's application context without retaining an Activity. */
    private static TextView newView() {
        return new TextView(RuntimeEnvironment.getApplication());
    }

    /** Drains main-thread delivery while giving the real parser thread a bounded chance to finish. */
    private static void await(BooleanSupplier condition) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) {
            org.robolectric.Shadows.shadowOf(Looper.getMainLooper()).idle();
            if (condition.getAsBoolean()) {
                return;
            }
            Thread.sleep(10);
        }
        assertTrue("HTML parse did not complete within ten seconds", condition.getAsBoolean());
    }
}
