package com.wubydax.dbeditor;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/*      Created by Roberto Mariani and Anna Berkovitch, 26/03/16
        This program is free software: you can redistribute it and/or modify
        it under the terms of the GNU General Public License as published by
        the Free Software Foundation, either version 3 of the License, or
        (at your option) any later version.

        This program is distributed in the hope that it will be useful,
        but WITHOUT ANY WARRANTY; without even the implied warranty of
        MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
        GNU General Public License for more details.

        You should have received a copy of the GNU General Public License
        along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

public class TableValuesFragment extends Fragment implements CheckSu.OnExecutedListener {
    public static final String TABLE_NAME = "table_name";
    private RecyclerView mRecyclerView;
    private String mTableName;
    private List<TableItems> mList;
    EditText search;

    public TableValuesFragment() {
    }

    public static TableValuesFragment newInstance(String tableName) {
        Bundle extras = new Bundle();
        extras.putString(TABLE_NAME, tableName);
        TableValuesFragment tableValuesFragment = new TableValuesFragment();
        tableValuesFragment.setArguments(extras);
        return tableValuesFragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mList = new ArrayList<>();
        mTableName = getArguments().getString(TABLE_NAME);
        View rootView = inflater.inflate(R.layout.fragment_table_values, container, false);
        search = (EditText) rootView.findViewById(R.id.searchKey);
        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.recyclerView);
        CheckSu checkIt = new CheckSu(getActivity(), mTableName);
        checkIt.setOnExecutedListener(this);
        checkIt.execute();
        return rootView;
    }

    @Override
    public void onExecuted() {
        String key, value;
        BufferedReader bufferedReader;
        try {
            File file = new File("/system/etc/current_db_" + mTableName + ".xml");
            bufferedReader = new BufferedReader(new FileReader(file));

            for (String line = bufferedReader.readLine(); line != null; line = bufferedReader.readLine()) {
                try {

                    if (line.contains("id=")) {
                        if (line.contains("value=")) {
                            key = line.substring(line.indexOf("name=") + 6, line.indexOf("value=") - 2);
                            value = line.substring(line.indexOf("value=") + 7, line.indexOf("package=") - 2);
                        } else {
                            key = line.substring(line.indexOf("name=") + 6, line.indexOf("package=") - 2);
                            value = "";
                        }
                        TableItems tableItems = new TableItems();
                        tableItems.key = key;
                        tableItems.value = value;
                        mList.add(tableItems);

                    }
                } catch (Exception e) {
                    //Catch exception if any
                    System.err.println("Error: " + e.getMessage());
                }
            }
            bufferedReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        final TableValuesAdapter tableValuesAdapter = new TableValuesAdapter();
        mRecyclerView.setAdapter(tableValuesAdapter);
        search.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tableValuesAdapter.getFilter().filter(s);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });


    }

    private void showDialog(final String key, final String value, final int position) {
        @SuppressLint("InflateParams") View view = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_layout, null, false);
        TextView keyText = (TextView) view.findViewById(R.id.textKey);
        final EditText editText = (EditText) view.findViewById(R.id.valueEditText);
        final ScrollView scroll = (ScrollView) view.findViewById(R.id.ScrollView1);
        if(value.matches("\\d+(?:\\.\\d+)?"))
        {
            editText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            editText.setSingleLine(true);
        }
        else
        {
            editText.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            editText.setHint(getResources().getString(R.string.enter_string));
        }
        keyText.setText(key);
        editText.setText(value);
        editText.setSelection(editText.getText().length());
        scroll.post(new Runnable() {
            @Override
            public void run() {
                scroll.fullScroll(View.FOCUS_DOWN);
            }
        });
        new AlertDialog.Builder(getActivity())
                .setTitle(getResources().getString(R.string.change_value))
                .setView(view)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String newValue = editText.getText().toString();
                        boolean isGranted = Settings.System.canWrite(getActivity());
                        if (!isGranted) {
                            Intent grantPermission = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                            startActivity(grantPermission);
                        } else {
                            switch(mTableName){
                                case "system":
                                    Settings.System.putString(getActivity().getContentResolver(), key, newValue);
                                    break;
                                case "global":
                                    Settings.Global.putString(getActivity().getContentResolver(), key, newValue);
                                    break;
                                case "secure":
                                    Settings.Secure.putString(getActivity().getContentResolver(), key, newValue);
                                    break;
                            }
                            mList.get(position).value = newValue;
                        }
                        mRecyclerView.getAdapter().notifyDataSetChanged();
                    }
                }).show();
    }



    public class TableValuesAdapter extends RecyclerView.Adapter<TableValuesAdapter.ViewHolder> implements Filterable{
        private List<TableItems>originalList;

        public TableValuesAdapter() {
            originalList = mList;
        }

        @Override
        public TableValuesAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.row, parent, false);

            return new ViewHolder(v);
        }


        @Override
        public void onBindViewHolder(TableValuesAdapter.ViewHolder holder, @SuppressLint("RecyclerView") int position) {
            holder.keyTextView.setText(mList.get(position).key);
            holder.valueTextView.setText(mList.get(position).value);
            holder.mPosition = position;


        }

        @Override
        public int getItemCount() {
            return mList != null ? mList.size() : 0;
        }

        public Filter getFilter() {
            final List<TableItems> list = new ArrayList<>();

            return new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    for(TableItems tableItems : originalList) {
                        if(tableItems.key.toLowerCase().contains(constraint.toString().toLowerCase())) {
                            list.add(tableItems);
                        }
                    }
                    FilterResults filterResults = new FilterResults();
                    filterResults.count = list.size();
                    filterResults.values = list;
                    return filterResults;
                }

                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    mList = (List<TableItems>) results.values;
                    notifyDataSetChanged();
                }
            };
        }


        class ViewHolder extends RecyclerView.ViewHolder {
            TextView keyTextView;
            TextView valueTextView;
            int mPosition;

            public ViewHolder(View itemView) {
                super(itemView);
                keyTextView = (TextView) itemView.findViewById(R.id.keyTextView);
                valueTextView = (TextView) itemView.findViewById(R.id.valueTextView);
                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final String key = mList.get(mPosition).key;
                        final String value = mList.get(mPosition).value;
                        showDialog(key, value, mPosition);

                    }
                });
            }
        }
    }

    public class TableItems {
        String key;
        String value;
    }
}