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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;

import com.android.launcher3.BaseActivity;
import com.android.launcher3.R;
import com.android.launcher3.DeviceProfile;
import com.android.launcher3.LauncherAnimUtils;
import com.android.launcher3.PagedView;
import com.android.launcher3.Utilities;
import com.android.launcher3.anim.Interpolators;
import com.android.launcher3.anim.SpringObjectAnimator;
import com.android.launcher3.config.FeatureFlags;
import com.android.launcher3.uioverrides.UiFactory;
import com.android.launcher3.util.ViewPool;

import com.android.systemui.shared.recents.model.Task;

import java.util.ArrayList;

public class TaskIconsView extends PagedView implements PagedView.PageCallbacks {

    private static final String TAG = "TaskIconsView";
    private int mActivePointerId;
    protected final BaseActivity mActivity;
    private int mDownX;
    private int mDownY;
    private final int mIconSize;
    private final ViewPool<TaskIcon> mIconViewPool;
    private RecentsView mRecentsView;
    private final PagedView.ScrollState mScrollState;
    private final Rect mTaskIconViewDeadZoneRect;
    private final int mTouchSlop;
    private VelocityTracker mVelocityTracker;

    public TaskIconsView(Context context) {
        this(context, null);
    }

    public TaskIconsView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        mRecentsView = null;
        mScrollState = new PagedView.ScrollState();
        mActivePointerId = -1;
        mTaskIconViewDeadZoneRect = new Rect();
        mIconViewPool = new ViewPool(context, this, R.layout.task_icon, 20, 10);
        mIconSize = getResources().getDimensionPixelSize(R.dimen.task_icon_size);
        mActivity = BaseActivity.fromContext(context);
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        setEnableFreeScroll(true);
        setLayoutDirection(Utilities.isRtl(getResources()) ? 1 : 0);
        setPageSpacing(getResources().getDimensionPixelSize(R.dimen.task_icons_page_spacing));
        updatePadding();
        mVelocityTracker = VelocityTracker.obtain();
    }

    public void updatePadding() {
        if (mActivity == null) {
            Log.w("TaskIconsView", "updatePadding: mActivity is null");
            return;
        }
        DeviceProfile dp = mActivity.getDeviceProfile();
        if (dp == null) {
            Log.w("TaskIconsView", "updatePadding: DeviceProfile is null");
            return;
        }
        int size = (dp.widthPx - mIconSize) / 2;
        setPadding(size, 0, size, 0);
    }

    public int getVelocityX(int i) {
        if (isInOverScroll()) {
            return 0;
        }
        return (int) (((float) (-i)) * 0.2f);
    }

    public void setRecentsView(RecentsView recentsView) {
        mRecentsView = recentsView;
    }

    public TaskIcon getTaskIcon(int taskId) {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            TaskIcon taskIcon = (TaskIcon) getChildAt(i);
            if (taskIcon != null && taskIcon.getTask() != null 
                        && taskIcon.getTask().key != null 
                        && taskIcon.getTask().key.id == taskId) {
                return taskIcon;
            }
        }
        return null;
    }

    public void applyLoadPlan(ArrayList<Task> tasks) {
        unloadVisibleTaskData();
        int size = tasks.size();
        Log.d("TaskIconsView", "applyLoadPlan: task icon size= " + size);
        for (int childCount = getChildCount(); childCount < size; childCount++) {
            addView(mIconViewPool.getView());
        }
        while (getChildCount() > size) {
            removeView(getChildAt(getChildCount() - 1));
        }
        for (int i = 0; i < size; i++) {
            TaskIcon taskIcon = (TaskIcon) getChildAt(i);
            taskIcon.bind(tasks.get(i));
            taskIcon.updateTaskIcon();
        }
        if (getVisibility() == 0) {
            showCurrentTask();
        }
        resetTaskIconsVisuals();
    }

    private void unloadVisibleTaskData() {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            ((TaskIcon) getChildAt(i)).unbind();
        }
    }

    @Override
    public void onViewRemoved(View view) {
        super.onViewRemoved(view);
        TaskIcon taskIcon = (TaskIcon) view;
        taskIcon.unbind();
        mIconViewPool.recycle(taskIcon);
    }

    public void onTaskRemoved(int index, int page) {
        boolean z = true;
        boolean z2 = index < page;
        if (index == 0 || index != page) {
            z = false;
        }
        int i3 = (z2 || z) ? page - 1 : page;
        Log.d("TaskIconsView", "onTaskRemoved: removedIndex= " + index + ", currentPage= " + page + ", removeLeftTask= " + z2 + ", removeCurrentTask= " + z + ", pageToSnapTo= " + i3);
        if (getChildCount() > 0) {
            snapToPageImmediately(i3);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        if (mRecentsView != null && !mRecentsView.isInOverView()) {
            return false;
        }
        super.onTouchEvent(motionEvent);
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(motionEvent);
        int x = (int) motionEvent.getX();
        int y = (int) motionEvent.getY();
        int action = motionEvent.getAction();
        if (action != 0) {
            if (action != 1) {
                if (action == 2) {
                    if (mTouchDownToStartHome && Math.hypot((double) (mDownX - x), (double) (mDownY - y)) > ((double) mTouchSlop)) {
                        mTouchDownToStartHome = false;
                    }
                    UiFactory.getRecentsOperationController().onTaskIconsDrag(mActivity);
                }
            }
            if (mTouchDownToStartHome) {
                UiFactory.getRecentsOperationController().startHome(mActivity);
            } else {
                UiFactory.getRecentsOperationController().onTaskIconsScroll(mActivity);
            }
            mTouchDownToStartHome = false;
        } else {
            updateDeadZoneRects();
            if (!mTaskIconViewDeadZoneRect.contains(getScrollX() + x, y)) {
                mTouchDownToStartHome = true;
            }
            mDownX = x;
            mDownY = y;
            mActivePointerId = motionEvent.getPointerId(0);
        }
        return true;
    }

    private boolean isDragging() {
        mVelocityTracker.computeCurrentVelocity(1000, (float) ViewConfiguration.get(getContext()).getScaledMaximumFlingVelocity());
        float xVelocity = mVelocityTracker.getXVelocity(mActivePointerId);
        mVelocityTracker.recycle();
        mVelocityTracker = null;
        return Math.abs(xVelocity) < ((float) mFlingThresholdVelocity);
    }

    private void updateDeadZoneRects() {
        mTaskIconViewDeadZoneRect.setEmpty();
        int childCount = getChildCount();
        if (childCount > 0) {
            View childAt = getChildAt(0);
            getChildAt(childCount - 1).getHitRect(mTaskIconViewDeadZoneRect);
            mTaskIconViewDeadZoneRect.union(childAt.getLeft(), childAt.getTop(), childAt.getRight(), childAt.getBottom());
            mTaskIconViewDeadZoneRect.inset(-40, -40);
        }
    }

    @Override
    public boolean computeScrollHelper() {
        updateCurveProperties();
        return super.computeScrollHelper();
    }

    public void updateCurveProperties() {
        if (getPageCount() != 0) {
            if (getPageAt(0).getMeasuredWidth() != 0) {
                int normalChildWidth = getNormalChildWidth() / 2;
                int paddingLeft = mInsets.left + getPaddingLeft() + getScrollX() + normalChildWidth;
                float normalChildWidth2 = (float) (getNormalChildWidth() + mPageSpacing);
                int pageCount = getPageCount();
                for (int i = 0; i < pageCount; i++) {
                    View childAt = getChildAt(i);
                    float left = ((float) paddingLeft) - ((float) (childAt.getLeft() + normalChildWidth));
                    mScrollState.linearInterpolation = Math.min(1.0f, Math.abs(left) / normalChildWidth2);
                    ((PagedView.PageCallbacks) childAt).onPageScroll(mScrollState);
                }
            }
        }
    }

    public void resetTaskIconsVisuals() {
        for (int i = 0; i < getChildCount(); i++) {
            TaskIcon taskIcon = (TaskIcon) getChildAt(i);
            if (taskIcon != null) {
                taskIcon.resetViewStatus();
            }
        }
        updateCurveProperties();
    }

    public void updateTaskIconsAlpha(float f) {
        for (int i = 0; i < getChildCount(); i++) {
            TaskIcon taskIcon = (TaskIcon) getChildAt(i);
            if (taskIcon != null) {
                taskIcon.setAlpha(f);
            }
        }
    }

    public void release() {
        unloadVisibleTaskData();
    }

    public void playTaskIconDismissAnimation(TaskIcon taskIcon) {
        int i;
        int i2;
        int i3;
        int i4;
        final TaskIcon taskIcon2 = taskIcon;
        AnimatorSet animatorSet = new AnimatorSet();
        int childCount = getChildCount();
        if (childCount != 0) {
            int[] iArr = new int[childCount];
            int i5 = 0;
            getPageScrolls(iArr, false, SIMPLE_SCROLL_LOGIC);
            int[] iArr2 = new int[childCount];
            getPageScrolls(iArr2, false, new PagedView.ComputePageScrollsLogic() {
                @Override
                public boolean shouldIncludeView(View view) {
                    return view.getVisibility() == View.GONE || view == taskIcon;
                }
            });
            int i6 = 1;
            int abs = childCount > 1 ? Math.abs(iArr[1] - iArr[0]) : 0;
            final int i7 = mCurrentPage;
            final int indexOfChild = indexOfChild(taskIcon);
            int i8 = 0;
            int i9 = 0;
            while (i8 < childCount) {
                View childAt = getChildAt(i8);
                if (childAt == taskIcon2) {
                    addDismissedTaskAnimations(taskIcon2, animatorSet);
                    i2 = i5;
                    i3 = i6;
                    i = abs;
                } else {
                    int i10 = mIsRtl ? i5 : abs;
                    if (mCurrentPage == indexOfChild) {
                        if (mCurrentPage == 0) {
                        }
                        i4 = (iArr2[i8] - iArr[i8]) + i10;
                        if (i4 != 0) {
                            i = abs;
                            if (!FeatureFlags.QUICKSTEP_SPRINGS.get() || !(childAt instanceof TaskIcon)) {
                                i3 = 1;
                                i2 = 0;
                                addAnim(ObjectAnimator.ofFloat(childAt, TRANSLATION_X, (float) i4), 300, Interpolators.PATH_4_0_3_1, animatorSet);
                            } else {
                                addAnim(new SpringObjectAnimator(childAt, LauncherAnimUtils.VIEW_TRANSLATE_X, 1.0f, 0.5f, 1500.0f, 0.0f, (float) i4), 300, Interpolators.PATH_4_0_3_1, animatorSet);
                                i3 = 1;
                                i2 = 0;
                            }
                            i9 = i3;
                        } else {
                            i3 = i6;
                            i = abs;
                            i2 = 0;
                        }
                    } else {
                        if (indexOfChild == mCurrentPage + i6) {
                        }
                        i4 = (iArr2[i8] - iArr[i8]) + i10;
                        if (i4 != 0) {
                        }
                    }
                    int i11 = abs;
                    i10 += i11;
                    i4 = (iArr2[i8] - iArr[i8]) + i10;
                    if (i4 != 0) {
                    }
                }
                i8++;
                i6 = i3;
                i5 = i2;
                abs = i;
            }
            if (i9 != 0) {
                ValueAnimator ofFloat = ValueAnimator.ofFloat(0.0f, 1.0f);
                ofFloat.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator valueAnimator) {
                        updateCurveProperties();
                    }
                });
                animatorSet.play(ofFloat);
            }
            if (taskIcon2 != null) {
                taskIcon2.setTranslationZ(0.1f);
            }
            animatorSet.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animator) {
                    removeView(taskIcon2);
                    onTaskRemoved(indexOfChild, i7);
                    resetTaskIconsVisuals();
                }
            });
            animatorSet.start();
        }
    }

    private void addDismissedTaskAnimations(View view, AnimatorSet animatorSet) {
        addAnim(ObjectAnimator.ofFloat(view, ALPHA, 0.0f), 300, Interpolators.ACCEL_2, animatorSet);
        if (!FeatureFlags.QUICKSTEP_SPRINGS.get() || !(view instanceof TaskIcon)) {
            addAnim(ObjectAnimator.ofFloat(view, TRANSLATION_Y, (float) (-view.getHeight())), 300, Interpolators.LINEAR, animatorSet);
            return;
        }
        addAnim(new SpringObjectAnimator(view, LauncherAnimUtils.VIEW_TRANSLATE_Y, 1.0f, 0.5f, 1500.0f, 0.0f, (float) (-view.getHeight())), 300, Interpolators.LINEAR, animatorSet);
    }

    public void showCurrentTask() {
        int taskIconsCurrentPage;
        int childCount = getChildCount() - 1;
        if (!(mRecentsView == null || (taskIconsCurrentPage = UiFactory.getRecentsOperationController().getTaskIconsCurrentPage(mActivity)) == -1)) {
            childCount = taskIconsCurrentPage;
        }
        setCurrentPage(childCount);
    }

    @Override
    public void onLayout(boolean z, int i, int i2, int i3, int i4) {
        super.onLayout(z, i, i2, i3, i4);
        showCurrentTask();
    }
}
