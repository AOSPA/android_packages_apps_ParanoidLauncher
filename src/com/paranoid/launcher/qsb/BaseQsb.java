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

import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.RippleDrawable;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.TextUtils.TruncateAt;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.android.launcher3.DeviceProfile;
import com.android.launcher3.Insettable;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.R;
import com.android.launcher3.Utilities;
import com.android.launcher3.graphics.NinePatchDrawHelper;
import com.android.launcher3.icons.ShadowGenerator.Builder;
import com.android.launcher3.util.Themes;
import com.android.launcher3.util.TransformingTouchDelegate;

import com.android.quickstep.WindowTransformSwipeHandler;

import com.paranoid.launcher.ParanoidLauncher;
import com.paranoid.launcher.ParanoidLauncherCallbacks;

public abstract class BaseQsb extends FrameLayout implements OnLongClickListener, Insettable {

    public static final Rect mSrcRect = new Rect();
    public final Paint mShadowPaint = new Paint(1);
    public final TransformingTouchDelegate mQsbDelegate;
    public final boolean mIsRtl;
    public View mMicIconView;
    public Paint mMicStrokePaint = new Paint(1);
    public Bitmap mShadowBitmap;
    private Bitmap mShadowBitmapAlpha;
    public final NinePatchDrawHelper mShadowHelper = new NinePatchDrawHelper();
    public float mMicStrokeWidth;

    public ParanoidLauncher mLauncher;

    public boolean mIsMainColorDark;
    private int mMicWidth;
    private int mShadowMargin;
    public int mColor;
    private int mColorAlpha;
    protected int mResult;

    public abstract int getMeasuredWidth(int width, DeviceProfile deviceProfile);

    public abstract void startSearch(String initialQuery, int result);

    public BaseQsb(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        mResult = 0;
        mLauncher = (ParanoidLauncher) Launcher.getLauncher(context);
        mIsMainColorDark = Themes.getAttrBoolean(mLauncher, R.attr.isMainColorDark);
        setOnLongClickListener(this);
        mMicWidth = getResources().getDimensionPixelSize(R.dimen.qsb_mic_width);
        mShadowMargin = getResources().getDimensionPixelSize(R.dimen.qsb_shadow_margin);
        mIsRtl = Utilities.isRtl(getResources());
        mQsbDelegate = new TransformingTouchDelegate(this);
        setTouchDelegate(mQsbDelegate);
        mShadowPaint.setColor(-1);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        mQsbDelegate.setDelegateView(mMicIconView);
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();
        mMicIconView = (View) findViewById(R.id.mic_icon);
        setTouchDelegate(mQsbDelegate);
        requestLayout();
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        if (motionEvent.getActionMasked() == 0) {
            View gIcon = findViewById(R.id.g_icon);
            int result = 0;
            int newResult = 1;
            if (mIsRtl) {
                if (Float.compare(motionEvent.getX(), (float) (gIcon.getLeft())) >= 0) {
                    result = 1;
                }
            } else {
                if (Float.compare(motionEvent.getX(), (float) (gIcon.getRight())) <= 0) {
                    result = 1;
                }
            }
            if (result == 0) {
                newResult = 2;
            }
            mResult = newResult;
        }
        return super.onTouchEvent(motionEvent);
    }

    @Override
    public void onLayout(boolean z, int i, int i2, int i3, int i4) {
        super.onLayout(z, i, i2, i3, i4);
        mMicIconView.getHitRect(mSrcRect);
        if (mIsRtl) {
            mSrcRect.left -= mShadowMargin;
        } else {
            mSrcRect.right += mShadowMargin;
        }
        mQsbDelegate.setBounds(mSrcRect.left, mSrcRect.top, mSrcRect.right, mSrcRect.bottom);
    }

    public void setColor(int color) {
        if (mColor != color) {
            mColor = color;
            mShadowBitmap = null;
            invalidate();
        }
    }

    public void setColorAlpha(int colorAlpha) {
        mColorAlpha = colorAlpha;
        if (mColorAlpha != mColor || mShadowBitmapAlpha != mShadowBitmap) {
            mShadowBitmapAlpha = null;
            invalidate();
        }
    }

