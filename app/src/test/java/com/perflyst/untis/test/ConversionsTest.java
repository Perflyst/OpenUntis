package com.perflyst.untis.test;

import android.content.Context;
import android.content.res.Resources;
import android.util.DisplayMetrics;

import com.perflyst.untis.utils.Conversions;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ConversionsTest {
	@Rule
	public ExpectedException exceptionGrabber = ExpectedException.none();

	@Test(expected = RuntimeException.class)
	public void dateOperations_constructor() {
		new Conversions();
	}

	@Test
	public void conversions_dp2px() {
		DisplayMetrics metrics = mock(DisplayMetrics.class);

		Resources resources = mock(Resources.class);
		when(resources.getDisplayMetrics())
				.thenReturn(metrics);

		Context context = mock(Context.class);
		when(context.getResources())
				.thenReturn(resources);


		metrics.density = 1.5f;

		Conversions.setScale(context);
		assertThat(Conversions.dp2px(12), is(18));


		metrics.density = 2;

		Conversions.setScale(context);
		assertThat(Conversions.dp2px(12), is(24));
	}

	@Test
	public void conversions_dp2px_noScale() {
		exceptionGrabber.expect(IllegalStateException.class);
		Conversions.dp2px(12);
	}
}
