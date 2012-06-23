/*
 * Copyright (C) 2012 EaseTheWorld
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * https://github.com/EaseTheWorld/MappingCursorLoader
 */

package dev.easetheworld.mappingcursorloader;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.Context;
import android.database.AbstractCursor;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.content.CursorLoader;
import android.util.Log;
import android.widget.BaseAdapter;

public class MappingCursorLoader extends CursorLoader {

	private static final String TAG = "MappingCursorLoader";
	private static final boolean DEBUG = true;
	
	private MappingCursor mCurrentCursor;
	private Layer[] mCurrentLayers; // remember this so it can be applied when data set changed.
	private boolean mIsLayerChanged = false; // to distinguish whether db is changed or layer is changed
	
	private BaseAdapter mAdapter;
	
	private boolean mIsLoading = false;
	
	public MappingCursorLoader(Context context, Uri uri,
			String[] projection, String selection, String[] selectionArgs,
			String sortOrder) {
		this(context, uri, projection, selection, selectionArgs, sortOrder, null);
	}
	
	// for collapse/expand group
	public MappingCursorLoader(Context context, Uri uri,
			String[] projection, String selection, String[] selectionArgs,
			String sortOrder, BaseAdapter adapter) {
		super(context, uri, projection, selection, selectionArgs, sortOrder);
		mAdapter = adapter;
	}
	
	@Override
	public Cursor loadInBackground() {
		MappingCursor currentCursor = mCurrentCursor;
		if (DEBUG) Log.v(TAG, "loadInBackground0");
		
		Cursor c;
		boolean isLayerChanged = mIsLayerChanged;
		mIsLayerChanged = false;
		if (isLayerChanged && currentCursor != null) { // original cursor is not changed. just layer changed
			c = currentCursor.getOriginalCursor(); // not db query, just layer
			if (DEBUG) Log.v(TAG, "loadInBackground1 layer changed current cursor="+currentCursor+", original="+c);
		} else {
			c = super.loadInBackground(); // db query
			if (DEBUG) Log.v(TAG, "loadInBackground1 db changed current cursor="+currentCursor+", original="+c);
		}
		
		// always make MappingCursor even if mCurrentLayers is null
		// because it could be closed in deliverResult.
		if (DEBUG) Log.v(TAG, "loadInBackground2 before mapping "+c);
		if (c != null)
			c = new MappingCursor(c, mCurrentLayers, isLayerChanged);
		if (DEBUG) Log.v(TAG, "loadInBackground3 after mapping "+c);
		
		return c;
	}
	
	@Override
	protected void onForceLoad() {
		super.onForceLoad();
		mIsLoading = true;
		if (DEBUG) Log.v(TAG, "onForceLoad");
	}

	@Override
	public void onCanceled(Cursor cursor) {
		MappingCursor newCursor = (MappingCursor)cursor;
		if (newCursor != null) {
			if (mCurrentCursor != null && mCurrentCursor.getOriginalCursor() == newCursor.getOriginalCursor()) { // do not close the original cursor because it is still used for mCurrentCursor
				newCursor.setKeepOriginalCursorOpened(true);
			} else { // old and new original cursor is different which means db is changed and old cursor is invalid.
				mCurrentCursor = null;
			}
		}
		super.onCanceled(cursor); // close the new cursor
		if (newCursor != null) {
			newCursor.setKeepOriginalCursorOpened(true);
		}
		mIsLoading = false;
		if (DEBUG) Log.v(TAG, "onCanceled");
	}

	@Override
	public void deliverResult(Cursor cursor) {
		mIsLoading = false;
		if (isReset()) {
			if (DEBUG) Log.v(TAG, "deliverResult - An async query came in while the loader is stopped.");
            if (cursor != null) {
                cursor.close();
            }
            return;
        }
		if (DEBUG) Log.v(TAG, "deliverResult - cursor="+cursor+", old="+mCurrentCursor);
		MappingCursor newCursor = (MappingCursor)cursor;
		newCursor.setGroupHeaderPositionMap(mCollapsedGroupKey);
		newCursor.finishLayerStatus();
		
		if (mCurrentCursor != null && newCursor != null && mCurrentCursor.getOriginalCursor() == newCursor.getOriginalCursor()) {
			if (DEBUG) Log.v(TAG, "deliverResult - Only layers are changed. Do not close the original cursor.");
			mCurrentCursor.setKeepOriginalCursorOpened(true);
		}
		super.deliverResult(cursor); // close the old cursor
		if (mCurrentCursor != null)
			mCurrentCursor.setKeepOriginalCursorOpened(false);
		
		mCurrentCursor = newCursor;
		if (DEBUG) Log.v(TAG, "deliverResult");
	}