    public void setMicPaint(float value) {
        mMicStrokeWidth = TypedValue.applyDimension(1, value, getResources().getDisplayMetrics());
        mMicStrokePaint.setStrokeWidth(mMicStrokeWidth);
        mMicStrokePaint.setStyle(Style.STROKE);
        mMicStrokePaint.setColor(-4341306);
    }

    @Override
    public void setInsets(Rect rect) {
        requestLayout();
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        DeviceProfile dp = mLauncher.getDeviceProfile();
        int round = Math.round(((float) dp.iconSizePx) * 0.92f);
        setMeasuredDimension(calculateMeasuredDimension(dp, round, widthMeasureSpec), View.MeasureSpec.getSize(heightMeasureSpec));
        for (int childCount = getChildCount() - 1; childCount >= 0; childCount--) {
            View childAt = getChildAt(childCount);
            measureChildWithMargins(childAt, widthMeasureSpec, 0, heightMeasureSpec, 0);
            if (childAt.getWidth() <= round) {
                LayoutParams layoutParams = (LayoutParams) childAt.getLayoutParams();
                int measuredWidth = (round - childAt.getWidth()) / 2;
                layoutParams.rightMargin = measuredWidth;
                layoutParams.leftMargin = measuredWidth;
            }
        }
    }

    public int calculateMeasuredDimension(DeviceProfile dp, int round, int widthMeasureSpec) {
        int width = getMeasuredWidth(MeasureSpec.getSize(widthMeasureSpec), dp);
        int calculateCellWidth = width - ((width / dp.inv.numHotseatIcons) - round);
        return getPaddingRight() + getPaddingLeft() + calculateCellWidth;
    }

    public void loadBitmap() {
        if (mShadowBitmap == null) {
            mShadowBitmap = getShadowBitmap(mColor);
        }
    }

    @Override
    public void draw(Canvas canvas) {
        drawCanvas(canvas, getWidth());
        super.draw(canvas);
    }

    public void drawCanvas(Canvas canvas, int width) {
        Canvas qsb = canvas;
        loadBitmap();
        drawShadow(mShadowBitmap, qsb);
        if (mMicStrokeWidth > WindowTransformSwipeHandler.SWIPE_DURATION_MULTIPLIER && mMicIconView.getVisibility() == 0) {
            int paddingLeft = mIsRtl ? getPaddingLeft() : (width - getPaddingRight()) - getMicWidth();
            int paddingTop = getPaddingTop();
            int paddingRight = mIsRtl ? getPaddingLeft() + getMicWidth() : width - getPaddingRight();
            int paddingBottom = LauncherAppState.getIDP(getContext()).iconBitmapSize - getPaddingBottom();
            float height = ((float) (paddingBottom - paddingTop)) * 0.5f;
            int micStrokeWidth = (int) (mMicStrokeWidth / 2.0f);
            if (mMicStrokePaint == null) {
                mMicStrokePaint = new Paint(1);
            }
            mMicStrokePaint.setColor(-4341306);
            canvas.drawRoundRect((float) (paddingLeft + micStrokeWidth), (float) (paddingTop + micStrokeWidth), (float) (paddingRight - micStrokeWidth), (float) ((paddingBottom - micStrokeWidth) + 1), height, height, mMicStrokePaint);
        }
    }

    public void drawShadow(Bitmap bitmap, Canvas canvas) {
        int shadowDimens = getShadowDimens(bitmap);
        int paddingTop = getPaddingTop() - ((bitmap.getHeight() - getHeightWithoutPadding()) / 2);
        int paddingLeft = getPaddingLeft() - shadowDimens;
        int width = (getWidth() - getPaddingRight()) + shadowDimens;
        if (mIsRtl) {
            paddingLeft += getRtlDimens();
        } else {
            width -= getRtlDimens();
        }
        mShadowHelper.draw(bitmap, canvas, (float) paddingLeft, (float) paddingTop, (float) width);
    }

