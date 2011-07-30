package com.innodroid.gtalkenabler;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;

public class GTalkEnablerActivity extends Activity {
	
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
    }
}