	private void notifyDataSetChanged() {
		mAdapter.notifyDataSetChanged();
	}

	@Override
	protected void onReset() {
		super.onReset();
		mIsLayerChanged = false;
		mCurrentLayers = null;
		mCurrentCursor = null;
	}

	/**
	 * Clear all the layers and set the layers.
	 * 
	 * Should be called from main thread.
	 * If this is called during loading, current loading will be canceled and start load again.
	 * 
	 * @param layers
	 */
	public void setLayers(Layer...layers) {
		if (DEBUG) Log.v(TAG, "setLayers isStarted="+isStarted()+", mIsLoading="+mIsLoading+", "+layers);
		mCurrentLayers = layers;
		mIsLayerChanged = true;
//		mCollapsedGroupKey.clear(); // after layer is changed, expand all.
		if (isStarted())
			forceLoad();
	}
	
	/**
	 * Clear all the layers.
	 * 
	 * Should be called from main thread.
	 * If this is called during loading, current loading will be canceled and start load again.
	 */
	public void clearLayers() {
		setLayers((Layer[])null);
	}
	
	/**
	 * Add the layer to the layer stack.
	 * If this is already added, do nothing. (unless isLayerChanged is true)
	 * 
	 * Should be called from main thread.
	 * If this is called during loading, current loading will be canceled and start load again.
	 * 
	 * If layer is changed (ex. FilterLayer's filter changed.), you must call addLayer(layer, true);
	 * so current loading can be canceled.
	 * 
	 * @param layer cannot be null
	 * @param isLayerChanged If true, clear the previous result and calculate again. Otherwise use the previous result.
	 */
	public void addLayer(Layer layer, boolean isLayerChanged) {
		if (layer == null)
			throw new IllegalArgumentException("Layer cannot be null.");
		if (DEBUG) Log.v(TAG, "addLayer "+layer+" to "+mCurrentLayers);
		Layer[] newLayers = mCurrentLayers;
		if (newLayers == null) { // this is the first layer
			newLayers = new Layer[] {layer};
		} else { // add this layer at the end of the current layers
			int index = findLayer(layer);
			if (DEBUG) Log.v(TAG, "addLayer index="+index+", layer="+layer);
			if (index < 0) { // make new array
				newLayers = compatArraysCopyOf(mCurrentLayers, mCurrentLayers.length+1);
				newLayers[newLayers.length-1] = layer; 
			}
		}
		if (isLayerChanged)
			layer.reset();
		setLayers(newLayers);
	}
	
	/**
	 * Remove the layer and above it.
	 * If this is already not found, do nothing.
	 * 
	 * Should be called from main thread.
	 * If this is called during loading, current loading will be canceled and start load again.
	 * 
	 * @param layer
	 */
	public void removeLayer(Layer layer) {
		int index = findLayer(layer);
		if (DEBUG) Log.v(TAG, "removeLayer index="+index+" from "+mCurrentLayers);
		if (index == -1) // this layer is not found. no change.
			return;
		Layer[] newLayers = null;
		if (index == 0) { // this is the first layer. so remove all.
			newLayers = null;
		} else { // make new array 0 ~ index
			newLayers = compatArraysCopyOf(mCurrentLayers, index);
		}
		setLayers(newLayers);
	}
	
	// return index or -1 if not found
	private int findLayer(Layer layer) {
		for (int i=0; i<mCurrentLayers.length; i++) {
			if (mCurrentLayers[i] == layer)
				return i;
		}
		return -1;
	}
	
	public static final int ROW_TYPE_CHILD = 0;
	public static final int ROW_TYPE_GROUP_EXPANDED = 1;
	public static final int ROW_TYPE_GROUP_COLLAPSED = 2;
	
