package com.sapuseven.untis.view;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * Uses a combination of a PageTransformer and swapping X & Y coordinates
 * of touch events to create the illusion of a vertically scrolling ViewPager.
 * <p>
 * Source: <a href="https://stackoverflow.com/a/22797619/5292296">StackOverflow</a>
 */
public class VerticalViewPager extends ViewPager {

	public VerticalViewPager(Context context) {
		super(context);
		init();
	}

	public VerticalViewPager(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	private void init() {
		// The majority of the magic happens here
		setPageTransformer(true, new VerticalPageTransformer());
		// The easiest way to get rid of the overscroll drawing that happens on the left and right
		setOverScrollMode(OVER_SCROLL_NEVER);
	}

	/**
	 * Swaps the X and Y coordinates of your touch event.
	 */
	private MotionEvent swapXY(MotionEvent ev) {
		float width = getWidth();
		float height = getHeight();

		float newX = (ev.getY() / height) * width;
		float newY = (ev.getX() / width) * height;

		ev.setLocation(newX, newY);

		return ev;
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		boolean intercepted = super.onInterceptTouchEvent(swapXY(ev));
		swapXY(ev); // return touch coordinates to original reference frame for any child views
		return intercepted;
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		return super.onTouchEvent(swapXY(ev));
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		if (getChildCount() < 1)
			super.onMeasure(widthMeasureSpec, heightMeasureSpec);

		int height = 0;
		for (int i = 0; i < getChildCount(); i++) {
			View child = getChildAt(i);
			child.measure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
			if (child.getMeasuredHeight() > height)
				height = child.getMeasuredHeight();
		}

		super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
	}

	private class VerticalPageTransformer implements ViewPager.PageTransformer {

		@Override
		public void transformPage(@NonNull View view, float position) {

			if (position < -1) { // [-Infinity,-2)
				// This page is way off-screen to the left.
				view.setAlpha(0);

			} else if (position <= 1) { // [-1,1]
				view.setAlpha(1);

				// Counteract the default slide transition
				view.setTranslationX(view.getWidth() * -position);

				//set Y position to swipe in from top
				float yPosition = position * view.getHeight();
				view.setTranslationY(yPosition);

			} else { // (2,+Infinity]
				// This page is way off-screen to the right.
				view.setAlpha(0);
			}
		}
	}
}