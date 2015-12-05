package com.example.edwardst4.blackbirdfortwitter;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.text.Html;
import android.view.View;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;


public class main_activity extends Activity {

    //these are the credentials from my twitter developer account along with the custom URL
    // that I specified in the manifest file
    public final static String TWITTER_KEY = "wNB64xwSHMHGUkv7vHmdlqJFG";
    public final static String TWITTER_SECRET = "UO9VeDfPrROwXd32p6ym2LThjOW0rbCZzAHqm48VnLezDF4v8Q";
    public final static String TWITTER_URL = "x-oauthflow-twitter://callback";

    //twitter oauth urls
    static final String AUTH_URL = "auth_url";
    static final String OAUTH_VERIFIER = "oauth_verifier";
    static final String OAUTH_TOKEN = "oauth_token";

    // Preference Constants
    static String PREFERENCE_NAME = "twitter_oauth";
    static final String PREF_KEY_OAUTH_TOKEN = "50537098-EkUXv8wFKiuvZQFe3YblJogiNwZcF1V4hckkGYzTw";
    static final String PREF_KEY_OAUTH_SECRET = "lHFgyjik1myXeSUxIFOrN8eJSpKVFPyx09NP1mvebT3vn";
    static final String PREF_KEY_TWITTER_LOGIN = "isTwitterLoggedIn";

    // a twitter instance
    private Twitter blackBird;
    //request a token from the user for accessing their account
    private RequestToken blackBirdToken;
    //shared preferences object to store user details
    private SharedPreferences userPrefs;

    //networkDetector
    private NetworkDetector networkDetector;
    // alert dialog manager
    AlertDialogManager alert = new AlertDialogManager();

    Button btnLogin;
    Button btnLogout;
    Button btnUpdateStatus;
    EditText txtUpdate;
    TextView lblUpdate;
    TextView lblUserName;
    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        networkDetector = new NetworkDetector(getApplicationContext());

        //check if internet is working
        if (!networkDetector.isConnectingToInternet()) {
            alert.showAlert(main_activity.this, "Internet Connection Error", "Please connect to a working internet connection.", false);
            return;
        }

        //check if twitter keys are set
        if (TWITTER_KEY.trim().length() == 0 || TWITTER_SECRET.trim().length() == 0) {
            alert.showAlert(main_activity.this, "Twitter oAuth Tokens", "Please oauth tokens first", false);
            return;
        }

        // UI elements
        btnLogin = (Button) findViewById(R.id.signin);
        btnLogout = (Button) findViewById(R.id.btnLogout);
        btnUpdateStatus = (Button) findViewById(R.id.btnUpdateStatus);
        txtUpdate = (EditText) findViewById(R.id.txtUpdateStatus);
        lblUpdate = (TextView) findViewById(R.id.lblUpdate);
        lblUserName = (TextView) findViewById(R.id.lblScreenName);

