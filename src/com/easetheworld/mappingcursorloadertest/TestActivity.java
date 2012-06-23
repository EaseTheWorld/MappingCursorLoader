package com.easetheworld.mappingcursorloadertest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.AsyncQueryHandler;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.easetheworld.mappingcursorloadertest.CheeseProvider.CheeseTable;

public class TestActivity extends ListActivity {
	
	private static final String CATEGORY_SAMPLE_CODE = "com.easetheworld.category.SAMPLE_CODE";
	private ProgressDialog mProgress;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // footer for reset data
        TextView footer = (TextView)LayoutInflater.from(this).inflate(android.R.layout.simple_list_item_1, null);
        footer.setText("Reset Data");
        getListView().addFooterView(footer);
        
        mProgress = new ProgressDialog(this);
        mProgress.setTitle("Test Data Loading...");
        mProgress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        
        setListAdapter(new SimpleAdapter(this, getData(this, CATEGORY_SAMPLE_CODE),
                android.R.layout.simple_list_item_1, new String[] { "title" },
                new int[] { android.R.id.text1 }));
        
		// check if test data is available
        AsyncQueryHandler queryHandler = new AsyncQueryHandler(getContentResolver()) {
			@Override
			protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
				if (cursor.getCount() == 0)
					resetData();
			}
		};
		queryHandler.startQuery(0, null, CheeseTable.CONTENT_URI, null, null, null, null);
	}
    
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
    	if (id < 0) { // footer
    		resetData();
    		return;
    	}
        Map map = (Map) l.getItemAtPosition(position);

        Intent intent = (Intent) map.get("intent");
        startActivity(intent);
    }
    
    private void resetData() {
		new InsertRandomCheeseDataTask().execute();
    }
    
	private class InsertRandomCheeseDataTask extends AsyncTask<Void, Integer, Void> {
		@Override
		protected void onPreExecute() {
	        mProgress.setMax(Cheeses.sCheeseStrings.length);
    		mProgress.show();
		}

		@Override
		protected Void doInBackground(Void... params) {
			getContentResolver().delete(CheeseTable.CONTENT_URI, null, null);
			List<String> l = Arrays.asList(Cheeses.sCheeseStrings);
			Collections.shuffle(l);
			for (int i=0; i<Cheeses.sCheeseStrings.length; i++) {
				ContentValues cv = new ContentValues();
				cv.put(CheeseTable.NAME, l.get(i));
				getContentResolver().insert(CheeseTable.CONTENT_URI, cv);
				if (i % 10 == 0)
					publishProgress(i);
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
    		mProgress.dismiss();
		}

		@Override
		protected void onProgressUpdate(Integer... values) {
			mProgress.setProgress(values[0]);
		}
		
		
	}
    
    // from ApiDemos.java
    private static List<Map<String, Object>> getData(Context context, String category) {
        List<Map<String, Object>> myData = new ArrayList<Map<String, Object>>();

        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(CATEGORY_SAMPLE_CODE);

        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> list = pm.queryIntentActivities(mainIntent, 0);

        if (list == null)
            return myData;
        
        for (ResolveInfo info : list) {
            CharSequence labelSeq = info.loadLabel(pm);
            String label = labelSeq != null
                    ? labelSeq.toString()
                    : info.activityInfo.name;
            Intent i = new Intent().setClassName(info.activityInfo.applicationInfo.packageName, info.activityInfo.name);
            
	        Map<String, Object> temp = new HashMap<String, Object>();
	        temp.put("title", label);
	        temp.put("intent", i);
	        myData.add(temp);
        }
        
        return myData;
    }
}