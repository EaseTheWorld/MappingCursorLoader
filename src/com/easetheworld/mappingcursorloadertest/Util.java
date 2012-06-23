package com.easetheworld.mappingcursorloadertest;

import java.util.Collections;
import java.util.List;

import android.database.Cursor;

import com.easetheworld.mappingcursorloadertest.CheeseProvider.CheeseTable;

import dev.easetheworld.mappingcursorloader.FilterLayer;
import dev.easetheworld.mappingcursorloader.MappingCursorLoader.Layer;
import dev.easetheworld.mappingcursorloader.SeparatorLayer;
import dev.easetheworld.mappingcursorloader.SortLayer;

public class Util  {
	
	public static class NameAlphabetSortLayer extends SortLayer {
		
		private static class CursorName extends SortLayer.ComparableCursorRow {
			private String mName;
			
			private CursorName(String name) {
				mName = name;
			}

			@Override
			public int compareTo(SortLayer.ComparableCursorRow another) {
				return mName.compareTo(((CursorName)another).mName);
			}
		}
		
		@Override
		protected SortLayer.ComparableCursorRow getComparableCursorRow(Cursor cursor) {
			return new CursorName(cursor.getString(CheeseTable.COLUMN_INDEX_NAME));
		}
		
		@Override
		public String toString() {
			return "Alphabet Sort";
		}
	}
	
	public static class NameLengthSortLayer extends SortLayer {
		
		private static class CursorName extends SortLayer.ComparableCursorRow {
			private String mName;
			
			private CursorName(String name) {
				mName = name;
			}

			@Override
			public int compareTo(SortLayer.ComparableCursorRow another) {
				return mName.length() - ((CursorName)another).mName.length();
			}
		}
		
		@Override
		protected SortLayer.ComparableCursorRow getComparableCursorRow(Cursor cursor) {
			return new CursorName(cursor.getString(CheeseTable.COLUMN_INDEX_NAME));
		}
		
		@Override
		public String toString() {
			return "Length Sort";
		}
	}
	
	public static class ReverseLayer extends Layer {
	
		@Override
		protected void onMapping(Cursor cursor, List<Integer> positionMap) {
			Collections.reverse(positionMap);
		}
		
		@Override
		public String toString() {
			return "Reverse";
		}
	}
	
	public static class NameFilterLayer extends FilterLayer {
		private CharSequence mFilter;
		public void setFilter(CharSequence filter) {
			mFilter = filter;
		}
		
		@Override
		protected boolean isIncluded(Cursor cursor) {
			if (mFilter == null) return true;
			String name = cursor.getString(CheeseTable.COLUMN_INDEX_NAME);
			return name.contains(mFilter);
		}
	}
	
	public static class NameFilterWithHeaderLayer extends NameFilterLayer {
		@Override
		protected void onMapping(Cursor mappingCursor, List<Integer> positionMap) {
			super.onMapping(mappingCursor, positionMap);
			addGroup("Result", 0, positionMap.size());
		}
	}
	
	public static class NameSeparatorLayer extends SeparatorLayer {
		@Override
		protected String getGroup(Cursor cursor) {
			String name = cursor.getString(CheeseTable.COLUMN_INDEX_NAME);
			return name.substring(0, 1);
		}
	}
}