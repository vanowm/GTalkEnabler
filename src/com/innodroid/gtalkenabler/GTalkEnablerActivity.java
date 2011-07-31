package com.innodroid.gtalkenabler;

import java.io.DataOutputStream;
import java.io.IOException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
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

public class GTalkEnablerActivity extends Activity {
	
	private static final int DIALOG_NOT_ROOT = 101;
	private static final int DIALOG_DB_ERROR = 102;
	private static final String DB_FILE_NAME = "gservices.db";
	private static final String DB_PATH = "/data/data/com.google.android.gsf/databases/";
	private static final String DB_TABLE = "main";
	private static final String DB_SETTING = "gtalk_vc_wifi_only";
	private String mPrivateDatabaseFile;
	private String mGoogleDatabaseFile;
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
				finish();
			}        	
        });
                
        mGoogleDatabaseFile = DB_PATH + DB_FILE_NAME;
        mPrivateDatabaseFile = getDatabasePath(DB_FILE_NAME).getAbsolutePath();
        
        if (copyFileAsRoot(mGoogleDatabaseFile, mPrivateDatabaseFile))
        	new ReadSettingTask().execute();
        else
        	showDialog(DIALOG_NOT_ROOT);
    }
    
    @Override
    protected Dialog onCreateDialog(int id) {
    	switch (id) {
    		case DIALOG_NOT_ROOT:
    			return createDialog("Root Denied", "Unable to get root access. Exiting application.");
    		case DIALOG_DB_ERROR:
    			return createDialog("Database Error", "Unable to access database. Is video chat enabled Talk installed?");
    		default:
    			return super.onCreateDialog(id);
    	}
    }
    
    private Dialog createDialog(String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					finish();
				} });
        return builder.create();
    }
    
    // 
    // This code from: 
    //		http://www.stealthcopter.com/blog/2010/01/android-requesting-root-access-in-your-app/
    //
    private boolean copyFileAsRoot(String src, String dest) {
    	Process p;  
    	try {  
    	   // Preform su to get root privledges  
    	   p = Runtime.getRuntime().exec("su");   
    	  
    	   // Attempt to write a file to a root-only  
    	   DataOutputStream os = new DataOutputStream(p.getOutputStream());  
    	   os.writeBytes("echo \"Do I have root?\" >/system/sd/temporary.txt\n");  
    	  
    	   os.writeBytes("cp -f " + src + " " + dest + "\n");
    	   os.writeBytes("chmod a+rw " + dest + "\n");
    	   
    	   // Close the terminal  
    	   os.writeBytes("exit\n");  
    	   os.flush();
    	   try {  
    	      p.waitFor();  
    	           if (p.exitValue() != 255) {  
    	              // TODO Code to run on success  
    	              return true;
    	           }  
    	   } catch (InterruptedException e) {  
    	      e.printStackTrace();  
    	   }  
    	} catch (IOException e) {  
  	      e.printStackTrace();  
    	}
    	
    	return false;
    }
    
    private class ReadSettingTask extends AsyncTask<Void, Void, Boolean> {
		@Override
		protected Boolean doInBackground(Void... arg0) {
			
			SQLiteDatabase db;
			
			try {
				db = openOrCreateDatabase(getDatabasePath(DB_FILE_NAME).getName(), SQLiteDatabase.OPEN_READWRITE, null);
				//db = SQLiteDatabase.openDatabase(mPrivateDatabaseFile, null, );
			} catch (Exception ex) {
				return null;
			}
	        
			try {
				Cursor cursor = db.query(DB_TABLE, new String[] { "`value`" }, "`name` = ?", new String[] { DB_SETTING }, null, null, null);
				
				if (!cursor.moveToFirst()) {
					mMustInsert = true; 
					return false;
				}
				
				return cursor.getString(0).compareToIgnoreCase("true") == 0;
			} finally {
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
    
    private class WriteSettingTask extends AsyncTask<Void, Void, Boolean> {
    	private boolean mWifiOnly;    	
    	
    	public WriteSettingTask(boolean wifiOnly) {
    		mWifiOnly = !wifiOnly;
    	}
    	
		@Override
		protected Boolean doInBackground(Void... arg0) {
			
			SQLiteDatabase db;
			ContentValues values = new ContentValues();
			values.put("name", DB_SETTING);
			values.put("value", Boolean.toString(mWifiOnly));
			
			try {
				db = openOrCreateDatabase(getDatabasePath(DB_FILE_NAME).getName(), SQLiteDatabase.OPEN_READWRITE, null);
			} catch (Exception ex) {
				return null;
			}
	        
			try {
				if (mMustInsert) {
					db.insertOrThrow(DB_TABLE, null, values);
				} else {
					db.update(DB_TABLE, values, "name = ?", new String[] { DB_SETTING });
				}
				db.close();
				
				return copyFileAsRoot(mPrivateDatabaseFile, mGoogleDatabaseFile);
			} catch (Exception ex) {
				ex.printStackTrace();
				return false;
			} finally {
				if (db.isOpen())
					db.close();
			}
		}

		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);

			if (result == null || result == false)
				showDialog(DIALOG_DB_ERROR);
			else
				finish();
		}
    }
}

