/*
 * ******************************************************************************
 *   Copyright (c) 2013-2015 Gabriele Mariotti.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *  *****************************************************************************
 */

package it.gmariotti.changelibs.library.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.AsyncTask;
import android.os.Build;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.Toast;

import it.gmariotti.changelibs.R;
import it.gmariotti.changelibs.library.Constants;
import it.gmariotti.changelibs.library.Util;
import it.gmariotti.changelibs.library.internal.ChangeLog;
import it.gmariotti.changelibs.library.internal.ChangeLogRecyclerViewAdapter;
import it.gmariotti.changelibs.library.parser.XmlParser;

/**
 * RecyclerView for ChangeLog
 *
 * @author Gabriele Mariotti (gabri.mariotti@gmail.com)
 */
public class ChangeLogRecyclerView extends RecyclerView {

    //--------------------------------------------------------------------------
    // Custom Attrs
    //--------------------------------------------------------------------------
    protected int mRowLayoutId= Constants.mRowLayoutId;
    protected int mRowHeaderLayoutId=Constants.mRowHeaderLayoutId;
    protected int mChangeLogFileResourceId=Constants.mChangeLogFileResourceId;
    protected String mChangeLogFileResourceUrl=null;

    //--------------------------------------------------------------------------
    protected static String TAG="ChangeLogRecyclerView";

    // Adapter
    protected ChangeLogRecyclerViewAdapter mAdapter;

    //--------------------------------------------------------------------------
    // Constructors
    //--------------------------------------------------------------------------

    public ChangeLogRecyclerView(Context context) {
        this(context, null);
    }

    public ChangeLogRecyclerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ChangeLogRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    //--------------------------------------------------------------------------
    // Init
    //--------------------------------------------------------------------------

    /**
     * Initialize
     *
     * @param attrs
     * @param defStyle
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    protected void init(AttributeSet attrs, int defStyle){

        //Init attrs
        initAttrs(attrs, defStyle);

        //Init adapter
        initAdapter();

        //Init the LayoutManager
        initLayoutManager();

    }

    /**
     * Init the Layout Manager
     */
    protected void initLayoutManager() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        this.setLayoutManager(layoutManager);
    }

    /**
     * Init custom attrs.
     *
     * @param attrs
     * @param defStyle
     */
    protected void initAttrs(AttributeSet attrs, int defStyle) {
        TypedArray a = getContext().getTheme().obtainStyledAttributes(
                attrs, R.styleable.ChangeLogListView, defStyle, defStyle);

        try {
            //Layout for rows and header
            mRowLayoutId = a.getResourceId(R.styleable.ChangeLogListView_rowLayoutId, mRowLayoutId);
            mRowHeaderLayoutId = a.getResourceId(R.styleable.ChangeLogListView_rowHeaderLayoutId, mRowHeaderLayoutId);

            //Changelog.xml file
            mChangeLogFileResourceId = a.getResourceId(R.styleable.ChangeLogListView_changeLogFileResourceId,mChangeLogFileResourceId);

            mChangeLogFileResourceUrl = a.getString(R.styleable.ChangeLogListView_changeLogFileResourceUrl);
            //String which is used in header row for Version
            //mStringVersionHeader= a.getResourceId(R.styleable.ChangeLogListView_StringVersionHeader,mStringVersionHeader);

        } finally {
            a.recycle();
        }
    }


    /**
     * Init adapter
     */
    protected void initAdapter() {

        try{
            //Read and parse changelog.xml
            XmlParser parse;
            if (mChangeLogFileResourceUrl!=null)
                parse = new XmlParser(getContext(),mChangeLogFileResourceUrl);
            else
                parse = new XmlParser(getContext(),mChangeLogFileResourceId);
            //ChangeLog chg=parse.readChangeLogFile();
            ChangeLog chg = new ChangeLog();

            //Create adapter and set custom attrs
            mAdapter = new ChangeLogRecyclerViewAdapter(getContext(),chg.getRows());
            mAdapter.setRowLayoutId(mRowLayoutId);
            mAdapter.setRowHeaderLayoutId(mRowHeaderLayoutId);

            //Parse in a separate Thread to avoid UI block with large files
            if (mChangeLogFileResourceUrl==null || (mChangeLogFileResourceUrl!=null && Util.isConnected(getContext()))) {
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                    new ParseAsyncTask(mAdapter, parse).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                else
                    new ParseAsyncTask(mAdapter, parse).execute();
            }
            else
                Toast.makeText(getContext(), R.string.changelog_internal_error_internet_connection, Toast.LENGTH_LONG).show();
            setAdapter(mAdapter);

        }catch (Exception e){
            Log.e(TAG, getResources().getString(R.string.changelog_internal_error_parsing), e);
        }

    }


    /**
     * Async Task to parse xml file in a separate thread
     *
     */
    protected class ParseAsyncTask extends AsyncTask<Void, Void, ChangeLog> {

        private ChangeLogRecyclerViewAdapter mAdapter;
        private XmlParser mParse;

        public ParseAsyncTask(ChangeLogRecyclerViewAdapter adapter,XmlParser parse){
            mAdapter=adapter;
            mParse= parse;
        }

        @Override
        protected ChangeLog doInBackground(Void... params) {

            try{
                if (mParse!=null){
                    ChangeLog chg=mParse.readChangeLogFile();
                    return chg;
                }
            }catch (Exception e){
                Log.e(TAG,getResources().getString(R.string.changelog_internal_error_parsing),e);
            }
            return null;
        }

        protected void onPostExecute(ChangeLog chg) {

            //Notify data changed
            if (chg!=null){
                mAdapter.add(chg.getRows());                            }
        }
    }


}
