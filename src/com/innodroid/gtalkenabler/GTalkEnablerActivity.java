package com.innodroid.gtalkenabler;

import java.io.DataOutputStream;
import java.io.IOException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;

public class GTalkEnablerActivity extends Activity {
	
	private static final int DIALOG_NOT_ROOT = 101;
	
	private CheckBox mEnableCheckBox;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        mEnableCheckBox = (CheckBox)findViewById(R.id.allow_3g_chat);
        
        ((Button)findViewById(R.id.save_button)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}        	
        });
        
        if (!requestRoot()) {
        	showDialog(DIALOG_NOT_ROOT);
        }
    }
    
    @Override
    protected Dialog onCreateDialog(int id) {
    	if (id == DIALOG_NOT_ROOT) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Root Denied")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setMessage("Unable to get root access. Exiting application.")
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						finish();
					} });
            return builder.create();
    	}
    	else {
    		return super.onCreateDialog(id);
    	}
    }
    
    // 
    // This code from: 
    //		http://www.stealthcopter.com/blog/2010/01/android-requesting-root-access-in-your-app/
    //
    private boolean requestRoot() {
    	Process p;  
    	try {  
    	   // Preform su to get root privledges  
    	   p = Runtime.getRuntime().exec("su");   
    	  
    	   // Attempt to write a file to a root-only  
    	   DataOutputStream os = new DataOutputStream(p.getOutputStream());  
    	   os.writeBytes("echo \"Do I have root?\" >/system/sd/temporary.txt\n");  
    	  
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
}

