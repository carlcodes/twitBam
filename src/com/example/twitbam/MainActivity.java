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
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

 
public class MainActivity extends Activity {
    // Constants
    

    
    /**
     * Register your here app https://dev.twitter.com/apps/new and get your
     * consumer key and secret
     * */
    
    
    static String TWITTER_CONSUMER_KEY = "Fr2JkMPXKb7u3VZYd6E3g";
    static String TWITTER_CONSUMER_SECRET = "ct29NzWHw8V9C2Ckc3YBy1wOPXyBJDlArBzz5e8";
 
    // Preference Constants
    static String PREFERENCE_NAME = "twitter_oauth";
    static final String PREF_KEY_OAUTH_TOKEN = "234582923-oc9Ii8iHYEZtOEJxp5a4lY827qdBsrSAGNhezEpN";
    static final String PREF_KEY_OAUTH_SECRET = "tLCQJoRY5QcVWZMd74Yd5B3kfOdsTylDIVGjYCiLZAwIC";
    static final String PREF_KEY_TWITTER_LOGIN = "isTwitterLogedIn";
 
    static final String TWITTER_CALLBACK_URL = "http://cse.msu.edu/~john3123";
 
    // Twitter oauth urls
    static final String URL_TWITTER_AUTH = "auth_url";
    static final String URL_TWITTER_OAUTH_VERIFIER = "oauth_verifier";
    static final String URL_TWITTER_OAUTH_TOKEN = "oath_url";
 
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
        if (Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
            }
        
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
        btnLogoutTwitter = (Button) findViewById(R.id.btnLogoutTwitter);
 
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
                //String status = txtUpdate.getText().toString();
 

                // update status
                
                final Handler handler = new Handler();
                
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                       /* do what you need to do */
                        new updateTwitterStatus().execute("");
                       /* and here comes the "trick" */
                       handler.postDelayed(this, 600000);
                    }
                 };
                
                
                handler.postDelayed(runnable, 600000);
                
                
                
                
                new updateTwitterStatus().execute("");

            }
        });
 
        /**
         * Button click event for logout from twitter
         * */
        btnLogoutTwitter.setOnClickListener(new View.OnClickListener() {
 
            @Override
            public void onClick(View arg0) {
                // Call logout twitter function
                logoutFromTwitter();
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
            
            ConfigurationBuilder builder = new ConfigurationBuilder();
            builder.setOAuthConsumerKey(TWITTER_CONSUMER_KEY);
            builder.setOAuthConsumerSecret(TWITTER_CONSUMER_SECRET);
             
            AccessToken accessToken = new AccessToken(PREF_KEY_OAUTH_TOKEN, PREF_KEY_OAUTH_SECRET);
            Twitter twitter = new TwitterFactory(builder.build()).getInstance(accessToken);
            
            //
            //SEARCH PARAMETERS (grab peoples tweets with these keywords mentioned
            //and use tweet them out as if they are mine, adding two hashtags
            //
            
            
            try {
                sendToTwitter("PGA golf", twitter);
            } catch (TwitterException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            try {
                sendToTwitter("michigan state", twitter);
            } catch (TwitterException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            try {
                sendToTwitter("east lansing", twitter);
            } catch (TwitterException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            try {
                sendToTwitter("golf", twitter);
            } catch (TwitterException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            
            return null;
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
                            "Tweet Bot Successful", Toast.LENGTH_SHORT)
                            .show();
                    
                    
                }
            });
        }
        
        protected void sendToTwitter(String searchTerm, Twitter twitter) throws TwitterException {
            twitter4j.Query query2 = new twitter4j.Query(searchTerm);
            QueryResult result2 = null;
            try {
                result2 = twitter.search(query2);
            } catch (TwitterException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
           
            //
            //Heart of the code right here
            //
            //this makes sure:
            //its less than 105 characters (so the hashtags can fit)
            //it's not a RT (RT's don't count)
            //it doesn't mention anyone (I previously got suspended for spamming on twitter
            //and I haven't tweeted it already.
            //
            //there's log stuff in here for debugging reasons
            //
            
            
            String turfbowl = " #GIS14_turfbowl #michiganstate ";
            for (twitter4j.Status status3 : result2.getTweets()) {
                if(status3.getText().length()<105 && !status3.getText().substring(0, 2).equals("RT") && 
                        !status3.getText().contains("@") && !status3.getText().contains("#GIS14_turfbowl")) {
                    twitter.updateStatus(status3.getText()+turfbowl);
                    Log.d("Status", "> " + status3.getText());
                    Log.d("LENGTH", "> " + status3.getText().length());
                    Log.d("1st", "> " + status3.getText().substring(0, 2));
                    
                }
            
            }
        }
 
    }
 
    /**
     * Function to logout from twitter
     * It will just clear the application shared preferences
     * */
    private void logoutFromTwitter() {
        // Clear the shared preferences
        Editor e = mSharedPreferences.edit();
        e.remove(PREF_KEY_OAUTH_TOKEN);
        e.remove(PREF_KEY_OAUTH_SECRET);
        e.remove(PREF_KEY_TWITTER_LOGIN);
        e.commit();
    }
 
    /**
     * Check user already logged in your application using twitter Login flag is
     * fetched from Shared Preferences
     * */
    private boolean isTwitterLoggedInAlready() {
        // return twitter login status from Shared Preferences
        return mSharedPreferences.getBoolean(PREF_KEY_TWITTER_LOGIN, false);
    }
 
    protected void onResume() {
        super.onResume();
    }
 
}