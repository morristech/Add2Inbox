package com.proton.addinbox;

import java.util.regex.Pattern;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.email_to_self.R;
import com.google.ads.AdView;
import com.proton.addinbox.constant.Constant;

/********************************************************************
 * Author: Akhil Acharya Date Started: 7 November 2012 Class Name:
 * MainActivity.java Class Objectives: 
 * 		1) Obtain user Gmail permission 
 * 		2) Obtain user Gmail account 
 * 		3) Save obtained data to sharedPreferences 
 * 		4) Display a simple tutorial 
 * 		5) Provide a method to test the application. 
 * Consider: Activity to display previous "adds" using NeoDatis ODB
 * ******************************************************************/

public class MainActivity extends Activity {

	// SharedPreference Objects
	SharedPreferences editPrefs;
	SharedPreferences.Editor editPrefsedit;

	// View Objects
	TextView loggedIn, explanation;
	Button share;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		init();
		getAccount();
	}

	/**Initialize all views and global objects**/
	private void init() {
		//Initialize SharedPreferences
		editPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		editPrefsedit = editPrefs.edit();

		//Get Typeface from Assets
		Typeface robotoTypeface = Typeface.createFromAsset(this.getAssets(),
				"Roboto-Light.ttf");
		
		//Initialize TextViews
		loggedIn = (TextView) findViewById(R.id.tvLoggedIn);
		loggedIn.setText(getResources().getString(R.string.main_actviity_textview_loggedin)
							+ " " + getEmail());
		loggedIn.setTextColor(Color.WHITE);
		loggedIn.setTypeface(robotoTypeface);
		

		explanation = (TextView) findViewById(R.id.tvExplain);
		explanation.setText(getResources().getString(R.string.main_activity_textview_explain));
		explanation.setTextColor(Color.WHITE);
		explanation.setTypeface(robotoTypeface);
		

		//Initialize button and set up OnClickListener
		share = (Button) findViewById(R.id.bShare1);
		share.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent startIntent = new Intent(Intent.ACTION_SEND);
				startIntent.setType("text/html");
				startIntent.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(R.string.main_activity_button_share_subject));
				startIntent.putExtra(Intent.EXTRA_TEXT, getResources().getString(R.string.main_activity_button_share_text));
				startActivity(Intent.createChooser(startIntent, getResources().getString(R.string.main_activity_dialog_intentpicker)));
			}
		});

		//Get rootview to change to "Holo" light-blue color. 
		View rootView = loggedIn.getRootView();
		rootView.setBackgroundColor(Color.parseColor("#33B5E5")); //holo blue. 

		if (editPrefs.getString(Constant.usernamePref, Constant.errorCheck).equals(Constant.errorCheck)) {
			String email = getEmail();
			editPrefs.edit().putString(Constant.usernamePref, email).commit();
		}
	}

	/** Get Account using the AccountManager (currently not used in this activity) **/
	private void getAccount() {
		AccountManager am = AccountManager.get(this);
		Account[] me = am.getAccounts();
		am.getAuthToken(me[0], "oauth2:https://mail.google.com/", null, this,
				new OnTokenAcquired(), null);
	}

	/** Callback class, called when "get account" (currently not used in this activity)**/
	private class OnTokenAcquired implements AccountManagerCallback<Bundle> {
		@Override
		public void run(AccountManagerFuture<Bundle> result) {
			try {
				Bundle bundle = result.getResult(); // Obtain Result bundle
				String token = bundle.getString(AccountManager.KEY_AUTHTOKEN); // Obtain Token
				editPrefsedit.putString(Constant.oauthToken, token); // Save to SharedPref
				editPrefsedit.commit();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/** Matches patterns to obtain Email Address. Available on API level 8+/2.2+**/
	public String getEmail() {
		Pattern emailPattern = Patterns.EMAIL_ADDRESS;
		Account[] accounts = AccountManager.get(this).getAccounts();
		for (Account account : accounts) {
			if (emailPattern.matcher(account.name).matches()) {
				return account.name;
			}
		}
		return null;
	}
}