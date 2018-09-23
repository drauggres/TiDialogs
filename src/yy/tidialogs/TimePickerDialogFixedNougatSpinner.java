/*
 * https://gist.github.com/jeffdgr8/6bc5f990bf0c13a7334ce385d482af9f
 * MIT License
 *
 * Copyright (c) 2018 Jeff Lockhart
 */

package yy.tidialogs;

import android.app.TimePickerDialog;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.TimePicker;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

/**
 * Workaround for this bug: https://code.google.com/p/android/issues/detail?id=222208
 * In Android 7.0 Nougat, spinner mode for the TimePicker in TimePickerDialog is
 * incorrectly displayed as clock, even when the theme specifies otherwise, such as:
 *
 *  <resources>
 *      <style name="Theme.MyApp" parent="Theme.AppCompat.Light.NoActionBar">
 *          <item name="android:timePickerStyle">@style/Widget.MyApp.TimePicker</item>
 *      </style>
 *
 *      <style name="Widget.MyApp.TimePicker" parent="android:Widget.Material.TimePicker">
 *          <item name="android:timePickerMode">spinner</item>
 *      </style>
 *  </resources>
 *
 * May also pass TimePickerDialog.THEME_HOLO_LIGHT as an argument to the constructor,
 * as this theme has the TimePickerMode set to spinner.
 */
public class TimePickerDialogFixedNougatSpinner extends TimePickerDialog {

	/**
	 * Creates a new time picker dialog.
	 *
	 * @param context the parent context
	 * @param listener the listener to call when the time is set
	 * @param hourOfDay the initial hour
	 * @param minute the initial minute
	 * @param is24HourView whether this is a 24 hour view or AM/PM
	 */
	public TimePickerDialogFixedNougatSpinner(Context context, OnTimeSetListener listener, int hourOfDay, int minute, boolean is24HourView) {

		super(context, listener, hourOfDay, minute, is24HourView);
		fixSpinner(context, hourOfDay, minute, is24HourView);
	}

	/**
	 * Creates a new time picker dialog with the specified theme.
	 *
	 * @param context the parent context
	 * @param themeResId the resource ID of the theme to apply to this dialog
	 * @param listener the listener to call when the time is set
	 * @param hourOfDay the initial hour
	 * @param minute the initial minute
	 * @param is24HourView Whether this is a 24 hour view, or AM/PM.
	 */
	public TimePickerDialogFixedNougatSpinner(Context context, int themeResId, OnTimeSetListener listener, int hourOfDay, int minute, boolean is24HourView) {

		super(context, themeResId, listener, hourOfDay, minute, is24HourView);
		fixSpinner(context, hourOfDay, minute, is24HourView);
	}

	private void fixSpinner(Context context, int hourOfDay, int minute, boolean is24HourView) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) { // android:timePickerMode spinner and clock began in Lollipop
			try {
				// Get the theme's android:timePickerMode
				final int MODE_SPINNER = 1;
				Class<?> styleableClass = Class.forName("com.android.internal.R$styleable");
				Field timePickerStyleableField = styleableClass.getField("TimePicker");
				int[] timePickerStyleable = (int[]) timePickerStyleableField.get(null);
				final TypedArray a = context.obtainStyledAttributes(null, timePickerStyleable, android.R.attr.timePickerStyle, 0);
				Field timePickerModeStyleableField = styleableClass.getField("TimePicker_timePickerMode");
				int timePickerModeStyleable = timePickerModeStyleableField.getInt(null);
				final int mode = a.getInt(timePickerModeStyleable, MODE_SPINNER);
				a.recycle();
				if (mode == MODE_SPINNER) {
					TimePicker timePicker = (TimePicker) findField(TimePickerDialog.class, TimePicker.class, "mTimePicker").get(this);
					Class<?> delegateClass = Class.forName("android.widget.TimePicker$TimePickerDelegate");
					Field delegateField = findField(TimePicker.class, delegateClass, "mDelegate");
					Object delegate = delegateField.get(timePicker);
					Class<?> spinnerDelegateClass;
					if (Build.VERSION.SDK_INT != Build.VERSION_CODES.LOLLIPOP) {
						spinnerDelegateClass = Class.forName("android.widget.TimePickerSpinnerDelegate");
					} else {
						// TimePickerSpinnerDelegate was initially misnamed TimePickerClockDelegate in API 21!
						spinnerDelegateClass = Class.forName("android.widget.TimePickerClockDelegate");
					}
					// In 7.0 Nougat for some reason the timePickerMode is ignored and the delegate is TimePickerClockDelegate
					if (delegate.getClass() != spinnerDelegateClass) {
						delegateField.set(timePicker, null); // throw out the TimePickerClockDelegate!
						timePicker.removeAllViews(); // remove the TimePickerClockDelegate views
						Constructor spinnerDelegateConstructor = spinnerDelegateClass.getConstructor(TimePicker.class, Context.class, AttributeSet.class, int.class, int.class);
						spinnerDelegateConstructor.setAccessible(true);
						// Instantiate a TimePickerSpinnerDelegate
						delegate = spinnerDelegateConstructor.newInstance(timePicker, context, null, android.R.attr.timePickerStyle, 0);
						delegateField.set(timePicker, delegate); // set the TimePicker.mDelegate to the spinner delegate
						// Set up the TimePicker again, with the TimePickerSpinnerDelegate
						timePicker.setIs24HourView(is24HourView);
						timePicker.setCurrentHour(hourOfDay);
						timePicker.setCurrentMinute(minute);
						timePicker.setOnTimeChangedListener(this);
					}
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	private static Field findField(Class objectClass, Class fieldClass, String expectedName) {
		try {
			Field field = objectClass.getDeclaredField(expectedName);
			field.setAccessible(true);
			return field;
		} catch (NoSuchFieldException e) {} // ignore
		// search for it if it wasn't found under the expected ivar name
		for (Field searchField : objectClass.getDeclaredFields()) {
			if (searchField.getType() == fieldClass) {
				searchField.setAccessible(true);
				return searchField;
			}
		}
		return null;
	}
}