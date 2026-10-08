package com.dimonvideo.client;

import android.content.SharedPreferences;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;

import com.dimonvideo.client.ui.settings.SiteLoginDialogFragment;
import com.dimonvideo.client.util.AppController;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowDialog;
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.util.ReflectionHelpers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Exercises the actual settings hierarchy and legacy account preferences without server access. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, application = SettingsActivityTest.TestApp.class)
public class SettingsActivityTest {
    private ActivityController<SettingsActivity> activity;
    private SharedPreferences preferences;
    private SettingsActivity.SettingsFragment fragment;

    /** Starts each case with isolated preferences before inflating the real settings activity. */
    @Before
    public void preparePreferences() {
        preferences = PreferenceManager.getDefaultSharedPreferences(RuntimeEnvironment.getApplication());
        preferences.edit().clear().commit();
    }

    /** Destroys the settings hierarchy and any dialog so preference listeners cannot survive a test. */
    @After
    public void releaseActivity() {
        if (activity != null) activity.pause().stop().destroy();
    }

    /** Sign-in is the first action; removed password fields do not leave broken dependency references. */
    @Test
    public void signedOutSettingsStartWithUnifiedLoginAndDisableAccountFeatures() {
        openSettings();
        assertEquals("dvc_site_login", fragment.getPreferenceScreen().getPreference(0).getKey());
        assertNull(fragment.findPreference("dvc_login"));
        assertNull(fragment.findPreference("dvc_password"));
        assertTrue(preference("dvc_site_login").isEnabled());
        assertFalse(preference("dvc_pm").isEnabled());
        assertFalse(preference("sett_cloud_backup").isEnabled());
        assertFalse(preference("dvc_new_files").isEnabled());
        assertFalse(preference("dvc_add_file").isEnabled());
        assertTrue(preference("dvc_register").isVisible());
    }

    /** Existing verified credentials remain usable and the password is never shown in the summary. */
    @Test
    public void legacyAccountIsPreservedAndSummaryUpdatesAfterAtomicAccountChange() {
        saveAccount("Alice", 12);
        openSettings();
        assertTrue(preference("dvc_pm").isEnabled());
        assertFalse(preference("dvc_register").isVisible());
        assertEquals(activity.get().getString(R.string.site_login_signed_in, "Alice"),
                preference("dvc_site_login").getSummary());
        saveAccount("Bob", 24);
        assertEquals(activity.get().getString(R.string.site_login_signed_in, "Bob"),
                preference("dvc_site_login").getSummary());
        assertFalse(preference("dvc_site_login").getSummary().toString().contains("private-test-password"));
        assertEquals("private-test-password", preferences.getString("dvc_password", null));
    }

    /** Double tapping the entry opens one dialog and does not mutate the account before submission. */
    @Test
    public void loginEntryOpensOneDialogWithoutSavingCandidateCredentials() {
        openSettings();
        Preference login = preference("dvc_site_login");
        login.getOnPreferenceClickListener().onPreferenceClick(login);
        login.getOnPreferenceClickListener().onPreferenceClick(login);
        assertNotNull(activity.get().getSupportFragmentManager()
                .findFragmentByTag(SiteLoginDialogFragment.TAG));
        assertEquals(2, activity.get().getSupportFragmentManager().getFragments().size());
        assertFalse(preferences.contains("dvc_login"));
        assertFalse(preferences.contains("dvc_password"));
    }

    /** Cancelling logout preserves the account; accepting removes its metadata and disables its features. */
    @Test
    public void logoutConfirmationClearsCredentialsAndAccountMetadataTogether() {
        saveAccount("Alice", 12);
        openSettings();
        Preference logout = preference("dvc_clear_login");
        logout.getOnPreferenceClickListener().onPreferenceClick(logout);
        AlertDialog dialog = (AlertDialog) ShadowDialog.getLatestDialog();
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();
        ShadowLooper.shadowMainLooper().idle();
        assertEquals(12, preferences.getInt("user_id", 0));
        assertTrue(preference("dvc_pm").isEnabled());
        logout.getOnPreferenceClickListener().onPreferenceClick(logout);
        dialog = (AlertDialog) ShadowDialog.getLatestDialog();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        ShadowLooper.shadowMainLooper().idle();
        assertEquals(0, preferences.getInt("auth_state", 0));
        assertEquals(0, preferences.getInt("user_id", 0));
        assertEquals(0, preferences.getInt("pm_unread", 0));
        assertFalse(preferences.contains("dvc_password"));
        assertFalse(preferences.contains("dvc_login"));
        assertFalse(preferences.contains("auth_rang"));
        assertFalse(preference("dvc_pm").isEnabled());
        assertTrue(preference("dvc_site_login").isEnabled());
        assertTrue(preference("dvc_register").isVisible());
    }

    /** Inflates the production preference XML and executes its lifecycle callbacks. */
    private void openSettings() {
        activity = Robolectric.buildActivity(SettingsActivity.class);
        activity.get().setTheme(R.style.AppTheme);
        activity.setup();
        fragment = (SettingsActivity.SettingsFragment) activity.get().getSupportFragmentManager()
                .findFragmentById(R.id.settings);
        assertNotNull(fragment);
    }

    /** Returns an expected action rather than accepting silently missing preferences. */
    private Preference preference(String key) {
        Preference preference = fragment.findPreference(key);
        assertNotNull(preference);
        return preference;
    }

    /** Populates the existing preference keys as a single verified session update. */
    private void saveAccount(String login, int id) {
        preferences.edit().putInt("auth_state", 1).putInt("user_id", id)
                .putString("dvc_login", login).putString("dvc_password", "private-test-password")
                .putInt("pm_unread", 7).putString("auth_rang", "member").commit();
    }

    /** Supplies only the app singleton and preferences, bypassing Room, Firebase and worker startup. */
    public static class TestApp extends AppController {
        /** Installs the application identity required by account-bound form checks. */
        @Override
        public void onCreate() { ReflectionHelpers.setStaticField(AppController.class, "sInstance", this); }
    }
}
