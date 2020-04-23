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
package com.paranoid.quickstep.views;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;

import com.android.launcher3.R;
import com.android.launcher3.pageindicators.PageIndicatorDots;

public class TaskIconsIndicatorDots extends PageIndicatorDots {

    public static final int SCROLL_DESTINATION = 1;

    public TaskIconsIndicatorDots(Context context) {
        this(context, null);
    }

    public TaskIconsIndicatorDots(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public TaskIconsIndicatorDots(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        mNumPages = 3;
        mActivePage = 1;
        setScroll(1, 1);
        mActiveColor = context.getResources().getColor(R.color.light_page_active_indicator_dot_color, null);
        mInActiveColor = context.getResources().getColor(android.R.color.transparent, null);
        mDotRadius = getResources().getDimension(R.dimen.task_icons_page_indicator_dot_size) / 2.0f;
    }

    @Override
    public void setScroll(int currentScroll, int totalScroll) {
        if (currentScroll < totalScroll) {
            animateToPosition(((float) mActivePage) - 0.5f);
        } else if (currentScroll > totalScroll) {
            animateToPosition(((float) mActivePage) + 0.5f);
        } else {
            animateToPosition(1.0f);
        }
    }
}
