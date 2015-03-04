package com.jktheunique.aws.type;

import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBAttribute;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBHashKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBTable;
import com.jktheunique.aws.Constants;

/**
 * Created by JKtheUnique on 2014-10-29.
 */
@DynamoDBTable(tableName= Constants.DDB_TABLE_NAME)
public class DataTestSet {
    private int id;
    private String name;
    private String address;
    private String testAttr;
    private Boolean isBoolean;
    private String testString;
    private String Teststring;

    @DynamoDBHashKey(attributeName = "id")
    public int getId(){
        return id;
    }

    public void setId(int id){
        this.id= id;
    }

    @DynamoDBAttribute(attributeName = "name")
    public String getName(){
        return name;
    }

    public void setName(String name){
        this.name=name;
    }

    @DynamoDBAttribute(attributeName = "address")
    public String getAddress(){
        return address;
    }

    public void setAddress(String address){
        this.address=address;
    }

    @DynamoDBAttribute(attributeName = "testAttr")
    public String getTestAttr(){
        return testAttr;
    }

    public void setTestAttr(String testAttr){
        this.testAttr=testAttr;
    }

    @DynamoDBAttribute(attributeName = "isBoolean")
    public Boolean getIsBoolean(){
        return isBoolean;
    }

    public void setIsBoolean(Boolean isBoolean){
        this.isBoolean = isBoolean;
    }
    @DynamoDBAttribute(attributeName = "testString")
    public String gettestString(){
        return testString;
    }

    public void settestString(String testString){
        this.testString = testString;
    }
    @DynamoDBAttribute(attributeName = "Teststring")
    public String getTeststring(){
        return Teststring;
    }

    public void setTeststring(String Teststring){
        this.Teststring = Teststring;
    }

}
