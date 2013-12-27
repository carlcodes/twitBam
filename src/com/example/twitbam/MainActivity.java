package com.example.twitbam;

import twitter4j.QueryResult;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;
import android.app.Activity;
import android.app.DownloadManager.Query;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends Activity {
// Constants
/**
    * Register your here app https://dev.twitter.com/apps/new and get your
    * consumer key and secret
    * */
    static String TWITTER_CONSUMER_KEY = "kGtMET0EQbkAYuhshOqZfg";
    static String TWITTER_CONSUMER_SECRET = "FYcHTqu2u9PVJNhvwvejW0GcWP2RgG7mzNK0WXDvqHo";
    
    // Preference Constants
    static String PREFERENCE_NAME = "twitter_oauth";
    static final String PREF_KEY_OAUTH_TOKEN = "242563533-YYzd9BahMRyHBUBWqp2PQFYlc0fIczR5ztldoGxb";
    static final String PREF_KEY_OAUTH_SECRET = "jsIcMBxk9Nk0fimIqw2cKRlhF78uofEgFBs3bsc";
    static final String PREF_KEY_TWITTER_LOGIN = "isTwitterLogedIn";
    
    static final String TWITTER_CALLBACK_URL = "oauth://t4jsample";
    
    // Twitter oauth urls
    static final String URL_TWITTER_AUTH = "auth_url";
    static final String URL_TWITTER_OAUTH_VERIFIER = "oauth_verifier";
    static final String URL_TWITTER_OAUTH_TOKEN = "oauth_token";
    
    static final List<Map<String, String>> values = new ArrayList<Map<String, String>>();
    
    // Login button
    Button btnLoginTwitter;
    // Update status button
    Button btnUpdateStatus;
    // Logout button
    Button btnLogoutTwitter;
    // EditText for update
    EditText txtUpdate;
    // lbl update
    TextView lblUpdate;
    TextView lblUserName;
    
    ListView tweets;
    
    // Progress dialog
    ProgressDialog pDialog;
    
    // Twitter
    private static Twitter twitter;
    private static RequestToken requestToken;
    
    // Shared Preferences
    private static SharedPreferences mSharedPreferences;
    
    // Internet Connection detector
    private ConnectionDetector cd;
    
    // Alert Dialog Manager
    AlertDialogManager alert = new AlertDialogManager();
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        if (Build.VERSION.SDK_INT > 9) {
//            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
//            StrictMode.setThreadPolicy(policy);
//            }
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        cd = new ConnectionDetector(getApplicationContext());

        // Check if Internet present
        if (!cd.isConnectingToInternet()) {
            // Internet Connection is not present
            alert.showAlertDialog(MainActivity.this, "Internet Connection Error",
                    "Please connect to working Internet connection", false);
            // stop executing code by return
            return;
        }

        // Check if twitter keys are set
        if(TWITTER_CONSUMER_KEY.trim().length() == 0 || TWITTER_CONSUMER_SECRET.trim().length() == 0){
            // Internet Connection is not present
            alert.showAlertDialog(MainActivity.this, "Twitter oAuth tokens", "Please set your twitter oauth tokens first!", false);
            // stop executing code by return
            return;
        }

        // All UI elements
        btnLoginTwitter = (Button) findViewById(R.id.btnLoginTwitter);
        btnUpdateStatus = (Button) findViewById(R.id.btnUpdateStatus);
        txtUpdate = (EditText) findViewById(R.id.txtUpdateStatus);
        lblUpdate = (TextView) findViewById(R.id.lblUpdate);
        lblUserName = (TextView) findViewById(R.id.lblUserName);
        tweets = (ListView) findViewById(R.id.listView1);

        
        
        SimpleAdapter simpleAdpt = new SimpleAdapter(this, values, android.R.layout.simple_list_item_1, new String[] {"username"}, new int[] {android.R.id.text1});
        
        // Assign adapter to ListView
        tweets.setAdapter(simpleAdpt); 
        
        
        
        // Shared Preferences
        mSharedPreferences = getApplicationContext().getSharedPreferences(
                "MyPref", 0);
        
            /**
             * Twitter login button click event will call loginToTwitter() function
             * */
            btnLoginTwitter.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View arg0) {
                    // Call login twitter function
                    loginToTwitter();
                    }
                });
            
            /**
             * Button click event to Update Status, will call updateTwitterStatus()
             * function
             * */
            btnUpdateStatus.setOnClickListener(new View.OnClickListener() {
     
                @Override
                public void onClick(View v) {
                    // Call update status function
                    // Get the status from EditText
                    String status = txtUpdate.getText().toString();
     
                    // Check for blank text
                    if (status.trim().length() > 0) {
                        // update status
                        new updateTwitterStatus().execute(status);
                        
                        SimpleAdapter simpleAdpt = new SimpleAdapter(MainActivity.this, values, android.R.layout.simple_list_item_1, new String[] {"username"}, new int[] {android.R.id.text1});
                        
                        // Assign adapter to ListView
                        tweets.setAdapter(simpleAdpt); 
                       
                        
                    } else {
                        // EditText is empty
                        Toast.makeText(getApplicationContext(),
                                "Please enter status message", Toast.LENGTH_SHORT)
                                .show();
                        
                    }
                }
            });
            
            

            /** This if conditions is tested once is
             * redirected from twitter page. Parse the uri to get oAuth
             * Verifier
             * */
            if (!isTwitterLoggedInAlready()) {
                    Uri uri = getIntent().getData();
                    if (uri != null && uri.toString().startsWith(TWITTER_CALLBACK_URL)) {
                        // oAuth verifier
                        String verifier = uri
                                    .getQueryParameter(URL_TWITTER_OAUTH_VERIFIER);

                        try {
                                    // Get the access token
                                    AccessToken accessToken = twitter.getOAuthAccessToken(
                                                requestToken, verifier);

                                    // Shared Preferences
                                    Editor e = mSharedPreferences.edit();

                                    // After getting access token, access token secret
                                    // store them in application preferences
                                    e.putString(PREF_KEY_OAUTH_TOKEN, accessToken.getToken());
                                    e.putString(PREF_KEY_OAUTH_SECRET,
                                                    accessToken.getTokenSecret());
                                    // Store login status - true
                                    e.putBoolean(PREF_KEY_TWITTER_LOGIN, true);
                                    e.commit(); // save changes

                                    Log.e("Twitter OAuth Token", "> " + accessToken.getToken());

                                    // Hide login button
                                    btnLoginTwitter.setVisibility(View.GONE);

                                    // Show Update Twitter
                                    lblUpdate.setVisibility(View.VISIBLE);
                                    txtUpdate.setVisibility(View.VISIBLE);
                                    btnUpdateStatus.setVisibility(View.VISIBLE);
                                    btnLogoutTwitter.setVisibility(View.VISIBLE);

                                    // Getting user details from twitter
                                    // For now i am getting his name only
                                    long userID = accessToken.getUserId();
                                    User user = twitter.showUser(userID);
                                    String username = user.getName();

                                // Displaying in xml ui
                                lblUserName.setText(Html.fromHtml("<b>Welcome " + username + "</b>"));
                                } catch (Exception e) {
                                        // Check log for login errors
                                        Log.e("Twitter Login Error", "> " + e.getMessage());
                                }
                        }
            }

    }
    
        /**
         * Function to login twitter
         * */
        private void loginToTwitter() {
            // Check if already logged in
            if (!isTwitterLoggedInAlready()) {
                ConfigurationBuilder builder = new ConfigurationBuilder();
                builder.setOAuthConsumerKey(TWITTER_CONSUMER_KEY);
                builder.setOAuthConsumerSecret(TWITTER_CONSUMER_SECRET);
                Configuration configuration = builder.build();

                TwitterFactory factory = new TwitterFactory(configuration);
                twitter = factory.getInstance();

                if(!(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)) {
                    try {
                        requestToken = twitter.getOAuthRequestToken(TWITTER_CALLBACK_URL);
                        this.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(requestToken.getAuthenticationURL())));
                    } catch (TwitterException e) {
                        e.printStackTrace();
                    }
                    }
                    else
                    {
                        new Thread(new Runnable() {
                            public void run() {
                                try {   
                                    requestToken = twitter.getOAuthRequestToken(TWITTER_CALLBACK_URL);
                                    MainActivity.this.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(requestToken.getAuthenticationURL())));
                                } catch (TwitterException e) {
                                    e.printStackTrace();
                                }
                            }
                        }).start();
                    }
            }
        }

        /**
         * Check user already logged in your application using twitter Login flag is
         * fetched from Shared Preferences
         * */
    private boolean isTwitterLoggedInAlready() {
        // return twitter login status from Shared Preferences
        return mSharedPreferences.getBoolean(PREF_KEY_TWITTER_LOGIN, false);
        }
    
    /**
     * Function to update status
     * */
    class updateTwitterStatus extends AsyncTask<String, String, String> {
     
        /**
         * Before starting background thread Show Progress Dialog
         * */
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pDialog = new ProgressDialog(MainActivity.this);
            pDialog.setMessage("Updating to twitter...");
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(false);
            pDialog.show();
        }
    
    
    /**
     * getting Places JSON
     * */
    protected String doInBackground(String... args) {
        Log.d("Tweet Text", "> " + args[0]);
        String status = args[0];
        try {
            ConfigurationBuilder builder = new ConfigurationBuilder();
            builder.setOAuthConsumerKey(TWITTER_CONSUMER_KEY);
            builder.setOAuthConsumerSecret(TWITTER_CONSUMER_SECRET);
             
            // Access Token 
            String access_token = mSharedPreferences.getString(PREF_KEY_OAUTH_TOKEN, "");
            // Access Token Secret
            String access_token_secret = mSharedPreferences.getString(PREF_KEY_OAUTH_SECRET, "");
             
            AccessToken accessToken = new AccessToken(access_token, access_token_secret);
            Twitter twitter = new TwitterFactory(builder.build()).getInstance(accessToken);
             
            // Update status
            //twitter4j.Status response = twitter.updateStatus(status);
             
            //Log.d("Status", "> " + response.getText());
            
            twitter4j.Query query = new twitter4j.Query(status);
            QueryResult result = twitter.search(query);
            values.clear();
            for (twitter4j.Status status1 : result.getTweets()) {
                System.out.println("@" + status1.getUser().getScreenName() + ":" + status1.getText());
                
                values.add(createTweet("username",status1.getText()));
                
            }
            
        } catch (TwitterException e) {
            // Error in updating status
            Log.d("Twitter Update Error", e.getMessage());
        }
        return null;
    }
    
    private HashMap<String, String> createTweet(String key, String name) {
            HashMap<String, String> tweet = new HashMap<String, String>();
            tweet.put(key, name);

            return tweet;
        }

    
    
    /**
     * After completing background task Dismiss the progress dialog and show
     * the data in UI Always use runOnUiThread(new Runnable()) to update UI
     * from background thread, otherwise you will get error
     * **/
    protected void onPostExecute(String file_url) {
        // dismiss the dialog after getting all products
        pDialog.dismiss();
        // updating UI from Background Thread
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(),
                        "Status tweeted successfully", Toast.LENGTH_SHORT)
                        .show();
                // Clearing EditText field
                txtUpdate.setText("");
            }
        });
    }

    }
}