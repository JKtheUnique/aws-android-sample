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

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.jktheunique.aws.R;
import com.jktheunique.aws.models.DownloadModel;
import com.jktheunique.aws.models.TransferModel;
import com.jktheunique.aws.models.UploadModel;
import com.jktheunique.aws.network.TransferController;


/*
 * This view handles user interaction with a single transfer, such as giving the
 * option to pause and abort, and also showing the user the progress of the transfer.
 */
public class TransferView extends LinearLayout {
    private static final String TAG = "TransferView";
    private Context mContext;
    private TransferModel mModel;
    private TextView mText;
    private ImageView mImage;
    private ImageButton mPause;
    private ImageButton mAbort;
    private TextView mCanceled;
    private TextView mCompleted;
    private Button mOpen;
    private ProgressBar mProgress;

    public TransferView(Context context, TransferModel model) {
        super(context);
        LayoutInflater.from(context).inflate(
                R.layout.layout_transfer_view,
                this,
                true);

        mContext = context;
        mModel = model;

        mImage = (ImageView) findViewById(R.id.image);
        mText = (TextView) findViewById(R.id.text);
        mPause = (ImageButton) findViewById(R.id.left_button);
        mAbort = (ImageButton) findViewById(R.id.right_button);
        mProgress = (ProgressBar) findViewById(R.id.progress);
        mCanceled = (TextView) findViewById(R.id.canceled);
        mCompleted = (TextView) findViewById(R.id.completed);
        mOpen = (Button) findViewById(R.id.open);

        mAbort.setImageResource(R.drawable.ic_action_cancel);

        mPause.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onPause();
            }
        });
        mAbort.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onAbort();
            }
        });

        refresh();
    }

    /* refresh method for public use */
    public void refresh() {
        refresh(mModel.getStatus());
    }

    /*
     * We use this method within the class so that we can have the UI update
     * quickly when the user selects something
     */
    private void refresh(TransferModel.Status status) {
        int leftRes = 0;
        int progress = 0;
        final MimeTypeMap m = MimeTypeMap.getSingleton();

        String mimeType;
        if(mModel instanceof UploadModel)
            mimeType =  mContext.getContentResolver().getType(mModel.getUri());
        else
            mimeType = m.getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(mModel.getUri().toString()));

        if (!TextUtils.isEmpty(mimeType)&&mimeType.startsWith("image")&&!((Activity)mContext).isDestroyed())
            Glide.with(mContext).load(mModel.getUri().toString()).into(mImage);

        switch (status) {
            case IN_PROGRESS:
                leftRes = R.drawable.ic_action_pause;
                progress = mModel.getProgress();
                break;
            case PAUSED:
                leftRes = R.drawable.ic_action_play;
                progress = mModel.getProgress();
                break;
            case CANCELED:
                leftRes = R.drawable.ic_action_play;
                progress = 0;
                mPause.setVisibility(View.GONE);
                mAbort.setVisibility(View.GONE);
                mCanceled.setVisibility(View.VISIBLE);
                break;
            case COMPLETED:
                leftRes = R.drawable.ic_action_play;
                progress = 100;
                mPause.setVisibility(View.GONE);
                mAbort.setVisibility(View.GONE);

                if (mModel instanceof DownloadModel) {
                    // if download completed, show option to open file
                    mOpen.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            // get the file extension
                            try {
                                // try opening activity to open file
                                String mimeType = m.getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(mModel.getUri().toString()));
                                Intent intent = new Intent(
                                        Intent.ACTION_GET_CONTENT);
                                intent.setDataAndType(mModel.getUri(), mimeType);
                                mContext.startActivity(intent);
                            } catch (ActivityNotFoundException e) {
                                Log.d(TAG, "", e);
                                // if file fails to be opened, show error
                                // message
                                Toast.makeText(
                                        mContext,
                                        "nothing_found_to_open_file",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                    mOpen.setVisibility(View.VISIBLE);
                    mCompleted.setVisibility(View.GONE);
                }else{
                    mCompleted.setVisibility(View.VISIBLE);
                    mOpen.setVisibility(View.GONE);
                }
                break;
        }

        mText.setText(mModel.getFileName());

        mPause.setImageResource(leftRes);
        mProgress.setProgress(progress);
    }

    /* What to do when user presses pause button */
    private void onPause() {
        if (mModel.getStatus() == TransferModel.Status.IN_PROGRESS) {
            TransferController.pause(mContext, mModel);
            refresh(TransferModel.Status.PAUSED);
        } else {
            TransferController.resume(mContext, mModel);
            refresh(TransferModel.Status.IN_PROGRESS);
        }
    }

    /* What to do when user presses abort button */
    private void onAbort() {
        TransferController.abort(mContext, mModel);
        refresh(TransferModel.Status.CANCELED);
    }
}
