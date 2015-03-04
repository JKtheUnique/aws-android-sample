package com.jktheunique.aws.adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.amazonaws.mobileconnectors.cognito.Record;
import com.jktheunique.aws.R;

/**
 * Created by JKtheUnique on 2014-10-27.
 */
public class DataSetContentsAdapter extends ArrayAdapter<Record> {
    public DataSetContentsAdapter(Context context, int resource) {
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

        Record record = getItem(position);
        // record key
        holder.tvKey.setText(record.getKey());
        // if the record is modified, mark it blue
        holder.tvKey.setTextColor(record.isModified() ? Color.BLUE : Color.BLACK);
        // if the record is deleted, put a strike
        if (record.isDeleted()) {
            holder.tvKey.setPaintFlags(holder.tvKey.getPaintFlags()
                    | Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            holder.tvKey.setPaintFlags(holder.tvKey.getPaintFlags()
                    & ~Paint.STRIKE_THRU_TEXT_FLAG);
        }
        // record value
        holder.tvValue.setText(record.getValue() == null ? "" : record.getValue());
        // record sync count
        holder.tvSyncCount.setText(String.valueOf(record.getSyncCount()));

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
