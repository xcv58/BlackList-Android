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
    private PackageManager pm;

    public MobileArrayAdapter(Context context, List<MyPackageInfo> packageInfoList) {
        super(context, R.layout.list, packageInfoList);
        this.context = context;
        this.packageInfoList = packageInfoList;
        pm = context.getPackageManager();
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

        textView_name.setText(myPackageInfo.getAppName());
        textView_label.setText(myPackageInfo.getPackageName());

        Drawable icon = myPackageInfo.getIcon();
        imageView.setImageDrawable((icon != null) ? icon : context.getResources().getDrawable( R.drawable.ic_launcher ));

        if (myPackageInfo.inList()) {
            rowView.setBackgroundColor(0xFFAA66CC);
            checkBox.setChecked(true);
        }

        return rowView;
    }
}
