package com.example.elm327can;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

public class DataAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private List<String> names=new Vector<>();//тут будут имена параметров
    private Map<String, Object> mapValues = new HashMap<>();//тут будут итемы для адаптера

    public void setItemValue(String name, Object value){
        setItemValue(name, value, true);
    }

    public void setItemValue(String name, Object value, boolean bUpdate){
        if(names.indexOf(name)<0) {
            names.add(name);
            //Collections.sort(names);//смысл сортировать? будем выводить в том порядке, в котором пришли итемы
        }
        mapValues.put(name,value);
        if (bUpdate) {
            notifyDataSetChanged();
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ViewHolder viewHolder = new ViewHolder(inflater.inflate(R.layout.sensordata_listitem, parent, false));
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ViewHolder viewHolder = (ViewHolder)holder;
        String name=names.get(position);
        viewHolder.setName(name);
        viewHolder.setValue(mapValues.get(name));
    }

    @Override
    public int getItemCount() {
        return mapValues.size();
    }

    private class ViewHolder extends RecyclerView.ViewHolder {
        private TextView textViewName;
        private TextView textViewValue;

        public ViewHolder(View itemView) {
            super(itemView);
            textViewName = itemView.findViewById(R.id.textViewName);
            textViewValue = itemView.findViewById(R.id.textViewValue);
        }

        public void setName(String name){
            textViewName.setText(name);
        }

        public void setValue(Object value){
            if (value!=null) {
                textViewValue.setVisibility(View.VISIBLE);
                textViewValue.setText(value.toString());
            }
            else {
                textViewValue.setVisibility(View.GONE);
                //textViewValue.setText("null!");
            }
        }
    }
}
