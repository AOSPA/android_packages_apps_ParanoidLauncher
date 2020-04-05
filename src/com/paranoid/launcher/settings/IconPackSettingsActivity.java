/*
 * Copyright (C) 2020 Paranoid Android
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

import static com.paranoid.launcher.provider.IconPackProvider.PREF_ICON_PACK;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListView;

import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragment;
import androidx.preference.PreferenceFragment.OnPreferenceStartFragmentCallback;
import androidx.preference.PreferenceFragment.OnPreferenceStartScreenCallback;
import androidx.preference.PreferenceGroup.PreferencePositionCallback;
import androidx.preference.PreferenceScreen;
import androidx.recyclerview.widget.RecyclerView;

import com.android.launcher3.R;
import com.android.launcher3.Utilities;
import com.android.launcher3.settings.PreferenceHighlighter;

import com.paranoid.launcher.settings.widget.IconPackPreference;

import java.util.ArrayList;

public class IconPackSettingsActivity extends Activity implements 
        OnPreferenceStartFragmentCallback, OnPreferenceStartScreenCallback {

    public static final String EXTRA_FRAGMENT_ARG_KEY = ":settings:fragment_args_key";
    public static final String EXTRA_SHOW_FRAGMENT_ARGS = ":settings:show_fragment_args";
    private static final int DELAY_HIGHLIGHT_DURATION_MILLIS = 600;
    public static final String SAVE_HIGHLIGHTED_KEY = "android:preference_highlighted";

    public static final String CURRENT_ICON_PACK = "pref_current_icon_pack";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        if (savedInstanceState == null) {
            Bundle args = new Bundle();
            String prefKey = getIntent().getStringExtra(EXTRA_FRAGMENT_ARG_KEY);
            if (!TextUtils.isEmpty(prefKey)) {
                args.putString(EXTRA_FRAGMENT_ARG_KEY, prefKey);
            }

            Fragment f = Fragment.instantiate(
                    this, getString("com.paranoid.launcher.settings.IconPackSettingsActivity$IconPackFragment"), args);
            // Display the fragment as the main content.
            getFragmentManager().beginTransaction()
                    .replace(android.R.id.content, f)
                    .commit();
        }
    }

    private boolean startFragment(String fragment, Bundle args, String key) {
        if (getFragmentManager().isStateSaved()) {
            // Sometimes onClick can come after onPause because of being posted on the handler.
            // Skip starting new fragments in that case.
            return false;
        }
        Fragment f = Fragment.instantiate(this, fragment, args);
        getFragmentManager()
                .beginTransaction()
                .replace(android.R.id.content, f)
                .addToBackStack(key)
                .commit();
        return true;
    }

    @Override
    public boolean onPreferenceStartFragment(
            PreferenceFragment preferenceFragment, Preference pref) {
        return startFragment(pref.getFragment(), pref.getExtras(), pref.getKey());
    }

    @Override
    public boolean onPreferenceStartScreen(PreferenceFragment caller, PreferenceScreen pref) {
        Bundle args = new Bundle();
        args.putString(PreferenceFragment.ARG_PREFERENCE_ROOT, pref.getKey());
        return startFragment(getString("com.paranoid.launcher.settings.IconPackSettingsActivity$IconPackFragment"), args, pref.getKey());
    }

    @Override
    public boolean onNavigateUp() {
        onBackPressed();
        return true;
    }

    public static class IconPackFragment extends PreferenceFragment {

        private String mHighLightKey;
        private boolean mPreferenceHighlighted = false;

        private ArrayList<IconPackPreference> mAvailableIconPacks = new ArrayList<>();
        private IconPackListAdapter mAdapter;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {

            View mainView = inflater.inflate(R.layout.icon_pack_settings_view, null);
            ListView currentList = (ListView) mainView.findViewById(R.id.current_list);

            mAdapter = new IconPackListAdapter(getContext(), mAvailableIconPacks);
            currentList.setAdapter(mAdapter);
            currentList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

            currentList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    IconPackPreference pref = (IconPackPreference) parent.getItemAtPosition(position);
                    CheckBox cb = (CheckBox) view.findViewById(android.R.id.checkbox);
                    if (pref.isEnabled()) {
                        pref.setChecked(!pref.isChecked());
                        cb.setChecked(pref.isChecked());
                        SharedPreferences sharedPrefs = Utilities.getDevicePrefs(getContext());
                        sharedPrefs.edit().putString(PREF_ICON_PACK, pref.getKey()).apply();
                    }
                }
            });
            updateIconPacks();
            return mainView;
        }

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

            getPreferenceManager().setSharedPreferencesName(Utils.SHARED_PREFERENCES_KEY);
            setPreferencesFromResource(R.xml.icon_pack_preferences, rootKey);
        }

        private Map<String, IconPackInfo> updateAvailableIconPacks() {
            Map<String, IconPackInfo> availablePacks = new HashMap<>();
            List<ResolveInfo> eligiblePacks = pm.queryIntentActivities(new Intent("com.novalauncher.THEME"), 0);
            eligiblePacks.addAll(pm.queryIntentActivities(new Intent("org.adw.launcher.icons.ACTION_PICK_ICON"), 0));
            eligiblePacks.addAll(pm.queryIntentActivities(new Intent("com.dlto.atom.launcher.THEME"), 0));
            eligiblePacks.addAll(pm.queryIntentActivities(new Intent("android.intent.action.MAIN").addCategory("com.anddoes.launcher.THEME"), 0));
            for (ResolveInfo r : eligiblePacks) {
                availablePacks.put(r.activityInfo.packageName, new IconPackInfo(r, pm));
            }
            return availablePacks;
        }

        private void updateIconPacks() {
            mAvailableIconPacks.clear();
            Map<String, IconPackInfo> packages = updateAvailableIconPacks();
            String[] packageNames = new String[packages.size()+1];
            CharSequence[] labels = new CharSequence[packages.size()+1];
            String defaultLabel = "System default";
            packageNames[0] = "";
            labels[0] = defaultLabel;
            int i = 1;
            for (Map.Entry<String, IconPackInfo> entry : packages.entrySet()) {
                packageNames[i] = entry.getKey();
                labels[i] = entry.getValue().label;
                i++;

                IconPackPreference pref = new IconPackPreference(getContext());
                pref.setKey(packageNames[i]);
                pref.setTitle(labels[i]);
                mAvailableIconPacks.add(pref);
            }
            mAdapter.notifyDataSetChanged();
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            outState.putBoolean(SAVE_HIGHLIGHTED_KEY, mPreferenceHighlighted);
        }

        protected String getParentKeyForPref(String key) {
            return null;
        }

        @Override
        public void onResume() {
            super.onResume();

            if (isAdded() && !mPreferenceHighlighted) {
                PreferenceHighlighter highlighter = createHighlighter();
                if (highlighter != null) {
                    getView().postDelayed(highlighter, DELAY_HIGHLIGHT_DURATION_MILLIS);
                    mPreferenceHighlighted = true;
                }
            }
        }

        private PreferenceHighlighter createHighlighter() {
            if (TextUtils.isEmpty(mHighLightKey)) {
                return null;
            }

            PreferenceScreen screen = getPreferenceScreen();
            if (screen == null) {
                return null;
            }

            RecyclerView list = getListView();
            PreferencePositionCallback callback = (PreferencePositionCallback) list.getAdapter();
            int position = callback.getPreferenceAdapterPosition(mHighLightKey);
            return position >= 0 ? new PreferenceHighlighter(list, position) : null;
        }

        private static class IconPackInfo {
            String packageName;
            CharSequence label;

            IconPackInfo(ResolveInfo r, PackageManager pm) {
                packageName = r.activityInfo.packageName;
                label = r.loadLabel(pm);
            }

            public IconPackInfo(String label, String packageName) {
                this.label = label;
                this.packageName = packageName;
            }
        }

        private class SettingsListAdapter extends ArrayAdapter<IconPackPreference> {

            public SettingsListAdapter(Context context, ArrayList<IconPackPreference> items) {
                super(context, R.layout.preference, items);
            }

            @Override
            public View getView(final int position, View view, ViewGroup parent) {
                IconPackPreference preference = getItem(position);
                view = preference.getView(view, parent);
                view.setBackgroundColor(android.R.color.transparent);

                return view;
            }
        }
    }
}