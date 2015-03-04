package com.jktheunique.aws.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.jktheunique.aws.Constants;
import com.jktheunique.aws.R;
import com.jktheunique.aws.adapter.DownloadListAdapter;
import com.jktheunique.aws.models.TransferModel;
import com.jktheunique.aws.network.TransferController;
import com.jktheunique.aws.util.CognitoSyncClientManager;
import com.jktheunique.aws.util.S3Util;
import com.jktheunique.aws.util.TransferView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by JKtheUnique on 2014-10-30.
 */
public class S3Activity extends Activity {
    private static final String TAG = "S3_ACTIVITY";

    private boolean exists = false;
    private boolean checked = false;
    private static final int REFRESH_DELAY = 500;

    private Button createBucket;
    private Button deleteBucket;
    private Button downloadButton;
    private Button uploadImage;
    private Button uploadVideo;

    private DownloadListAdapter downloadListAdapter;
    private ListView downloadList;
    private TextView loadingText;
    private Button refreshButton;
    private AlertDialog dialog;

    private ArrayList<S3ObjectSummary> selectedList = new ArrayList<S3ObjectSummary>();

    private Timer mTimer;
    private LinearLayout transferLayout;
    private TransferModel[] mModels = new TransferModel[0];

    private CognitoCachingCredentialsProvider provider;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_s3);

        initLayout();
        initParams();
    }

    private void initLayout(){
        transferLayout = (LinearLayout) findViewById(R.id.transfer_layout);

        createBucket = (Button) findViewById(R.id.create_bucket);
        deleteBucket = (Button) findViewById(R.id.delete_bucket);
        downloadButton = (Button) findViewById(R.id.download);
        uploadImage = (Button) findViewById(R.id.upload_image);
        uploadVideo = (Button) findViewById(R.id.upload_video);

        createBucket.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new CreateBucket().execute();
            }
        });
        deleteBucket.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new DeleteBucket().execute();
            }
        });
        uploadImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!checked) {
                    Toast("Please wait a moment...");
                }
                else if (!exists) {
                    Toast("You must first create the bucket");
                }
                else {
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("image/*");
                    startActivityForResult(intent, 0);
                }
            }
        });

        uploadVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!checked) {
                    Toast("Please wait a moment...");
                }
                else if (!exists) {
                    Toast("You must first create the bucket");
                }
                else {
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("video/*");
                    startActivityForResult(intent, 0);
                }
            }
        });

        downloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                downloadListAdapter = new DownloadListAdapter(S3Activity.this, selectedList,S3Util.getPrefix(provider));
                getDownloadList();
            }
        });

    }

    private void getDownloadList(){
        final LinearLayout downloadLayout = (LinearLayout) getLayoutInflater().inflate(R.layout.layout_download_list,null);
        downloadList = (ListView) downloadLayout.findViewById(R.id.download_list);
        loadingText = (TextView) downloadLayout.findViewById(R.id.now_loading);
        Button downButton = (Button) downloadLayout.findViewById(R.id.download_button);
        refreshButton = (Button) downloadLayout.findViewById(R.id.refresh_button);
        downloadList.setAdapter(downloadListAdapter);
        downloadList.setOnItemClickListener(itemListener);
        refreshList();
        downButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                download();
                dialog.dismiss();
                downloadList = null;
                refreshButton = null;
            }
        });

        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refreshList();
            }
        });

        dialog = new AlertDialog.Builder(S3Activity.this)
                .setTitle("Download")
                .setView(downloadLayout)
                .setPositiveButton("Close", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        downloadList = null;
                        refreshButton = null;
                    }
                }).show();
    }

    private AdapterView.OnItemClickListener itemListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            S3ObjectSummary item = downloadListAdapter.getItem(position);
            boolean checked = false;
            // try removing, if it wasn't there add
            if (!selectedList.remove(item)) {
                selectedList.add(item);
                checked = true;
            }
            ((CheckBox)view.findViewById(R.id.checkbox)).setChecked(checked);
        }
    };

    private void download(){
        String[] keys = new String[selectedList.size()];
        int i = 0;
        for (S3ObjectSummary obj : selectedList) {
            keys[i] = obj.getKey();
            i++;
        }
        TransferController.download(S3Activity.this, keys);
    }

    private void refreshList(){
        if(downloadList!=null&&refreshButton!=null&&loadingText!=null){
            new RefreshTask().execute();
        }
    }

    private void initParams(){
//        s3Client = S3Util.getS3Client(CognitoSyncClientManager.getCredentialsProvider());
        provider = CognitoSyncClientManager.getCredentialsProvider();
        checkS3Client();
        new CheckBucketExists().execute();

        mTimer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                S3Activity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        syncModels();
                        for (int i = 0; i < transferLayout.getChildCount(); i++) {
                            ((TransferView) transferLayout.getChildAt(i)).refresh();
                        }
                    }
                });
            }
        };
        mTimer.schedule(task, 0, REFRESH_DELAY);

    }

    private void checkS3Client(){
        S3Util.checkS3Client(provider);
    }

    @Override
    protected void onActivityResult(int reqCode, int resCode, Intent data) {
        if (resCode == Activity.RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                TransferController.upload(this, uri);
            }
        }
    }

    @Override
    protected void onDestroy() {
        mTimer.cancel();
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        syncModels();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mTimer.purge();
    }

    /* makes sure that we are up to date on the transfers */
    private void syncModels() {
        TransferModel[] models = TransferModel.getAllTransfers();
        if (mModels.length != models.length) {
            // add the transfers we haven't seen yet
            for (int i = mModels.length; i < models.length; i++) {
                transferLayout.addView(new TransferView(this, models[i]), 0);
            }
            mModels = models;
        }
    }


    private class CreateBucket extends AsyncTask<Object, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Object... params) {
            checkS3Client();
            if (!S3Util.doesBucketExist()) {
                S3Util.createBucket();
                return true;
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (!result) {
                Toast("Bucket already exists");
            }
            else {
                Toast("Bucket successfully created!");
            }
            exists = true;
        }
    }

    private class CheckBucketExists extends AsyncTask<Object, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Object... params) {
            return S3Util.doesBucketExist();
        }

        @Override
        protected void onPostExecute(Boolean result) {
            checked = true;
            exists = result;
        }
    }

    private class DeleteBucket extends AsyncTask<Object, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Object... params) {
            checkS3Client();
            if (S3Util.doesBucketExist()) {
                S3Util.deleteBucket();
                return true;
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (!result) {
                Toast("Bucket does not exist");
            }
            else {
                Toast("Bucket successfully deleted!");
            }
            exists = false;
        }
    }

    private class RefreshTask extends AsyncTask<Void, Void, List<S3ObjectSummary>> {
        @Override
        protected void onPreExecute() {
            if (loadingText != null && downloadList != null) {
                loadingText.setVisibility(View.VISIBLE);
                downloadList.setVisibility(View.GONE);
            }
            if (refreshButton != null){
                refreshButton.setEnabled(false);
                refreshButton.setText("Refreshing...");
            }
            if(refreshButton !=null)
                downloadButton.setEnabled(false);
        }

        @Override
        protected List<S3ObjectSummary> doInBackground(Void... params) {
            // get all the objects in bucket
            return S3Util.listObjects(provider);
        }

        @Override
        protected void onPostExecute(List<S3ObjectSummary> objects) {
            // now that we have all the keys, add them all to the adapter
            if(loadingText!=null&&downloadList!=null) {
                loadingText.setVisibility(View.GONE);
                downloadList.setVisibility(View.VISIBLE);
            }
            downloadListAdapter.clear();
            downloadListAdapter.addAll(objects);
            selectedList.clear();
            if(refreshButton!=null) {
                refreshButton.setEnabled(true);
                refreshButton.setText("Refresh");
            }
            if(downloadButton!=null)
                downloadButton.setEnabled(true);
        }
    }

    private void Toast(String value){
        if(!TextUtils.isEmpty(value)) {
            Log.d(TAG, value);
            Toast.makeText(S3Activity.this, TAG+ ": " + value, Toast.LENGTH_SHORT).show();
        }else if(value==null){
            Log.d(TAG, "null");
            Toast.makeText(S3Activity.this, TAG+ ": " + "null", Toast.LENGTH_SHORT).show();
        }else{
            Log.d(TAG, "");
            Toast.makeText(S3Activity.this, TAG+ ": " + "", Toast.LENGTH_SHORT).show();
        }
    }

}
