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

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.android.systemui.shared.recents.model.Task;

import com.android.launcher3.R;
import com.android.launcher3.PagedView;
import com.android.launcher3.anim.Interpolators;
import com.android.launcher3.uioverrides.UiFactory;
import com.android.launcher3.util.ViewPool;

import com.android.quickstep.RecentsModel;
import com.android.quickstep.TaskIconCache;

import java.util.function.Consumer;

public class TaskIcon extends ImageView implements PagedView.PageCallbacks, ViewPool.Reusable {

    private static final String TAG = TaskIcon.class.getSimpleName();
    public static final float MAX_PAGE_SCRIM_ALPHA = 0.6f;

    private BitmapDrawable mBitmapDrawable;
    private TaskIconCache.IconLoadRequest mIconLoadRequest;
    private int mIconSize;
    private final Paint mOverlayPaint;
    private final Rect mOverlayRect;
    private final Paint mPaint;
    private Task mTask;
    private AnimatorSet mTaskIconBounceAnimation;

    public TaskIcon(Context context) {
        this(context, null);
    }

    public TaskIcon(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public TaskIcon(Context context, AttributeSet attributeSet, int res) {
        super(context, attributeSet, res);
        mPaint = new Paint(3);
        mOverlayPaint = new Paint(3);
        mIconSize = getResources().getDimensionPixelSize(R.dimen.task_icon_size);
        mOverlayRect = new Rect(0, 0, mIconSize, mIconSize);
        mOverlayPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));
        mOverlayPaint.setColor(-16777216);
        setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                UiFactory.getRecentsOperationController().onTaskIconsClick(getContext(), getTaskIconsView());
                playTaskIconBounceAnimation();
            }
        });
    }

    public void bind(Task task) {
        cancelPendingLoadTasks();
        mTask = task;
    }

    public void unbind() {
        cancelPendingLoadTasks();
        recycleTaskIcon(mTask);
        mTask = null;
    }

    private void recycleTaskIcon(Task task) {
        Drawable drawable = getDrawable();
        if (task != null && drawable != null) {
            drawable.setCallback(null);
            setImageDrawable(null);
        }
    }

    public Task getTask() {
        return mTask;
    }

    public void updateTaskIcon() {
        if (mTask != null) {
            cancelPendingLoadTasks();
            mIconLoadRequest = RecentsModel.INSTANCE.lambda$get$0$MainThreadInitializedObject(getContext()).getIconCache().updateIconInBackground(mTask, new Consumer() {
                @Override
                public void accept(Object obj) {
                    setIcon((Task) obj.icon);
                    invalidate();
                }
            });
        }
    }

    private void cancelPendingLoadTasks() {
        if (mIconLoadRequest != null) {
            mIconLoadRequest.cancel();
            mIconLoadRequest = null;
        }
    }

    private void setIcon(Drawable drawable) {
        if (drawable == null || mTask == null) {
            mTask.icon = null;
            return;
        }
        drawable.setBounds(0, 0, mIconSize, mIconSize);
        mBitmapDrawable = (BitmapDrawable) drawable;
        Paint paint = mBitmapDrawable.getPaint();
        paint.setAntiAlias(true);
        paint.setFilterBitmap(true);
        paint.setColor(-16777216);
        mTask.icon = mBitmapDrawable;
    }

    public void resetViewStatus() {
        setTranslationX(0.0f);
        setTranslationY(0.0f);
        setAlpha(1.0f);
    }

    public void onPageScroll(PagedView.ScrollState scrollState) {
        mOverlayPaint.setAlpha((int) (Interpolators.FAST_OUT_SLOW_IN.getInterpolation(scrollState.linearInterpolation) * 0.6f * 255.0f));
        invalidate();
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mBitmapDrawable != null) {
            int saveLayer = canvas.saveLayer(null, null);
            canvas.drawBitmap(mBitmapDrawable.getBitmap(), (Rect) null, mOverlayRect, mPaint);
            canvas.drawRect(mOverlayRect, mOverlayPaint);
            canvas.restoreToCount(saveLayer);
        }
    }

    public void onRecycle() {
        resetViewStatus();
        invalidate();
    }

    public TaskIconsView getTaskIconsView() {
        return (TaskIconsView) getParent();
    }

    public void playTaskIconBounceAnimation() {
        Log.d(TAG, "playTaskIconBounceAnimation: play bounce.");
        if (mTaskIconBounceAnimation != null) {
            mTaskIconBounceAnimation.cancel();
        }
        mTaskIconBounceAnimation = new AnimatorSet();
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(this, View.SCALE_X, 1.0f, 0.8f).setDuration(150);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(this, View.SCALE_Y, 1.0f, 0.8f).setDuration(150);
        ObjectAnimator scaleXDelay = ObjectAnimator.ofFloat(this, View.SCALE_X, 0.8f, 1.0f).setDuration(150);
        scaleXDelay.setStartDelay(150);
        ObjectAnimator scaleYDelay = ObjectAnimator.ofFloat(this, View.SCALE_Y, 0.8f, 1.0f).setDuration(150);
        scaleYDelay.setStartDelay(150);
        mTaskIconBounceAnimation.setInterpolator(Interpolators.PATH_3_0_7_1);
        mTaskIconBounceAnimation.playTogether(scaleX, scaleY, scaleXDelay, scaleYDelay);
        mTaskIconBounceAnimation.start();
    }
}
