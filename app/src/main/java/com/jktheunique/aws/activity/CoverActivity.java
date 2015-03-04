package com.jktheunique.aws.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.cognito.DatasetMetadata;
import com.facebook.AppEventsLogger;
import com.facebook.Session;
import com.facebook.SessionState;
import com.jktheunique.aws.Constants;
import com.jktheunique.aws.R;
import com.jktheunique.aws.util.CognitoSyncClientManager;

import java.util.ArrayList;
import java.util.List;


public class CoverActivity extends Activity {

    private Button facebookButton;
    private String FBAccessToken;
    private final String FB_TOKEN = "FB_TOKEN";

    private Button testButton1;
    private Button testButton2;
    private Button testButton3;
    private Button testButton4;
    private Button testButton5;
    private Button testButton6;

    private Session activeSession;

    private String identifyId = "";
    private String identifyToken = "";

    private SharedPreferences preferences;
    private CognitoCachingCredentialsProvider provider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cover);
        initLayout();
        setLayoutInfo();
        initParams();
    }

    @Override
    protected void onResume(){
        super.onResume();
        AppEventsLogger.activateApp(this);
    }

    @Override
    protected void onPause(){
        super.onPause();
        AppEventsLogger.deactivateApp(this);
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Session.getActiveSession().onActivityResult(this, requestCode, resultCode, data);
    }
    private void initLayout(){
        facebookButton = (Button) findViewById(R.id.facebook_button);

        testButton1 = (Button) findViewById(R.id.test_button_1);
        testButton2 = (Button) findViewById(R.id.test_button_2);
        testButton3 = (Button) findViewById(R.id.test_button_3);
        testButton4 = (Button) findViewById(R.id.test_button_4);
        testButton5 = (Button) findViewById(R.id.test_button_5);
        testButton6 = (Button) findViewById(R.id.test_button_6);
    }

    private void setLayoutInfo(){
        if(preferences == null)
            preferences = PreferenceManager.getDefaultSharedPreferences(CoverActivity.this);

        facebookButton.setOnClickListener(facebookLoginListener);
        testButton1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getCognitoId();
            }
        });

        testButton2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast("Identify",identifyId);
                Toast("Identify",identifyToken);
            }
        });

        testButton3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startOtherActivity(DataSetListActivity.class);
//                Intent intent = new Intent(CoverActivity.this, DataSetListActivity.class);
//                startActivity(intent);
            }
        });

        testButton4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<DatasetMetadata> datasetMetadataList = CognitoSyncClientManager.getSyncManager().listDatasets();
                if(datasetMetadataList.size()>0) {
                    Bundle extra = new Bundle();
                    extra.putString(DataSetContentsListActivity.DATA_SET_NAME,datasetMetadataList.get(0).getDatasetName());
                    startOtherActivity(DataSetContentsListActivity.class, extra);

//                    Intent intent = new Intent(CoverActivity.this, DataSetContentsListActivity.class);
//                    intent.putExtra(DataSetContentsListActivity.DATA_SET_NAME,datasetMetadataList.get(0).getDatasetName());
//                    startActivity(intent);
                }else{
                    Toast("Cognito","No DataSet Exist");
                }
            }
        });

        testButton5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startOtherActivity(DynamoActivity.class);
//                Intent intent = new Intent(CoverActivity.this, DynamoActivity.class);
//                startActivity(intent);
            }
        });

        testButton6.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startOtherActivity(S3Activity.class);
//                Intent intent = new Intent(CoverActivity.this, S3Activity.class);
//                startActivity(intent);
            }
        });

    }

    private void initParams(){
        CognitoSyncClientManager.init(CoverActivity.this);
        provider = CognitoSyncClientManager.getCredentialsProvider();
        if(!TextUtils.isEmpty(provider.getCachedIdentityId())){
            identifyId = provider.getCachedIdentityId();
        }
    }

    private View.OnClickListener facebookLoginListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
