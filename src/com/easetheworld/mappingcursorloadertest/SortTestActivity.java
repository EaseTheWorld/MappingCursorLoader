package com.easetheworld.mappingcursorloadertest;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.Loader;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.easetheworld.mappingcursorloadertest.Util.ReverseLayer;

import dev.easetheworld.mappingcursorloader.MappingCursorLoader;
import dev.easetheworld.mappingcursorloader.MappingCursorLoader.Layer;

public class SortTestActivity extends FragmentActivity implements BaseListFragment.CreateLoaderListener {
	
	private MappingCursorLoader mLoader;
	private ViewGroup mButtons;
	
	private Layer[] mLayerArray = new Layer[] {
		new Util.NameAlphabetSortLayer(),
		new Util.NameLengthSortLayer(),
		new ReverseLayer()
	};
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sort);
        
        mButtons = (ViewGroup)findViewById(R.id.buttons);
        for (Layer r : mLayerArray) {
        	Button btn = new Button(this);
        	btn.setTextSize(12);
        	btn.setText(r.toString());
        	btn.setTag(r);
        	btn.setOnClickListener(mAddClickListener);
        	mButtons.addView(btn);
        }
    }
    
    public void clickHandler(View v) {
    	mLoader.clearLayers();
    	for (int i=0; i<mLayerArray.length; i++) {
        	View btn = mButtons.getChildAt(i);
        	btn.setEnabled(true);
        }
    }
    
    private View.OnClickListener mAddClickListener = new View.OnClickListener() {
		
		@Override
		public void onClick(View v) {
			Layer layer = (Layer)v.getTag();
			
	    	mLoader.addLayer(layer, false);
	    	int newIndex = 0;
	    	for (int i=0; i<mButtons.getChildCount(); i++) {
	    		if (mButtons.getChildAt(i).isEnabled()) {
	    			newIndex = i;
	    			break;
	    		}
	    	}
	    	View btn = mButtons.findViewWithTag(layer);
	    	btn.setEnabled(false);
	    	mButtons.removeView(btn);
	    	mButtons.addView(btn, newIndex);
		}
	};
    
    private Layer getLayerByName(String name) {
    	for (Layer r : mLayerArray) {
    		if (name.equals(r.toString()))
    			return r;
    	}
    	return null;
    }

	@Override
	public void onLoaderCreated(Loader<Cursor> loader, int id, Bundle args) {
		mLoader = (MappingCursorLoader)loader;
	}
}