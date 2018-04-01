package com.sapuseven.untis.utils.lazyload;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;

import com.sapuseven.untis.view.SortableListView;

public class SimpleFloatViewManager implements SortableListView.FloatViewManager {
	private Bitmap floatBitmap;
	private ImageView imageView;
	private int floatBGColor = Color.BLACK;
	private final ListView listView;

	protected SimpleFloatViewManager(ListView lv) {
		listView = lv;
	}

	public void setBackgroundColor(int color) {
		floatBGColor = color;
	}

	@Override
	public View onCreateFloatView(int position) {
		if (listView == null)
			return null;

		View v = listView.getChildAt(position + listView.getHeaderViewsCount() - listView.getFirstVisiblePosition());

		if (v == null)
			return null;

		v.setPressed(false);

		v.setDrawingCacheEnabled(true);
		floatBitmap = Bitmap.createBitmap(v.getDrawingCache());
		v.setDrawingCacheEnabled(false);

		if (imageView == null) {
			imageView = new ImageView(listView.getContext());
		}
		imageView.setBackgroundColor(floatBGColor);
		imageView.setPadding(0, 0, 0, 0);
		imageView.setImageBitmap(floatBitmap);
		imageView.setLayoutParams(new ViewGroup.LayoutParams(v.getWidth(), v.getHeight()));

		return imageView;
	}

	@Override
	public void onDestroyFloatView(View floatView) {
		((ImageView) floatView).setImageDrawable(null);

		floatBitmap.recycle();
		floatBitmap = null;
	}
}