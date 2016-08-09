package com.wubydax.dbeditor;


import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;

import com.stericson.RootShell.exceptions.RootDeniedException;
import com.stericson.RootShell.execution.Command;
import com.stericson.RootTools.RootTools;

import java.io.IOException;
import java.util.concurrent.TimeoutException;


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
public class CheckSu extends AsyncTask<String, Integer, Boolean> {
    private ProgressDialog mProgressDialog;
    private Context mContext;
    private String mTableName;
    private OnExecutedListener mListener;

    public CheckSu(Context context, String tableName) {
        mContext = context;
        mTableName = tableName;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        mProgressDialog = ProgressDialog.show(mContext, mContext.getResources().getString(R.string.gaining_root), mContext.getResources().getString(R.string.please_wait), true, false);
    }

    @Override
    protected Boolean doInBackground(String... params) {

        if (RootTools.isAccessGiven()) {
            Command mount = new Command(0, "mount -o remount,rw /system");
            Command copy = new Command(0, "cp -f /data/system/users/0/settings_" + mTableName +".xml /system/etc/current_db_" + mTableName +".xml");
            Command perm = new Command(0, "chmod 0644 /system/etc/current_db_" + mTableName + ".xml");
            Command unMount = new Command(0, "mount -o remount,ro /system");

            try {
                RootTools.getShell(true).add(mount);
                RootTools.getShell(true).add(copy);
                RootTools.getShell(true).add(perm);
                RootTools.getShell(true).add(unMount);
            } catch (IOException | TimeoutException | RootDeniedException e) {
                e.printStackTrace();
            }
            return true;
        } else {
            return false;
        }
    }


    public void setOnExecutedListener(OnExecutedListener listener) {
        mListener = listener;
    }

    @Override
    protected void onPostExecute(Boolean result) {


        mProgressDialog.dismiss();

        if (!result) {
            //If no su access detected throw and alert dialog with single button that will finish the activity
            AlertDialog.Builder mNoSuBuilder = new AlertDialog.Builder(mContext);
            mNoSuBuilder.setTitle(R.string.missing_su_title);
            mNoSuBuilder.setMessage(R.string.missing_su);
            mNoSuBuilder.setNegativeButton(R.string.exit, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                }
            });
            mNoSuBuilder.create();
            Dialog mNoSu = mNoSuBuilder.create();
            mNoSu.show();
        } else {

            if (mListener != null) {
                mListener.onExecuted();
            }
        }

    }


    public interface OnExecutedListener {
        void onExecuted();
    }

}