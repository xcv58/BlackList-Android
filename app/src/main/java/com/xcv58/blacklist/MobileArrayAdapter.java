package com.xcv58.blacklist;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

/**
 * Created by xcv58 on 11/19/14.
 */
public class MobileArrayAdapter extends ArrayAdapter<MyPackageInfo> {
    private final Context context;
    private List<MyPackageInfo> packageInfoList;

    public MobileArrayAdapter(Context context, List<MyPackageInfo> packageInfoList) {
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
        CheckBox checkBox = (CheckBox) rowView.findViewById(R.id.check);

        // set one view
        MyPackageInfo myPackageInfo =  packageInfoList.get(position);
        String name = myPackageInfo.getAppName(context);
        textView_name.setText(name);
        textView_label.setText(myPackageInfo.getPackageName());

        if (myPackageInfo.inList()) {
            rowView.setBackgroundColor(0xFFAA66CC);
            checkBox.setChecked(true);
        }

        try {
            Drawable icon = getContext().getPackageManager().getApplicationIcon(myPackageInfo.getPackageName());
            imageView.setImageDrawable(icon);
        } catch (PackageManager.NameNotFoundException e) {
            imageView.setImageResource(R.drawable.ic_launcher);
            e.printStackTrace();
        }
        return rowView;
    }
}