	/**
	 * @param cursor
	 * @return current row's type (Child, Expanded group, Collapsed group)
	 */
	public static int getRowType(Cursor cursor) {
		if (cursor instanceof MappingCursor) {
			return ((MappingCursor)cursor).getRowType(); 
		} else
			return ROW_TYPE_CHILD;
	}
	
	/**
	 * Useful for SectionIndexer.getSections()
	 * 
	 * @param cursor
	 * @return Key array of all groups
	 */
	public static String[] getGroupKeys(Cursor cursor) {
		if (cursor instanceof MappingCursor) {
			return ((MappingCursor)cursor).getGroupKeys();
		} else
			return null;
	}
	
	/**
	 * Usefor for SectionIndexer.getPositionForSection()
	 * 
	 * @param cursor
	 * @return group's position
	 */
	public static int getGroupHeaderPosition(Cursor cursor, int groupIndex) {
		if (cursor instanceof MappingCursor) {
			return ((MappingCursor)cursor).getGroupHeaderPosition(groupIndex);
		} else
			return 0;
	}
	
	public static abstract class Layer {
		private ArrayList<Integer> mResultPositionMap;
		private ArrayList<Object[]> mResultGroupDataRows;
		private Layer mPrevLayer;
		private Cursor mDataCursor;
		
		private boolean mHasValidResult = false; // this is set in main thread
		
		private void doMapping(Cursor mappingCursor, ArrayList<Integer> positionMap, ArrayList<Object[]> groupDataRows, Layer prevLayer, Cursor dataCursor) {
			mPrevLayer = prevLayer;
			mDataCursor = dataCursor;
			mResultPositionMap = positionMap;
			mResultGroupDataRows = groupDataRows;
			mappingCursor.moveToPosition(-1);
			onMapping(mappingCursor, positionMap);
		}
		
		/**
		 * Child class will modify positionMap to change order or insert/remove cursor row.
		 * if j = positionMap.get(i), ith row of the mapping cursor is actually jth row of the original cursor
		 * 
		 * Run in worker thread.
		 * 
		 * @param mappingCursor initial position is -1
		 * @param positionMap
		 */
		protected abstract void onMapping(Cursor mappingCursor, List<Integer> positionMap);
		
		// clear the previous result or cancel it if already mapping started.
		// if child class wants to map again, it has to call reset. ex. FilterLayer.setFilter()
		// must be called in main thread
		private void reset() {
			mHasValidResult = false;
		}
		
		// must be called in main thread
		private void finish() {
			mHasValidResult = true;
		}
		
		// if this layer used same cursor and same prev layer, it already has valid result.
		private boolean hasValidResult(Cursor cursor, Layer prev) {
			return (mHasValidResult && mDataCursor == cursor && mPrevLayer == prev);
		}
		
		/**
		 * Child class use this method to add group at the end.
		 * 
		 * Should be called from worker thread (onMapping())
		 * 
		 * @param groupKey
		 * @param groupStartPosition
		 * @param groupSize
		 */
		protected final void addGroup(String groupKey, int groupStartPosition, int groupSize) {
			addGroup(-1, groupKey, groupStartPosition, groupSize);
		}
		
		/**
		 * Child class use this method to add group at the specified location.
		 * If inserted in the middle, it will increase the start position of the later groups not to overwrap each other.
		 * 
		 * Should be called from worker thread (onMapping())
		 * 
		 * @param location
		 * @param groupKey
		 * @param groupStartPosition
		 * @param groupSize
		 */
		protected final void addGroup(int location, String groupKey, int groupStartPosition, int groupSize) {
			Object[] row = new Object[] {mResultGroupDataRows.size(), groupKey, groupStartPosition, groupSize};
			if (location < 0 || location >= mResultGroupDataRows.size()) {
				mResultGroupDataRows.add(row); // at the end
			} else {
				mResultGroupDataRows.add(location, row); // specified location
				for (int i=location+1; i<mResultGroupDataRows.size(); i++) { // adjust start position of the later groups
					Object[] groupData = mResultGroupDataRows.get(i);
					int startPos = (Integer)groupData[GROUP_CURSOR_COLUMN_START_POS];
					groupData[GROUP_CURSOR_COLUMN_START_POS] = startPos + groupSize;
				}
			}
		}
		
