package com.perflyst.untis.utils;

import android.annotation.SuppressLint;
import android.view.GestureDetector;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.AdapterView;

import com.perflyst.untis.utils.lazyload.SimpleFloatViewManager;
import com.perflyst.untis.view.SortableListView;

/**
 * Class that starts and stops item drags on a {@link SortableListView}
 * based on touch gestures. This class also inherits from
 * {@link SimpleFloatViewManager}, which provides basic float View
 * creation.
 */
public class DragSortController extends SimpleFloatViewManager implements View.OnTouchListener, GestureDetector.OnGestureListener {
	/**
	 * Drag init mode enum.
	 */
	public static final int ON_DOWN = 0;
	private static final int ON_DRAG = 1;
	private static final int ON_LONG_PRESS = 2;
	private static final int MISS = -1;
	private int mDragInitMode = ON_DOWN;
	private boolean mSortEnabled = true;
	private final GestureDetector mDetector;
	private final int mTouchSlop;
	private int mHitPos = MISS;

	private final int[] mTempLoc = new int[2];

	private int mItemX;
	private int mItemY;

	private int mCurrX;
	private int mCurrY;

	private boolean mDragging = false;

	private final int mDragHandleId;

	private boolean mCanDrag;

	private final SortableListView mDslv;

	/**
	 * By default, sorting is enabled, and removal is disabled.
	 *
	 * @param dslv         The DSLV instance
	 * @param dragHandleId The resource id of the View that represents
	 *                     the drag handle in a list item.
	 */
	public DragSortController(SortableListView dslv, int dragHandleId, int dragInitMode) {
		super(dslv);
		mDslv = dslv;
		mDetector = new GestureDetector(dslv.getContext(), this);
		mTouchSlop = ViewConfiguration.get(dslv.getContext()).getScaledTouchSlop();
		mDragHandleId = dragHandleId;
		setDragInitMode(dragInitMode);
	}

	/**
	 * Set how a drag is initiated. Needs to be one of
	 * {@link DragSortController#ON_DOWN}, {@link DragSortController#ON_DRAG}, or {@link DragSortController#ON_LONG_PRESS}.
	 *
	 * @param mode The drag init mode.
	 */
	private void setDragInitMode(int mode) {
		mDragInitMode = mode;
	}

	/**
	 * Enable/Disable list item sorting. Disabling is useful if only item
	 * removal is desired. Prevents drags in the vertical direction.
	 *
	 * @param enabled Set <code>true</code> to enable list
	 *                item sorting.
	 */
	public void setSortEnabled(boolean enabled) {
		mSortEnabled = enabled;
	}

	/**
	 * Sets flags to restrict certain motions of the floating View
	 * based on DragSortController settings (such as remove mode).
	 * Starts the drag on the SortableListView.
	 *
	 * @param position The list item position (includes headers).
	 * @param deltaX   Touch x-coord minus left edge of floating View.
	 * @param deltaY   Touch y-coord minus top edge of floating View.
	 */
	private void startDrag(int position, int deltaX, int deltaY) {
		int dragFlags = 0;
		if (mSortEnabled) {
			dragFlags |= SortableListView.DRAG_POS_Y | SortableListView.DRAG_NEG_Y;
		}

		mDragging = mDslv.startDrag(position - mDslv.getHeaderViewsCount(), dragFlags, deltaX,
				deltaY);
	}

	@SuppressLint("ClickableViewAccessibility")
	@Override
	public boolean onTouch(View v, MotionEvent ev) {
		if (!mDslv.isDragEnabled() || mDslv.listViewIntercepted()) {
			return false;
		}

		mDetector.onTouchEvent(ev);

		int action = ev.getAction() & MotionEvent.ACTION_MASK;
		switch (action) {
			case MotionEvent.ACTION_DOWN:
				mCurrX = (int) ev.getX();
				mCurrY = (int) ev.getY();
				break;
			case MotionEvent.ACTION_CANCEL:
				mDragging = false;
				break;
		}

		return false;
	}

	/**
	 * Get the position to start dragging based on the ACTION_DOWN
	 * MotionEvent. This function simply calls
	 * {@link #dragHandleHitPosition(MotionEvent)}. Override
	 * to change drag handle behavior;
	 * this function is called internally when an ACTION_DOWN
	 * event is detected.
	 *
	 * @param ev The ACTION_DOWN MotionEvent.
	 * @return The list position to drag if a drag-init gesture is
	 * detected; MISS if unsuccessful.
	 */
	private int startDragPosition(MotionEvent ev) {
		return dragHandleHitPosition(ev);
	}

	/**
	 * Checks for the touch of an item's drag handle and returns
	 * that item's position if a drag handle touch was detected.
	 *
	 * @param ev The ACTION_DOWN MotionEvent.
	 * @return The list position of the item whose drag handle was
	 * touched; MISS if unsuccessful.
	 */
	private int dragHandleHitPosition(MotionEvent ev) {
		return viewIdHitPosition(ev, mDragHandleId);
	}

	private int viewIdHitPosition(MotionEvent ev, int id) {
		final int x = (int) ev.getX();
		final int y = (int) ev.getY();

		int touchPos = mDslv.pointToPosition(x, y); // includes headers/footers

		final int numHeaders = mDslv.getHeaderViewsCount();
		final int numFooters = mDslv.getFooterViewsCount();
		final int count = mDslv.getCount();

		if (touchPos != AdapterView.INVALID_POSITION && touchPos >= numHeaders
				&& touchPos < (count - numFooters)) {
			final View item = mDslv.getChildAt(touchPos - mDslv.getFirstVisiblePosition());
			final int rawX = (int) ev.getRawX();
			final int rawY = (int) ev.getRawY();

			View dragBox = id == 0 ? item : item.findViewById(id);
			if (dragBox != null) {
				dragBox.getLocationOnScreen(mTempLoc);

				if (rawX > mTempLoc[0] && rawY > mTempLoc[1] &&
						rawX < mTempLoc[0] + dragBox.getWidth() &&
						rawY < mTempLoc[1] + dragBox.getHeight()) {

					mItemX = item.getLeft();
					mItemY = item.getTop();

					return touchPos;
				}
			}
		}

		return MISS;
	}

	@Override
	public boolean onDown(MotionEvent ev) {
		mHitPos = startDragPosition(ev);
		if (mHitPos != MISS && mDragInitMode == ON_DOWN) {
			startDrag(mHitPos, (int) ev.getX() - mItemX, (int) ev.getY() - mItemY);
		}

		mCanDrag = true;

		return true;
	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
		final int y1 = (int) e1.getY();
		final int x2 = (int) e2.getX();
		final int y2 = (int) e2.getY();
		final int deltaX = x2 - mItemX;
		final int deltaY = y2 - mItemY;

		if (mCanDrag && !mDragging && mHitPos != MISS) {
			if (mDragInitMode == ON_DRAG && Math.abs(y2 - y1) > mTouchSlop && mSortEnabled) {
				startDrag(mHitPos, deltaX, deltaY);
			}
		}

		return true;
	}

	@Override
	public void onLongPress(MotionEvent e) {
		if (mHitPos != MISS && mDragInitMode == ON_LONG_PRESS) {
			mDslv.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
			startDrag(mHitPos, mCurrX - mItemX, mCurrY - mItemY);
		}
	}

	@Override
	public final boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
		return false;
	}

	@Override
	public boolean onSingleTapUp(MotionEvent ev) {
		return true;
	}

	@Override
	public void onShowPress(MotionEvent ev) {
	}
}
