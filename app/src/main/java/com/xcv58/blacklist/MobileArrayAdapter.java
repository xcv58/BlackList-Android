package com.xcv58.blacklist;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

/**
 * Created by xcv58 on 11/19/14.
 */
public class MobileArrayAdapter extends ArrayAdapter<PackageInfo> {
    private final Context context;
    private List<PackageInfo> packageInfoList;

    public MobileArrayAdapter(Context context, List<PackageInfo> packageInfoList) {
        super(context, R.layout.list, packageInfoList);
        this.context = context;
        this.packageInfoList = packageInfoList;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View rowView = inflater.inflate(R.layout.list, parent, false);
        TextView textView_label = (TextView) rowView.findViewById(R.id.label);
        TextView textView_name = (TextView) rowView.findViewById(R.id.name);
        ImageView imageView = (ImageView) rowView.findViewById(R.id.logo);

        // set one view
        PackageInfo packageInfo =  packageInfoList.get(position);
        String name = packageInfo.applicationInfo.loadLabel(this.context.getPackageManager()).toString();
        textView_name.setText(name);
        textView_label.setText(packageInfo.packageName);

        try {
            Drawable icon = getContext().getPackageManager().getApplicationIcon(packageInfo.packageName);
            imageView.setImageDrawable(icon);
        } catch (PackageManager.NameNotFoundException e) {
            imageView.setImageResource(R.drawable.ic_launcher);
            e.printStackTrace();
        }

        return rowView;
    }
}
