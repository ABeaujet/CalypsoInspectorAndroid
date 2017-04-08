package fr.mikado.calypsoinspectorandroid;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by alexis on 06/04/17.
 */
public class CalypsoDumpListAdapter extends BaseExpandableListAdapter {

    private Context context;
    private ArrayList<String> headers;
    private HashMap<String, ArrayList<String>> children;
    private static Typeface lucidaConsole;

    public CalypsoDumpListAdapter(Context context, ArrayList<String> headers, HashMap<String, ArrayList<String>> children){
        this.context = context;
        this.headers = headers;
        this.children = children;

        if(lucidaConsole == null){
            lucidaConsole = Typeface.createFromAsset(context.getAssets(), "lucida_console.ttf");
        }
    }

    public void addHeader(String headerName){
        this.headers.add(headerName);
        this.children.put(headerName, new ArrayList<String>());
    }

    public void addChildren(String headerName, ArrayList<String> children){
        ArrayList<String> cat = this.children.get(headerName);
        if(cat == null) {
            this.addHeader(headerName);
            cat = this.children.get(headerName);
        }
        cat.addAll(children);
    }

    public void clear(){
        this.headers = new ArrayList<>();
        this.children = new HashMap<>();
    }

    @Override
    public int getGroupCount() {
        return this.headers.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return this.children.get(this.headers.get(groupPosition)).size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return this.headers.get(groupPosition);
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return this.children.get(this.headers.get(groupPosition)).get(childPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        String headerTitle = (String)this.getGroup(groupPosition);
        if(convertView == null){
            LayoutInflater infalInflater = (LayoutInflater) this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = infalInflater.inflate(R.layout.list_group, null);
        }
        TextView labelListHeader = (TextView)convertView.findViewById(R.id.labelListHeader);
        labelListHeader.setText(headerTitle);
        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        String childText = (String)this.getChild(groupPosition, childPosition);
        if(convertView == null){
            LayoutInflater infalInflater = (LayoutInflater) this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = infalInflater.inflate(R.layout.list_item, null);
        }
        TextView labelListItem = (TextView)convertView.findViewById(R.id.labelListItem);
        labelListItem.setText(childText);
        labelListItem.setTypeface(lucidaConsole);
        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return false;
    }
}




















