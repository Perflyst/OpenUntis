package de.perflyst.untis.adapter;

import android.support.annotation.NonNull;

import de.perflyst.untis.activity.ActivityRoomFinder;

import java.util.Calendar;

import static de.perflyst.untis.utils.DateOperations.getStartDateFromWeek;

public class AdapterItemRoomFinder implements Comparable<AdapterItemRoomFinder> {
	static final int STATE_OCCUPIED = 0;
	static final int STATE_FREE = 1;
	private static final int STATE_LOADING = -1;
	private final String name;
	private final ActivityRoomFinder roomFinderActivity;
	private boolean[] states;
	private boolean loading;
	private long date;

	public AdapterItemRoomFinder(ActivityRoomFinder roomFinderActivity, String name, boolean loading) {
		this.roomFinderActivity = roomFinderActivity;
		this.name = name;
		this.loading = loading;
	}

	private long getDate() {
		return date;
	}

	public void setDate(long date) {
		this.date = date;
	}

	private int getIndex() {
		return roomFinderActivity.getCurrentHourIndex();
	}

	public void setStates(boolean[] states) {
		this.states = states;
	}

	public String getName() {
		return name;
	}

	int getState(int index) {
		if (isLoading())
			return STATE_LOADING;
		int i = 0, hours = 0;
		while (index + i < states.length && !states[index + i]) {
			hours++;
			i++;
		}
		return hours;
	}

	@Override
	public int compareTo(@NonNull AdapterItemRoomFinder o) {
		int state1 = getState(getIndex());
		int state2 = o.getState(o.getIndex());

		if (state1 < state2)
			return 1;
		else if (state1 > state2)
			return -1;
		else
			return getName().compareTo(o.getName());
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof AdapterItemRoomFinder && ((AdapterItemRoomFinder) o).getName().equals(getName());
	}

	boolean isLoading() {
		return loading;
	}

	public void setLoading() {
		this.loading = true;
	}

	public boolean isOutdated() {
		return getDate() != getStartDateFromWeek(Calendar.getInstance(), 0, true).getTimeInMillis();
	}
}
