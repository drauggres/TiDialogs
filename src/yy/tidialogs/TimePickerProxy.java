package yy.tidialogs;

import java.util.Calendar;
import java.util.Date;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.view.TiUIView;

import android.R;
import android.app.Activity;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.text.format.DateFormat;
import android.os.Build;
import android.widget.TimePicker;

import ti.modules.titanium.ui.widget.picker.TiTimePickerDialog;

@Kroll.proxy(creatableInModule = TidialogsModule.class)
public class TimePickerProxy extends BaseDialogProxy
{
	private class BasicTimePicker extends BaseUIDialog
	{

		private int hour;
		private int minute;
		private boolean is24HourView;

		private String okButtonTitle;
		private String cancelButtonTitle;

		public BasicTimePicker(TiViewProxy proxy)
		{
			super(proxy);
		}

		protected TimePickerDialog getDialog()
		{
			if (dialog != null) {
				return (TimePickerDialog) dialog;
			}
			TimePickerDialog.OnTimeSetListener timeSetListener = new TimePickerDialog.OnTimeSetListener() {

				@Override
				public void onTimeSet(TimePicker selectedTime, int selectedHour, int selectedMinute)
				{
					// TODO Auto-generated method stub

					hour = selectedHour;
					minute = selectedMinute;

					KrollDict data = new KrollDict();

					Calendar calendar = Calendar.getInstance();
					calendar.set(Calendar.HOUR_OF_DAY, hour);
					calendar.set(Calendar.MINUTE, minute);
					Date value = calendar.getTime();

					data.put("value", value);
					data.put("hour", hour);
					data.put("minute", minute);
					fireEvent("click", data);
				}
			};

			// TimePickerDialog has a bug in Android 4.x
			// If build version is using Android 4.x, use
			// our TiTimePickerDialog. It was fixed from Android 5.0.
			TimePickerDialog picker;

			Activity activity = TiApplication.getAppCurrentActivity();
			if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH)
				&& (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)) {
				picker = new TiTimePickerDialog(activity, timeSetListener, hour, minute, is24HourView);
			} else {
				picker = new TimePickerDialog(activity, timeSetListener, hour, minute, is24HourView);
			}

			picker.setCanceledOnTouchOutside(false);

			picker.setButton(DialogInterface.BUTTON_POSITIVE, okButtonTitle, picker);

			picker.setOnDismissListener(dismissListener);

			dialog = picker;
			return picker;
		}

		@Override
		public void processProperties(KrollDict d)
		{
			super.processProperties(d);

			Calendar c = Calendar.getInstance();
			if (d.containsKey("value")) {
				c.setTime((Date) d.get("value"));
				hour = c.get(Calendar.HOUR_OF_DAY);
				minute = c.get(Calendar.MINUTE);
			} else {
				if (d.containsKey("hour")) {
					hour = d.getInt("hour");
				} else {
					hour = c.get(Calendar.HOUR_OF_DAY);
				}
				if (d.containsKey("minute")) {
					minute = d.getInt("minute");
				} else {
					minute = c.get(Calendar.MINUTE);
				}
			}

			if (d.containsKey("format24")) {
				is24HourView = TiConvert.toBoolean(d, "format24");
			} else {
				is24HourView = DateFormat.is24HourFormat(TiApplication.getAppCurrentActivity());
			}

			if (d.containsKey("okButtonTitle")) {
				okButtonTitle = d.getString("okButtonTitle");
			} else {
				okButtonTitle =
					TiApplication.getAppCurrentActivity().getApplication().getResources().getString(R.string.ok);
			}
			if (d.containsKey("cancelButtonTitle")) {
				cancelButtonTitle = d.getString("cancelButtonTitle");
			} else {
				cancelButtonTitle =
					TiApplication.getAppCurrentActivity().getApplication().getResources().getString(R.string.cancel);
			}
		}
	}

	public TimePickerProxy()
	{
		super();
	}

	@Override
	public TiUIView createView(Activity activity)
	{
		return new BasicTimePicker(this);
	}

	@Override
	public void handleCreationDict(KrollDict options)
	{
		super.handleCreationDict(options);
	}
}