package com.sapuseven.untis.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

/**
 * @author paul
 * @version 1.0
 * @since 2017-01-22
 */

public class IrregularLessonView extends View {
	private final float scale;
	private final Paint paint;

	public IrregularLessonView(Context context, AttributeSet attrs) {
		super(context, attrs);
		scale = context.getResources().getDisplayMetrics().density;
		paint = new Paint();
		paint.setColor(Color.RED);
		paint.setStrokeWidth(dp2px(3));
		paint.setAntiAlias(true);
		paint.setStrokeCap(Paint.Cap.ROUND);
	}

	@Override
	public void onDraw(Canvas canvas) {
		float width = getMeasuredWidth();
		float height = getMeasuredHeight();
		canvas.drawLine(dp2px(3), dp2px(3), width - dp2px(3), height - dp2px(3), paint);
		canvas.drawLine(width - dp2px(3), dp2px(3), dp2px(3), height - dp2px(3), paint);
	}

	@SuppressWarnings("SameParameterValue")
	private int dp2px(int dp) {
		return (int) (dp * scale + 0.5f);
	}
}