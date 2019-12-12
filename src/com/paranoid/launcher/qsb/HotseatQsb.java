/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use file except in compliance with the License.
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

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.View.MeasureSpec;
import android.widget.FrameLayout;

import com.android.launcher3.DeviceProfile;
import com.android.launcher3.Hotseat;
import com.android.launcher3.InsettableFrameLayout;
import com.android.launcher3.R;
import com.android.launcher3.views.BaseDragLayer;

public class HotseatQsb extends Hotseat {

    public final int mMarginBottom;

    public boolean mIsTransposed;

    public AllAppsQsbControllerImpl mAllAppsQsb;

    public int mWidth;

    public HotseatQsb(Context context) {
        this(context, null, 0);
    }

    public HotseatQsb(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public HotseatQsb(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        mMarginBottom = getResources().getDimensionPixelSize(R.dimen.hotseat_qsb_bottom_margin);
    }

    @Override
    public void onDraw(Canvas canvas) {
        if (mIsTransposed && mAllAppsQsb != null) {
            DeviceProfile dp = mActivity.getWallpaperDeviceProfile();
            if (mWidth <= 0) {
                mWidth = mAllAppsQsb.calculateMeasuredDimension(mActivity.getWallpaperDeviceProfile(), Math.round(((float) dp.iconSizePx) * 0.92f), MeasureSpec.makeMeasureSpec(getWidth(), MeasureSpec.EXACTLY));
            }
            int marginBottom = dp.hotseatBarSizePx - dp.hotseatCellHeightPx;
            int minBottom = (((marginBottom - mMarginBottom) - dp.getInsets().bottom) / 2) + dp.getInsets().bottom + mMarginBottom;
            int save = canvas.save();
            canvas.translate(((float) (getWidth() - mWidth)) * 0.5f, (float) (getHeight() - minBottom));
            mAllAppsQsb.drawCanvas(canvas, mWidth);
            int width = mAllAppsQsb.getWidth() - mWidth;
            int childCount = mAllAppsQsb.getChildCount();
            for (int parent = 0; parent < childCount; parent++) {
                View child = mAllAppsQsb.getChildAt(parent);
                if (child.getVisibility() == 0) {
                    int absoluteGravity = Gravity.getAbsoluteGravity(((FrameLayout.LayoutParams) child.getLayoutParams()).gravity, getLayoutDirection());
                    int left = child.getLeft();
                    int top = child.getTop();
                    int i6 = absoluteGravity & 7;
                    if (i6 == 1) {
                        left -= width / 2;
                    } else if (i6 == 5) {
                        left -= width;
                    }
                    canvas.translate((float) left, (float) top);
                    child.draw(canvas);
                    canvas.translate((float) (-left), (float) (-top));
                }
            }
            canvas.restoreToCount(save);
        }
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int mode = MeasureSpec.getMode(widthMeasureSpec);
        int mode2 = MeasureSpec.getMode(heightMeasureSpec);
        int size = MeasureSpec.getSize(widthMeasureSpec);
        int size2 = MeasureSpec.getSize(heightMeasureSpec);
        int paddingRight = size - (getPaddingRight() + getPaddingLeft());
        int paddingBottom = size2 - (getPaddingBottom() + getPaddingTop());
        getShortcutsAndWidgets().setRotation(getRotationMode().surfaceRotation);
        if (getRotationMode().isTransposed) {
            int i3 = paddingBottom;
            paddingBottom = paddingRight;
            paddingRight = i3;
        }
        if (mFixedCellWidth < 0 || mFixedCellHeight < 0) {
            int i4 = paddingRight / getCountX();
            int calculateCellHeight = DeviceProfile.calculateCellHeight(paddingBottom, getCountY());
            if (!(i4 == mCellWidth && calculateCellHeight == mCellHeight)) {
                mCellWidth = i4;
                mCellHeight = calculateCellHeight;
                getShortcutsAndWidgets().setCellDimensions(mCellWidth, mCellHeight, getCountX(), getCountY());
            }
        }
        int i5 = mFixedWidth;
        if (i5 > 0) {
            int i6 = mFixedHeight;
            if (i6 > 0) {
                paddingRight = i5;
                paddingBottom = i6;
                getShortcutsAndWidgets().measure(MeasureSpec.makeMeasureSpec(paddingRight, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(paddingBottom, MeasureSpec.EXACTLY));
                int measuredWidth = getShortcutsAndWidgets().getMeasuredWidth();
                int measuredHeight = getShortcutsAndWidgets().getMeasuredHeight();
                if (mFixedWidth > 0 || mFixedHeight <= 0) {
                    setMeasuredDimension(size, size2);
                } else {
                    setMeasuredDimension(measuredWidth, measuredHeight);
                }
                mWidth = -1;
            }
        }
        if (mode == 0 || mode2 == 0) {
            throw new RuntimeException("CellLayout cannot have UNSPECIFIED dimensions");
        }
        getShortcutsAndWidgets().measure(MeasureSpec.makeMeasureSpec(paddingRight, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(paddingBottom, MeasureSpec.EXACTLY));
        int measuredWidth2 = getShortcutsAndWidgets().getMeasuredWidth();
        int measuredHeight2 = getShortcutsAndWidgets().getMeasuredHeight();
        if (mFixedWidth > 0) {
        }
        setMeasuredDimension(size, size2);
        mWidth = -1;
    }

    @Override
    public void setInsets(Rect rect) {
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) getLayoutParams();
        DeviceProfile dp = mActivity.getWallpaperDeviceProfile();
        Rect insets = dp.getInsets();
        if (dp.isVerticalBarLayout()) {
            lp.height = -1;
            if (dp.isSeascape()) {
                lp.gravity = 3;
                lp.width = dp.hotseatBarSizePx + insets.left;
            } else {
                lp.gravity = 5;
                lp.width = dp.hotseatBarSizePx + insets.right;
            }
        } else {
            lp.gravity = 80;
            lp.width = -1;
            lp.height = dp.hotseatBarSizePx + insets.bottom;
        }
        Rect padding = dp.getHotseatLayoutPadding();
        setPadding(padding.left, padding.top, padding.right, padding.bottom);
        setLayoutParams(lp);
        InsettableFrameLayout.dispatchInsets(this, insets);
        mIsTransposed = mActivity.getRotationMode().isTransposed;
        setWillNotDraw(!mIsTransposed);
        AllAppsQsbControllerImpl allAppsQsb = null;
        if (mIsTransposed) {
            BaseDragLayer dragLayer = mActivity.getDragLayer();
            if (dragLayer != null) {
                allAppsQsb = dragLayer.findViewById(R.id.search_container_all_apps_plus);
            }
            mAllAppsQsb = allAppsQsb;
            return;
        }
        mAllAppsQsb = null;
    }
}
