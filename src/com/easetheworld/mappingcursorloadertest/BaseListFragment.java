package com.easetheworld.mappingcursorloadertest;

import android.content.AsyncQueryHandler;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SectionIndexer;
import android.widget.TextView;
import android.widget.Toast;

import com.easetheworld.mappingcursorloadertest.CheeseProvider.CheeseTable;

import dev.easetheworld.mappingcursorloader.MappingCursorLoader;

public class BaseListFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {
	
	private MappingCursorLoader mLoader;
	private SimpleCursorAdapter mAdapter;
	private AsyncQueryHandler mQueryHandler;
	private static final boolean USE_SECTION_INDEXER = Build.VERSION.SDK_INT > Build.VERSION_CODES.GINGERBREAD_MR1; // Until gingerbread, FastScroller overlay shows at wrong position
	
	@Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        
        mAdapter = new MyAdapter(getActivity(),
                android.R.layout.simple_list_item_1, null,
                new String[] { CheeseTable.NAME },
                new int[] { android.R.id.text1 },
                0); 
        setListAdapter(mAdapter);
        
        getLoaderManager().initLoader(0, null, this);
        
        registerForContextMenu(getListView());
        
		mQueryHandler = new AsyncQueryHandler(getActivity().getContentResolver()) {
			@Override
			protected void onUpdateComplete(int token, Object cookie, int result) {
				// TODO Auto-generated method stub
				super.onUpdateComplete(token, cookie, result);
			}
		};
    }
	
	@Override
    public void onListItemClick(ListView l, View v, int position, long id) {
		Cursor c = (Cursor)l.getItemAtPosition(position);
		int rowType = MappingCursorLoader.getRowType(c);
		if (rowType == MappingCursorLoader.ROW_TYPE_CHILD) {
			String name = c.getString(CheeseTable.COLUMN_INDEX_NAME);
			Toast.makeText(v.getContext(), name+", id="+id+", pos="+position, Toast.LENGTH_SHORT).show();
		} else {
			String key = c.getString(MappingCursorLoader.GROUP_CURSOR_COLUMN_KEY);
			int size = c.getInt(MappingCursorLoader.GROUP_CURSOR_COLUMN_SIZE);
			Toast.makeText(v.getContext(), "Group "+key+" size="+size, Toast.LENGTH_SHORT).show();
			// toggle collapse/expand
			if (rowType == MappingCursorLoader.ROW_TYPE_GROUP_EXPANDED)
				mLoader.collapseGroup(key);
			else
				mLoader.expandGroup(key);
		}
    }
	
    private static final int MENU_DELETE = 0;
    private static final int MENU_SET_FLAG1 = 1;
    private static final int MENU_UNSET_FLAG1 = 2;
    private static final int MENU_SET_FLAG2 = 3;
    private static final int MENU_UNSET_FLAG2 = 4;

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
    	AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;
    	Cursor c = (Cursor)getListView().getItemAtPosition(info.position);
    	if (MappingCursorLoader.getRowType(c) == MappingCursorLoader.ROW_TYPE_CHILD) { // child
    		String name = c.getString(CheeseTable.COLUMN_INDEX_NAME);
    		menu.setHeaderTitle(name);
    		menu.add(0, MENU_DELETE, 0, "Delete");
    		if (c.getInt(CheeseTable.COLUMN_INDEX_FLAG1) == 1)
    			menu.add(0, MENU_UNSET_FLAG1, 0, "Unset Yellow");
    		else
    			menu.add(0, MENU_SET_FLAG1, 0, "Set Yellow");
    		if (c.getInt(CheeseTable.COLUMN_INDEX_FLAG2) == 1)
    			menu.add(0, MENU_UNSET_FLAG2, 0, "Unset Star");
    		else
    			menu.add(0, MENU_SET_FLAG2, 0, "Set Star");
    	} else { // group
    		
    	}
	}
	
    @Override
	public boolean onContextItemSelected(MenuItem item) {
    	AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
    	if (item.getItemId() == MENU_DELETE) {
	    	mQueryHandler.startDelete(0, null, CheeseTable.CONTENT_URI, CheeseTable.ID+"="+info.id, null);
    	} else {
    		ContentValues cv = new ContentValues();
    		String column = null;
    		int value = 0;
    		switch(item.getItemId()) {
	    	case MENU_SET_FLAG1:
	    		column = CheeseTable.FLAG1;
	    		value = 1;
	    		break;
	    	case MENU_UNSET_FLAG1:
	    		column = CheeseTable.FLAG1;
	    		value = 0;
	    		break;
	    	case MENU_SET_FLAG2:
	    		column = CheeseTable.FLAG2;
	    		value = 1;
	    		break;
	    	case MENU_UNSET_FLAG2:
	    		column = CheeseTable.FLAG2;
	    		value = 0;
	    		break;
    		}
    		cv.put(column, value);
	    	mQueryHandler.startUpdate(0, null, CheeseTable.CONTENT_URI, cv, CheeseTable.ID+"="+info.id, null);
    	}
		return super.onContextItemSelected(item);
	}
	
	public static interface CreateLoaderListener {
		public void onLoaderCreated(Loader<Cursor> loader, int id, Bundle args);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		// Now create and return a CursorLoader that will take care of
        // creating a Cursor for the data being displayed.
		MappingCursorLoader loader = new MappingCursorLoader(getActivity(), CheeseTable.CONTENT_URI, null, null, null, null, mAdapter);
		if (getActivity() instanceof CreateLoaderListener) {
			((CreateLoaderListener)getActivity()).onLoaderCreated(loader, id, args);
		}
		mLoader = loader;
		return loader;
	}
	
	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		// Swap the new cursor in.  (The framework will take care of closing the
        // old cursor once we return.)
		mAdapter.swapCursor(data);
        getListView().setFastScrollEnabled(true); // before onLoadFinished, SectionIndexer.getSections() returns null
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		// This is called when the last Cursor provided to onLoadFinished()
        // above is about to be closed.  We need to make sure we are no
        // longer using it.
		mAdapter.swapCursor(null);
	}
	
	private static class MyAdapter extends SimpleCursorAdapter implements SectionIndexer {

		public MyAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags) {
			super(context, layout, c, from, to, flags);
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			if (getItemViewType(cursor.getPosition()) == GROUP_VIEW_TYPE) {
				return LayoutInflater.from(context).inflate(android.R.layout.preference_category, null);
			} else
				return super.newView(context, cursor, parent);
		}

		@Override
		public void bindView(View convertView, Context context, Cursor cursor) {
			if (getItemViewType(cursor.getPosition()) == GROUP_VIEW_TYPE) {
				TextView tv = (TextView)convertView;
				String key = cursor.getString(MappingCursorLoader.GROUP_CURSOR_COLUMN_KEY);
				int size = cursor.getInt(MappingCursorLoader.GROUP_CURSOR_COLUMN_SIZE);
				boolean isExpanded = MappingCursorLoader.getRowType(cursor) == MappingCursorLoader.ROW_TYPE_GROUP_EXPANDED;
				tv.setText((isExpanded ? "¡å" : "¢º") + " "+key+" ("+size+")");
			} else {
				super.bindView(convertView, context, cursor);
				TextView tv = (TextView)convertView.findViewById(android.R.id.text1);
				if (cursor.getInt(CheeseTable.COLUMN_INDEX_FLAG1) == 1)
					tv.setTextColor(Color.YELLOW);
				else
					tv.setTextColor(Color.WHITE);
				if (cursor.getInt(CheeseTable.COLUMN_INDEX_FLAG2) == 1)
					tv.setCompoundDrawablesWithIntrinsicBounds(0, 0, android.R.drawable.star_on, 0);
				else
					tv.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
			}
		}
		
		private static final int GROUP_VIEW_TYPE = 1;

		@Override
		public int getItemViewType(int position) {
			Cursor c = (Cursor) getItem(position);
			if (MappingCursorLoader.getRowType(c) != MappingCursorLoader.ROW_TYPE_CHILD)
				return GROUP_VIEW_TYPE; 
			else
				return 0;
		}

		@Override
		public int getViewTypeCount() {
			return super.getViewTypeCount() + 1; // 1 means GROUP_VIEW_TYPE
		}

		@Override
		public int getPositionForSection(int section) {
			if (USE_SECTION_INDEXER)
				return MappingCursorLoader.getGroupHeaderPosition((Cursor)getItem(0), section);
			else
				return 0;
		}

		@Override
		public int getSectionForPosition(int position) {
			return 0;
		}

		@Override
		public Object[] getSections() {
			if (USE_SECTION_INDEXER)
				return MappingCursorLoader.getGroupKeys((Cursor)getItem(0));
			else
				return null;
		}
	}
}