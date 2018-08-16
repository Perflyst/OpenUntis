package de.perflyst.untis.adapter;

import android.content.Context;

import de.perflyst.untis.view.SortableListViewItem;

public class AdapterItemFeatures extends SortableListViewItem {
	private String title;
	private String desc;
	private int id;
	private String label;

	public AdapterItemFeatures(Context context) {
		super(context);
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}
}
