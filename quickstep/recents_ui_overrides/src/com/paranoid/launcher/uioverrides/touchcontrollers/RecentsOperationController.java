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
package com.paranoid.launcher.uioverrides.touchcontrollers;

import android.content.Context;
import android.util.Log;
import android.view.HapticFeedbackConstants;

import com.android.launcher3.BaseActivity;
import com.android.launcher3.BaseDraggingActivity;
import com.android.launcher3.anim.Interpolators;
import com.android.quickstep.views.RecentsView;

import com.paranoid.quickstep.views.TaskIconsIndicatorDots;
import com.paranoid.quickstep.views.TaskIconsView;

public class RecentsOperationController {

    private static final String TAG = "RecentsOperationController";

    private final int SNAP_TASK_DURATION = 425;
    private int mVibratePage = -1;

    public static int getReverseIndex(int childCount, int position) {
        return (childCount - position) - 1;
    }

    public void onRecentsScroll(BaseActivity activity) {
        if (activity instanceof BaseDraggingActivity) {
            BaseDraggingActivity baseDraggingActivity = (BaseDraggingActivity) activity;
            RecentsView recentsView = (RecentsView) baseDraggingActivity.getOverviewPanel();
            if (recentsView == null) {
                Log.w(TAG, "onRecentsScroll: recentsView is null.");
                return;
            }
            TaskIconsView taskIconsView = baseDraggingActivity.getTaskIconsView();
            if (taskIconsView == null) {
                Log.w(TAG, "onRecentsScroll: taskIconsView is null.");
                return;
            }
            taskIconsView.snapToPage(getReverseIndex(taskIconsView.getChildCount(), recentsView.getPageNearestToCenterOfScreen()));
            resetIndicator(activity);
            return;
        }
        Log.w(TAG, "onRecentsScroll: activity= " + activity + ", return");
    }

    public void onTaskIconsScroll(BaseActivity activity) {
        if (activity instanceof BaseDraggingActivity) {
            BaseDraggingActivity baseDraggingActivity = (BaseDraggingActivity) activity;
            RecentsView recentsView = (RecentsView) baseDraggingActivity.getOverviewPanel();
            if (recentsView == null) {
                Log.w(TAG, "onTaskIconsScroll: recentsView is null.");
                return;
            }
            TaskIconsView taskIconsView = baseDraggingActivity.getTaskIconsView();
            if (taskIconsView == null) {
                Log.w(TAG, "onTaskIconsScroll: taskIconsView is null.");
                return;
            }
            recentsView.snapToPage(getReverseIndex(recentsView.getChildCount(), taskIconsView.getPageNearestToCenterOfScreen()));
            resetIndicator(activity);
            return;
        }
        Log.w(TAG, "onTaskIconsScroll: activity= " + activity + ", return");
    }

    public void onTaskIconsDrag(BaseActivity activity) {
        int reverseIndex;
        if (activity instanceof BaseDraggingActivity) {
            BaseDraggingActivity baseDraggingActivity = (BaseDraggingActivity) activity;
            RecentsView recentsView = (RecentsView) baseDraggingActivity.getOverviewPanel();
            if (recentsView == null) {
                Log.w(TAG, "onTaskIconsDrag: recentsView is null.");
                return;
            }
            TaskIconsView taskIconsView = baseDraggingActivity.getTaskIconsView();
            if (taskIconsView == null) {
                Log.w(TAG, "onTaskIconsDrag: taskIconsView is null.");
                return;
            }
            int pageNearestToCenterOfScreen = taskIconsView.getPageNearestToCenterOfScreen();
            int pageNearestToCenterOfScreen2 = recentsView.getPageNearestToCenterOfScreen();
            updateIndicator(activity, taskIconsView.getCurrentPage(), pageNearestToCenterOfScreen);
            if (pageNearestToCenterOfScreen != getReverseIndex(recentsView.getChildCount(), pageNearestToCenterOfScreen2) 
                        && recentsView.getCurrentPage() != (reverseIndex = getReverseIndex(recentsView.getChildCount(), pageNearestToCenterOfScreen)) 
                        && mVibratePage != reverseIndex) {
                mVibratePage = reverseIndex;
                recentsView.snapToPage(reverseIndex, 425, Interpolators.DEACCEL_2);
                recentsView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                return;
            }
            return;
        }
        Log.w(TAG, "onTaskIconsDrag: activity= " + activity + ", return");
    }

    public void onTaskIconsClick(Context context, int position) {
        try {
            BaseDraggingActivity baseDraggingActivity = (BaseDraggingActivity) BaseDraggingActivity.fromContext(context);
            RecentsView recentsView = (RecentsView) baseDraggingActivity.getOverviewPanel();
            if (recentsView == null) {
                Log.w(TAG, "onTaskIconsClick: recentsView is null.");
                return;
            }
            TaskIconsView taskIconsView = baseDraggingActivity.getTaskIconsView();
            if (taskIconsView == null) {
                Log.w(TAG, "onTaskIconsClick: taskIconsView is null.");
                return;
            }
            int reverseIndex = getReverseIndex(recentsView.getChildCount(), position);
            if (recentsView.getCurrentPage() != reverseIndex) {
                taskIconsView.snapToPage(position, 425);
                recentsView.snapToPage(reverseIndex, 425);
                return;
            }
            recentsView.playTaskBounceAnimation(reverseIndex);
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "onTaskIconsClick: " + e.getMessage() + ", return");
        }
    }

    public void startHome(BaseActivity activity) {
        if (activity instanceof BaseDraggingActivity) {
            RecentsView recentsView = (RecentsView) ((BaseDraggingActivity) activity).getOverviewPanel();
            if (recentsView == null) {
                Log.w(TAG, "startHome: recentsView is null.");
            } else {
                recentsView.startHome();
            }
        } else {
            Log.w(TAG, "startHome: activity= " + activity + ", return");
        }
    }

    private void resetIndicator(BaseActivity activity) {
        updateIndicator(activity, 1, 1);
    }

    public void updateIndicatorForRecents(BaseActivity activity, int i, int i2) {
        updateIndicator(activity, i2, i);
    }

    private void updateIndicator(BaseActivity activity, int i, int i2) {
        if (activity instanceof BaseDraggingActivity) {
            TaskIconsIndicatorDots overviewIndicator = ((BaseDraggingActivity) activity).getOverviewIndicator();
            if (overviewIndicator == null) {
                Log.w(TAG, "updateIndicator: pageIndicator is null.");
            } else if (overviewIndicator != null) {
                overviewIndicator.setScroll(i, i2);
            } else {
                Log.w(TAG, "updateIndicator: pageIndicator is null.");
            }
        } else {
            Log.w(TAG, "updateIndicator: activity= " + activity + ", return");
        }
    }

    public int getTaskIconsCurrentPage(BaseActivity activity) {
        if (activity instanceof BaseDraggingActivity) {
            RecentsView recentsView = (RecentsView) ((BaseDraggingActivity) activity).getOverviewPanel();
            if (recentsView == null) {
                Log.w(TAG, "getTaskIconsCurrentPage: recentsView is null.");
                return -1;
            }
            return getReverseIndex(recentsView.getChildCount(), recentsView.getPageNearestToCenterOfScreen());
        }
        Log.w(TAG, "getTaskIconsCurrentPage: activity= " + activity + ", return");
        return -1;
    }
}