//            CognitoSyncClientManager.init(CoverActivity.this);
            facebookLogin();
        }
    };

    private void facebookLogin(){
        activeSession = Session.getActiveSession();

        if(activeSession==null || activeSession.isClosed()){
            activeSession = new Session.Builder(CoverActivity.this).setApplicationId(Constants.FACEBOOK_APP_ID).build();
            Session.setActiveSession(activeSession);
        }

        if (!activeSession.isOpened()) {
            Session.OpenRequest openRequest = new Session.OpenRequest(CoverActivity.this);
            ArrayList<String> permission = new ArrayList<String>();
//            permission.add("user_actions.music");
            permission.add("email");
//            permission.add("user_likes");
//            permission.add("user_about_me");
//            permission.add("user_birthday");
            permission.add("public_profile");
//	        permission.add("basic_info");
            openRequest.setPermissions(permission);
            openRequest.setCallback(facebookStatusCallback);
            activeSession.openForRead(openRequest);
//	        session.openForPublish(openRequest); //here dialog is opened, user is able to enter credentials
        }else{
            facebookStatusCallback.call(activeSession, SessionState.OPENED, null);
        }
    }

    private Session.StatusCallback facebookStatusCallback = new Session.StatusCallback() {
        @Override
        public void call(Session session, SessionState state, Exception exception) {

            if(session.isOpened()){
                FBAccessToken = session.getAccessToken();
                preferences.edit().putString(FB_TOKEN,FBAccessToken).apply();
                Toast("token", FBAccessToken);
                CognitoSyncClientManager.init(CoverActivity.this);
                CognitoSyncClientManager.addLogins("graph.facebook.com", FBAccessToken);
            }


        }
    };

    private void getCognitoId(){
//        final CognitoCredentialsProvider provider = CognitoSyncClientManager.getCredentialsProvider();
        final ProgressDialog dialog = ProgressDialog.show(CoverActivity.this, "Cognito", "Log_in");

        if(TextUtils.isEmpty(FBAccessToken)){
            if(TextUtils.isEmpty(preferences.getString(FB_TOKEN,""))) {
                Toast("identify","FB_TOKEN is empty");
                return;
            }else{
                FBAccessToken = preferences.getString(FB_TOKEN,"");
                CognitoSyncClientManager.addLogins("graph.facebook.com", FBAccessToken);
            }
        }

        AsyncTask<Void, Void, Void> providerTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                identifyId = provider.getIdentityId();
                identifyToken = provider.getToken();
                return null;
            }

            @Override
            protected void onPostExecute(Void params){
                dialog.dismiss();
                Toast.makeText(CoverActivity.this,"done",Toast.LENGTH_SHORT).show();
                Log.d("identify","done");
            }
        };
        providerTask.execute();

    }

    private void Toast(String tag,String value){
        if(!TextUtils.isEmpty(value)) {
            Log.d(tag, value);
            Toast.makeText(CoverActivity.this, tag + ": " + value, Toast.LENGTH_SHORT).show();
        }else if(value==null){
            Log.d(tag, "null");
            Toast.makeText(CoverActivity.this, tag + ": " + "null", Toast.LENGTH_SHORT).show();
        }else{
            Log.d(tag, "");
            Toast.makeText(CoverActivity.this, tag + ": " + "", Toast.LENGTH_SHORT).show();
        }
    }

    private void startOtherActivity(Class destActivity) {
        startOtherActivity(destActivity,null);
    }
    private void startOtherActivity(Class destActivity, Bundle extra){
        if(isCognitoAvailable()) {
            Intent intent = new Intent(CoverActivity.this, destActivity);
            if(extra!=null && extra.size()>0)
                intent.putExtras(extra);
            startActivity(intent);
        }else{
            Toast("Identify","You Need CognitoID");
        }
    }

    private boolean isCognitoAvailable(){
        boolean ret = false;
        if(!TextUtils.isEmpty(identifyId)){
            if(TextUtils.isEmpty(FBAccessToken)){
                if(TextUtils.isEmpty(preferences.getString(FB_TOKEN,""))) {
                    Toast("FBLOGIN","Login Facebook First");
                }else{
                    FBAccessToken = preferences.getString(FB_TOKEN,"");
                    CognitoSyncClientManager.addLogins("graph.facebook.com", FBAccessToken);
                    ret = true;
                }
            }else{
                CognitoSyncClientManager.addLogins("graph.facebook.com", FBAccessToken);
                ret = true;
            }
        }else {
            if(TextUtils.isEmpty(FBAccessToken)) {
                if (TextUtils.isEmpty(preferences.getString(FB_TOKEN, ""))) {
                    Toast("FBLOGIN", "Login Facebook First");
                } else {
                }
            }
            Toast("Cognito", "You Need Cognito Login");
        }
        return ret;
    }
}