    public Bitmap getShadowBitmap(int color) {
        float bitmapSize = (float) LauncherAppState.getInstance(getContext()).getInvariantDeviceProfile().iconBitmapSize;
        return createBitmap(0.010416667f * bitmapSize, 0.020833334f * bitmapSize, color);
    }

    public Bitmap createBitmap(float shadowBlur, float keyShadowDistance, int color) {
        int height = getHeightWithoutPadding();
        int heightSpec = height + 20;
        Builder builder = new Builder(color);
        builder.shadowBlur = shadowBlur;
        builder.keyShadowDistance = keyShadowDistance;
        if (mIsMainColorDark) {
            builder.ambientShadowAlpha = (int) (((float) builder.ambientShadowAlpha) * 2.8E-45f);
        }
        builder.keyShadowAlpha = builder.ambientShadowAlpha;
        Bitmap pill = builder.createPill(heightSpec, height);
        if (Utilities.ATLEAST_OREO) {
            return pill.copy(Config.HARDWARE, false);
        }
        return pill;
    }

    public int getShadowDimens(Bitmap bitmap) {
        return (bitmap.getWidth() - (getHeightWithoutPadding() + 20)) / 2;
    }

    public int getHeightWithoutPadding() {
        return (getHeight() - getPaddingTop()) - getPaddingBottom();
    }

    public int getRtlDimens() {
        return 0;
    }

    public int getMicWidth() {
        return mMicWidth;
    }

    public void setMicRipple() {
        int width;
        int height;
        int micWidth;
        int micHeight;
        InsetDrawable insetDrawable = (InsetDrawable) getResources().getDrawable(R.drawable.bg_qsb_click_feedback).mutate();
        RippleDrawable rippleDrawable = (RippleDrawable) insetDrawable.getDrawable();
        if (mIsRtl) {
            width = getRtlDimens();
        } else {
            width = 0;
        }
        if (mIsRtl) {
            height = 0;
        } else {
            height = getRtlDimens();
        }
        rippleDrawable.setLayerInset(0, width, 0, height, 0);
        setBackground(insetDrawable);
        RippleDrawable rippleDrawable2 = (RippleDrawable) rippleDrawable.getConstantState().newDrawable().mutate();
        int shadowMargin = mShadowMargin;
        rippleDrawable2.setLayerInset(0, 0, shadowMargin, 0, shadowMargin);
        mMicIconView.setBackground(rippleDrawable2);
        mMicIconView.getLayoutParams().width = getMicWidth();
        if (mIsRtl) {
            micWidth = 0;
        } else {
            micWidth = getMicWidth() - mMicWidth;
        }
        if (mIsRtl) {
            micHeight = getMicWidth() - mMicWidth;
        } else {
            micHeight = 0;
        }
        mMicIconView.setPadding(micWidth, 0, micHeight, 0);
        mMicIconView.requestLayout();
    }

    public boolean onLongClick(View view) {
        if (view != this) {
            return false;
        }
        return isClipboard();
    }

    public boolean isClipboard() {
        String clipboardText = getClipboardText();
        Intent settingsIntent = createSettingsIntent();
        if (settingsIntent == null && clipboardText == null) {
            return false;
        }
        startActionMode(new ActionModeController(this, clipboardText, settingsIntent), 1);
        return true;
    }

    public String getClipboardText() {
        ClipData primaryClip = ((ClipboardManager) ContextCompat.getSystemService(getContext(), ClipboardManager.class)).getPrimaryClip();
        if (primaryClip != null) {
            for (int i = 0; i < primaryClip.getItemCount(); i++) {
                CharSequence text = primaryClip.getItemAt(i).coerceToText(getContext());
                if (!TextUtils.isEmpty(text)) {
                    return text.toString();
                }
            }
        }
        return null;
    }

    public Intent createSettingsIntent() {
        return null;
    }

    public void fallbackSearch(String action) {
        try {
            getContext().startActivity(new Intent(action)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK)
                .setPackage(ParanoidLauncherCallbacks.SEARCH_PACKAGE));
        } catch (ActivityNotFoundException e) {
        }
    }
}
