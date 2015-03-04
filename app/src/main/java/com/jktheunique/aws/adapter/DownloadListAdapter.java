package com.jktheunique.aws.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.jktheunique.aws.R;
import com.jktheunique.aws.util.S3Util;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by JKtheUnique on 2014-11-03.
 */
public class DownloadListAdapter extends ArrayAdapter<S3ObjectSummary> {

    private Context context;
    private ArrayList<S3ObjectSummary> selectedObjects;
    private String prefix;

    public DownloadListAdapter(Context context, ArrayList<S3ObjectSummary> selectedList, String prefix) {
        super(context, R.layout.layout_download_list_item);
        this.context = context;
        selectedObjects = selectedList;
        this.prefix = prefix;
    }

    @Override
    public View getView(int pos, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(
                    R.layout.layout_download_list_item, null);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        S3ObjectSummary summary = getItem(pos);
        holder.checkbox.setChecked(selectedObjects.contains(summary));
        holder.key.setText(S3Util.getFileName(summary.getKey()));
        holder.size.setText(String.valueOf(summary.getSize()));
        return convertView;
    }

    public void addAll(Collection<? extends S3ObjectSummary> collection) {
        for (S3ObjectSummary obj : collection) {
            // if statement removes the "folder" from showing up
            if (!obj.getKey().equals(prefix))
            {
                add(obj);
            }
        }
    }

    private class ViewHolder {
        private CheckBox checkbox;
        private TextView key;
        private TextView size;

        private ViewHolder(View view) {
            checkbox = (CheckBox) view.findViewById(R.id.checkbox);
            key = (TextView) view.findViewById(R.id.key);
            size = (TextView) view.findViewById(R.id.size);
        }
    }
}
