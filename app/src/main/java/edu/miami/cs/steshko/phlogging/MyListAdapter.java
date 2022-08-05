package edu.miami.cs.steshko.phlogging;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.SimpleAdapter;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class MyListAdapter extends SimpleAdapter implements SimpleAdapter.ViewBinder{

    public MyListAdapter(Context context, List<? extends Map<String, ?>> data, int resource, String[] keyNames, int[] fieldIds){
        super(context,data,resource,keyNames,fieldIds);


        setViewBinder(this);
    }


    public View getView(int position, View convertView, ViewGroup parent){
        View view;
        view = super.getView(position, convertView, parent);


        return view;
    }


    @Override
    public boolean setViewValue(View view, Object o, String s) {
        return false;
    }
}