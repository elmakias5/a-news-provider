package m.co.rh.id.a_news_provider.app.provider;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import m.co.rh.id.a_news_provider.app.workmanager.ConstantsWork;
import m.co.rh.id.a_news_provider.app.workmanager.PeriodicRssSyncWorker;
import m.co.rh.id.aprovider.Provider;
import m.co.rh.id.aprovider.ProviderValue;

public class AppSharedPreferences {
    private static final String SHARED_PREFERENCES_NAME = "RssSharedPreferences";
    private ProviderValue<ExecutorService> mExecutorService;
    private ProviderValue<WorkManager> mWorkManager;
    private SharedPreferences mSharedPreferences;

    private boolean mPeriodicSyncInit;
    private String mPeriodicSyncInitKey;

    private boolean mEnablePeriodicSync;
    private String mEnablePeriodicSyncKey;

    private int mPeriodicSyncRssHour;
    private String mPeriodicSyncRssHourKey;

    private int mSelectedTheme;
    private String mSelectedThemeKey;

    public AppSharedPreferences(Provider provider, Context context) {
        mExecutorService = provider.lazyGet(ExecutorService.class);
        mWorkManager = provider.lazyGet(WorkManager.class);
        mSharedPreferences = context.getSharedPreferences(
                SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        initValue();
    }

    private void initValue() {
        mPeriodicSyncInitKey = SHARED_PREFERENCES_NAME
                + ".periodicSyncInit";
        mEnablePeriodicSyncKey = SHARED_PREFERENCES_NAME
                + ".enablePeriodicSync";
        mPeriodicSyncRssHourKey = SHARED_PREFERENCES_NAME
                + ".periodicSyncRssHour";
        mSelectedThemeKey = SHARED_PREFERENCES_NAME
                + ".selectedTheme";

        boolean periodicSyncInit = mSharedPreferences.getBoolean(mPeriodicSyncInitKey, false);
        periodicSyncInit(periodicSyncInit);
        boolean enablePeriodicSync = mSharedPreferences.getBoolean(mEnablePeriodicSyncKey, true);
        enablePeriodicSync(enablePeriodicSync);
        int periodicSyncRssHour = mSharedPreferences.getInt(
                mPeriodicSyncRssHourKey, 6);
        periodicSyncRssHour(periodicSyncRssHour);
        if (!isPeriodicSyncInit()) {
            initPeriodicSync();
            periodicSyncInit(true);
        }
        int selectedTheme = mSharedPreferences.getInt(
                mSelectedThemeKey,
                AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        setSelectedTheme(selectedTheme);
    }

    private void initPeriodicSync() {
        if (getPeriodicSyncRssHour() > 0 && isEnablePeriodicSync()) {
            PeriodicWorkRequest.Builder rssSyncBuilder = new PeriodicWorkRequest.Builder(PeriodicRssSyncWorker.class,
                    mPeriodicSyncRssHour, TimeUnit.HOURS);
            rssSyncBuilder.setConstraints(new Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build());
            PeriodicWorkRequest periodicWorkRequest = rssSyncBuilder.build();
            mWorkManager.get().enqueueUniquePeriodicWork(ConstantsWork.UNIQUE_PERIODIC_RSS_SYNC,
                    ExistingPeriodicWorkPolicy.REPLACE
                    , periodicWorkRequest);
        } else {
            mWorkManager.get().cancelUniqueWork(ConstantsWork.UNIQUE_PERIODIC_RSS_SYNC);
        }
    }

    private void enablePeriodicSync(boolean b) {
        mEnablePeriodicSync = b;
        mExecutorService.get().execute(() ->
                mSharedPreferences.edit().putBoolean(mEnablePeriodicSyncKey, b)
                        .commit());
    }

    public boolean isEnablePeriodicSync() {
        return mEnablePeriodicSync;
    }

    private void periodicSyncRssHour(int hour) {
        mPeriodicSyncRssHour = hour;
        mExecutorService.get().execute(() ->
                mSharedPreferences.edit().putInt(mPeriodicSyncRssHourKey, hour)
                        .commit());
    }

    public int getPeriodicSyncRssHour() {
        return mPeriodicSyncRssHour;
    }

    public void setPeriodicSyncRssHour(int hour) {
        periodicSyncRssHour(hour);
        initPeriodicSync();
    }

    public boolean isPeriodicSyncInit() {
        return mPeriodicSyncInit;
    }

    private void periodicSyncInit(boolean b) {
        mPeriodicSyncInit = b;
        mExecutorService.get().execute(() ->
                mSharedPreferences.edit().putBoolean(mPeriodicSyncInitKey, b)
                        .commit());
    }

    public void setEnablePeriodicSync(boolean checked) {
        enablePeriodicSync(checked);
        initPeriodicSync();
    }

    private void selectedTheme(int setting) {
        mSelectedTheme = setting;
        mExecutorService.get().execute(() ->
                mSharedPreferences.edit().putInt(mSelectedThemeKey, setting)
                        .commit());
    }

    public void setSelectedTheme(int setting) {
        selectedTheme(setting);
        AppCompatDelegate.setDefaultNightMode(setting);
    }

    public int getSelectedTheme() {
        return mSelectedTheme;
    }
}
