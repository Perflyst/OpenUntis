package de.perflyst.untis.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.SystemClock;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.util.SparseIntArray;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

import de.perflyst.untis.R;
import de.perflyst.untis.utils.DragSortController;

import java.util.ArrayList;

public class SortableListView extends ListView {
	private final static int DRAG_POS_X = 0x1;
	private final static int DRAG_NEG_X = 0x2;
	public final static int DRAG_POS_Y = 0x4;
	public final static int DRAG_NEG_Y = 0x8;

	private final static int IDLE = 0;
	private final static int DROPPING = 1;
	private final static int STOPPED = 2;
	private final static int DRAGGING = 3;

	private static final int NO_CANCEL = 0;
	private static final int ON_TOUCH_EVENT = 1;
	private static final int ON_INTERCEPT_TOUCH_EVENT = 2;

	private static final int sCacheSize = 3;

	private View mFloatView;

	private final Point mFloatLoc = new Point();
	private final Point mTouchLoc = new Point();
	private int mFloatViewMid;
	private boolean mFloatViewOnMeasured = false;
	private DataSetObserver mObserver;
	private float mFloatAlpha = 1.0f;
	private float mCurrFloatAlpha = 1.0f;
	private int mFloatPos;
	private int mFirstExpPos;
	private int mSecondExpPos;
	private boolean mAnimate = false;
	private int mSrcPos;
	private int mDragDeltaX;
	private int mDragDeltaY;
	private DragListener mDragListener;
	private DropListener mDropListener;
	private boolean mDragEnabled = true;
	private int mDragState = IDLE;
	private int mItemHeightCollapsed = 1;
	private int mFloatViewHeight;
	private int mFloatViewHeightHalf;
	private int mWidthMeasureSpec = 0;
	private View[] mSampleViewTypes = new View[1];
	private DragScroller mDragScroller;
	private float mDragUpScrollStartFrac = 1.0f / 3.0f;
	private float mDragDownScrollStartFrac = 1.0f / 3.0f;
	private int mUpScrollStartY;
	private int mDownScrollStartY;
	private float mDownScrollStartYF;
	private float mUpScrollStartYF;
	private float mDragUpScrollHeight;
	private float mDragDownScrollHeight;
	private float mMaxScrollSpeed = 0.5f;
	private final DragScrollProfile mScrollProfile = (w, t) -> mMaxScrollSpeed * w;
	private int mX;
	private int mY;
	private int mLastY;
	private int mDragFlags = 0;
	private boolean mLastCallWasIntercept = false;
	private boolean mInTouchEvent = false;
	private FloatViewManager mFloatViewManager = null;
	private MotionEvent mCancelEvent;
	private int mCancelMethod = NO_CANCEL;
	private float mSlideRegionFrac = 0.25f;
	private float mSlideFrac = 0.0f;
	private boolean mBlockLayoutRequests = false;
	private boolean mIgnoreTouchEvent = false;
	private final HeightCache mChildHeightCache = new HeightCache(sCacheSize);
	private LiftAnimator mLiftAnimator;
	private DropAnimator mDropAnimator;
	private boolean mListViewIntercepted = false;

	public SortableListView(Context context) {
		super(context);

		init(context, null, 0);
	}

	public SortableListView(Context context, AttributeSet attrs) {
		super(context, attrs);

		init(context, attrs, 0);
	}