		private ArrayList<Integer> getResultPositionMap() {
			return mResultPositionMap;
		}
		
		private ArrayList<Object[]> getResultGroupDataRows() {
			return mResultGroupDataRows;
		}
	}
	
	private Set<String> mCollapsedGroupKey = new HashSet<String>();
	
	/**
	 * Collapse groups.
	 * collapseGroup("A", "B"); is faster than collapseGroup("A"); collapseGroup("B");
	 * because it notifies once.
	 * 
	 * Should be called from main thread.
	 * 
	 * @param groupKeys
	 */
	public void collapseGroup(String... groupKeys) {
		for (String groupKey : groupKeys)
			mCollapsedGroupKey.add(groupKey);
		internalGroupCollapseChange();
	}
	
	/**
	 * Expand groups.
	 * expandGroup("A", "B"); is faster than expandGroup("A"); expandGroup("B");
	 * because it notifies once.
	 * 
	 * Should be called from main thread.
	 * 
	 * @param groupKeys
	 */
	public void expandGroup(String... groupKeys) {
		for (String groupKey : groupKeys)
			mCollapsedGroupKey.remove(groupKey);
		internalGroupCollapseChange();
	}
	
	/**
	 * Expand all groups. 
	 */
	public void expandAllGroup() {
		mCollapsedGroupKey.clear();
		internalGroupCollapseChange();
	}
	
	private void internalGroupCollapseChange() {
		if (!mIsLoading) {
			if (mCurrentCursor != null) {
				mCurrentCursor.setGroupHeaderPositionMap(mCollapsedGroupKey);
				notifyDataSetChanged();
			}
		}
	}
	
	public static final int GROUP_CURSOR_COLUMN_ID = 0;
	public static final int GROUP_CURSOR_COLUMN_KEY = 1;
	public static final int GROUP_CURSOR_COLUMN_START_POS = 2;
	public static final int GROUP_CURSOR_COLUMN_SIZE = 3;
	
	private static final String[] GROUP_CURSOR_COLUMNS_NAME = {
		BaseColumns._ID, "key", "start", "size"
	};
	
	private static class MappingCursor extends AbstractCursor {
		private Cursor mCursor;
		
		private Cursor mOriginalCursor;
		private ArrayList<Integer> mPositionMap;
		
		private Cursor mGroupCursor;
		private List<Integer> mGroupHeaderPositionMap;
		private String[] mGroupKeys;
		
		private Layer[] mLayers;
		
		public MappingCursor(Cursor cursor, Layer[] layers, boolean layerChanged) {
			super();
			
			if (DEBUG) Log.v(TAG, "cursor count="+cursor.getCount()+", isClosed="+cursor.isClosed()+", layers="+layers);
			
			mOriginalCursor = cursor; // never null
			mCursor = mOriginalCursor;
			
			if (layers == null)
				return;
			
			mLayers = layers;
			
        	ArrayList<Integer> positionMap = null;
        	ArrayList<Object[]> groupDataRows = null;
        	Layer prev = null;
        	boolean needNewMapping = false;
        	for (Layer l : layers) {
				if (DEBUG) Log.v(TAG, "Layer "+l);
        		if (!needNewMapping) {
        			if (l.hasValidResult(mOriginalCursor, prev)) {
	        			positionMap = l.getResultPositionMap();
	        			groupDataRows = l.getResultGroupDataRows();
						if (DEBUG) Log.v(TAG, "  This layer already has valid mapping.");
        			} else {
        				needNewMapping = true;
        			}
        		}
        		if (needNewMapping) {
					// This should be copied by value (clone), not copied by reference.
					// Because otherwise positionMap(=mPositionMap) will be modified which affects cursor iteration.
					positionMap = getCloneOrDefaultPositionMap(positionMap, cursor.getCount());
					groupDataRows = getCloneOrEmptyGroupDataRows(groupDataRows);
					if (DEBUG) Log.v(TAG, "  Do the mapping");
					l.doMapping(this, positionMap, groupDataRows, prev, mOriginalCursor);
        		}
				mPositionMap = positionMap; // update the map so cursor can be updated in the next iteration(but group cursor is not updated)
				if (DEBUG) Log.v(TAG, "  New Position Map size="+positionMap.size()+" "+positionMap);
				if (DEBUG) Log.v(TAG, "  New Group Data Rows size="+groupDataRows.size());
        		prev = l;
        	}
        	
			// make group cursor from groupDataRows
			if (groupDataRows.size() > 0) {
				MatrixCursor mc = new MatrixCursor(GROUP_CURSOR_COLUMNS_NAME, groupDataRows.size());
				int i = 0;
				mGroupKeys = new String[groupDataRows.size()];
				for (Object[] groupDataRow : groupDataRows) {
					mc.addRow(groupDataRow);
					mGroupKeys[i++] = (String)groupDataRow[GROUP_CURSOR_COLUMN_KEY];
				}
				mGroupCursor = mc;
				mGroupHeaderPositionMap = new ArrayList<Integer>(groupDataRows.size());
			} else {
				mGroupCursor = null;
				mGroupHeaderPositionMap = null;
			}
		}
		
