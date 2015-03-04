package com.jktheunique.aws.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBScanExpression;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.PaginatedScanList;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.DeleteTableRequest;
import com.amazonaws.services.dynamodbv2.model.DescribeTableRequest;
import com.amazonaws.services.dynamodbv2.model.DescribeTableResult;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.jktheunique.aws.Constants;
import com.jktheunique.aws.R;
import com.jktheunique.aws.adapter.DataTestSetAdapter;
import com.jktheunique.aws.type.DataTestSet;
import com.jktheunique.aws.util.CognitoSyncClientManager;

import java.util.ArrayList;

/**
 * Created by JKtheUnique on 2014-10-29.
 */
public class DynamoActivity extends Activity{
    private static String TAG = "DYNAMO_ACTIVITY";

    private Button createButton;
    private Button insertButton;
    private Button listButton;
    private Button deleteButton;

    private ListView listView;

    private ArrayList<DataTestSet> resultListData = new ArrayList<DataTestSet>();

    private AmazonDynamoDBClient ddb;
    private DataTestSetAdapter testSetAdapter;
    private DataTestSet workingSet;
    private DynamoDBMapper DDBMapper;
    private boolean isWorking = false;

    private ProgressDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dynamo);

        initLayout();
        initParams();

        getDynamoDBClient();
    }

    private void initLayout(){
        listView = (ListView) findViewById(R.id.listview);

        LinearLayout header = (LinearLayout) getLayoutInflater().inflate(R.layout.adapter_data_test_set,null);
        header.findViewById(R.id.refresh_button).setVisibility(View.INVISIBLE);
        ((TextView)header.findViewById(R.id.test_id)).setText("Id");
        ((TextView)header.findViewById(R.id.test_name)).setText("Name");
        ((TextView)header.findViewById(R.id.test_address)).setText("address");
        ((TextView)header.findViewById(R.id.test_attr)).setText("attr");
        ((TextView)header.findViewById(R.id.test_test_string)).setText("testString");
        listView.addHeaderView(header);


        createButton = (Button) findViewById(R.id.create_table);
        insertButton = (Button) findViewById(R.id.insert_data);
        listButton = (Button) findViewById(R.id.get_list);
        deleteButton = (Button) findViewById(R.id.delete_table);

        createButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                runDynamoDBManagerTask(DynamoDBManagerType.CREATE_TABLE);
            }
        });
        insertButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                runDynamoDBManagerTask(DynamoDBManagerType.INSERT_DATA);
            }
        });
        listButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                runDynamoDBManagerTask(DynamoDBManagerType.LIST_DATA);
            }
        });
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                runDynamoDBManagerTask(DynamoDBManagerType.CLEAN_UP);
            }
        });
    }

    private AdapterView.OnItemClickListener itemListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            final DataTestSet thisSet = (DataTestSet)parent.getItemAtPosition(position);
            LinearLayout layout = (LinearLayout)getLayoutInflater().inflate(R.layout.layout_change_test_set,null);
            final EditText idInput = (EditText)layout.findViewById(R.id.test_set_id_input);
            final EditText nameInput = (EditText)layout.findViewById(R.id.test_set_name_input);
            final EditText addressInput = (EditText)layout.findViewById(R.id.test_set_address_input);
            final EditText attrInput = (EditText)layout.findViewById(R.id.test_set_attr_input);
            final EditText testStringInput = (EditText)layout.findViewById(R.id.test_set_test_string_input);
            final EditText TeststringInput = (EditText)layout.findViewById(R.id.test_set_string_test_input);
            final CheckBox isBooleanInput = (CheckBox)layout.findViewById(R.id.test_set_isboolean_input);

            idInput.setText(String.valueOf(thisSet.getId()));
            nameInput.setText(thisSet.getName());
            addressInput.setText(thisSet.getAddress());
            attrInput.setText(thisSet.getTestAttr());
            testStringInput.setText(thisSet.gettestString());
            TeststringInput.setText(thisSet.getTeststring());
            isBooleanInput.setChecked(thisSet.getIsBoolean());

            new AlertDialog.Builder(DynamoActivity.this)
                    .setTitle("Change This item")
                    .setView(layout)
                    .setPositiveButton("OK",new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if(canSaveWorkingSet(idInput.getText().toString())) {
                                thisSet.setId(Integer.parseInt(idInput.getText().toString()));
                                thisSet.setName(nameInput.getText().toString());
                                thisSet.setAddress(addressInput.getText().toString());
                                thisSet.setTestAttr(attrInput.getText().toString());
                                thisSet.settestString(testStringInput.getText().toString());
                                thisSet.setTeststring(TeststringInput.getText().toString());
                                thisSet.setIsBoolean(isBooleanInput.isChecked());

                                runDynamoDBManagerTask(DynamoDBManagerType.UPDATE_ONE,thisSet);
                            }
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

    private AdapterView.OnItemLongClickListener deleteItemListener = new AdapterView.OnItemLongClickListener() {
        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
            DataTestSet thisSet = (DataTestSet)parent.getItemAtPosition(position);

            runDynamoDBManagerTask(DynamoDBManagerType.DELETE_ONE,thisSet);
            return false;
        }
    };

    private boolean canSaveWorkingSet(String idString){
        boolean ret = false;
        if(!TextUtils.isEmpty(idString)) {
            try {
                int id = Integer.parseInt(idString);
                ret = true;
            } catch (NumberFormatException e) {
                ret = false;
            }
        }
        return ret;
    }

    private void initParams(){
        testSetAdapter = new DataTestSetAdapter(DynamoActivity.this, R.layout.adapter_data_test_set,new DataTestSetAdapter.refreshListener() {
            @Override
            public void refreshThis(DataTestSet thisSet) {
                runDynamoDBManagerTask(DynamoDBManagerType.GET_ONE,thisSet);
            }
            @Override
            public void updateThis(final DataTestSet thisSet) {
                LinearLayout layout = (LinearLayout)getLayoutInflater().inflate(R.layout.layout_change_test_set,null);
                final EditText idInput = (EditText)layout.findViewById(R.id.test_set_id_input);
                final EditText nameInput = (EditText)layout.findViewById(R.id.test_set_name_input);
                final EditText addressInput = (EditText)layout.findViewById(R.id.test_set_address_input);
                final EditText attrInput = (EditText)layout.findViewById(R.id.test_set_attr_input);
                final EditText testStringInput = (EditText)layout.findViewById(R.id.test_set_test_string_input);
                final EditText TeststringInput = (EditText)layout.findViewById(R.id.test_set_string_test_input);
                final CheckBox isBooleanInput = (CheckBox)layout.findViewById(R.id.test_set_isboolean_input);

                idInput.setText(String.valueOf(thisSet.getId()));
                nameInput.setText(thisSet.getName());
                addressInput.setText(thisSet.getAddress());
                attrInput.setText(thisSet.getTestAttr());
                testStringInput.setText(thisSet.gettestString());
                TeststringInput.setText(thisSet.getTeststring());
                isBooleanInput.setChecked(thisSet.getIsBoolean());

                new AlertDialog.Builder(DynamoActivity.this)
                        .setTitle("Change This item")
                        .setView(layout)
                        .setPositiveButton("OK",new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if(canSaveWorkingSet(idInput.getText().toString())) {
                                    thisSet.setId(Integer.parseInt(idInput.getText().toString()));
                                    thisSet.setName(nameInput.getText().toString());
                                    thisSet.setAddress(addressInput.getText().toString());
                                    thisSet.setTestAttr(attrInput.getText().toString());
                                    thisSet.settestString(testStringInput.getText().toString());
                                    thisSet.setTeststring(TeststringInput.getText().toString());
                                    thisSet.setIsBoolean(isBooleanInput.isChecked());

                                    runDynamoDBManagerTask(DynamoDBManagerType.UPDATE_ONE,thisSet);
                                }
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
            @Override
            public void deleteThis(DataTestSet thisSet) {
                runDynamoDBManagerTask(DynamoDBManagerType.DELETE_ONE,thisSet);
            }
        });
    }


    private void getDynamoDBClient(){
        ddb = new AmazonDynamoDBClient(CognitoSyncClientManager.getCredentialsProvider());
        ddb.setRegion(Region.getRegion(Regions.US_EAST_1));
        DDBMapper = new DynamoDBMapper(ddb);
    }

    private void createTable(){
        Log.d(TAG, "Create table called");

        KeySchemaElement kse = new KeySchemaElement().withAttributeName(
                "id").withKeyType(KeyType.HASH);
        AttributeDefinition ad = new AttributeDefinition().withAttributeName(
                "id").withAttributeType(ScalarAttributeType.N);
        ProvisionedThroughput pt = new ProvisionedThroughput()
                .withReadCapacityUnits(10l).withWriteCapacityUnits(5l);

        CreateTableRequest request = new CreateTableRequest()
                .withTableName(Constants.DDB_TABLE_NAME)
                .withKeySchema(kse).withAttributeDefinitions(ad)
                .withProvisionedThroughput(pt);

        try {
            Log.d(TAG, "Sending Create table request");
            ddb.createTable(request);
            Log.d(TAG, "Create request response successfully recieved");
        } catch (AmazonServiceException ex) {
            Log.e(TAG, "Error sending create table request", ex);
        }
    }

    public String getTestTableStatus() {

        try {
            DescribeTableRequest request = new DescribeTableRequest()
                    .withTableName(Constants.DDB_TABLE_NAME);
            DescribeTableResult result = ddb.describeTable(request);

            String status = result.getTable().getTableStatus();
            return status == null ? "" : status;

        } catch (ResourceNotFoundException e) {
        } catch (AmazonServiceException ex) {
            wipeCredentialsOnAuthError(ex);
        }

        return "";
    }


    public boolean wipeCredentialsOnAuthError(AmazonServiceException ex) {
        Log.e(TAG, "Error, wipeCredentialsOnAuthError called" + ex);
        isWorking=false;
        dialog.dismiss();
        if (
            // STS
            // http://docs.amazonwebservices.com/STS/latest/APIReference/CommonErrors.html
                ex.getErrorCode().equals("IncompleteSignature")
                        || ex.getErrorCode().equals("InternalFailure")
                        || ex.getErrorCode().equals("InvalidClientTokenId")
                        || ex.getErrorCode().equals("OptInRequired")
                        || ex.getErrorCode().equals("RequestExpired")
                        || ex.getErrorCode().equals("ServiceUnavailable")

                        // DynamoDB
                        // http://docs.amazonwebservices.com/amazondynamodb/latest/developerguide/ErrorHandling.html#APIErrorTypes
                        || ex.getErrorCode().equals("AccessDeniedException")
                        || ex.getErrorCode().equals("IncompleteSignatureException")
                        || ex.getErrorCode().equals(
                        "MissingAuthenticationTokenException")
                        || ex.getErrorCode().equals("ValidationException")
                        || ex.getErrorCode().equals("InternalFailure")
                        || ex.getErrorCode().equals("InternalServerError")) {

            return true;
        }

        return false;
    }

    private void runDynamoDBManagerTask(DynamoDBManagerType type){
        runDynamoDBManagerTask(type,null);
    }

    private void runDynamoDBManagerTask(DynamoDBManagerType type, DataTestSet workingSet){
        if(!isWorking){
            if(workingSet!=null)
                this.workingSet = workingSet;
            new DynamoDBManagerTask().execute(type);
        }else
            Toast.makeText(DynamoActivity.this, "DynamoDBManager is Working",Toast.LENGTH_SHORT).show();

    }


    private class DynamoDBManagerTask extends
            AsyncTask<DynamoDBManagerType, Void, DynamoDBManagerTaskResult> {
        protected void onPreExecute(){
            dialog = ProgressDialog.show(DynamoActivity.this, "DynamoDBManager","onWorking");
            isWorking = true;
        }

        protected DynamoDBManagerTaskResult doInBackground(
                DynamoDBManagerType... types) {
            String tableStatus = getTestTableStatus();

            DynamoDBManagerTaskResult result = new DynamoDBManagerTaskResult();
            result.setTableStatus(tableStatus);
            result.setTaskType(types[0]);

            if (types[0] == DynamoDBManagerType.CREATE_TABLE) {
                if (tableStatus.length() == 0) {
                    createTable();
                }
            }else if (types[0] == DynamoDBManagerType.INSERT_DATA) {
                if (tableStatus.equalsIgnoreCase("ACTIVE")) {
                    insertData();
                }
            }else if (types[0] == DynamoDBManagerType.LIST_DATA) {
                if (tableStatus.equalsIgnoreCase("ACTIVE")) {
                   resultListData = getData();
                }
            }else if (types[0] == DynamoDBManagerType.CLEAN_UP) {
                if (tableStatus.equalsIgnoreCase("ACTIVE")) {
                    cleanUp();
                }
            }else if (types[0] == DynamoDBManagerType.GET_ONE) {
                if (tableStatus.equalsIgnoreCase("ACTIVE")&&workingSet!=null) {
                    workingSet = getDataSet();
                }
            }else if (types[0] == DynamoDBManagerType.UPDATE_ONE) {
                if (tableStatus.equalsIgnoreCase("ACTIVE")&&workingSet!=null) {
                    updateDataSet();
                }
            }else if (types[0] == DynamoDBManagerType.DELETE_ONE) {
                if (tableStatus.equalsIgnoreCase("ACTIVE")&&workingSet!=null) {
                    deleteDataSet();
                }
            }

            return result;
        }

        protected void onPostExecute(DynamoDBManagerTaskResult result) {

            if (result.getTaskType() == DynamoDBManagerType.CREATE_TABLE) {
                if (result.getTableStatus().length() != 0) {
                    Toast.makeText(
                            DynamoActivity.this,
                            "The test table already exists.\nTable Status: "
                                    + result.getTableStatus(),
                            Toast.LENGTH_LONG).show();
                }
            }else if (result.getTableStatus().equalsIgnoreCase("ACTIVE")
                    && result.getTaskType() == DynamoDBManagerType.LIST_DATA) {
                if(resultListData.size()>0){
                    refreshAdapter();
                }
            }else if (result.getTableStatus().equalsIgnoreCase("ACTIVE")
                    && result.getTaskType() == DynamoDBManagerType.INSERT_DATA) {
                Toast.makeText(DynamoActivity.this,
                        "Users inserted successfully!", Toast.LENGTH_SHORT).show();

            }else if (result.getTableStatus().equalsIgnoreCase("ACTIVE")
                    && result.getTaskType() == DynamoDBManagerType.GET_ONE) {
                if(resultListData.size()>0){
                    int index = -1;
                    for(DataTestSet currentSet : resultListData){
                        if(currentSet.getId() == workingSet.getId())
                            index = resultListData.indexOf(currentSet);
                    }
                    if(index!=-1) {
                        resultListData.remove(index);
                        resultListData.add(index, workingSet);
                    }
                    refreshAdapter();
                }
            }else if (result.getTableStatus().equalsIgnoreCase("ACTIVE")
                    && result.getTaskType() == DynamoDBManagerType.UPDATE_ONE) {
                if(resultListData.size()>0){
                    int index = -1;
                    for(DataTestSet currentSet : resultListData){
                        if(currentSet.getId() == workingSet.getId())
                            index = resultListData.indexOf(currentSet);
                    }
                    if(index!=-1) {
                        resultListData.remove(index);
                        resultListData.add(index, workingSet);
                    }
                    refreshAdapter();
                }
            }else if (result.getTableStatus().equalsIgnoreCase("ACTIVE")
                    && result.getTaskType() == DynamoDBManagerType.DELETE_ONE) {
                if(resultListData.size()>0){
                        resultListData.remove(workingSet);
                    refreshAdapter();
                }
            }else if (!result.getTableStatus().equalsIgnoreCase("ACTIVE")) {
                Toast.makeText(
                        DynamoActivity.this,
                        "The test table is not ready yet.\nTable Status: "
                                + result.getTableStatus(), Toast.LENGTH_LONG)
                        .show();
            }
            dialog.dismiss();
            isWorking = false;
        }
    }

    private void refreshAdapter(){
        testSetAdapter.clear();
        for(DataTestSet testSet: resultListData){
            testSetAdapter.add(testSet);
        }
        listView.setAdapter(testSetAdapter);
//        listView.setOnItemClickListener(itemListener);
//        listView.setOnItemLongClickListener(deleteItemListener);


    }

    private enum DynamoDBManagerType {
        GET_TABLE_STATUS, CREATE_TABLE, INSERT_DATA, LIST_DATA, CLEAN_UP, UPDATE_ONE, GET_ONE, DELETE_ONE
    }

    private class DynamoDBManagerTaskResult {
        private DynamoDBManagerType taskType;
        private String tableStatus;

        public DynamoDBManagerType getTaskType() {
            return taskType;
        }

        public void setTaskType(DynamoDBManagerType taskType) {
            this.taskType = taskType;
        }

        public String getTableStatus() {
            return tableStatus;
        }

        public void setTableStatus(String tableStatus) {
            this.tableStatus = tableStatus;
        }
    }



    public void insertData() {

        try {
            for (int i = 1; i <= 10; i++) {
                int currentPosition = i+resultListData.size();
                String current = String.valueOf(currentPosition);
                DataTestSet testSet = new DataTestSet();
                testSet.setId(currentPosition);
                testSet.setName("testName" + current);
                testSet.setAddress("testAddr" + current);
                testSet.setTestAttr("Attr" + " A " + current);
                testSet.setIsBoolean(i % 2 == 0 ? true : false);
                testSet.settestString("testString " + current);
                testSet.setTeststring("Teststring " + current);

                Log.d(TAG, "Inserting users");
                DDBMapper.save(testSet);
                Log.d(TAG, "Users inserted");
            }
        } catch (AmazonServiceException ex) {
            Log.e(TAG, "Error inserting users");
                    wipeCredentialsOnAuthError(ex);
        }
    }

    /*
     * Scans the table and returns the list of users.
     */
    public ArrayList<DataTestSet> getData() {

        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression();
        try {
            PaginatedScanList<DataTestSet> result = DDBMapper.scan(
                    DataTestSet.class, scanExpression);

            ArrayList<DataTestSet> resultList = new ArrayList<DataTestSet>();
            for (DataTestSet up : result) {
                resultList.add(up);
            }
            return resultList;

        } catch (AmazonServiceException ex) {
            wipeCredentialsOnAuthError(ex);
        }

        return null;
    }

    public DataTestSet getDataSet() {
        try {
            DataTestSet DataTestSet = DDBMapper.load(DataTestSet.class,
                    workingSet.getId());

            return DataTestSet;

        } catch (AmazonServiceException ex) {
         wipeCredentialsOnAuthError(ex);
        }

        return null;
    }

    public void updateDataSet() {

        try {
            DDBMapper.save(workingSet);

        } catch (AmazonServiceException ex) {
           wipeCredentialsOnAuthError(ex);
        }
    }

    public void deleteDataSet() {

        try {
            DDBMapper.delete(workingSet);

        } catch (AmazonServiceException ex) {
                    wipeCredentialsOnAuthError(ex);
        }
    }

    public void cleanUp() {

        DeleteTableRequest request = new DeleteTableRequest()
                .withTableName(Constants.DDB_TABLE_NAME);
        try {
            ddb.deleteTable(request);

        } catch (AmazonServiceException ex) {
                    wipeCredentialsOnAuthError(ex);
        }
    }


}