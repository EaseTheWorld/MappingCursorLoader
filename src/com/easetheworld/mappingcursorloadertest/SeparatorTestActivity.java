package com.easetheworld.mappingcursorloadertest;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.Loader;
import dev.easetheworld.mappingcursorloader.MappingCursorLoader;

public class SeparatorTestActivity extends FragmentActivity implements BaseListFragment.CreateLoaderListener {
	
	private MappingCursorLoader mLoader;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.separator);
    }

	@Override
	public void onLoaderCreated(Loader<Cursor> loader, int id, Bundle args) {
		mLoader = (MappingCursorLoader)loader;
		mLoader.setLayers(new Util.NameAlphabetSortLayer(), new Util.NameSeparatorLayer());
	}
}