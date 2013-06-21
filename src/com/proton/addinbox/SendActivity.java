package com.proton.addinbox;

import com.email_to_self.R;
import com.proton.addinbox.constant.Constant;
import com.proton.addinbox.jmail.GmailSenderOauth;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.widget.Toast;

/********************************************************************
 * Author: Akhil Acharya 
 * Date Started: 7 November 2012 
 * Class Name: SendActivity.java 
 * Class Objectives: 
 * 		1) Receive Intent.ACTION_SEND intent 
 * 		2) Parse Intent and Convert to String 
 * 		3) Leverage OAuth key with JavaMail to send mail via AsyncTask 4) Quit Activity 
 * Consider: Convert into IntentService?
 * ******************************************************************/

public class SendActivity extends Activity {

	SharedPreferences getPrefs;
	String oauthtoken;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getPrefs = PreferenceManager.getDefaultSharedPreferences(this);

		/** Obtain EmailAddress and Oauth token from SharedPreference storage (Not used) **/
		// oauthtoken = getPrefs.getString(Constant.oauthToken,
		// Constant.errorCheck);

		/**	One thing I initially overlooked was that AuthTokens DO expire after a set period, requiring the token to be refreshed  **/
		
		/**Rather than checking if the token was expired, a new token is generated each time the activity runs. 
		 * I'm not sure if this is efficient enough **/
		getAccount();
	}

	/** Get Account using the AccountManager **/
	private void getAccount() {
		AccountManager am = AccountManager.get(this);
		Account[] me = am.getAccounts();
		am.getAuthToken(me[0], "oauth2:https://mail.google.com/", null, this,   //currently only supports the MAIN Google Account on device. Selector coming soon! 
				new OnTokenAcquired(), null);
	}

	/** Callback class, called when "get account" **/
	private class OnTokenAcquired implements AccountManagerCallback<Bundle> {
		@Override
		public void run(AccountManagerFuture<Bundle> result) {
			String token = null;
			try {
				Bundle bundle = result.getResult(); // Obtain Result bundle
				token = bundle.getString(AccountManager.KEY_AUTHTOKEN);
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (token != null) {
					setupSending(token);
				}
			}
		}
	}

	private void setupSending(String authToken) {
		String emailAddress = getPrefs.getString(Constant.usernamePref, Constant.errorCheck);
		
		// This is a "hack" - directly casting  or using the toString method causes a null pointer. 
		Object em = getIntent().getExtras().get(Intent.EXTRA_TEXT); 
		Object bd = getIntent().getExtras().get(Intent.EXTRA_SUBJECT);


		String emailBody = null, emailSubject = null;
		/**
		 * Prevent activity from passing a null object, instantiate as a
		 * 0-length String
		 */
		if (em == null && bd == null) {
			emailBody = "";
			emailSubject = "";
		}else if(bd == null){
			emailSubject = ""; 
		}else if(em == null){
			emailBody = ""; 
		}else {
			emailBody = em.toString();
			emailSubject = bd.toString();
		}

		/**Default Subject MUST include some mention of the App Name in order to  allow in-Gmail tagging/sorting **/
		emailSubject = "[" + this.getResources().getString(R.string.app_name) + "] " + emailSubject;

		/** Add the obligatory self-promotion **/
		emailBody = emailBody + this.getResources().getString(R.string.send_activity_text_ad);

		/** Parameters required for AsyncTask **/
		Object[] params = { emailSubject, emailBody, emailAddress, authToken};

		/**
		 * Failsafe: Sometimes Initial activity is not called. Indicate login
		 * failure and start the needed Activity
		 **/
		if (emailAddress.equals(Constant.errorCheck)) {
			Toast.makeText(	this, getResources().getString(R.string.send_activity_toast_error_login), 1).show();
			startActivity(new Intent(SendActivity.this, MainActivity.class));
			this.finish();
		}

		/**
		 * Ensure that the Oauthtoken has been retrieved correctly and that
		 * airplane mode is OFF
		 **/
		/** TODO: Determine if 3G, and cache accordingly **/
		if (!authToken.equals(Constant.errorCheck) && !isAirplaneModeOn(this)) {
			try {
				Toast.makeText(this, getResources().getString(R.string.send_activity_toast_sending_one),	Toast.LENGTH_LONG).show();
				// start AsyncTask
				new send_mail_task_oath().execute(params);
			} catch (Exception e) {
				e.printStackTrace();
				// Handle exception, alert user.
				Toast.makeText(this, getResources().getString(R.string.send_activity_toast_error_general), Toast.LENGTH_LONG).show();
			} finally {
				SendActivity.this.finish();
			}
		} else {
			// Alert User
			Toast.makeText(this, getResources().getString(R.string.send_activity_toast_error_login),	Toast.LENGTH_SHORT).show();
		}
	}

	/** Asynctask: Instantiates GmailSenderOauth object, passes parameters. **/
	private class send_mail_task_oath extends AsyncTask<Object, Void, Void> {
		Exception mException;

		@Override
		protected Void doInBackground(Object... params) {
			GmailSenderOauth sender = new GmailSenderOauth();
			try {
				 /** Param[0]: Subject, Param[1]: Body Param[2]: User, Param [3]:oAuthtoken, Param[2]: Recipients (Same as User) **/
				sender.sendMail(params[0].toString(), params[1].toString(),	params[2].toString(), params[3].toString(), params[2].toString()); 
			} catch (Exception e) {
				mException = e;
				e.printStackTrace();
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void v) {
			super.onPostExecute(v);
			// alert user
			Toast.makeText(getBaseContext(), getResources().getString(R.string.send_activity_toast_sending_two), 1).show();
		}
	}

	public boolean isAirplaneModeOn(Context c) {
		return Settings.System.getInt(c.getContentResolver(),
				Settings.System.AIRPLANE_MODE_ON, 0) != 0;
	}

}
