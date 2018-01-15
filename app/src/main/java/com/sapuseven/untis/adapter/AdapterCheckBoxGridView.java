package com.sapuseven.untis.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.Filter;
import android.widget.Filterable;

import com.sapuseven.untis.R;

import java.util.ArrayList;
import java.util.List;

public class AdapterCheckBoxGridView extends BaseAdapter implements Filterable {
	private final List<String> originalItems;
	private final ItemFilter filter = new ItemFilter();
	private final LayoutInflater inflater;
	private final List<String> selectedItems;
	private List<String> filteredItems;

	public AdapterCheckBoxGridView(@NonNull Context context, @NonNull List<String> objects) {
		filteredItems = objects;
		originalItems = new ArrayList<>(filteredItems);
		selectedItems = new ArrayList<>();
		inflater = LayoutInflater.from(context);
	}

	@NonNull
	public Filter getFilter() {
		return filter;
	}

	@Override
	public int getCount() {
		return filteredItems.size();
	}

	@Override
	public Object getItem(int position) {
		return position;
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		Holder holder = new Holder();
		if (convertView == null)
			convertView = inflater.inflate(R.layout.grid_view_item_checkboxes, parent, false);
		holder.checkBox = convertView.findViewById(R.id.checkbox);
		holder.checkBox.setText(filteredItems.get(position));
		holder.checkBox.setOnCheckedChangeListener(null);
		holder.checkBox.setChecked(selectedItems.contains(filteredItems.get(position)));
		holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
			if (isChecked && !selectedItems.contains(filteredItems.get(position)))
				selectedItems.add(filteredItems.get(position));
			else if (selectedItems.contains(filteredItems.get(position)))
				selectedItems.remove(filteredItems.get(position));
		});
		return convertView;
	}

	public List<String> getSelectedItems() {
		return selectedItems;
	}

	private class Holder {
		CheckBox checkBox;
	}

	private class ItemFilter extends Filter {
		@Override
		protected void publishResults(CharSequence constraint, FilterResults results) {
			filteredItems.clear();
			try {
				//noinspection unchecked
				filteredItems.addAll((List<String>) results.values);
			} catch (ClassCastException e) {
				filteredItems.addAll(originalItems);
			}
			notifyDataSetChanged();
		}

		@Override
		protected FilterResults performFiltering(CharSequence constraint) {
			FilterResults results = new FilterResults();
			List<String> filteredList = new ArrayList<>();

			if (filteredItems == null) {
				filteredItems = new ArrayList<>(filteredList);
			}

			if (constraint == null || constraint.length() == 0) {
				results.count = originalItems.size();
				results.values = originalItems;
			} else {
				constraint = constraint.toString().toLowerCase();
				for (int i = 0; i < originalItems.size(); i++) {
					String data = originalItems.get(i);
					if (data.toLowerCase().contains(constraint.toString().toLowerCase()))
						filteredList.add(data);

				}
				results.count = filteredList.size();
				results.values = filteredList;
			}
			return results;
		}
	}
}
