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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import android.database.Cursor;
import dev.easetheworld.mappingcursorloader.MappingCursorLoader.Layer;

/**
 * Choose some items with condition and copy them at the top.
 * An item is in a group if the item satisfies the group condition.
 * An item can be in many groups.
 * Group with no member will not be shown.
 * 
 * Every item is also in default group.
 * Default group can be hidden if original data don't have any group and call setDefaultGroup(null)
 * 
 * For example,
 * Original Data is 1, 2, 3, 4, 5 ->
 *
 * Group Even
 * 2 4
 * Group Prime
 * 2 3 5
 * Default
 * 1 2 3 4 5
 */
public class ConditionalGroupsLayer extends Layer {
	
	private String mDefaultGroupKey;
	private Map<String, Group> mGroupMap = new Hashtable<String, Group>(); // new HashMap<String, Group>(); // this needs to be synchronized because it is written in main thread and read in worker thread
	
	@Override
	protected void onMapping(Cursor mappingCursor, List<Integer> positionMap) {
		String defaultGroupKey = mDefaultGroupKey;
		
		Collection<Group> groups = mGroupMap.values();
		Group[] groupList = groups.toArray(new Group[0]);
		Arrays.sort(groupList);
		
		ArrayList<Integer> members = new ArrayList<Integer>();
		int position = 0;
		int groupCount = 0;
		for (Group group : groupList) {
			members.clear();
			mappingCursor.moveToPosition(-1);
			while(mappingCursor.moveToNext()) { // choose group members
				if (group.condition.isSatisfied(mappingCursor)) {
					members.add(positionMap.get(mappingCursor.getPosition() + position));
				}
			}
			if (members.size() > 0) {
				positionMap.addAll(position, members);
				addGroup(groupCount, group.key, position, members.size()); // add at the top
				position += members.size();
				groupCount++;
			}
		}
		
		// default group
		if (defaultGroupKey != null)
			addGroup(defaultGroupKey, position, mappingCursor.getCount());
	}
	
	public static interface Condition {
		public boolean isSatisfied(Cursor cursor);
	}
	
	private static class Group implements Comparable<Group> {
		String key;
		Condition condition;
		int order;
		
		private Group(String key, Condition condition, int order) {
			this.key = key;
			this.condition = condition;
			this.order = order;
		}

		@Override
		public int compareTo(Group that) {
			return this.order - that.order;
		}
	}
	
	// if groupKey is null, don't show default group (which includes all data)
	public void setDefaultGroup(String groupKey) {
		mDefaultGroupKey = groupKey;
	}
	
	public void addGroup(String groupKey, Condition groupCondition) {
		addGroup(groupKey, groupCondition, mGroupMap.size());
	}
	
	public void addGroup(String groupKey, Condition groupCondition, int order) {
		mGroupMap.put(groupKey, new Group(groupKey, groupCondition, order));
	}
	
	public void removeGroup(String groupKey) {
		mGroupMap.remove(groupKey);
	}
	
	public void clearGroup() {
		mGroupMap.clear();
	}
}