package com.sapuseven.untis.test;

import com.sapuseven.untis.utils.DateOperations;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class DateOperationsTest {
	@Rule
	public ExpectedException exceptionGrabber = ExpectedException.none();

	@Test(expected = RuntimeException.class)
	public void dateOperations_constructor() {
		new DateOperations();
	}

	@Test
	public void dateOperations_startDateFromWeek() {
		Calendar week = Calendar.getInstance(Locale.ENGLISH);
		week.set(2018, Calendar.MAY, 9);
		week.set(Calendar.HOUR_OF_DAY, 1);
		week.set(Calendar.MINUTE, 1);
		week.set(Calendar.SECOND, 1);
		week.set(Calendar.MILLISECOND, 1);

		assertThat(week.get(Calendar.YEAR), is(2018));
		assertThat(week.get(Calendar.MONTH), is(Calendar.MAY));
		assertThat(week.get(Calendar.DAY_OF_MONTH), is(9));
		assertThat(week.get(Calendar.HOUR_OF_DAY), is(1));
		assertThat(week.get(Calendar.MINUTE), is(1));
		assertThat(week.get(Calendar.SECOND), is(1));
		assertThat(week.get(Calendar.MILLISECOND), is(1));

		Calendar startDay = DateOperations.getStartDateFromWeek(week, 0);
		assertThat(startDay.get(Calendar.YEAR), is(2018));
		assertThat(startDay.get(Calendar.MONTH), is(Calendar.MAY));
		assertThat(startDay.get(Calendar.DAY_OF_MONTH), is(7));
		assertThat(startDay.get(Calendar.HOUR_OF_DAY), is(1));
		assertThat(startDay.get(Calendar.MINUTE), is(1));
		assertThat(startDay.get(Calendar.SECOND), is(1));

		Calendar startDayWithOffset = DateOperations.getStartDateFromWeek(week, 1);
		assertThat(startDayWithOffset.get(Calendar.YEAR), is(2018));
		assertThat(startDayWithOffset.get(Calendar.MONTH), is(Calendar.MAY));
		assertThat(startDayWithOffset.get(Calendar.DAY_OF_MONTH), is(8));
		assertThat(startDayWithOffset.get(Calendar.HOUR_OF_DAY), is(1));
		assertThat(startDayWithOffset.get(Calendar.MINUTE), is(1));
		assertThat(startDayWithOffset.get(Calendar.SECOND), is(1));

		Calendar startDayResetTime = DateOperations.getStartDateFromWeek(week, 0, true);
		assertThat(startDayResetTime.get(Calendar.YEAR), is(2018));
		assertThat(startDayResetTime.get(Calendar.MONTH), is(Calendar.MAY));
		assertThat(startDayResetTime.get(Calendar.DAY_OF_MONTH), is(7));
		assertThat(startDayResetTime.get(Calendar.HOUR_OF_DAY), is(0));
		assertThat(startDayResetTime.get(Calendar.MINUTE), is(0));
		assertThat(startDayResetTime.get(Calendar.SECOND), is(0));
	}

	@Test
	public void dateOperations_addDaysToInt() {
		assertThat(DateOperations.addDaysToInt(20180509, 2), is(20180511));
		assertThat(DateOperations.addDaysToInt(20180509, 24), is(20180602));
		assertThat(DateOperations.addDaysToInt(20180509, 0), is(20180509));

		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'", Locale.ENGLISH);
		assertThat(DateOperations.addDaysToInt(20180509, 2, dateFormat), is("2018-05-11T"));
		assertThat(DateOperations.addDaysToInt(20180509, 24, dateFormat), is("2018-06-02T"));
		assertThat(DateOperations.addDaysToInt(20180509, 0, dateFormat), is("2018-05-09T"));

		assertThat(DateOperations.addDaysToInt(1, 0, dateFormat), nullValue());
		assertThat(DateOperations.addDaysToInt(-1, 0, dateFormat), nullValue());
		assertThat(DateOperations.addDaysToInt(1, 0), is(1));
		assertThat(DateOperations.addDaysToInt(-1, 0), is(-1));
	}

	@Test
	public void dateOperations_getStringDateFromInt() {
		assertThat(DateOperations.getStringDateFromInt(20180415, Locale.ENGLISH), is("15. Apr"));
		assertThat(DateOperations.getStringDateFromInt(20190502, Locale.ENGLISH), is("2. May"));
		assertThat(DateOperations.getStringDateFromInt(20200502, Locale.GERMAN), is("2. Mai"));

		assertThat(DateOperations.getStringDateFromInt(1, Locale.ENGLISH), nullValue());
		assertThat(DateOperations.getStringDateFromInt(-1, Locale.ENGLISH), nullValue());
		assertThat(DateOperations.getStringDateFromInt(1, Locale.GERMAN), nullValue());
		assertThat(DateOperations.getStringDateFromInt(-1, Locale.GERMAN), nullValue());
	}

	@Test
	public void dateOperations_getDayNameFromInt() {
		assertThat(DateOperations.getDayNameFromInt(20180416, Locale.ENGLISH), is("Mon"));
		assertThat(DateOperations.getDayNameFromInt(20190502, Locale.ENGLISH), is("Thu"));
		assertThat(DateOperations.getDayNameFromInt(20200502, Locale.GERMAN), is("Sa"));

		assertThat(DateOperations.getDayNameFromInt(1, Locale.ENGLISH), nullValue());
		assertThat(DateOperations.getDayNameFromInt(-1, Locale.ENGLISH), nullValue());
		assertThat(DateOperations.getDayNameFromInt(1, Locale.GERMAN), nullValue());
		assertThat(DateOperations.getDayNameFromInt(-1, Locale.GERMAN), nullValue());
	}

	@Test
	public void dateOperations_parseFromISO() throws ParseException {
		Date expected = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.ENGLISH).parse("2018-05-09T22:10");
		assertThat(DateOperations.parseFromISO("2018-05-09T22:10"), is(expected));
	}

	@Test
	public void dateOperations_getComparableTime() {
		assertThat(DateOperations.getComparableTime("08:15"), is(815));
		assertThat(DateOperations.getComparableTime("10:30Z"), is(1030));
		assertThat(DateOperations.getComparableTime("T13:45"), is(1345));

		exceptionGrabber.expect(IllegalArgumentException.class);
		DateOperations.getComparableTime("");
	}
}
