package de.perflyst.untis.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import static de.perflyst.untis.utils.Conversions.dp2px;

public class TimetableItemBackground extends View {
	private final Paint topPaint = new Paint();
	private final Paint bottomPaint = new Paint();
	private final Paint dividerPaint = new Paint();
	private final Paint indicatorPaint = new Paint();
	private final Path indicatorPath = new Path();
	private int dividerPosition;
	private boolean drawIndicator = false;
	private Paint maskPaint;
	private Paint paint;
	private Bitmap maskBitmap;
	private float cornerRadius = 0;

	public TimetableItemBackground(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	private void init() {
		paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		maskPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
		maskPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

		setWillNotDraw(false);
	}

	@Override
	public void draw(Canvas canvas) {
		int width = canvas.getWidth();
		int height = canvas.getHeight();

		Bitmap offscreenBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		Canvas offscreenCanvas = new Canvas(offscreenBitmap);

		super.draw(canvas);

		if (dividerPosition == height) {
			offscreenCanvas.drawRect(0, 0, width, height, bottomPaint);
		} else if (dividerPosition == 0) {
			offscreenCanvas.drawRect(0, 0, width, height, topPaint);
		} else {
			offscreenCanvas.drawRect(0, 0, width, height - dividerPosition, topPaint);
			offscreenCanvas.drawRect(0, height - dividerPosition, width, height, bottomPaint);
			offscreenCanvas.drawRect(0, height - dividerPosition - dp2px(1), width, height - dividerPosition + dp2px(1), dividerPaint);
		}

		if (drawIndicator) {
			if (indicatorPath.isEmpty()) {
				indicatorPath.moveTo(width - dp2px(8), height);
				indicatorPath.lineTo(width, height);
				indicatorPath.lineTo(width, height - dp2px(8));
				indicatorPath.close();
			}

			offscreenCanvas.drawPath(indicatorPath, indicatorPaint);
		}

		if (maskBitmap == null)
			maskBitmap = createMask(canvas.getWidth(), canvas.getHeight());

		offscreenCanvas.drawBitmap(maskBitmap, 0, 0, maskPaint);
		canvas.drawBitmap(offscreenBitmap, 0, 0, paint);
	}

	public void setTopColor(int topColor) {
		topPaint.setColor(topColor);
	}

	public void setBottomColor(int bottomColor) {
		bottomPaint.setColor(bottomColor);
	}

	public void setDividerColor(int dividerColor) {
		dividerPaint.setColor(dividerColor);
	}

	public void setDividerPosition(int dividerPosition) {
		this.dividerPosition = dividerPosition;
	}

	public void setIndicatorColor(int color) {
		this.drawIndicator = true;
		this.indicatorPaint.setStyle(Paint.Style.FILL);
		this.indicatorPaint.setColor(color);
	}

	private Bitmap createMask(int width, int height) {
		Bitmap mask = Bitmap.createBitmap(width, height, Bitmap.Config.ALPHA_8);
		Canvas c = new Canvas(mask);

		Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		paint.setColor(Color.WHITE);
		c.drawRect(0, 0, width, height, paint);

		paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
		c.drawRoundRect(new RectF(0, 0, width, height), cornerRadius, cornerRadius, paint);

		return mask;
	}

	public void setCornerRadius(int cornerRadius) {
		this.cornerRadius = cornerRadius;
	}
}