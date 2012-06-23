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

public abstract class FilterLayer extends Layer {
	@Override
	protected void onMapping(Cursor mappingCursor, List<Integer> positionMap) {
		int removedCount = 0;
		while(mappingCursor.moveToNext()) {
			if (!isIncluded(mappingCursor)) {
				positionMap.remove(mappingCursor.getPosition() - removedCount);
				removedCount++;
			}
		}
	}
	
	// return true if this row should be included, false otherwise.
	protected abstract boolean isIncluded(Cursor cursor); 
}