	public SortableListView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);

		init(context, attrs, defStyleAttr);
	}

	private void init(Context context, AttributeSet attrs, int defStyleAttr) {
		int dropAnimDuration = 150;

		if (attrs != null) {
			TypedArray a = context.obtainStyledAttributes(attrs,
					R.styleable.SortableListView, defStyleAttr, 0);

			mItemHeightCollapsed = Math.max(1, a.getDimensionPixelSize(
					R.styleable.SortableListView_collapsed_height, 1));

			mFloatAlpha = a.getFloat(R.styleable.SortableListView_float_alpha, mFloatAlpha);
			mCurrFloatAlpha = mFloatAlpha;

			mDragEnabled = a.getBoolean(R.styleable.SortableListView_drag_enabled, mDragEnabled);

			mSlideRegionFrac = Math.max(0.0f,
					Math.min(1.0f, 1.0f - a.getFloat(
							R.styleable.SortableListView_slide_shuffle_speed,
							0.75f)));

			mAnimate = mSlideRegionFrac > 0.0f;

			float frac = a.getFloat(
					R.styleable.SortableListView_drag_scroll_start,
					mDragUpScrollStartFrac);

			setDragScrollStart(frac);

			mMaxScrollSpeed = a.getFloat(
					R.styleable.SortableListView_max_drag_scroll_speed,
					mMaxScrollSpeed);

			dropAnimDuration = a.getInt(
					R.styleable.SortableListView_drop_animation_duration,
					dropAnimDuration);

			boolean useDefault = a.getBoolean(
					R.styleable.SortableListView_use_default_controller,
					true);

			if (useDefault) {
				boolean sortEnabled = a.getBoolean(
						R.styleable.SortableListView_sort_enabled,
						true);
				int dragInitMode = a.getInt(
						R.styleable.SortableListView_drag_start_mode,
						DragSortController.ON_DOWN);
				int dragHandleId = a.getResourceId(
						R.styleable.SortableListView_drag_handle_id,
						0);
				int bgColor = a.getColor(
						R.styleable.SortableListView_float_background_color,
						Color.BLACK);

				DragSortController controller = new DragSortController(this, dragHandleId, dragInitMode);
				controller.setSortEnabled(sortEnabled);
				controller.setBackgroundColor(bgColor);

				mFloatViewManager = controller;
				setOnTouchListener(controller);
			}

			a.recycle();
		}

		mDragScroller = new DragScroller();

		float smoothness = 0.5f;
		mLiftAnimator = new LiftAnimator(smoothness, 100);
		if (dropAnimDuration > 0) {
			mDropAnimator = new DropAnimator(smoothness, dropAnimDuration);
		}

		mCancelEvent = MotionEvent.obtain(0, 0, MotionEvent.ACTION_CANCEL, 0f, 0f, 0f, 0f, 0, 0f,
				0f, 0, 0);

		mObserver = new DataSetObserver() {
			private void cancel() {
				if (mDragState == DRAGGING) {
					cancelDrag();
				}
			}

			@Override
			public void onChanged() {
				cancel();
			}

			@Override
			public void onInvalidated() {
				cancel();
			}
		};
	}

	@Override
	public void setAdapter(ListAdapter adapter) {
		AdapterWrapper mAdapterWrapper;
		if (adapter != null) {
			mAdapterWrapper = new AdapterWrapper(adapter);
			adapter.registerDataSetObserver(mObserver);

			if (adapter instanceof DropListener) {
				setDropListener((DropListener) adapter);
			}
			if (adapter instanceof DragListener) {
				setDragListener((DragListener) adapter);
			}
		} else {
			mAdapterWrapper = null;
		}

		super.setAdapter(mAdapterWrapper);
	}

	private void drawDivider(int expPosition, Canvas canvas) {
		final Drawable divider = getDivider();
		final int dividerHeight = getDividerHeight();

		if (divider != null && dividerHeight != 0) {
			final ViewGroup expItem = (ViewGroup) getChildAt(expPosition
					- getFirstVisiblePosition());
			if (expItem != null) {
				final int l = getPaddingLeft();
				final int r = getWidth() - getPaddingRight();
				final int t;
				final int b;

				final int childHeight = expItem.getChildAt(0).getHeight();

				if (expPosition > mSrcPos) {
					t = expItem.getTop() + childHeight;
					b = t + dividerHeight;
				} else {
					b = expItem.getBottom() - childHeight;
					t = b - dividerHeight;
				}

				canvas.save();
				canvas.clipRect(l, t, r, b);
				divider.setBounds(l, t, r, b);
				divider.draw(canvas);
				canvas.restore();
			}
		}
	}

	@Override
	protected void dispatchDraw(Canvas canvas) {
		super.dispatchDraw(canvas);

		if (mDragState != IDLE) {
			if (mFirstExpPos != mSrcPos) {
				drawDivider(mFirstExpPos, canvas);
			}
			if (mSecondExpPos != mFirstExpPos && mSecondExpPos != mSrcPos) {
				drawDivider(mSecondExpPos, canvas);
			}
		}

		if (mFloatView != null) {
			final int w = mFloatView.getWidth();
			final int h = mFloatView.getHeight();

			int x = mFloatLoc.x;

			int width = getWidth();
			if (x < 0)
				x = -x;
			float alphaMod;
			if (x < width) {
				alphaMod = ((float) (width - x)) / ((float) width);
				alphaMod *= alphaMod;
			} else {
				alphaMod = 0;
			}

			final int alpha = (int) (255f * mCurrFloatAlpha * alphaMod);

			canvas.save();

			canvas.translate(mFloatLoc.x, mFloatLoc.y);
			canvas.clipRect(0, 0, w, h);

			canvas.saveLayerAlpha(0, 0, w, h, alpha, Canvas.ALL_SAVE_FLAG);
			mFloatView.draw(canvas);
			canvas.restore();
			canvas.restore();
		}
	}

	private int getItemHeight(int position) {
		View v = getChildAt(position - getFirstVisiblePosition());

		if (v != null) {
			return v.getHeight();
		} else {
			return calcItemHeight(position, getChildHeight(position));
		}
	}

	private int getShuffleEdge(int position, int top) {
		final int numHeaders = getHeaderViewsCount();
		final int numFooters = getFooterViewsCount();

		if (position <= numHeaders || (position >= getCount() - numFooters)) {
			return top;
		}

		int divHeight = getDividerHeight();

		int edge;

		int maxBlankHeight = mFloatViewHeight - mItemHeightCollapsed;
		int childHeight = getChildHeight(position);
		int itemHeight = getItemHeight(position);

		int otop = top;
		if (mSecondExpPos <= mSrcPos) {
			if (position == mSecondExpPos && mFirstExpPos != mSecondExpPos) {
				if (position == mSrcPos) {
					otop = top + itemHeight - mFloatViewHeight;
				} else {
					int blankHeight = itemHeight - childHeight;
					otop = top + blankHeight - maxBlankHeight;
				}
			} else if (position > mSecondExpPos && position <= mSrcPos) {
				otop = top - maxBlankHeight;
			}

		} else {
			if (position > mSrcPos && position <= mFirstExpPos) {
				otop = top + maxBlankHeight;
			} else if (position == mSecondExpPos && mFirstExpPos != mSecondExpPos) {
				int blankHeight = itemHeight - childHeight;
				otop = top + blankHeight;
			}
		}

		if (position <= mSrcPos) {
			edge = otop + (mFloatViewHeight - divHeight - getChildHeight(position - 1)) / 2;
		} else {
			edge = otop + (childHeight - divHeight - mFloatViewHeight) / 2;
		}

		return edge;
	}

	private boolean updatePositions() {
		final int first = getFirstVisiblePosition();
		int startPos = mFirstExpPos;
		View startView = getChildAt(startPos - first);

		if (startView == null) {
			startPos = first + getChildCount() / 2;
			startView = getChildAt(startPos - first);
		}
		int startTop = startView.getTop();

		int itemHeight = startView.getHeight();

		int edge = getShuffleEdge(startPos, startTop);
		int lastEdge = edge;

		int divHeight = getDividerHeight();

		int itemPos = startPos;
		int itemTop = startTop;
		if (mFloatViewMid < edge) {
			while (itemPos >= 0) {
				itemPos--;
				itemHeight = getItemHeight(itemPos);

				if (itemPos == 0) {
					edge = itemTop - divHeight - itemHeight;
					break;
				}

				itemTop -= itemHeight + divHeight;
				edge = getShuffleEdge(itemPos, itemTop);

				if (mFloatViewMid >= edge) {
					break;
				}

				lastEdge = edge;
			}
		} else {
			final int count = getCount();
			while (itemPos < count) {
				if (itemPos == count - 1) {
					edge = itemTop + divHeight + itemHeight;
					break;
				}

				itemTop += divHeight + itemHeight;
				itemHeight = getItemHeight(itemPos + 1);
				edge = getShuffleEdge(itemPos + 1, itemTop);

				if (mFloatViewMid < edge) {
					break;
				}

				lastEdge = edge;
				itemPos++;
			}
		}

		final int numHeaders = getHeaderViewsCount();
		final int numFooters = getFooterViewsCount();

		boolean updated = false;

		int oldFirstExpPos = mFirstExpPos;
		int oldSecondExpPos = mSecondExpPos;
		float oldSlideFrac = mSlideFrac;

		if (mAnimate) {
			int edgeToEdge = Math.abs(edge - lastEdge);

			int edgeTop, edgeBottom;
			if (mFloatViewMid < edge) {
				edgeBottom = edge;
				edgeTop = lastEdge;
			} else {
				edgeTop = edge;
				edgeBottom = lastEdge;
			}

			int slideRgnHeight = (int) (0.5f * mSlideRegionFrac * edgeToEdge);
			float slideRgnHeightF = (float) slideRgnHeight;
			int slideEdgeTop = edgeTop + slideRgnHeight;
			int slideEdgeBottom = edgeBottom - slideRgnHeight;

			if (mFloatViewMid < slideEdgeTop) {
				mFirstExpPos = itemPos - 1;
				mSecondExpPos = itemPos;
				mSlideFrac = 0.5f * ((float) (slideEdgeTop - mFloatViewMid)) / slideRgnHeightF;

			} else if (mFloatViewMid < slideEdgeBottom) {
				mFirstExpPos = itemPos;
				mSecondExpPos = itemPos;
			} else {
				mFirstExpPos = itemPos;
				mSecondExpPos = itemPos + 1;
				mSlideFrac = 0.5f * (1.0f + ((float) (edgeBottom - mFloatViewMid))
						/ slideRgnHeightF);

			}

		} else {
			mFirstExpPos = itemPos;
			mSecondExpPos = itemPos;
		}

		if (mFirstExpPos < numHeaders) {
			itemPos = numHeaders;
			mFirstExpPos = itemPos;
			mSecondExpPos = itemPos;
		} else if (mSecondExpPos >= getCount() - numFooters) {
			itemPos = getCount() - numFooters - 1;
			mFirstExpPos = itemPos;
			mSecondExpPos = itemPos;
		}

		if (mFirstExpPos != oldFirstExpPos || mSecondExpPos != oldSecondExpPos
				|| mSlideFrac != oldSlideFrac) {
			updated = true;
		}

		if (itemPos != mFloatPos) {
			if (mDragListener != null) {
				mDragListener.drag(mFloatPos - numHeaders, itemPos - numHeaders);
			}

			mFloatPos = itemPos;
			updated = true;
		}

		return updated;
	}

	private void cancelDrag() {
		if (mDragState == DRAGGING) {
			mDragScroller.stopScrolling(true);
			destroyFloatView();
			clearPositions();
			adjustAllItems();

			if (mInTouchEvent) {
				mDragState = STOPPED;
			} else {
				mDragState = IDLE;
			}
		}
	}

	private void clearPositions() {
		mSrcPos = -1;
		mFirstExpPos = -1;
		mSecondExpPos = -1;
		mFloatPos = -1;
	}

	private void dropFloatView() {
		mDragState = DROPPING;

		if (mDropListener != null && mFloatPos >= 0 && mFloatPos < getCount()) {
			final int numHeaders = getHeaderViewsCount();
			mDropListener.drop(mSrcPos - numHeaders, mFloatPos - numHeaders);
		}

		destroyFloatView();

		adjustOnReorder();
		clearPositions();
		adjustAllItems();

		if (mInTouchEvent) {
			mDragState = STOPPED;
		} else {
			mDragState = IDLE;
		}
	}

	private void adjustOnReorder() {
		final int firstPos = getFirstVisiblePosition();

		if (mSrcPos < firstPos) {
			View v = getChildAt(0);
			int top = 0;
			if (v != null) {
				top = v.getTop();
			}

			setSelectionFromTop(firstPos - 1, top - getPaddingTop());
		}
	}

	private void stopDrag() {
		if (mFloatView != null) {
			mDragScroller.stopScrolling(true);

			if (mDropAnimator != null) {
				mDropAnimator.start();
			} else {
				dropFloatView();
			}

		}
	}

	@SuppressLint("ClickableViewAccessibility")
	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		if (mIgnoreTouchEvent) {
			mIgnoreTouchEvent = false;
			return false;
		}

		if (!mDragEnabled) {
			return super.onTouchEvent(ev);
		}

		boolean more = false;

		boolean lastCallWasIntercept = mLastCallWasIntercept;
		mLastCallWasIntercept = false;

		if (!lastCallWasIntercept) {
			saveTouchCoords(ev);
		}

		if (mDragState == DRAGGING) {
			onDragTouchEvent(ev);
			more = true;
		} else {
			if (mDragState == IDLE) {
				if (super.onTouchEvent(ev)) {
					more = true;
				}
			}

			int action = ev.getAction() & MotionEvent.ACTION_MASK;

			switch (action) {
				case MotionEvent.ACTION_CANCEL:
				case MotionEvent.ACTION_UP:
					doActionUpOrCancel();
					break;
				default:
					if (more) {
						mCancelMethod = ON_TOUCH_EVENT;
					}
			}
		}

		return more;
	}

	private void doActionUpOrCancel() {
		mCancelMethod = NO_CANCEL;
		mInTouchEvent = false;
		if (mDragState == STOPPED) {
			mDragState = IDLE;
		}
		mCurrFloatAlpha = mFloatAlpha;
		mListViewIntercepted = false;
		mChildHeightCache.clear();
	}

	private void saveTouchCoords(MotionEvent ev) {
		int action = ev.getAction() & MotionEvent.ACTION_MASK;
		if (action != MotionEvent.ACTION_DOWN) {
			mLastY = mY;
		}
		mX = (int) ev.getX();
		mY = (int) ev.getY();
		if (action == MotionEvent.ACTION_DOWN) {
			mLastY = mY;
		}
	}

	public boolean listViewIntercepted() {
		return mListViewIntercepted;
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		if (!mDragEnabled) {
			return super.onInterceptTouchEvent(ev);
		}

		saveTouchCoords(ev);
		mLastCallWasIntercept = true;

		int action = ev.getAction() & MotionEvent.ACTION_MASK;

		if (action == MotionEvent.ACTION_DOWN) {
			if (mDragState != IDLE) {
				mIgnoreTouchEvent = true;
				return true;
			}
			mInTouchEvent = true;
		}

		boolean intercept = false;

		if (mFloatView != null) {
			intercept = true;
		} else {
			if (super.onInterceptTouchEvent(ev)) {
				mListViewIntercepted = true;
				intercept = true;
			}

			switch (action) {
				case MotionEvent.ACTION_CANCEL:
				case MotionEvent.ACTION_UP:
					doActionUpOrCancel();
					break;
				default:
					if (intercept) {
						mCancelMethod = ON_TOUCH_EVENT;
					} else {
						mCancelMethod = ON_INTERCEPT_TOUCH_EVENT;
					}
			}
		}

		if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
			mInTouchEvent = false;
		}

		return intercept;
	}

	private void setDragScrollStart(float heightFraction) {
		setDragScrollStarts(heightFraction, heightFraction);
	}

	private void setDragScrollStarts(float upperFrac, float lowerFrac) {
		if (lowerFrac > 0.5f) {
			mDragDownScrollStartFrac = 0.5f;
		} else {
			mDragDownScrollStartFrac = lowerFrac;
		}

		if (upperFrac > 0.5f) {
			mDragUpScrollStartFrac = 0.5f;
		} else {
			mDragUpScrollStartFrac = upperFrac;
		}

		if (getHeight() != 0) {
			updateScrollStarts();
		}
	}

	private void continueDrag(int x, int y) {
		mFloatLoc.x = x - mDragDeltaX;
		mFloatLoc.y = y - mDragDeltaY;

		doDragFloatView(true);

		int minY = Math.min(y, mFloatViewMid + mFloatViewHeightHalf);
		int maxY = Math.max(y, mFloatViewMid - mFloatViewHeightHalf);

		int currentScrollDir = mDragScroller.getScrollDir();

		if (minY > mLastY && minY > mDownScrollStartY && currentScrollDir != DragScroller.DOWN) {
			if (currentScrollDir != DragScroller.STOP) {
				mDragScroller.stopScrolling(true);
			}

			mDragScroller.startScrolling(DragScroller.DOWN);
		} else if (maxY < mLastY && maxY < mUpScrollStartY && currentScrollDir != DragScroller.UP) {
			if (currentScrollDir != DragScroller.STOP) {
				mDragScroller.stopScrolling(true);
			}

			mDragScroller.startScrolling(DragScroller.UP);
		} else if (maxY >= mUpScrollStartY && minY <= mDownScrollStartY
				&& mDragScroller.isScrolling()) {
			mDragScroller.stopScrolling(true);
		}
	}

	private void updateScrollStarts() {
		final int padTop = getPaddingTop();
		final int listHeight = getHeight() - padTop - getPaddingBottom();
		float heightF = (float) listHeight;

		mUpScrollStartYF = padTop + mDragUpScrollStartFrac * heightF;
		mDownScrollStartYF = padTop + (1.0f - mDragDownScrollStartFrac) * heightF;

		mUpScrollStartY = (int) mUpScrollStartYF;
		mDownScrollStartY = (int) mDownScrollStartYF;

		mDragUpScrollHeight = mUpScrollStartYF - padTop;
		mDragDownScrollHeight = padTop + listHeight - mDownScrollStartYF;
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		updateScrollStarts();
	}

	private void adjustAllItems() {
		final int first = getFirstVisiblePosition();
		final int last = getLastVisiblePosition();

		int begin = Math.max(0, getHeaderViewsCount() - first);
		int end = Math.min(last - first, getCount() - 1 - getFooterViewsCount() - first);

		for (int i = begin; i <= end; ++i) {
			View v = getChildAt(i);
			if (v != null) {
				adjustItem(first + i, v, false);
			}
		}
	}

	private void adjustItem(int position, View v, boolean invalidChildHeight) {
		ViewGroup.LayoutParams lp = v.getLayoutParams();
		int height;
		if (position != mSrcPos && position != mFirstExpPos && position != mSecondExpPos) {
			height = ViewGroup.LayoutParams.WRAP_CONTENT;
		} else {
			height = calcItemHeight(position, v, invalidChildHeight);
		}

		if (height != lp.height) {
			lp.height = height;
			v.setLayoutParams(lp);
		}

		if (position == mFirstExpPos || position == mSecondExpPos) {
			if (position < mSrcPos) {
				((SortableListViewItem) v).setGravity(Gravity.BOTTOM);
			} else if (position > mSrcPos) {
				((SortableListViewItem) v).setGravity(Gravity.TOP);
			}
		}

		int oldVis = v.getVisibility();
		int vis = View.VISIBLE;

		if (position == mSrcPos && mFloatView != null) {
			vis = View.INVISIBLE;
		}

		if (vis != oldVis) {
			v.setVisibility(vis);
		}
	}

	private int getChildHeight(int position) {
		if (position == mSrcPos) {
			return 0;
		}

		View v = getChildAt(position - getFirstVisiblePosition());

		if (v != null) {
			return getChildHeight(position, v, false);
		} else {
			int childHeight = mChildHeightCache.get(position);
			if (childHeight != -1) {
				return childHeight;
			}

			final ListAdapter adapter = getAdapter();
			int type = adapter.getItemViewType(position);

			final int typeCount = adapter.getViewTypeCount();
			if (typeCount != mSampleViewTypes.length) {
				mSampleViewTypes = new View[typeCount];
			}

			if (type >= 0) {
				if (mSampleViewTypes[type] == null) {
					v = adapter.getView(position, null, this);
					mSampleViewTypes[type] = v;
				} else {
					v = adapter.getView(position, mSampleViewTypes[type], this);
				}
			} else {
				v = adapter.getView(position, null, this);
			}

			childHeight = getChildHeight(position, v, true);

			mChildHeightCache.add(position, childHeight);

			return childHeight;
		}
	}

	private int getChildHeight(int position, View item, boolean invalidChildHeight) {
		if (position == mSrcPos) {
			return 0;
		}

		View child;
		if (position < getHeaderViewsCount() || position >= getCount() - getFooterViewsCount()) {
			child = item;
		} else {
			child = ((ViewGroup) item).getChildAt(0);
		}

		ViewGroup.LayoutParams lp = child.getLayoutParams();

		if (lp != null) {
			if (lp.height > 0) {
				return lp.height;
			}
		}

		int childHeight = child.getHeight();

		if (childHeight == 0 || invalidChildHeight) {
			measureItem(child);
			childHeight = child.getMeasuredHeight();
		}

		return childHeight;
	}

	private int calcItemHeight(int position, View item, boolean invalidChildHeight) {
		return calcItemHeight(position, getChildHeight(position, item, invalidChildHeight));
	}

	private int calcItemHeight(int position, int childHeight) {
		boolean isSliding = mAnimate && mFirstExpPos != mSecondExpPos;
		int maxNonSrcBlankHeight = mFloatViewHeight - mItemHeightCollapsed;
		int slideHeight = (int) (mSlideFrac * maxNonSrcBlankHeight);

		int height;

		if (position == mSrcPos) {
			if (mSrcPos == mFirstExpPos) {
				if (isSliding) {
					height = slideHeight + mItemHeightCollapsed;
				} else {
					height = mFloatViewHeight;
				}
			} else if (mSrcPos == mSecondExpPos) {
				height = mFloatViewHeight - slideHeight;
			} else {
				height = mItemHeightCollapsed;
			}
		} else if (position == mFirstExpPos) {
			if (isSliding) {
				height = childHeight + slideHeight;
			} else {
				height = childHeight + maxNonSrcBlankHeight;
			}
		} else if (position == mSecondExpPos) {
			height = childHeight + maxNonSrcBlankHeight - slideHeight;
		} else {
			height = childHeight;
		}

		return height;
	}

	@Override
	public void requestLayout() {
		if (!mBlockLayoutRequests) {
			super.requestLayout();
		}
	}

	private int adjustScroll(int movePos, View moveItem, int oldFirstExpPos, int oldSecondExpPos) {
		int adjust = 0;

		final int childHeight = getChildHeight(movePos);

		int moveHeightBefore = moveItem.getHeight();
		int moveHeightAfter = calcItemHeight(movePos, childHeight);

		int moveBlankBefore = moveHeightBefore;
		int moveBlankAfter = moveHeightAfter;

		if (movePos != mSrcPos) {
			moveBlankBefore -= childHeight;
			moveBlankAfter -= childHeight;
		}

		int maxBlank = mFloatViewHeight;
		if (mSrcPos != mFirstExpPos && mSrcPos != mSecondExpPos) {
			maxBlank -= mItemHeightCollapsed;
		}

		if (movePos <= oldFirstExpPos) {
			if (movePos > mFirstExpPos) {
				adjust += maxBlank - moveBlankAfter;
			}
		} else if (movePos == oldSecondExpPos) {
			if (movePos <= mFirstExpPos) {
				adjust += moveBlankBefore - maxBlank;
			} else if (movePos == mSecondExpPos) {
				adjust += moveHeightBefore - moveHeightAfter;
			} else {
				adjust += moveBlankBefore;
			}
		} else {
			if (movePos <= mFirstExpPos) {
				adjust -= maxBlank;
			} else if (movePos == mSecondExpPos) {
				adjust -= moveBlankAfter;
			}
		}

		return adjust;
	}

	private void measureItem(View item) {
		ViewGroup.LayoutParams lp = item.getLayoutParams();
		if (lp == null) {
			lp = new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			item.setLayoutParams(lp);
		}
		int wspec = ViewGroup.getChildMeasureSpec(mWidthMeasureSpec, getListPaddingLeft()
				+ getListPaddingRight(), lp.width);
		int hspec;
		if (lp.height > 0) {
			hspec = MeasureSpec.makeMeasureSpec(lp.height, MeasureSpec.EXACTLY);
		} else {
			hspec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
		}
		item.measure(wspec, hspec);
	}

	private void measureFloatView() {
		if (mFloatView != null) {
			measureItem(mFloatView);
			mFloatViewHeight = mFloatView.getMeasuredHeight();
			mFloatViewHeightHalf = mFloatViewHeight / 2;
		}
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);

		if (mFloatView != null) {
			if (mFloatView.isLayoutRequested()) {
				measureFloatView();
			}
			mFloatViewOnMeasured = true;
		}
		mWidthMeasureSpec = widthMeasureSpec;
	}

	@Override
	protected void layoutChildren() {
		super.layoutChildren();

		if (mFloatView != null) {
			if (mFloatView.isLayoutRequested() && !mFloatViewOnMeasured) {
				measureFloatView();
			}
			mFloatView.layout(0, 0, mFloatView.getMeasuredWidth(), mFloatView.getMeasuredHeight());
			mFloatViewOnMeasured = false;
		}
	}

	private boolean onDragTouchEvent(MotionEvent ev) {
		switch (ev.getAction() & MotionEvent.ACTION_MASK) {
			case MotionEvent.ACTION_CANCEL:
				if (mDragState == DRAGGING) {
					cancelDrag();
				}
				doActionUpOrCancel();
				break;
			case MotionEvent.ACTION_UP:

				if (mDragState == DRAGGING) {
					stopDrag();
				}
				doActionUpOrCancel();
				break;
			case MotionEvent.ACTION_MOVE:
				continueDrag((int) ev.getX(), (int) ev.getY());
				break;
		}

		return true;
	}

	public boolean startDrag(int position, int dragFlags, int deltaX, int deltaY) {
		if (!mInTouchEvent || mFloatViewManager == null) {
			return false;
		}

		View v = mFloatViewManager.onCreateFloatView(position);

		return v != null && startDrag(position, v, dragFlags, deltaX, deltaY);

	}

	private boolean startDrag(int position, View floatView, int dragFlags, int deltaX, int deltaY) {
		if (mDragState != IDLE || !mInTouchEvent || mFloatView != null || floatView == null
				|| !mDragEnabled) {
			return false;
		}

		if (getParent() != null) {
			getParent().requestDisallowInterceptTouchEvent(true);
		}

		int pos = position + getHeaderViewsCount();
		mFirstExpPos = pos;
		mSecondExpPos = pos;
		mSrcPos = pos;
		mFloatPos = pos;

		mDragState = DRAGGING;
		mDragFlags = 0;
		mDragFlags |= dragFlags;

		mFloatView = floatView;
		measureFloatView();

		mDragDeltaX = deltaX;
		mDragDeltaY = deltaY;

		mFloatLoc.x = mX - mDragDeltaX;
		mFloatLoc.y = mY - mDragDeltaY;

		final View srcItem = getChildAt(mSrcPos - getFirstVisiblePosition());

		if (srcItem != null) {
			srcItem.setVisibility(View.INVISIBLE);
		}

		switch (mCancelMethod) {
			case ON_TOUCH_EVENT:
				super.onTouchEvent(mCancelEvent);
				break;
			case ON_INTERCEPT_TOUCH_EVENT:
				super.onInterceptTouchEvent(mCancelEvent);
				break;
		}

		requestLayout();

		if (mLiftAnimator != null) {
			mLiftAnimator.start();
		}

		return true;
	}

	private void doDragFloatView(boolean forceInvalidate) {
		int movePos = getFirstVisiblePosition() + getChildCount() / 2;
		View moveItem = getChildAt(getChildCount() / 2);

		if (moveItem == null) {
			return;
		}

		doDragFloatView(movePos, moveItem, forceInvalidate);
	}

	private void doDragFloatView(int movePos, View moveItem, boolean forceInvalidate) {
		mBlockLayoutRequests = true;

		updateFloatView();

		int oldFirstExpPos = mFirstExpPos;
		int oldSecondExpPos = mSecondExpPos;

		boolean updated = updatePositions();

		if (updated) {
			adjustAllItems();
			int scroll = adjustScroll(movePos, moveItem, oldFirstExpPos, oldSecondExpPos);

			setSelectionFromTop(movePos, moveItem.getTop() + scroll - getPaddingTop());
			layoutChildren();
		}

		if (updated || forceInvalidate) {
			invalidate();
		}

		mBlockLayoutRequests = false;
	}

	private void updateFloatView() {
		if (mFloatViewManager != null) {
			mTouchLoc.set(mX, mY);
		}

		final int floatX = mFloatLoc.x;
		final int floatY = mFloatLoc.y;

		int padLeft = getPaddingLeft();
		if ((mDragFlags & DRAG_POS_X) == 0 && floatX > padLeft) {
			mFloatLoc.x = padLeft;
		} else if ((mDragFlags & DRAG_NEG_X) == 0 && floatX < padLeft) {
			mFloatLoc.x = padLeft;
		}

		final int numHeaders = getHeaderViewsCount();
		final int numFooters = getFooterViewsCount();
		final int firstPos = getFirstVisiblePosition();
		final int lastPos = getLastVisiblePosition();

		int topLimit = getPaddingTop();
		if (firstPos < numHeaders) {
			topLimit = getChildAt(numHeaders - firstPos - 1).getBottom();
		}
		if ((mDragFlags & DRAG_NEG_Y) == 0) {
			if (firstPos <= mSrcPos) {
				topLimit = Math.max(getChildAt(mSrcPos - firstPos).getTop(), topLimit);
			}
		}

		int bottomLimit = getHeight() - getPaddingBottom();
		if (lastPos >= getCount() - numFooters - 1) {
			bottomLimit = getChildAt(getCount() - numFooters - 1 - firstPos).getBottom();
		}
		if ((mDragFlags & DRAG_POS_Y) == 0) {
			if (lastPos >= mSrcPos) {
				bottomLimit = Math.min(getChildAt(mSrcPos - firstPos).getBottom(), bottomLimit);
			}
		}

		if (floatY < topLimit) {
			mFloatLoc.y = topLimit;
		} else if (floatY + mFloatViewHeight > bottomLimit) {
			mFloatLoc.y = bottomLimit - mFloatViewHeight;
		}

		mFloatViewMid = mFloatLoc.y + mFloatViewHeightHalf;
	}

	private void destroyFloatView() {
		if (mFloatView != null) {
			mFloatView.setVisibility(GONE);
			if (mFloatViewManager != null) {
				mFloatViewManager.onDestroyFloatView(mFloatView);
			}
			mFloatView = null;
			invalidate();
		}
	}

	private void setDragListener(DragListener l) {
		mDragListener = l;
	}

	public boolean isDragEnabled() {
		return mDragEnabled;
	}

	public void setDropListener(DropListener l) {
		mDropListener = l;
	}

	public interface FloatViewManager {
		View onCreateFloatView(int position);

		void onDestroyFloatView(View floatView);
	}

	interface DragListener {
		void drag(int from, int to);
	}

	public interface DropListener {
		void drop(int from, int to);
	}

	interface DragScrollProfile {
		float getSpeed(float w, long t);
	}

	private class AdapterWrapper extends BaseAdapter {
		private final ListAdapter mAdapter;

		AdapterWrapper(ListAdapter adapter) {
			super();
			mAdapter = adapter;

			mAdapter.registerDataSetObserver(new DataSetObserver() {
				public void onChanged() {
					notifyDataSetChanged();
				}

				public void onInvalidated() {
					notifyDataSetInvalidated();
				}
			});
		}

		@Override
		public long getItemId(int position) {
			return mAdapter.getItemId(position);
		}

		@Override
		public Object getItem(int position) {
			return mAdapter.getItem(position);
		}

		@Override
		public int getCount() {
			return mAdapter.getCount();
		}

		@Override
		public boolean areAllItemsEnabled() {
			return mAdapter.areAllItemsEnabled();
		}

		@Override
		public boolean isEnabled(int position) {
			return mAdapter.isEnabled(position);
		}

		@Override
		public int getItemViewType(int position) {
			return mAdapter.getItemViewType(position);
		}

		@Override
		public int getViewTypeCount() {
			return mAdapter.getViewTypeCount();
		}

		@Override
		public boolean hasStableIds() {
			return mAdapter.hasStableIds();
		}

		@Override
		public boolean isEmpty() {
			return mAdapter.isEmpty();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			SortableListViewItem v;
			View child;

			if (convertView != null) {
				v = (SortableListViewItem) convertView;
				View oldChild = v.getChildAt(0);

				child = mAdapter.getView(position, oldChild, SortableListView.this);
				if (child != oldChild) {
					if (oldChild != null) {
						v.removeViewAt(0);
					}
					v.addView(child);
				}
			} else {
				child = mAdapter.getView(position, null, SortableListView.this);
				v = new SortableListViewItem(getContext());
				v.setLayoutParams(new AbsListView.LayoutParams(
						ViewGroup.LayoutParams.MATCH_PARENT,
						ViewGroup.LayoutParams.WRAP_CONTENT));
				v.addView(child);
			}

			adjustItem(position + getHeaderViewsCount(), v, true);

			return v;
		}
	}

	private class HeightCache {
		private final SparseIntArray mMap;
		private final ArrayList<Integer> mOrder;
		private final int mMaxSize;

		HeightCache(int size) {
			mMap = new SparseIntArray(size);
			mOrder = new ArrayList<>(size);
			mMaxSize = size;
		}

		void add(int position, int height) {
			int currHeight = mMap.get(position, -1);
			if (currHeight != height) {
				if (currHeight == -1) {
					if (mMap.size() == mMaxSize) {
						mMap.delete(mOrder.remove(0));
					}
				} else {
					mOrder.remove((Integer) position);
				}
				mMap.put(position, height);
				mOrder.add(position);
			}
		}

		int get(int position) {
			return mMap.get(position, -1);
		}

		void clear() {
			mMap.clear();
			mOrder.clear();
		}

	}

	private class SmoothAnimator implements Runnable {
		long mStartTime;

		private final float mDurationF;

		private final float mAlpha;
		private final float mA;
		private final float mB;
		private final float mC;
		private final float mD;

		private boolean mCanceled;

		SmoothAnimator(float smoothness, int duration) {
			mAlpha = smoothness;
			mDurationF = (float) duration;
			mA = mD = 1f / (2f * mAlpha * (1f - mAlpha));
			mB = mAlpha / (2f * (mAlpha - 1f));
			mC = 1f / (1f - mAlpha);
		}

		float transform(float frac) {
			if (frac < mAlpha) {
				return mA * frac * frac;
			} else if (frac < 1f - mAlpha) {
				return mB + mC * frac;
			} else {
				return 1f - mD * (frac - 1f) * (frac - 1f);
			}
		}

		void start() {
			mStartTime = SystemClock.uptimeMillis();
			mCanceled = false;
			onStart();
			post(this);
		}

		void cancel() {
			mCanceled = true;
		}

		void onStart() {
		}

		void onUpdate(float frac, float smoothFrac) {
		}

		void onStop() {
		}

		@Override
		public void run() {
			if (mCanceled) {
				return;
			}

			float fraction = ((float) (SystemClock.uptimeMillis() - mStartTime)) / mDurationF;

			if (fraction >= 1f) {
				onUpdate(1f, 1f);
				onStop();
			} else {
				onUpdate(fraction, transform(fraction));
				post(this);
			}
		}
	}

	private class LiftAnimator extends SmoothAnimator {
		private float mInitDragDeltaY;
		private float mFinalDragDeltaY;

		LiftAnimator(float smoothness, int duration) {
			super(smoothness, duration);
		}

		@Override
		public void onStart() {
			mInitDragDeltaY = mDragDeltaY;
			mFinalDragDeltaY = mFloatViewHeightHalf;
		}

		@Override
		public void onUpdate(float frac, float smoothFrac) {
			if (mDragState != DRAGGING) {
				cancel();
			} else {
				mDragDeltaY = (int) (smoothFrac * mFinalDragDeltaY + (1f - smoothFrac)
						* mInitDragDeltaY);
				mFloatLoc.y = mY - mDragDeltaY;
				doDragFloatView(true);
			}
		}
	}

	private class DropAnimator extends SmoothAnimator {
		private int mDropPos;
		private int srcPos;
		private float mInitDeltaY;
		private float mInitDeltaX;

		DropAnimator(float smoothness, int duration) {
			super(smoothness, duration);
		}

		@Override
		public void onStart() {
			mDropPos = mFloatPos;
			srcPos = mSrcPos;
			mDragState = DROPPING;
			mInitDeltaY = mFloatLoc.y - getTargetY();
			mInitDeltaX = mFloatLoc.x - getPaddingLeft();
		}

		private int getTargetY() {
			final int first = getFirstVisiblePosition();
			final int otherAdjust = (mItemHeightCollapsed + getDividerHeight()) / 2;
			View v = getChildAt(mDropPos - first);
			int targetY = -1;
			if (v != null) {
				if (mDropPos == srcPos) {
					targetY = v.getTop();
				} else if (mDropPos < srcPos) {
					targetY = v.getTop() - otherAdjust;
				} else {
					targetY = v.getBottom() + otherAdjust - mFloatViewHeight;
				}
			} else {
				cancel();
			}

			return targetY;
		}

		@Override
		public void onUpdate(float frac, float smoothFrac) {
			final int targetY = getTargetY();
			final int targetX = getPaddingLeft();
			final float deltaY = mFloatLoc.y - targetY;
			final float deltaX = mFloatLoc.x - targetX;
			final float f = 1f - smoothFrac;
			if (f < Math.abs(deltaY / mInitDeltaY) || f < Math.abs(deltaX / mInitDeltaX)) {
				mFloatLoc.y = targetY + (int) (mInitDeltaY * f);
				mFloatLoc.x = getPaddingLeft() + (int) (mInitDeltaX * f);
				doDragFloatView(true);
			}
		}

		@Override
		public void onStop() {
			dropFloatView();
		}

	}

	private class DragScroller implements Runnable {
		final static int STOP = -1;
		final static int UP = 0;
		final static int DOWN = 1;
		private boolean mAbort;
		private long mPrevTime;
		private long mCurrTime;
		private int dy;
		private float dt;
		private long tStart;
		private int scrollDir;
		private float mScrollSpeed;

		private boolean mScrolling = false;

		DragScroller() {
		}

		boolean isScrolling() {
			return mScrolling;
		}

		int getScrollDir() {
			return mScrolling ? scrollDir : STOP;
		}

		void startScrolling(int dir) {
			if (!mScrolling) {
				mAbort = false;
				mScrolling = true;
				tStart = SystemClock.uptimeMillis();
				mPrevTime = tStart;
				scrollDir = dir;
				post(this);
			}
		}

		void stopScrolling(boolean now) {
			if (now) {
				SortableListView.this.removeCallbacks(this);
				mScrolling = false;
			} else {
				mAbort = true;
			}

		}

		@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
		@Override
		public void run() {
			if (mAbort) {
				mScrolling = false;
				return;
			}

			final int first = getFirstVisiblePosition();
			final int last = getLastVisiblePosition();
			final int count = getCount();
			final int padTop = getPaddingTop();
			final int listHeight = getHeight() - padTop - getPaddingBottom();

			int minY = Math.min(mY, mFloatViewMid + mFloatViewHeightHalf);
			int maxY = Math.max(mY, mFloatViewMid - mFloatViewHeightHalf);

			if (scrollDir == UP) {
				View v = getChildAt(0);

				if (v == null) {
					mScrolling = false;
					return;
				} else {
					if (first == 0 && v.getTop() == padTop) {
						mScrolling = false;
						return;
					}
				}
				mScrollSpeed = mScrollProfile.getSpeed((mUpScrollStartYF - maxY)
						/ mDragUpScrollHeight, mPrevTime);
			} else {
				View v = getChildAt(last - first);
				if (v == null) {
					mScrolling = false;
					return;
				} else {
					if (last == count - 1 && v.getBottom() <= listHeight + padTop) {
						mScrolling = false;
						return;
					}
				}
				mScrollSpeed = -mScrollProfile.getSpeed((minY - mDownScrollStartYF)
						/ mDragDownScrollHeight, mPrevTime);
			}

			mCurrTime = SystemClock.uptimeMillis();
			dt = (float) (mCurrTime - mPrevTime);

			dy = Math.round(mScrollSpeed * dt);

			int movePos;
			if (dy >= 0) {
				dy = Math.min(listHeight, dy);
				movePos = first;
			} else {
				dy = Math.max(-listHeight, dy);
				movePos = last;
			}

			final View moveItem = getChildAt(movePos - first);
			int top = moveItem.getTop() + dy;

			if (movePos == 0 && top > padTop) {
				top = padTop;
			}

			mBlockLayoutRequests = true;

			setSelectionFromTop(movePos, top - padTop);
			SortableListView.this.layoutChildren();
			invalidate();

			mBlockLayoutRequests = false;

			doDragFloatView(movePos, moveItem, false);

			mPrevTime = mCurrTime;

			post(this);
		}
	}
}
