package com.innodroid.gtalkenabler;

import java.io.File;
import java.io.IOException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;

import com.stericson.RootTools.RootTools;
import com.stericson.RootTools.RootToolsException;

public class GTalkEnablerActivity extends Activity {
	
	private static final int DIALOG_NOT_ROOT = 101;
	private static final int DIALOG_DB_ERROR = 102;
	private static final int DIALOG_BAD_VERSION = 103;
	private static final int DIALOG_CONFIRM = 104;
	private static final String DB_FILE_NAME = "gservices.db";
	private static final String DB_PATH = "/data/data/com.google.android.gsf/databases/";
	private static final String DB_TABLE = "main";
	private static final String DB_SETTING = "gtalk_vc_wifi_only";
	private File mPrivateDatabaseFile;
	private String mPrivateDatabasePath;
	private String mGoogleDatabasePath;
	private boolean mMustInsert;
	private CheckBox mEnableCheckBox;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        mEnableCheckBox = (CheckBox)findViewById(R.id.allow_3g_chat);
        
        ((Button)findViewById(R.id.save_button)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				new WriteSettingTask(mEnableCheckBox.isChecked()).execute();
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
    protected Dialog onCreateDialog(int id) {
    	switch (id) {
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
    
    private Dialog createDialog(String title, String message, int icon) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title)
            .setIcon(icon)
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					finish();
				} });
        return builder.create();
    }
    
    private void copyFileAsRoot(String src, String dest) throws IOException, InterruptedException, RootToolsException {
	    RootTools.sendShell("cp -f " + src + " " + dest + "\n");
	    RootTools.sendShell("chmod 777 " + dest + "\n");
    }
    
    private abstract class BaseTask extends AsyncTask<Void, Void, Boolean> {
    	private ProgressDialog mProgressDialog;
    	
    	@Override
    	protected void onPreExecute() {
    		super.onPreExecute();
    		
    		mProgressDialog = ProgressDialog.show(GTalkEnablerActivity.this, null, "Please Wait", true, false);
    	}
    	
    	@Override
    	protected void onPostExecute(Boolean result) {
    		super.onPostExecute(result);
    		
    		mProgressDialog.dismiss();
    	}
    	
    	@Override
    	protected void onCancelled() {
    		super.onCancelled();

    		mProgressDialog.dismiss();
    	}
    }
    
    private class ReadSettingTask extends BaseTask {
		@Override
		protected Boolean doInBackground(Void... arg0) {			
			SQLiteDatabase db = null;
			
			try {
				copyFileAsRoot(mGoogleDatabasePath, mPrivateDatabasePath);
				
				db = SQLiteDatabase.openDatabase(mPrivateDatabasePath, null, SQLiteDatabase.OPEN_READWRITE);
				Cursor cursor = db.query(DB_TABLE, new String[] { "value" }, "name = ?", new String[] { DB_SETTING }, null, null, null);
				
				if (!cursor.moveToFirst()) {
					mMustInsert = true; 
					return false;
				}
				
				return cursor.getString(0).compareToIgnoreCase("true") == 0;
			} catch (Exception ex) {
				ex.printStackTrace();
				return null;
			} finally {
				if (db != null && db.isOpen())
					db.close();
			}
		}

		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);

			if (result == null)
				showDialog(DIALOG_DB_ERROR);
			else
				mEnableCheckBox.setChecked(!result);
		}
    }
    
    private class WriteSettingTask extends BaseTask {
    	private boolean mWifiOnly;    	
    	
    	public WriteSettingTask(boolean wifiOnly) {
    		mWifiOnly = !wifiOnly;
    	}
    	
		@Override
		protected Boolean doInBackground(Void... arg0) {
			
			SQLiteDatabase db = null;
			ContentValues values = new ContentValues();
			values.put("name", DB_SETTING);
			values.put("value", Boolean.toString(mWifiOnly));
			
			try {
				db = SQLiteDatabase.openDatabase(mPrivateDatabasePath, null, SQLiteDatabase.OPEN_READWRITE);

				if (mMustInsert) {
					db.insertOrThrow(DB_TABLE, null, values);
				} else {
					db.update(DB_TABLE, values, "name = ?", new String[] { DB_SETTING });
				}
				db.close();
				
				copyFileAsRoot(mPrivateDatabasePath, mGoogleDatabasePath);
				return true;
			} catch (Exception ex) {
				ex.printStackTrace();
				return false;
			} finally {
				if (db != null && db.isOpen())
					db.close();
			}
		}

		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);

			if (result == null || result == false)
				showDialog(DIALOG_DB_ERROR);
			else
				showDialog(DIALOG_CONFIRM);
		}
    }
}

