package com.sapuseven.untis.view;

import android.content.Context;
import android.graphics.Canvas;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.Gravity;

/**
 * @author paul
 * @version 1.0
 * @since 2016-11-02
 */

public class VerticalTextView extends android.support.v7.widget.AppCompatTextView {
	private final boolean topDown;

	public VerticalTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
		final int gravity = getGravity();
		if (Gravity.isVertical(gravity) && (gravity & Gravity.VERTICAL_GRAVITY_MASK) == Gravity.BOTTOM) {
			setGravity((gravity & Gravity.HORIZONTAL_GRAVITY_MASK) | Gravity.TOP);
			topDown = false;
		} else
			topDown = true;
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		//noinspection SuspiciousNameCombination
		super.onMeasure(heightMeasureSpec, widthMeasureSpec);
		setMeasuredDimension(getMeasuredHeight(), getMeasuredWidth());
	}

	@Override
	protected void onDraw(Canvas canvas) {
		TextPaint textPaint = getPaint();
		textPaint.setColor(getCurrentTextColor());
		textPaint.drawableState = getDrawableState();

		canvas.save();

		if (topDown) {
			canvas.translate(getWidth(), 0);
			canvas.rotate(90);
		} else {
			canvas.translate(0, getHeight());
			canvas.rotate(-90);
		}


		canvas.translate(getCompoundPaddingLeft(), getExtendedPaddingTop());

		getLayout().draw(canvas);
		canvas.restore();
	}
}