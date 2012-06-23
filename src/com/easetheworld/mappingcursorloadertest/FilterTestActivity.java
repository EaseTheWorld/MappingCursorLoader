package com.easetheworld.mappingcursorloadertest;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.Loader;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import dev.easetheworld.mappingcursorloader.MappingCursorLoader;

public class FilterTestActivity extends FragmentActivity implements BaseListFragment.CreateLoaderListener {
	
	private MappingCursorLoader mLoader;
	private EditText mEditText;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.filter);
        mEditText = (EditText)findViewById(android.R.id.edit);
        mEditText.addTextChangedListener(mTextWatcher);
    }
    
	@Override
	protected void onDestroy() {
		super.onDestroy();
        mEditText.removeTextChangedListener(mTextWatcher);
	}
    
    private Util.NameFilterLayer mNameFilterLayer = new Util.NameFilterLayer();
    
    private TextWatcher mTextWatcher = new TextWatcher() {

		@Override
		public void afterTextChanged(Editable s) { }

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {
			if (s.length() == 0)
				mLoader.removeLayer(mNameFilterLayer);
			else {
				mNameFilterLayer.setFilter(s);
				mLoader.addLayer(mNameFilterLayer, true);
			}
		}
    };

	@Override
	public void onLoaderCreated(Loader<Cursor> loader, int id, Bundle args) {
		mLoader = (MappingCursorLoader)loader;
		mLoader.setLayers(new Util.NameAlphabetSortLayer());
	}
}