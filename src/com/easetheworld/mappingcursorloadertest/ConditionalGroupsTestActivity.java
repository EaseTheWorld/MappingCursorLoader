package com.easetheworld.mappingcursorloadertest;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.Loader;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import com.easetheworld.mappingcursorloadertest.CheeseProvider.CheeseTable;

import dev.easetheworld.mappingcursorloader.MappingCursorLoader;
import dev.easetheworld.mappingcursorloader.ConditionalGroupsLayer;

public class ConditionalGroupsTestActivity extends FragmentActivity implements BaseListFragment.CreateLoaderListener {
	
	private MappingCursorLoader mLoader;
	
	private CheckBox mCheckYellow;
	private CheckBox mCheckBig;
	
	private static final String KEY_YELLOW = "Yellow";
	private static final String KEY_STAR = "Star";
	private static final String KEY_DEFAULT = "All";
	
	private static final int ORDER_YELLOW = 0;
	private static final int ORDER_STAR = 1;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.conditional);
        
        mCheckYellow = (CheckBox)findViewById(R.id.flagYellow);
        mCheckBig = (CheckBox)findViewById(R.id.flagStar);
        
        mCheckYellow.setChecked(true);
        mCheckBig.setChecked(true);
        
        mLayer.addGroup(KEY_YELLOW, mConditionYellow, ORDER_YELLOW);
        mLayer.addGroup(KEY_STAR, mConditionStar, ORDER_STAR);
        mLayer.setDefaultGroup(KEY_DEFAULT);
        
        mCheckYellow.setOnCheckedChangeListener(mChangeListener);
        mCheckBig.setOnCheckedChangeListener(mChangeListener);
    }
    
    private ConditionalGroupsLayer mLayer = new ConditionalGroupsLayer();
    private ConditionalGroupsLayer.Condition mConditionYellow = new ConditionalGroupsLayer.Condition() {
		@Override
		public boolean isSatisfied(Cursor cursor) {
			if (cursor.getInt(CheeseTable.COLUMN_INDEX_FLAG1) > 0)
				return true;
			else
				return false;
		}
	};
    private ConditionalGroupsLayer.Condition mConditionStar = new ConditionalGroupsLayer.Condition() {
		@Override
		public boolean isSatisfied(Cursor cursor) {
			if (cursor.getInt(CheeseTable.COLUMN_INDEX_FLAG2) > 0)
				return true;
			else
				return false;
		}
	};
    
    private CheckBox.OnCheckedChangeListener mChangeListener = new CheckBox.OnCheckedChangeListener() {
		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			String key = null;
			ConditionalGroupsLayer.Condition condition = null;
			int order = 0;
			switch(buttonView.getId()) {
			case R.id.flagYellow:
				key = KEY_YELLOW;
				condition = mConditionYellow;
				order = ORDER_YELLOW;
				break;
			case R.id.flagStar:
				key = KEY_STAR;
				condition = mConditionStar;
				order = ORDER_STAR;
				break;
			}
			if (isChecked)
		        mLayer.addGroup(key, condition, order);
			else
		        mLayer.removeGroup(key);
			mLoader.addLayer(mLayer, true);
		}
    };

	@Override
	public void onLoaderCreated(Loader<Cursor> loader, int id, Bundle args) {
		mLoader = (MappingCursorLoader)loader;
		mLoader.setLayers(new Util.NameAlphabetSortLayer(), mLayer);
	}
}