/*
 * Copyright (C) 2018 CypherOS
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
package com.paranoid.launcher.qsb;

import static com.android.launcher3.LauncherState.ALL_APPS_CONTENT;
import static com.android.launcher3.LauncherState.ALL_APPS_HEADER;
import static com.android.launcher3.LauncherState.HOTSEAT_SEARCH_BOX;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.IntProperty;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.animation.Interpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.graphics.ColorUtils;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.OnScrollListener;

import com.android.launcher3.BaseRecyclerView;
import com.android.launcher3.DeviceProfile;
import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.Utilities;
import com.android.launcher3.allapps.AllAppsContainerView;
import com.android.launcher3.allapps.SearchUiManager;
import com.android.launcher3.allapps.search.AllAppsSearchBarController;
import com.android.launcher3.anim.Interpolators;
import com.android.launcher3.anim.PropertySetter;
import com.android.launcher3.uioverrides.WallpaperColorInfo;
import com.android.launcher3.uioverrides.WallpaperColorInfo.OnChangeListener;
import com.android.launcher3.util.Themes;

import com.android.quickstep.WindowTransformSwipeHandler;

import com.paranoid.launcher.ParanoidUtils;
import com.paranoid.launcher.ParanoidLauncher;
import com.paranoid.launcher.ParanoidLauncherCallbacks;
import com.paranoid.launcher.qsb.configs.ConfigurationBuilder;
import com.paranoid.launcher.qsb.configs.QsbConfiguration;
import com.paranoid.launcher.qsb.search.DefaultSearchView;
import com.paranoid.launcher.search.SearchThread;

public class AllAppsQsb extends BaseQsb implements OnClickListener, SearchUiManager, OnChangeListener {

    public Context mContext;
    public QsbConfiguration mQsbConfig;
    public int mMarginAdjusting;
    public Bitmap mQsbScroll;
    public float mFixedTranslationY;
    public AllAppsContainerView mAppsView;
    public boolean mKeepDefaultView;
    public DefaultSearchView mDefaultSearchView;
    public int mShadowAlpha;
    public boolean mUseDefaultSearch;

    public AllAppsQsb(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public AllAppsQsb(Context context, AttributeSet attributeSet, int res) {
        super(context, attributeSet, res);
        mContext = context;
        mShadowAlpha = 0;
        setOnClickListener(this);
        mQsbConfig = QsbConfiguration.getInstance(context);
        mMarginAdjusting = mContext.getResources().getDimensionPixelSize(R.dimen.qsb_margin_top_adjusting);
        mFixedTranslationY = Math.round(getTranslationY());
        setClipToPadding(false);
        setTranslationY(0);
    }

    @Override
    public void setInsets(Rect insets) {
        setSearchType();
        ((MarginLayoutParams) getLayoutParams()).topMargin = (int) Math.max(-mFixedTranslationY, insets.top - mMarginAdjusting);
        requestLayout();
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        WallpaperColorInfo instance = WallpaperColorInfo.getInstance(getContext());
        instance.addOnChangeListener(this);
        onExtractedColorsChanged(instance);
        updateConfiguration();
    }

    @Override
    public void onDetachedFromWindow() {
        WallpaperColorInfo.getInstance(getContext()).removeOnChangeListener(this);
        super.onDetachedFromWindow();
    }

    @Override
    public void onExtractedColorsChanged(WallpaperColorInfo wallpaperColorInfo) {
        setColor(ColorUtils.compositeColors(ColorUtils.compositeColors(Themes.getAttrBoolean(mLauncher, R.attr.isMainColorDark) ? -335544321 : -855638017, Themes.getAttrColor(mLauncher, R.attr.allAppsScrimColor)), wallpaperColorInfo.getMainColor()));
    }

    @Override
    public void initialize(AllAppsContainerView allAppsContainerView) {
        mAppsView = allAppsContainerView;
        mAppsView.addElevationController(new OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                setShadowAlpha(((BaseRecyclerView) recyclerView).getCurrentScrollY());
            }
        });
        mAppsView.setRecyclerViewVerticalFadingEdgeEnabled(true);
    }

    public void updateConfiguration() {
        setColorAlpha(mColor);
        setMicPaint(0.0f);
        setMicRipple();
    }

    @Override
    public void onClick(View view) {
        if (view == this) {
            startSearch("", mResult);
        }
    }

    @Override
    public void startSearch(String initialQuery, int result) {
        ConfigurationBuilder config = new ConfigurationBuilder(this, true);
        if (mLauncher.getLauncherCallbacks().getClient().startSearch(config.build(), config.getExtras())) {
            mLauncher.getLauncherCallbacks().getQsbController().playQsbAnimation();
        } else {
            searchFallback(initialQuery);
        }
        mResult = 0;
    }

    public void searchFallback(String query) {
        if (mDefaultSearchView == null) {
            ensureFallbackView();
        }
        mDefaultSearchView.setText(query);
        mDefaultSearchView.showKeyboard();
    }

    public void resetSearch() {
        setShadowAlpha(0);
        if (mUseDefaultSearch) {
            resetFallbackView();
        } else if (!mKeepDefaultView) {
            removeDefaultView();
        }
    }

    public void ensureFallbackView() {
        setOnClickListener(null);
        mDefaultSearchView = (DefaultSearchView) mLauncher.getLayoutInflater().inflate(R.layout.all_apps_google_search_fallback, this, false);
        mDefaultSearchView.mAllAppsQsb = this;
        mDefaultSearchView.mApps = mAppsView.getApps();
        mDefaultSearchView.mAppsView = mAppsView;
        SearchThread searchThread = new SearchThread(mDefaultSearchView.getContext());
        mDefaultSearchView.mController.initialize(searchThread, mDefaultSearchView, Launcher.getLauncher(mDefaultSearchView.getContext()), mDefaultSearchView);
        addView(mDefaultSearchView);
    }

    public void removeDefaultView() {
        if (mDefaultSearchView != null) {
            mDefaultSearchView.clearSearchResult();
            setOnClickListener(this);
            removeView(mDefaultSearchView);
            mDefaultSearchView = null;
        }
    }

    public void resetFallbackView() {
        if (mDefaultSearchView != null) {
            mDefaultSearchView.reset();
            mDefaultSearchView.clearSearchResult();
        }
    }

    @Override
    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        View view = (View) getParent();
        setTranslationX((float) ((view.getPaddingLeft() + ((((view.getWidth() - view.getPaddingLeft()) - view.getPaddingRight()) - (i3 - i)) / 2)) - i));
        offsetTopAndBottom((int) mFixedTranslationY);
    }

    @Override
    public int getMeasuredWidth(int width, DeviceProfile dp) {
        int leftRightPadding = dp.desiredWorkspaceLeftRightMarginPx
                + dp.cellLayoutPaddingLeftRightPx;
        int rowWidth = width - leftRightPadding * 2;
        return rowWidth;
    }

    @Override
    public void draw(Canvas canvas) {
        if (mShadowAlpha > 0) {
            if (mQsbScroll == null) {
                mQsbScroll = createBitmap(mContext.getResources().getDimension(
                      R.dimen.hotseat_qsb_scroll_shadow_blur_radius), mContext.getResources().getDimension(R.dimen.hotseat_qsb_scroll_key_shadow_offset), 0);
            }
            mShadowHelper.paint.setAlpha(mShadowAlpha);
            drawShadow(mQsbScroll, canvas);
            mShadowHelper.paint.setAlpha(255);
        }
        super.draw(canvas);
    }

    @Override
    public float getScrollRangeDelta(Rect insets) {
        if (mLauncher.getDeviceProfile().isVerticalBarLayout()) {
            return WindowTransformSwipeHandler.SWIPE_DURATION_MULTIPLIER;
        }
        DeviceProfile dp = mLauncher.getWallpaperDeviceProfile();
        int height = (dp.hotseatBarSizePx - dp.hotseatCellHeightPx) - getLayoutParams().height;
        int marginBottom = insets.bottom;
        return (float) (getLayoutParams().height + Math.max(-mFixedTranslationY, insets.top - mMarginAdjusting) + mFixedTranslationY + marginBottom + ((int) (((float) (height - marginBottom)) * 0.45f)));
    }

    public void setShadowAlpha(int alpha) {
        alpha = Utilities.boundToRange(alpha, 0, 255);
        if (mShadowAlpha != alpha) {
            mShadowAlpha = alpha;
            invalidate();
        }
    }

    @Override
    public boolean isClipboard() {
        if (mDefaultSearchView != null) {
            return false;
        }
        return super.isClipboard();
    }

    protected void setSearchType() {
        boolean useDefaultSearch = !ParanoidUtils.hasPackageInstalled(Launcher.getLauncher(mContext), ParanoidLauncherCallbacks.SEARCH_PACKAGE);
        if (mUseDefaultSearch != useDefaultSearch) {
            removeDefaultView();
            mUseDefaultSearch = useDefaultSearch;
            ((ImageView) findViewById(R.id.g_icon)).setImageResource(mUseDefaultSearch ? R.drawable.ic_allapps_search : R.drawable.ic_super_g_color);
            if (mMicIconView != null) {
				mMicIconView.setAlpha(mUseDefaultSearch ? 0.0f : 1.0f);
			}
            if (mUseDefaultSearch) {
                ensureFallbackView();
            }
        }
    }

    @Override
    public void setContentVisibility(int visibleElements, PropertySetter setter, Interpolator interpolator) {
        boolean hasSearchBoxContent = (visibleElements & HOTSEAT_SEARCH_BOX) != 0 && (visibleElements & ALL_APPS_HEADER) != 0;
        float alpha = hasSearchBoxContent ? 1.0f : 0.0f;
        setter.setViewAlpha(this, 1, interpolator);
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    @Override
    public void preDispatchKeyEvent(KeyEvent keyEvent) {
    }

    public void setKeepDefaultView(boolean canKeep) {
        mKeepDefaultView = canKeep;
    }
}
