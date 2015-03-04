package com.jktheunique.aws.adapter;

import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.amazonaws.mobileconnectors.cognito.DatasetMetadata;
import com.jktheunique.aws.R;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * Created by JKtheUnique on 2014-10-27.
 */
public class DataSetAdapter extends ArrayAdapter<DatasetMetadata> {
    DateFormat df = SimpleDateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);

    public DataSetAdapter(Context context, int resource) {
        super(context, resource);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.adapter_data_set,
                    parent, false);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        DatasetMetadata dataset = getItem(position);
        // dataset name
        holder.tvKey.setText(dataset.getDatasetName());
        // if the dataset is deleted, put a strike
        holder.tvKey.setPaintFlags(holder.tvKey.getPaintFlags()
                & ~Paint.STRIKE_THRU_TEXT_FLAG);
        // last modified
        holder.tvValue.setText(df.format(dataset.getLastModifiedDate()));
        holder.tvSyncCount.setText(String.valueOf(dataset.getRecordCount()));

        return convertView;
    }

    static class ViewHolder {
        TextView tvKey;
        TextView tvValue;
        TextView tvSyncCount;

        public ViewHolder(View rootView) {
            tvKey = (TextView) rootView.findViewById(R.id.key);
            tvValue = (TextView) rootView.findViewById(R.id.value);
            tvSyncCount = (TextView) rootView.findViewById(R.id.count);
        }
    }
}