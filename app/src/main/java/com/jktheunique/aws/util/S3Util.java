/*
 * Copyright 2010-2014 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.jktheunique.aws.util;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.DeleteObjectsRequest.KeyVersion;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.jktheunique.aws.Constants;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/* 
 * This class just handles getting the client since we don't need to have more than
 * one per application
 */
public class S3Util {
    private static AmazonS3Client s3Client;

    public static String getPrefix(CognitoCachingCredentialsProvider provider) {
        return provider.getCachedIdentityId() + "/";
    }

    public static AmazonS3Client getS3Client(CognitoCachingCredentialsProvider provider) {
        if (s3Client == null) {
            s3Client = new AmazonS3Client(provider);
        }
        return s3Client;
    }

    public static void checkS3Client(CognitoCachingCredentialsProvider provider){
        if(s3Client== null){
            s3Client = new AmazonS3Client(provider);
        }
    }

    public static String getFileName(String path) {
        return path.substring(path.lastIndexOf("/") + 1);
    }

    public static boolean doesBucketExist() {
        return s3Client.doesBucketExist(Constants.S3_BUCKET_NAME.toLowerCase(Locale.US));
    }

    public static void createBucket() {
        s3Client.createBucket(Constants.S3_BUCKET_NAME.toLowerCase(Locale.US));
    }

    public static void deleteBucket() {
        String name = Constants.S3_BUCKET_NAME.toLowerCase(Locale.US);
        List<S3ObjectSummary> objData = s3Client.listObjects(name).getObjectSummaries();
        if (objData.size() > 0) {
            DeleteObjectsRequest emptyBucket = new DeleteObjectsRequest(name);
            List<KeyVersion> keyList = new ArrayList<KeyVersion>();
            for (S3ObjectSummary summary : objData) {
                keyList.add(new KeyVersion(summary.getKey()));
            }
            emptyBucket.withKeys(keyList);
            s3Client.deleteObjects(emptyBucket);
        }
        s3Client.deleteBucket(name);
    }

    public static List<S3ObjectSummary> listObjects(CognitoCachingCredentialsProvider provider){
        return s3Client.listObjects(Constants.S3_BUCKET_NAME.toLowerCase(Locale.US),getPrefix(provider)).getObjectSummaries();
    }
}
