package com.jktheunique.aws.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.jktheunique.aws.R;
import com.jktheunique.aws.type.DataTestSet;

/**
 * Created by JKtheUnique on 2014-10-29.
 */
public class DataTestSetAdapter extends ArrayAdapter<DataTestSet> {
    refreshListener listener;

    public DataTestSetAdapter(Context context, int resource, refreshListener refreshListener) {
        super(context, resource);
        listener = refreshListener;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.adapter_data_test_set,
                    parent, false);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        DataTestSet testSet = getItem(position);

        holder.testId.setText(String.valueOf(testSet.getId()));
        holder.testName.setText(testSet.getName());
        holder.testAddress.setText(testSet.getAddress());
        holder.testAttr.setText(testSet.getTestAttr());
        holder.testtestString.setText(testSet.gettestString());

        holder.refreshButton.setTag(testSet);
        holder.refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.refreshThis((DataTestSet)v.getTag());
            }
        });

        holder.wrapper.setTag(testSet);
        holder.wrapper.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.updateThis((DataTestSet)v.getTag());
            }
        });
        holder.wrapper.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                listener.deleteThis((DataTestSet)v.getTag());
                return false;
            }
        });
        return convertView;
    }

    static class ViewHolder {
        TextView testId;
        TextView testName;
        TextView testAddress;
        TextView testAttr;
        TextView testtestString;
        LinearLayout wrapper;
        Button refreshButton;

        public ViewHolder(View rootView) {
            wrapper = (LinearLayout)rootView.findViewById(R.id.content_wrapper);
            testId= (TextView) rootView.findViewById(R.id.test_id);
            testName = (TextView) rootView.findViewById(R.id.test_name);
            testAddress = (TextView) rootView.findViewById(R.id.test_address);
            testAttr= (TextView) rootView.findViewById(R.id.test_attr);
            testtestString= (TextView) rootView.findViewById(R.id.test_test_string);
            refreshButton = (Button) rootView.findViewById(R.id.refresh_button);
        }
    }

    public interface refreshListener{
        public void refreshThis(DataTestSet set);
        public void updateThis(DataTestSet set);
        public void deleteThis(DataTestSet set);
    }
}
