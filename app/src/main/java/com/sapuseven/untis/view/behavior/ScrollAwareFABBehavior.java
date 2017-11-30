package com.sapuseven.untis.view.behavior;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.FloatingActionButton.Behavior;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.View;

public class ScrollAwareFABBehavior extends Behavior {
	public ScrollAwareFABBehavior(Context context, AttributeSet attrs) {
		super();
	}

	@Override
	public boolean onStartNestedScroll(@NonNull CoordinatorLayout coordinatorLayout,
	                                   @NonNull FloatingActionButton child, @NonNull View directTargetChild,
	                                   @NonNull View target, int nestedScrollAxes, int type) {
		return nestedScrollAxes == ViewCompat.SCROLL_AXIS_VERTICAL ||
				super.onStartNestedScroll(coordinatorLayout, child, directTargetChild,
						target, nestedScrollAxes, type);
	}

	@Override
	public void onNestedScroll(@NonNull CoordinatorLayout coordinatorLayout, @NonNull FloatingActionButton child,
	                           @NonNull View target, int dxConsumed, int dyConsumed,
	                           int dxUnconsumed, int dyUnconsumed, int type) {
		super.onNestedScroll(coordinatorLayout, child, target, dxConsumed, dyConsumed,
				dxUnconsumed, dyUnconsumed, type);

		if (dyConsumed > 0 && child.getVisibility() == View.VISIBLE)
			child.hide(new FloatingActionButton.OnVisibilityChangedListener() {
				@Override
				public void onHidden(FloatingActionButton fab) {
					super.onShown(fab);
					fab.setVisibility(View.INVISIBLE);
				}
			});
		else if (dyConsumed < 0 && child.getVisibility() != View.VISIBLE)
			child.show();
	}
}