		// called in main thread
		private void finishLayerStatus() {
			if (mLayers == null)
				return;
			for (Layer l : mLayers)
				l.finish();
		}
		
		private static ArrayList<Integer> getCloneOrDefaultPositionMap(ArrayList<Integer> list, int defaultSize) {
			ArrayList<Integer> result;
			if (list == null) {
	        	result = new ArrayList<Integer>(defaultSize);
	        	for (int i=0; i<defaultSize; i++)
	        		result.add(i);
        	} else {
				result = (ArrayList<Integer>)list.clone();
        	}
			return result;
		}
		
		private static ArrayList<Object[]> getCloneOrEmptyGroupDataRows(ArrayList<Object[]> list) {
			ArrayList<Object[]> result;
			if (list == null) {
	        	result = new ArrayList<Object[]>();
        	} else {
				result = (ArrayList<Object[]>)list.clone();
        	}
			return result;
		}
		
		private Cursor getOriginalCursor() {
			return mOriginalCursor;
		}
		
		private boolean mKeepOriginalCursorOpened = false;
		
		private void setKeepOriginalCursorOpened(boolean flag) {
			mKeepOriginalCursorOpened = flag;
		}
		
		@Override
		public void close() {
			super.close();
			if (mKeepOriginalCursorOpened)
				return;
			mOriginalCursor.close();
		}
		
		// make mGroupHeaderPositionMap
		// must be called in main thread because this changes the count
		private void setGroupHeaderPositionMap(Set<String> collapsedGroupKey) {
			if (mGroupCursor == null) return;
			mGroupHeaderPositionMap.clear();
			int pos = 0;
			int oldPos = mGroupCursor.getPosition();
			mGroupCursor.moveToPosition(-1);
			while(mGroupCursor.moveToNext()) {
				String groupKey = mGroupCursor.getString(GROUP_CURSOR_COLUMN_KEY);
				int groupSize = mGroupCursor.getInt(GROUP_CURSOR_COLUMN_SIZE);
				mGroupHeaderPositionMap.add(pos);
				pos++; // 1 for group header
				if (!collapsedGroupKey.contains(groupKey))
					pos += groupSize; // group's children
			}
			mGroupCursor.moveToPosition(oldPos);
			mGroupHeaderPositionMap.add(pos); // this is the total count
		}
		
		private static final int GROUP_HEADER_POSITION_BASE = -1;

		// read mGroupHeaderPositionMap
		// < 0 : group header, >= 0 : position in original cursor
		private int getRealPosition(int position) {
			if (mPositionMap == null)
				return position;
			else {
				if (mGroupCursor != null) { // groups & children
					int index = Collections.binarySearch(mGroupHeaderPositionMap, position);
					if (index >= 0) { // return negative for group header
						return GROUP_HEADER_POSITION_BASE - index;
					} else { // child
						int groupNum = -index - 2;
						int oldPos = mGroupCursor.getPosition();
						mGroupCursor.moveToPosition(groupNum);
						int groupStartPos = mGroupCursor.getInt(GROUP_CURSOR_COLUMN_START_POS);
						mGroupCursor.moveToPosition(oldPos);
						position = position - mGroupHeaderPositionMap.get(groupNum) + groupStartPos - 1;
					}
				}
				return mPositionMap.get(position);
			}
		}
		
