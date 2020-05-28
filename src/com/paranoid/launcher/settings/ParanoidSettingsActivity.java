/*
 * Copyright (C) 2019-2020 Paranoid Android
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

import static com.paranoid.launcher.providers.IconPackProvider.PREF_ICON_PACK;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.TwoStatePreference;
import android.text.TextUtils;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import com.android.launcher3.LauncherFiles;
import com.android.launcher3.R;
import com.android.launcher3.Utilities;
import com.android.launcher3.settings.SettingsActivity;
import com.android.launcher3.settings.SettingsActivity.LauncherSettingsFragment;

import com.paranoid.launcher.ParanoidLauncherCallbacks;
import com.paranoid.launcher.ParanoidUtils;

public class ParanoidSettingsActivity extends SettingsActivity {

    public static final String MINUS_ONE_KEY = "pref_enable_minus_one";
    public static final String ICON_PACK = "pref_icon_pack";
    public final static String SHOW_PREDICTIONS_PREF = "pref_show_predictions";

    /**
     * This fragment shows the launcher paranoid preferences.
     */
    public static class ParanoidLauncherSettingsFragment extends LauncherSettingsFragment implements
        Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener, SharedPreferences.OnSharedPreferenceChangeListener {

        private String mHighLightKey;
        private boolean mPreferenceHighlighted = false;

        private Preference mIconPack;

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
            Utilities.getPrefs(getContext()).registerOnSharedPreferenceChangeListener(this);

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
            }
            mIconPack = screen.findPreference(ICON_PACK);
            mIconPack.setOnPreferenceClickListener(this);
            updateIconPackSummary();
            findPreference(SHOW_PREDICTIONS_PREF).setOnPreferenceChangeListener(this);
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            Utilities.getPrefs(getContext()).unregisterOnSharedPreferenceChangeListener(this);
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

        private String getApplicationName(String packageName) {
            PackageManager pm = getContext().getPackageManager();
            ApplicationInfo ai = null;
            String applicationName = null;
            try {
                ai = pm.getApplicationInfo(packageName, 0);
                applicationName = (String) pm.getApplicationLabel(ai);
            } catch (NameNotFoundException e) {
                applicationName = "System default";
            }
            return applicationName;
        }

        private void updateIconPackSummary() {
            SharedPreferences prefs = Utilities.getPrefs(getContext());
            String packageName = prefs.getString(PREF_ICON_PACK, "");
            mIconPack.setSummary(String.format(
                    getContext().getResources().getString(
                    R.string.icon_pack_summary), getApplicationName(packageName)));

        }

        @Override
        public boolean onPreferenceChange(Preference preference, final Object newValue) {
            switch (preference.getKey()) {
                case SHOW_PREDICTIONS_PREF:
                    if ((boolean) newValue) {
                        return true;
                    }
                    ParanoidSettingsActivity.SuggestionConfirmationFragment confirmationFragment = new ParanoidSettingsActivity.SuggestionConfirmationFragment();
                    confirmationFragment.setTargetFragment(this, 0);
                    confirmationFragment.show(getFragmentManager(), preference.getKey());
                    break;
            }
            return false;
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

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (PREF_ICON_PACK.equals(key)) {
                updateIconPackSummary();
            }
        }
    }

    public static class SuggestionConfirmationFragment extends DialogFragment implements DialogInterface.OnClickListener {
        public void onClick(final DialogInterface dialogInterface, final int n) {
            if (getTargetFragment() instanceof PreferenceFragment) {
                Preference preference = ((PreferenceFragmentCompat) getTargetFragment())
                        .findPreference(SHOW_PREDICTIONS_PREF);
                if (preference instanceof TwoStatePreference) {
                    ((TwoStatePreference) preference).setChecked(false);
                }
            }
        }

        public Dialog onCreateDialog(final Bundle bundle) {
            return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.title_disable_suggestions_prompt)
                    .setMessage(R.string.msg_disable_suggestions_prompt)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(R.string.label_turn_off_suggestions, this).create();
        }
    }
}
