package com.jktheunique.aws.activity;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.mobileconnectors.cognito.CognitoSyncManager;
import com.amazonaws.mobileconnectors.cognito.Dataset;
import com.amazonaws.mobileconnectors.cognito.DatasetMetadata;
import com.amazonaws.mobileconnectors.cognito.Record;
import com.amazonaws.mobileconnectors.cognito.SyncConflict;
import com.amazonaws.mobileconnectors.cognito.exceptions.DataStorageException;
import com.amazonaws.services.cognitoidentity.model.NotAuthorizedException;
import com.jktheunique.aws.R;
import com.jktheunique.aws.adapter.DataSetAdapter;
import com.jktheunique.aws.adapter.DataSetContentsAdapter;
import com.jktheunique.aws.util.CognitoSyncClientManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by JKtheUnique on 2014-10-27.
 */
public class DataSetContentsListActivity extends Activity {

    public static final String TAG = "DataSetContentsListActivity";
    public static final String DATA_SET_NAME = "DATA_SET_NAME";

    private ListView listView;
    private ActionBar actionBar;

    private Button syncButton;
    private Button addButton;

    private Dataset dataset;
    private DataSetContentsAdapter dataSetContentsAdapter;
    private String datasetName;

    private CognitoSyncManager client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_set_list);

        initLayout();
        initParams();
        setLayout();
        setHeader();
        synchronize(false);
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
        dataSetContentsAdapter = new DataSetContentsAdapter(DataSetContentsListActivity.this,R.layout.adapter_data_set);
        if(getIntent().hasExtra(DATA_SET_NAME))
            datasetName = getIntent().getStringExtra(DATA_SET_NAME);
        else
            datasetName = "test";
        dataset = client.openOrCreateDataset(datasetName);
    }

    private void setLayout(){
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(datasetName);

        listView.setOnItemClickListener(recordListener);
        listView.setOnItemLongClickListener(recordLongListener);

        syncButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                synchronize(false);
            }
        });
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LinearLayout changeRecordLayout = (LinearLayout)getLayoutInflater().inflate(R.layout.layout_change_record,null);
                final EditText keyInput = (EditText)changeRecordLayout.findViewById(R.id.key_input);
                final EditText valueInput = (EditText)changeRecordLayout.findViewById(R.id.value_input);
                new AlertDialog.Builder(DataSetContentsListActivity.this)
                        .setTitle("New Record")
                        .setView(changeRecordLayout)
                        .setPositiveButton("Commit", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (!TextUtils.isEmpty(keyInput.getText().toString().trim())) {
                                    dataset.put(keyInput.getText().toString(), valueInput.getText().toString());
                                    synchronize(false);
                                }else
                                    Toast.makeText(DataSetContentsListActivity.this, "Key cannot be empty string", Toast.LENGTH_SHORT).show();
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

    private AdapterView.OnItemClickListener recordListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            final Record record = dataSetContentsAdapter.getItem(position-1);
            LinearLayout changeRecordLayout = (LinearLayout)getLayoutInflater().inflate(R.layout.layout_change_record,null);
            final EditText keyInput = (EditText)changeRecordLayout.findViewById(R.id.key_input);
            keyInput.setEnabled(false);
            final EditText valueInput = (EditText)changeRecordLayout.findViewById(R.id.value_input);
            keyInput.setText(record.getKey());
            valueInput.setText(record.getValue());
            new AlertDialog.Builder(DataSetContentsListActivity.this)
                    .setTitle("Change Record")
                    .setView(changeRecordLayout)
                    .setPositiveButton("Commit", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dataset.put(record.getKey(),valueInput.getText().toString());
                            refreshDataSetContentsList();
                        }
                    })
                    .setNegativeButton("Cancel",new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    })
                    .show();
        }
    };

    private AdapterView.OnItemLongClickListener recordLongListener = new AdapterView.OnItemLongClickListener() {
        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
            final Record record = dataSetContentsAdapter.getItem(position-1);
            new AlertDialog.Builder(DataSetContentsListActivity.this)
                    .setTitle("Delete Record")
                    .setMessage("Delete This Record??")
                    .setPositiveButton("Commit", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dataset.remove(record.getKey());
                        }
                    })
                    .setNegativeButton("Cancel",new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    })
                    .show();
            return false;
        }
    };

    private void setHeader(){
        LinearLayout header = (LinearLayout)getLayoutInflater().inflate(R.layout.adapter_data_set,null);
        ((TextView)header.findViewById(R.id.key)).setText("Key");
        ((TextView)header.findViewById(R.id.value)).setText("Value");
        ((TextView)header.findViewById(R.id.count)).setText("SyncCount");
        listView.addHeaderView(header);
    }

    private void refreshDataSetContentsList() {
        dataSetContentsAdapter.clear();
        for (Record record : dataset.getAllRecords()) {
            dataSetContentsAdapter.add(record);
        }
        dataSetContentsAdapter.notifyDataSetChanged();
        listView.setAdapter(dataSetContentsAdapter);
    }

    private void synchronize(final boolean finish) {
        final ProgressDialog dialog = ProgressDialog.show(DataSetContentsListActivity.this,
                "Syncing", "Please wait");
        Log.i("Sync", "synchronize: " + finish);
        dataset.synchronize(new Dataset.SyncCallback() {
            @Override
            public void onSuccess(Dataset dataset, final List<Record> newRecords) {
                Log.i("Sync", "success");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dialog.dismiss();
                        if (finish) {
                            finish();
                        }
                        refreshDataSetContentsList();
                        Log.i("Sync", String.format("%d records synced", newRecords.size()));
                        Toast.makeText(DataSetContentsListActivity.this,
                                "Successful!", Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onFailure(final DataStorageException dse) {
                Log.i("Sync", "failure: ", dse);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dialog.dismiss();
                        Log.e("Sync", "failed: " + dse);
                        Toast.makeText(DataSetContentsListActivity.this,
                                "Failed due to\n" + dse.getMessage(), Toast.LENGTH_LONG)
                                .show();
                    }
                });
            }

            @Override
            public boolean onConflict(final Dataset dataset,
                                      final List<SyncConflict> conflicts) {
                Log.i("Sync", "conflict: " + conflicts);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dialog.dismiss();
                        Log.i(TAG,
                                String.format("%s records in conflict", conflicts.size()));
                        List<Record> resolvedRecords = new ArrayList<Record>();
                        for (SyncConflict conflict : conflicts) {
                            Log.i(TAG,
                                    String.format("remote: %s; local: %s",
                                            conflict.getRemoteRecord(),
                                            conflict.getLocalRecord()));
                            /* resolve by taking remote records */
                            resolvedRecords.add(conflict.resolveWithRemoteRecord());

                            /* resolve by taking local records */
                            // resolvedRecords.add(conflict.resolveWithLocalRecord());

                            /*
                             * resolve with customized logic, e.g. concatenate
                             * strings
                             */
                            // String newValue =
                            // conflict.getRemoteRecord().getValue()
                            // + conflict.getLocalRecord().getValue();
                            // resolvedRecords.add(conflict.resolveWithValue(newValue));
                        }
                        dataset.resolve(resolvedRecords);
                        refreshDataSetContentsList();
                        Toast.makeText(
                                DataSetContentsListActivity.this,
                                String.format(
                                        "%s records in conflict. Resolve by taking remote records",
                                        conflicts.size()),
                                Toast.LENGTH_LONG).show();
                    }
                });
                return true;
            }

            @Override
            public boolean onDatasetDeleted(Dataset dataset, String datasetName) {
                Log.i("Sync", "delete: " + datasetName);
                return true;
            }

            @Override
            public boolean onDatasetsMerged(Dataset dataset, List<String> datasetNames) {
                Log.i("Sync", "merge: " + datasetNames);
                return false;
            }
        });
    }
}

