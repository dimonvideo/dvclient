package com.dimonvideo.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.app.Application;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.fragment.NavHostFragment;

import com.dimonvideo.client.databinding.ActivityMainBinding;
import com.dimonvideo.client.ui.forum.ForumFragment;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, application = Application.class)
public class MainActivityNavigationTest {
    @Test
    public void forumCanBeRestoredBeforeActivityViewsAreInflated() {
        assertNull(MainActivity.binding);
        assertNotNull(new ForumFragment());
    }

    @Test
    public void recreatingWithAReplacementFragmentKeepsTheNavigationHostOnTheBackStack() {
        ActivityController<LayoutHostActivity> activity = Robolectric.buildActivity(LayoutHostActivity.class);
        activity.setup();
        try {
            FragmentManager fragments = activity.get().getSupportFragmentManager();
            fragments.executePendingTransactions();
            assertSame(activity.get().navHost, fragments.getPrimaryNavigationFragment());
            openRefresh(fragments);

            activity.recreate();

            fragments = activity.get().getSupportFragmentManager();
            Fragment refresh = fragments.findFragmentByTag("refresh");
            assertNotNull(refresh);
            assertNotNull(refresh.getView());
            assertSame(refresh, fragments.findFragmentById(R.id.nav_host_fragment));
            assertTrue(fragments.popBackStackImmediate());
            assertSame(activity.get().navHost, fragments.findFragmentById(R.id.nav_host_fragment));
            assertSame(activity.get().navHost, fragments.getPrimaryNavigationFragment());
        } finally {
            activity.pause().stop().destroy();
        }
    }

    @Test
    public void legacyReplacementCannotRestoreAnUntaggedHostOverTheNewHost() {
        Intent legacyLaunch = new Intent(RuntimeEnvironment.getApplication(), LayoutHostActivity.class)
                .putExtra("legacy-host", true);
        ActivityController<LayoutHostActivity> activity =
                Robolectric.buildActivity(LayoutHostActivity.class, legacyLaunch).setup();
        try {
            FragmentManager fragments = activity.get().getSupportFragmentManager();
            assertNull(activity.get().navHost.getTag());
            fragments.beginTransaction().add(new Fragment(), "unrelated").commitNow();
            openRefresh(fragments);

            activity.recreate();

            fragments = activity.get().getSupportFragmentManager();
            assertEquals(0, fragments.getBackStackEntryCount());
            assertNull(fragments.findFragmentByTag("refresh"));
            assertNotNull(fragments.findFragmentByTag("unrelated"));
            assertNotNull(activity.get().navHost.getTag());
            assertSame(activity.get().navHost, fragments.findFragmentById(R.id.nav_host_fragment));
            assertSame(activity.get().navHost, fragments.getPrimaryNavigationFragment());
        } finally {
            activity.pause().stop().destroy();
        }
    }

    private void openRefresh(FragmentManager fragments) {
        fragments.beginTransaction()
                .replace(R.id.nav_host_fragment, new RefreshFragment(), "refresh")
                .addToBackStack("refresh")
                .commit();
        fragments.executePendingTransactions();
    }

    public static class RefreshFragment extends Fragment {
        public RefreshFragment() {
            super(android.R.layout.simple_list_item_1);
        }
    }

    /** Inflates MainActivity's real layout without starting account or network services. */
    public static class LayoutHostActivity extends AppCompatActivity {
        private NavHostFragment navHost;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            setTheme(R.style.AppTheme_NoActionBar);
            super.onCreate(savedInstanceState);
            setContentView(ActivityMainBinding.inflate(getLayoutInflater()).getRoot());
            FragmentManager fragments = getSupportFragmentManager();
            if (savedInstanceState == null && getIntent().getBooleanExtra("legacy-host", false)) {
                navHost = new NavHostFragment();
                fragments.beginTransaction().add(R.id.nav_host_fragment, navHost)
                        .setPrimaryNavigationFragment(navHost)
                        .commitNow();
            } else {
                navHost = MainActivity.resolveMainNavHost(fragments);
            }
            navHost.getNavController();
        }
    }
}
