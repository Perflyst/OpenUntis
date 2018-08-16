package de.perflyst.untis.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import de.perflyst.untis.R;

import java.util.List;

public class AdapterFeatures extends BaseAdapter {
	private static LayoutInflater inflater = null;
	private final List<AdapterItemFeatures> data;

	public AdapterFeatures(Context context, List<AdapterItemFeatures> data) {
		this.data = data;
		inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	@Override
	public int getCount() {
		return data.size();
	}

	@Override
	public Object getItem(int position) {
		return data.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(final int position, final View convertView, ViewGroup parent) {
		View v = convertView;

		if (data.get(position).getLabel() != null) {
			if (v == null || !(v instanceof TextView))
				v = inflater.inflate(R.layout.list_item_features_text, parent, false);

			((TextView) v.findViewById(R.id.tvText)).setText(data.get(position).getLabel());
		} else {
			v = inflater.inflate(R.layout.list_item_features, parent, false);

			((TextView) v.findViewById(R.id.tvTitle)).setText(data.get(position).getTitle());
			((TextView) v.findViewById(R.id.tvDesc)).setText(data.get(position).getDesc());
		}

		return v;
	}

	public void remove(int index) {
		data.remove(index);
	}

	public void insert(AdapterItemFeatures item, int index) {
		data.add(index, item);
	}

	public List<AdapterItemFeatures> getData() {
		return data;
	}
}