        //sharePreferences
        userPrefs = getApplicationContext().getSharedPreferences("MyPref", 0);

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                login();
            }
        });

        btnUpdateStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String status = txtUpdate.getText().toString();

                // check for blank text
                if (status.trim().length() > 0) {
                    new UpdateStatus().execute(status);
                } else {
                    //EditText is empty
                    Toast.makeText(getApplicationContext(), "Please enter a status message", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                logout();
            }
        });

        //parse the uri to get the oAuth verifier
        if (!isTwitterLoggedInAlready()) {
            Uri uri = getIntent().getData();
            if (uri != null && uri.toString().startsWith(TWITTER_URL)) {
                //oauth verifier
                String verifier = uri.getQueryParameter(OAUTH_VERIFIER);
                try {
                    //get the access token
                    AccessToken accessToken = blackBird.getOAuthAccessToken(blackBirdToken, verifier);

                    //shared preferences
                    SharedPreferences.Editor e = userPrefs.edit();
                    //after getting token and secret store them in app prefs
                    e.putString(PREF_KEY_OAUTH_TOKEN, accessToken.getToken());
                    e.putString(PREF_KEY_OAUTH_SECRET, accessToken.getTokenSecret());
                    //store login status
                    e.putBoolean(PREF_KEY_TWITTER_LOGIN, true);
                    e.commit(); //save changes

                    Log.e("Twitter OAuth Token", "> " + accessToken.getToken());
                    //hide login button
                    btnLogin.setVisibility(View.GONE);

                    //show new Update Screen
                    lblUpdate.setVisibility(View.VISIBLE);
                    txtUpdate.setVisibility(View.VISIBLE);
                    btnUpdateStatus.setVisibility(View.VISIBLE);
                    btnLogout.setVisibility(View.VISIBLE);

                    //get user details from twitter
                    long userID = accessToken.getUserId();
                    User user = blackBird.showUser(userID);
                    String username = user.getName();

                    //display username in xml UI
                    lblUserName.setText(Html.fromHtml("<b>Welcome to BlackBird " + username + "</b>"));
                } catch (Exception e) {
                    Log.e("Twitter Login Error", "> " + e.getMessage());
                }
            }
        }
    }

    class UpdateStatus extends AsyncTask<String, String, String> {
        // show the progress dialog before starting the background task

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new ProgressDialog(main_activity.this);
            progressDialog.setMessage("Updating to twitter...");
            progressDialog.setIndeterminate(false);
            progressDialog.setCancelable(false);
            progressDialog.show();
        }

        protected String doInBackground(String... args) {
            Log.d("Tweet text", "> " + args[0]);
            String status = args[0];
            try {
                ConfigurationBuilder builder = new ConfigurationBuilder();
                builder.setOAuthConsumerKey(TWITTER_KEY);
                builder.setOAuthConsumerSecret(TWITTER_SECRET);

                //Access Token
                String access_token = userPrefs.getString(PREF_KEY_OAUTH_TOKEN, "");
                String access_token_secret = userPrefs.getString(PREF_KEY_OAUTH_SECRET, "");

                AccessToken accessToken = new AccessToken(access_token, access_token_secret);
                Twitter twitter = new TwitterFactory(builder.build()).getInstance(accessToken);

                // update status
                twitter4j.Status response = twitter.updateStatus(status);

                Log.d("Status", "> " + response.getText());
            } catch (TwitterException e) {
                Log.d("Twitter update error", e.getMessage());
            }
            return null;
        }

        // after completing the background task we will dismiss the progress dialog
        protected void onPostExecute(String file_url) {
            //dismiss the dialog after getting everything
            progressDialog.dismiss();
            //update the UI from background thread
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "Status tweeted successfully!!", Toast.LENGTH_SHORT).show();
                    //clear the edittext after the user tweets
                    txtUpdate.setText("");
                }
            });
        }
    }


    private void login() {
        //check if user is logged in already
        if (!isTwitterLoggedInAlready()) {
            ConfigurationBuilder builder = new ConfigurationBuilder();
            builder.setOAuthConsumerKey(TWITTER_KEY);
            builder.setOAuthConsumerSecret(TWITTER_SECRET);
            Configuration config = builder.build();

            TwitterFactory factory = new TwitterFactory(config);
            blackBird = factory.getInstance();

            try {
             /*   StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();

                StrictMode.setThreadPolicy(policy); */
                blackBirdToken = blackBird.getOAuthRequestToken(TWITTER_URL);
                this.startActivity(new Intent(Intent.ACTION_VIEW, Uri
                        .parse(blackBirdToken.getAuthenticationURL())));
                Log.d("Success", "Keys Accepted");
            } catch (TwitterException e) {
                e.printStackTrace();
            }
        } else {
            // if the user already logged in to twitter display toast
            Toast.makeText(getApplicationContext(), "Already Logged in to Twitter", Toast.LENGTH_LONG).show();
        }
    }

    private void logout() {
        //clear the shared prefs that contain user secret and key
        SharedPreferences.Editor e = userPrefs.edit();
        e.remove(PREF_KEY_OAUTH_TOKEN);
        e.remove(PREF_KEY_OAUTH_SECRET);
        e.remove(PREF_KEY_TWITTER_LOGIN);
        e.commit();

        //show and hide the appropriate views again as if first logging in
        btnLogout.setVisibility(View.GONE);
        btnUpdateStatus.setVisibility(View.GONE);
        txtUpdate.setVisibility(View.GONE);
        lblUpdate.setVisibility(View.GONE);
        lblUserName.setText("");
        lblUserName.setVisibility(View.GONE);

        btnLogin.setVisibility(View.VISIBLE);
    }

    private boolean isTwitterLoggedInAlready() {
        return userPrefs.getBoolean(PREF_KEY_TWITTER_LOGIN, false);
    }

    protected void onResume() {
        super.onResume();
    }


}
