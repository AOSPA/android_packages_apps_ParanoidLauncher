/*
 * Copyright (C) 2019 Paranoid Android
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.paranoid.launcher.settings;

import android.os.Bundle;
import android.text.TextUtils;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.launcher3.LauncherFiles;
import com.android.launcher3.R;
import com.android.launcher3.settings.SettingsActivity;
import com.android.launcher3.settings.SettingsActivity.LauncherSettingsFragment;

import com.paranoid.launcher.ParanoidLauncherCallbacks;
import com.paranoid.launcher.ParanoidUtils;

public class ParanoidSettingsActivity extends SettingsActivity {

    public static final String MINUS_ONE_KEY = "pref_enable_minus_one";
    public static final String ICON_PACK = "pref_icon_pack";

    /**
     * This fragment shows the launcher paranoid preferences.
     */
    public static class ParanoidLauncherSettingsFragment extends LauncherSettingsFragment implements Preference.OnPreferenceClickListener() {

        private String mHighLightKey;
        private boolean mPreferenceHighlighted = false;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            final Bundle args = getArguments();
            mHighLightKey = args == null ? null : args.getString(EXTRA_FRAGMENT_ARG_KEY);
            if (rootKey == null && !TextUtils.isEmpty(mHighLightKey)) {
                rootKey = getParentKeyForPref(mHighLightKey);
            }

            if (savedInstanceState != null) {
                mPreferenceHighlighted = savedInstanceState.getBoolean(SAVE_HIGHLIGHTED_KEY);
            }

            getPreferenceManager().setSharedPreferencesName(LauncherFiles.SHARED_PREFERENCES_KEY);
            setPreferencesFromResource(R.xml.launcher_preferences, rootKey);

            PreferenceScreen screen = getPreferenceScreen();
            for (int i = screen.getPreferenceCount() - 1; i >= 0; i--) {
                Preference preference = screen.getPreference(i);
                // Initiate our preferences then the remaining stock ones
                if (!initParanoidPreference(preference)) {
                    screen.removePreference(preference);
                }
                if (!initPreference(preference)) {
                    screen.removePreference(preference);
                }
                preference.setOnPreferenceClickListener(this);
            }
        }

        protected boolean initParanoidPreference(Preference preference) {
            switch (preference.getKey()) {
                case MINUS_ONE_KEY:
                    return ParanoidUtils.hasPackageInstalled(getActivity(),
                            ParanoidLauncherCallbacks.SEARCH_PACKAGE);
                case ICON_PACK:
                    return true;
            }
            return true;
        }

        @Override
        public boolean onPreferenceClick(Preference preference) {
            switch (preference.getKey()) {
                case ICON_PACK: {
                    Intent iconPack = new Intent(getActivity(), IconPackSettingsActivity.class);
                    startActivity(iconPack);

                }
                break;
            }
            return false;
        }
    }
}