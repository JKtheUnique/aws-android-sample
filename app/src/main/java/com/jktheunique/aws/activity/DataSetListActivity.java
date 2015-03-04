package com.jktheunique.aws.activity;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.mobileconnectors.cognito.CognitoSyncManager;
import com.amazonaws.mobileconnectors.cognito.DatasetMetadata;
import com.amazonaws.mobileconnectors.cognito.exceptions.DataStorageException;
import com.amazonaws.services.cognitoidentity.model.NotAuthorizedException;
import com.jktheunique.aws.R;
import com.jktheunique.aws.adapter.DataSetAdapter;
import com.jktheunique.aws.util.CognitoSyncClientManager;

import java.util.List;

public class DataSetListActivity extends Activity {

    public static final String TAG = "DataSetListActivity";

    private ListView listView;
    private ActionBar actionBar;
    private Button syncButton;
    private Button addButton;

    private CognitoSyncManager client;
    private DataSetAdapter dataSetAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_set_list);

        initLayout();
        initParams();
        setLayout();
        setHeader();
        refreshDatasetMetadata();
    }

    @Override
    protected void onResume(){
        super.onResume();
    }

    private void initLayout(){
        listView = (ListView) findViewById(R.id.listview);
        syncButton = (Button) findViewById(R.id.sync_button);
        addButton = (Button) findViewById(R.id.add_button);
        actionBar = getActionBar();
   }

    private void initParams(){
        client = CognitoSyncClientManager.getSyncManager();
        dataSetAdapter = new DataSetAdapter(DataSetListActivity.this,R.layout.adapter_data_set);
    }

    private void setLayout(){
        actionBar.setDisplayShowTitleEnabled(true);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                DatasetMetadata data = dataSetAdapter.getItem(position - 1);
                Intent intent = new Intent(DataSetListActivity.this, DataSetContentsListActivity.class);
                intent.putExtra(DataSetContentsListActivity.DATA_SET_NAME, data.getDatasetName());
                startActivity(intent);
            }
        });
        syncButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refreshDatasetMetadata();
            }
        });
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final EditText input = new EditText(DataSetListActivity.this);
                input.setSingleLine(true);

                new AlertDialog.Builder(DataSetListActivity.this)
                        .setTitle("Add DataSet")
                        .setView(input)

                                // Set up the buttons
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String datasetName = input.getText().toString();
                                if (!TextUtils.isEmpty(datasetName.trim())) {
                                    client.openOrCreateDataset(datasetName);
                                    refreshDataSetList();
                                }
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        })
                        .show();
            }
        });
    }

    private void setHeader(){
        LinearLayout header = (LinearLayout)getLayoutInflater().inflate(R.layout.adapter_data_set,null);
        ((TextView)header.findViewById(R.id.key)).setText("DataSet");
        ((TextView)header.findViewById(R.id.value)).setText("Modified");
        ((TextView)header.findViewById(R.id.count)).setText("SyncCount");
        listView.addHeaderView(header);
    }

    private void refreshDataSetList() {
        List<DatasetMetadata> datasets = client.listDatasets();
        dataSetAdapter.clear();
        for (DatasetMetadata dataset : datasets) {
            dataSetAdapter.add(dataset);
        }
        dataSetAdapter.notifyDataSetChanged();
        listView.setAdapter(dataSetAdapter);

        actionBar.setTitle("Datasets");
    }

    private void refreshDatasetMetadata() {
        new RefreshDatasetMetadataTask().execute();
    }


    private class RefreshDatasetMetadataTask extends AsyncTask<Void, Void, Void> {
        ProgressDialog dialog;
        boolean authError;

        @Override
        protected void onPreExecute() {
            dialog = ProgressDialog.show(DataSetListActivity.this,
                    "Syncing", "Please wait");
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                client.refreshDatasetMetadata();
            } catch (DataStorageException dse) {
                Log.e(TAG, "failed to refresh dataset metadata", dse);
            } catch (NotAuthorizedException e) {
                Log.e(TAG, "failed to refresh dataset metadata", e);
                authError = true;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            dialog.dismiss();
            if (!authError) {
                refreshDataSetList();
            }
            else {
                // Probably an authentication (or lackthereof) error
                Toast.makeText(DataSetListActivity.this, "authfailed",Toast.LENGTH_SHORT).show();
            }
        }
    }

}