		@Override
		public int getCount() {
			if (mPositionMap == null)
				return mOriginalCursor.getCount();
			else {
				if (mGroupHeaderPositionMap != null && mGroupHeaderPositionMap.size() > 0)
					return mGroupHeaderPositionMap.get(mGroupHeaderPositionMap.size() - 1);
				else
					return mPositionMap.size();
			}
		}
		
		@Override
		public boolean onMove(int oldPosition, int newPosition) {
			newPosition = getRealPosition(newPosition);
			if (newPosition < 0 && mGroupCursor != null) { // group header
				mCursor = mGroupCursor;
				newPosition = GROUP_HEADER_POSITION_BASE - newPosition;
			} else {
				mCursor = mOriginalCursor;
			}
			boolean ret = mCursor.moveToPosition(newPosition);
			return ret;
		}
		
		private int getRowType() {
			if (mCursor  == mGroupCursor) {
				int groupPos = mGroupCursor.getPosition();
				if (mGroupHeaderPositionMap.get(groupPos + 1) - mGroupHeaderPositionMap.get(groupPos) > 1)
					return ROW_TYPE_GROUP_EXPANDED;
				else
					return ROW_TYPE_GROUP_COLLAPSED;
			} else {
				return ROW_TYPE_CHILD;
			}
		}
		
		private int getGroupHeaderPosition(int groupIndex) {
			return mGroupHeaderPositionMap.get(groupIndex);
		}
		
		private String[] getGroupKeys() {
			return mGroupKeys;
		}

		@Override
		public Bundle getExtras() {
			return mCursor.getExtras();
		}

		@Override
		public Bundle respond(Bundle extras) {
			return mCursor.respond(extras);
		}

		@Override
		public double getDouble(int column) {
			return mCursor.getDouble(column);
		}

		@Override
		public float getFloat(int column) {
			return mCursor.getFloat(column);
		}

		@Override
		public int getInt(int column) {
			return mCursor.getInt(column);
		}

		@Override
		public long getLong(int column) {
			return mCursor.getLong(column);
		}

		@Override
		public short getShort(int column) {
			return mCursor.getShort(column);
		}

		@Override
		public String getString(int column) {
			return mCursor.getString(column);
		}

		@Override
		public boolean isNull(int column) {
			return mCursor.isNull(column);
		}

		@Override
		public String[] getColumnNames() {
			return mOriginalCursor.getColumnNames();
		}

		@Override
		public void deactivate() {
			mOriginalCursor.deactivate();
		}

		@Override
		public void registerContentObserver(ContentObserver observer) {
			mOriginalCursor.registerContentObserver(observer);
		}

		@Override
		public void registerDataSetObserver(DataSetObserver observer) {
			mOriginalCursor.registerDataSetObserver(observer);
		}

		@Override
		public void unregisterContentObserver(ContentObserver observer) {
			mOriginalCursor.unregisterContentObserver(observer);
		}

		@Override
		public void unregisterDataSetObserver(DataSetObserver observer) {
			mOriginalCursor.unregisterDataSetObserver(observer);
		}
	}
	
	private static<T> T[] compatArraysCopyOf(T[] original, int newLength) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.GINGERBREAD) {
			return copyOf(original, newLength);
		} else {
			return Arrays.copyOf(original, newLength); // Since API Level 9
		}
	}
	
	// from jdk1.6.0_25/src.zip/java/util/Arrays.java
	private static <T> T[] copyOf(T[] original, int newLength) {
        return (T[]) copyOf(original, newLength, original.getClass());
    }
	
	private static <T,U> T[] copyOf(U[] original, int newLength, Class<? extends T[]> newType) {
        T[] copy = ((Object)newType == (Object)Object[].class)
            ? (T[]) new Object[newLength]
            : (T[]) Array.newInstance(newType.getComponentType(), newLength);
        System.arraycopy(original, 0, copy, 0,
                         Math.min(original.length, newLength));
        return copy;
    }
}