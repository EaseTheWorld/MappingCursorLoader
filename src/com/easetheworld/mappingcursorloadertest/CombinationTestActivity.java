package com.easetheworld.mappingcursorloadertest;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.Loader;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.Toast;

import com.easetheworld.mappingcursorloadertest.CheeseProvider.CheeseTable;

import dev.easetheworld.mappingcursorloader.ConditionalGroupsLayer;
import dev.easetheworld.mappingcursorloader.MappingCursorLoader;

public class CombinationTestActivity extends FragmentActivity implements BaseListFragment.CreateLoaderListener {
	
	private MappingCursorLoader mLoader;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.combination);
        
        EditText editText = (EditText)findViewById(android.R.id.edit);
        editText.addTextChangedListener(mTextWatcher);
        
        mConditionalGroupsLayer.addGroup("Yellow", new ConditionalGroupsLayer.Condition() {
        	@Override
        	public boolean isSatisfied(Cursor cursor) {
        		if (cursor.getInt(CheeseTable.COLUMN_INDEX_FLAG1) > 0)
        			return true;
        		else
        			return false;
        	}
        });
        mConditionalGroupsLayer.addGroup("Star", new ConditionalGroupsLayer.Condition() {
        	@Override
        	public boolean isSatisfied(Cursor cursor) {
        		if (cursor.getInt(CheeseTable.COLUMN_INDEX_FLAG2) > 0)
        			return true;
        		else
        			return false;
        	}
        });
    }
    
    private Util.NameAlphabetSortLayer mNameSortLayer = new Util.NameAlphabetSortLayer();
    
    private Util.NameSeparatorLayer mNameSeparatorLayer = new Util.NameSeparatorLayer();
    
    private ConditionalGroupsLayer mConditionalGroupsLayer = new ConditionalGroupsLayer();
    
    private Util.NameFilterLayer mNameFilterLayer = new Util.NameFilterWithHeaderLayer();
    
    private TextWatcher mTextWatcher = new TextWatcher() {

		@Override
		public void afterTextChanged(Editable s) { }

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {
			if (s.length() == 0)
				changeLayer(false);
			else {
				mNameFilterLayer.setFilter(s);
				changeLayer(true);
			}
		}
    };
    
    private void changeLayer(boolean isSearch) {
    	if (isSearch) {
			mLoader.setLayers(mNameSortLayer, mNameFilterLayer);
			Toast.makeText(this, "Sort + Filter", Toast.LENGTH_SHORT).show();
    	} else {
			mLoader.setLayers(mNameSortLayer, mNameSeparatorLayer, mConditionalGroupsLayer);
			Toast.makeText(this, "Sort + Separator + Conditional Groups", Toast.LENGTH_SHORT).show();
    	}
    }

	@Override
	public void onLoaderCreated(Loader<Cursor> loader, int id, Bundle args) {
		mLoader = (MappingCursorLoader)loader;
		changeLayer(false);
	}
}