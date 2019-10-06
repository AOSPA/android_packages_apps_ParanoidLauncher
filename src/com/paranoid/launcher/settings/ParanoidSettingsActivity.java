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

package com.paranoid.launcher;

import android.os.Bundle;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragment;

import com.android.launcher3.settings.SettingsActivity;
import com.android.launcher3.settings.SettingsActivity.LauncherSettingsFragment;

import com.paranoid.launcher.ParanoidLauncherCallbacks;
import com.paranoid.launcher.ParanoidUtils;

public class ParanoidSettingsActivity extends SettingsActivity {

	public static final String KEY_MINUS_ONE = "pref_enable_minus_one";

    /**
     * This fragment shows the Paranoid launcher preferences.
     */
    public static class ParanoidSettingsFragment extends LauncherSettingsFragment {

        /**
         * Initializes a preference. This is called for every preference. Returning false here
         * will remove that preference from the list.
         */
        @Override
        protected boolean initPreference(Preference preference) {
            super.initPreference(preference);
			switch (preference.getKey()) {
				case KEY_MINUS_ONE:
                    return ParanoidUtils.hasPackageInstalled(getActivity(),
                            ParanoidLauncherCallbacks.SEARCH_PACKAGE);
			}
            return true;
        }
    }
}