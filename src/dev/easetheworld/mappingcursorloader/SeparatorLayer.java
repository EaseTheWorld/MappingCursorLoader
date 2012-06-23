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

import java.util.List;

import android.database.Cursor;
import dev.easetheworld.mappingcursorloader.MappingCursorLoader.Layer;

// Grouping by getGroup(Cursor)
// Assume the cursor is already sorted by group.(otherwise binary search makes no sense)
/**
 * Grouping by getGroup()
 * Groups can be alphabet, date, type ...
 * Assume the cursor rows of the same group are next to each other. (Otherwise binary search makes no sense!)
 */
public abstract class SeparatorLayer extends Layer {

	@Override
	protected void onMapping(Cursor mappingCursor, List<Integer> positionMap) {
		if (mappingCursor.getCount() == 0)
			return;
		do {
			mappingCursor.moveToNext();
			String currentGroup = getGroup(mappingCursor);
			int groupStart = mappingCursor.getPosition();
			
			// binary search
			int l = groupStart;
			int r = mappingCursor.getCount();
			while(l + 1 < r) { // l : current group, r : next group
				int m = (l + r) / 2;
				mappingCursor.moveToPosition(m);
				if (currentGroup.equals(getGroup(mappingCursor))) {
					l = m;
				} else {
					r = m;
				}
			}
			mappingCursor.moveToPosition(l);
			
			addGroup(currentGroup, groupStart, l - groupStart + 1);
		} while(!mappingCursor.isLast());
	}
	
	// must not return null.
	// return the data which divides the cursor rows.
	protected abstract String getGroup(Cursor cursor);
}