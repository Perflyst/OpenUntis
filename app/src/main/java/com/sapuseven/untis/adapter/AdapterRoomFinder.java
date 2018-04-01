package com.sapuseven.untis.adapter;

import android.support.annotation.NonNull;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.sapuseven.untis.R;
import com.sapuseven.untis.activity.ActivityRoomFinder;

import java.util.ArrayList;

public class AdapterRoomFinder extends RecyclerView.Adapter<AdapterRoomFinder.ViewHolder> {
	private final ActivityRoomFinder activity;
	private final ArrayList<AdapterItemRoomFinder> roomList;

	public AdapterRoomFinder(ActivityRoomFinder activity, ArrayList<AdapterItemRoomFinder> roomList) {
		this.activity = activity;
		this.roomList = roomList;

		setHasStableIds(true);
	}

	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_room_finder, parent, false);
		v.setOnClickListener(activity);
		return new ViewHolder(v);
	}

	@Override
	public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
		int currentHourIndex = activity.getCurrentHourIndex();
		AdapterItemRoomFinder room = roomList.get(position);

		holder.tvName.setText(room.getName());

		if (room.getState(currentHourIndex) == AdapterItemRoomFinder.STATE_OCCUPIED)
			holder.tvDetails.setText(activity.getResources().getString(R.string.room_desc_occupied));
		else if (room.getState(currentHourIndex) >= AdapterItemRoomFinder.STATE_FREE)
			holder.tvDetails.setText(activity.getResources().getQuantityString(R.plurals.room_desc, room.getState(currentHourIndex), room.getState(currentHourIndex)));
		else
			holder.tvDetails.setText(activity.getResources().getString(R.string.loading_data));

		if (room.getState(currentHourIndex) >= AdapterItemRoomFinder.STATE_FREE && !room.isLoading()) {
			holder.ivState.setImageResource(R.drawable.ic_room_available);
			holder.ivState.setVisibility(View.VISIBLE);
			holder.pbState.setVisibility(View.GONE);
			holder.btnRoomExpired.setVisibility(room.isOutdated() ? View.VISIBLE : View.GONE);
		} else if (room.getState(currentHourIndex) == AdapterItemRoomFinder.STATE_OCCUPIED && !room.isLoading()) {
			holder.ivState.setImageResource(R.drawable.ic_room_occupied);
			holder.ivState.setVisibility(View.VISIBLE);
			holder.pbState.setVisibility(View.GONE);
			holder.btnRoomExpired.setVisibility(room.isOutdated() ? View.VISIBLE : View.GONE);
		} else {
			holder.ivState.setVisibility(View.GONE);
			holder.pbState.setVisibility(View.VISIBLE);
			holder.btnRoomExpired.setVisibility(View.GONE);
		}

		holder.btnDelete.setOnClickListener(v -> activity.showDeleteItemDialog(holder.getAdapterPosition()));

		holder.btnRoomExpired.setOnClickListener(v -> activity.refreshItem(holder.getAdapterPosition()));
	}

	@Override
	public int getItemCount() {
		return roomList.size();
	}

	@Override
	public long getItemId(int position) {
		return roomList.get(position).hashCode();
	}

	static class ViewHolder extends RecyclerView.ViewHolder {
		final TextView tvName;
		final TextView tvDetails;
		final AppCompatImageView ivState;
		final ProgressBar pbState;
		final ImageButton btnDelete;
		final ImageButton btnRoomExpired;

		ViewHolder(View rootView) {
			super(rootView);

			tvName = rootView.findViewById(R.id.tvName);
			tvDetails = rootView.findViewById(R.id.tvDetails);
			ivState = rootView.findViewById(R.id.ivState);
			pbState = rootView.findViewById(R.id.pbState);
			btnDelete = rootView.findViewById(R.id.btnDelete);
			btnRoomExpired = rootView.findViewById(R.id.btnRoomExpired);
		}
	}
}