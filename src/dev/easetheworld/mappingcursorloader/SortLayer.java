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

import java.util.Arrays;
import java.util.List;

import android.database.Cursor;
import dev.easetheworld.mappingcursorloader.MappingCursorLoader.Layer;

public abstract class SortLayer extends Layer {

	@Override
	protected void onMapping(Cursor mappingCursor, List<Integer> positionMap) {
		// make rows from cursor (each row includes its original position)
		ComparableCursorRow[] rows = new ComparableCursorRow[mappingCursor.getCount()];
		while(mappingCursor.moveToNext()) {
			ComparableCursorRow row = getComparableCursorRow(mappingCursor);
			row.mPosition = positionMap.get(mappingCursor.getPosition()); // original position
			rows[mappingCursor.getPosition()] = row;
		}
		
		// sort rows
		Arrays.sort(rows);
		
		// reposition
		for (int i=0; i<rows.length; i++) {
			positionMap.set(i, rows[i].mPosition);
		}
	}
	
	// Comparable class which contain a cursor row data
	public static abstract class ComparableCursorRow implements Comparable<ComparableCursorRow>{
		private int mPosition;
		
		@Override
		public abstract int compareTo(ComparableCursorRow another); 
	}
	
	// must not return null.
	// create the class which extends ComparableCursorRow and implement compareTo
	protected abstract ComparableCursorRow getComparableCursorRow(Cursor cursor);
}