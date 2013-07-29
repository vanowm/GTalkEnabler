package com.innodroid.gtalkenabler;

import java.io.File;
import java.io.IOException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;

import com.stericson.RootTools.RootTools;
import com.stericson.RootTools.RootToolsException;

public class GTalkEnablerActivity extends Activity
{
	
	private static final int DIALOG_NOT_ROOT = 101;
	private static final int DIALOG_DB_ERROR = 102;
	private static final int DIALOG_BAD_VERSION = 103;
	private static final int DIALOG_CONFIRM = 104;
	private static final int AUTO_NONE = 0;
	private static final int AUTO_REBOOT = 1;
	private static final int AUTO_KILL = 2;
	private static final String DB_FILE_NAME = "gservices.db";
	private static final String DB_PATH = "/data/data/com.google.android.gsf/databases/";
	private static String DB_TABLE = "overrides";
	private static final String DB_TABLE2 = "main";
	private static final String DB_SETTING = "gtalk_vc_wifi_only";
	private File mPrivateDatabaseFile;
	private String mPrivateDatabasePath;
	private String mGoogleDatabasePath;
	private boolean mMustInsert;
	private int sAuto;
	private String appName = "GTalk";
	private String sSettings = "settings";
	private RadioGroup mAuto;
	private int aAuto[] = {R.id.radio_none, R.id.radio_reboot, R.id.radio_kill};
	private PackageManager pm;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		ApplicationInfo ai;
		pm = (PackageManager)getApplicationContext().getPackageManager();
		try 
		{
			ai = pm.getApplicationInfo("com.google.android.talk", 0);
			appName = (String) pm.getApplicationLabel(ai);
		} catch (Exception e) {}
		try
		{
			setTitle(getString(R.string.app_name) + " v" + getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
		}
		catch(Exception e){}
		setContentView(R.layout.main);
		((Button)findViewById(R.id.button_enable)).setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				new WriteSettingTask(true).execute();
			}
		});
		((Button)findViewById(R.id.button_disable)).setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				new WriteSettingTask(false).execute();
			}
		});

		TextView t = (TextView)findViewById(R.id.textView);
		t.setText(t.getText().toString().replace("%APP%", appName));
		RadioButton r = (RadioButton)findViewById(R.id.radio_kill);
		r.setText(r.getText().toString().replace("%APP%", appName));
		final SharedPreferences settings = getSharedPreferences(sSettings, MODE_PRIVATE);
		sAuto = settings.getInt("auto", 2);
		mAuto = (RadioGroup)findViewById(R.id.radiogroup_auto);
		if (sAuto >= aAuto.length || sAuto < 0)
			sAuto = 2;

		mAuto.check(aAuto[sAuto]);
		mAuto.setOnCheckedChangeListener(new OnCheckedChangeListener()
		{
			@Override
			public void onCheckedChanged(RadioGroup radioGroup, int checkedId)
			{
				SharedPreferences.Editor editor = settings.edit();
					editor.putInt("auto", getId(checkedId));
					editor.commit();
			}
		});

		mGoogleDatabasePath = DB_PATH + DB_FILE_NAME;
		mPrivateDatabaseFile = new File(getFilesDir(), DB_FILE_NAME);
		mPrivateDatabasePath = mPrivateDatabaseFile.getAbsolutePath();
		
		// I wish this worked on honeycomb or later... but it doesnt 
		//if (Build.VERSION.SDK_INT > Build.VERSION_CODES.GINGERBREAD_MR1)
			//showDialog(DIALOG_BAD_VERSION);
		if (RootTools.isRootAvailable() && RootTools.isAccessGiven())
			new ReadSettingTask().execute();
		else
			showDialog(DIALOG_NOT_ROOT);
	}

	@Override
	protected Dialog onCreateDialog(int id)
	{
		switch (id)
		{
			case DIALOG_NOT_ROOT:
				return createDialog("Root Denied", "Unable to get root access. Exiting application.", android.R.drawable.ic_dialog_alert);
			case DIALOG_DB_ERROR:
				return createDialog("Database Error", "Unable to access database. Is video chat enabled Talk installed?", android.R.drawable.ic_dialog_alert);
			case DIALOG_BAD_VERSION:
				return createDialog("Sorry", "This doesn't work on your version of Android OS", android.R.drawable.ic_dialog_alert);
			case DIALOG_CONFIRM:
				return createDialog("Done", "Settings updated. You may need to reboot.", android.R.drawable.ic_dialog_info);
			default:
				return super.onCreateDialog(id);
		}
	}
	
	private Dialog createDialog(String title, String message, int icon)
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(title)
				.setIcon(icon)
				.setMessage(message)
				.setCancelable(false)
				.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						finish();
					}
				});
		return builder.create();
	}

	private void copyFileAsRoot(String src, String dest) throws IOException, InterruptedException, RootToolsException
	{
		RootTools.sendShell("cp -f " + src + " " + dest + "\n");
		RootTools.sendShell("chmod 777 " + dest + "\n");
	}

	private abstract class BaseTask extends AsyncTask<Void, Void, Boolean>
	{
		private ProgressDialog mProgressDialog;
		@Override
		protected void onPreExecute()
		{
			super.onPreExecute();
			mProgressDialog = ProgressDialog.show(GTalkEnablerActivity.this, null, "Please Wait", true, false);
		}
		
		@Override
		protected void onPostExecute(Boolean result)
		{
			super.onPostExecute(result);
			mProgressDialog.dismiss();
		}
		
		@Override
		protected void onCancelled() {
			super.onCancelled();

			mProgressDialog.dismiss();
		}
	}

	private class ReadSettingTask extends BaseTask
	{
		@Override
		protected Boolean doInBackground(Void... arg0)
		{
			SQLiteDatabase db = null;
			
			try
			{
				copyFileAsRoot(mGoogleDatabasePath, mPrivateDatabasePath);
				
				db = SQLiteDatabase.openDatabase(mPrivateDatabasePath, null, SQLiteDatabase.OPEN_READWRITE);
				Cursor cursor = db.query(DB_TABLE, new String[] { "value" }, "name = ?", new String[] { DB_SETTING }, null, null, null);
				
				if (!cursor.moveToFirst())
				{
					mMustInsert = true; 
					return true;
				}
				return cursor.getString(0).compareToIgnoreCase("true") == 0;
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
				if (db != null && db.isOpen())
					db.close();

				try
				{
					copyFileAsRoot(mGoogleDatabasePath, mPrivateDatabasePath);
					DB_TABLE = DB_TABLE2;
					db = SQLiteDatabase.openDatabase(mPrivateDatabasePath, null, SQLiteDatabase.OPEN_READWRITE);
					Cursor cursor = db.query(DB_TABLE2, new String[] { "value" }, "name = ?", new String[] { DB_SETTING }, null, null, null);
					if (!cursor.moveToFirst())
					{
						mMustInsert = true; 
						return true;
					}

					return cursor.getString(0).compareToIgnoreCase("true") == 0;
				}
				catch (Exception ex2)
				{
					ex2.printStackTrace();
					return null;
				}
				finally
				{
					if (db != null && db.isOpen())
						db.close();
				}

			}
			finally
			{
				if (db != null && db.isOpen())
					db.close();
			}
		}

		@Override
		protected void onPostExecute(Boolean result)
		{
			super.onPostExecute(result);

			if (result == null)
				showDialog(DIALOG_DB_ERROR);
			else
				((TextView)findViewById(R.id.status_string)).setText(result ? "disabled" : "enabled");
		}
	}
	
	private class WriteSettingTask extends BaseTask
	{
		private boolean mWifiOnly;

		public WriteSettingTask(boolean wifiOnly)
		{
			mWifiOnly = !wifiOnly;
		}
		
		@Override
		protected Boolean doInBackground(Void... arg0)
		{
			SQLiteDatabase db = null;
			ContentValues values = new ContentValues();
			values.put("name", DB_SETTING);
			values.put("value", Boolean.toString(mWifiOnly));

			try
			{
				db = SQLiteDatabase.openDatabase(mPrivateDatabasePath, null, SQLiteDatabase.OPEN_READWRITE);
	
				if (mMustInsert)
					db.insertOrThrow(DB_TABLE, null, values);
				else
					db.update(DB_TABLE, values, "name = ?", new String[] { DB_SETTING });
	
				db.close();
				
				copyFileAsRoot(mPrivateDatabasePath, mGoogleDatabasePath);
				return true;
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
				return false;
			}
			finally
			{
				if (db != null && db.isOpen())
					db.close();
			}
		}

		@Override
		protected void onPostExecute(Boolean result)
		{
			super.onPostExecute(result);
	
			if (result == null || result == false)
				showDialog(DIALOG_DB_ERROR);
			else
			{
				String command = null;
				switch(getId(mAuto.getCheckedRadioButtonId()))
				{
					case AUTO_NONE:
						showDialog(DIALOG_CONFIRM);
						break;
					case AUTO_REBOOT:
						command = "su -c reboot";
						break;
					case AUTO_KILL:
						command = "su -c am start -a android.intent.action.MAIN -n com.google.android.talk/com.google.android.talk.SignoutActivity -d content://com.google.android.providers.talk/accounts && am force-stop com.google.android.talk && am force-stop com.google.android.gsf && am broadcast -a android.intent.action.PACKAGE_ADDED -n com.google.android.gsf/.gtalkservice.PackageInstalledReceiver";
						break;
				}
				if (command != null)
				try
				{
					Runtime.getRuntime().exec(command);
				} catch (Exception e) {}
				finish();
			}
		}
	}

	private int getId(int id)
	{
		int r = 2;
		for(int i = 0; i < aAuto.length; i++)
		if (aAuto[i] == id)
		{
			r = i;
			break;
		}
		return r;
	}
}

