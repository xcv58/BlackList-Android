package com.xcv58.joulerenergymanager;

import android.content.Context;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

/**
 * Created by xcv58 on 11/24/14.
 */
public class OptionsAdapter extends ArrayAdapter<MyOption> {

    private final Context context;
    private List<MyOption> optionList;
    private PackageManager pm;

    public OptionsAdapter(Context context, List<MyOption> optionList) {
        super(context, R.layout.option_list, optionList);
        this.context = context;
        this.optionList = optionList;
        pm = context.getPackageManager();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View rowView = inflater.inflate(R.layout.option_list, parent, false);


        TextView textView = (TextView) rowView.findViewById(R.id.option);
        MyOption myOption = optionList.get(position);
        textView.setText(myOption.getName());
        if (myOption.isSelected()) {
            rowView.setBackgroundColor(0xFFAA66CC);
        }
        return rowView;
    }
}
