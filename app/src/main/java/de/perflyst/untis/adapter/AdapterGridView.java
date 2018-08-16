package de.perflyst.untis.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;

import java.util.ArrayList;
import java.util.List;

public class AdapterGridView extends ArrayAdapter<String> implements Filterable {
	private final List<String> originalItems;
	private final ItemFilter filter = new ItemFilter();
	private List<String> filteredItems;

	public AdapterGridView(@NonNull Context context, @NonNull List<String> objects) {
		super(context, android.R.layout.simple_list_item_1, objects);
		filteredItems = objects;
		originalItems = new ArrayList<>(filteredItems);
	}

	@NonNull
	public Filter getFilter() {
		return filter;
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