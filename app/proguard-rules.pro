# FragmentManager saves these class names when restoring navigation across app versions.
-keepnames class com.dimonvideo.client.** extends androidx.fragment.app.Fragment

# Firebase, EventBus, Room and WorkManager supply their own consumer rules.
