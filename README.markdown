EaseTheWorld's MappingCursorLoader
==================================

You can get various cursors which have different position mapping from original cursor
with `MappingCursorLoader` and `MappingCursorLoader.Layer`.

Let's say your database has data {3, 6, 9, 12, 15, 18},

`MappingCursorLoader` basically returns the same as `CursorLoader`.
- `MappingCursorLoader` with no `Layer` -->
  {3, 6, 9, 12, 15, 18}

You can change the position mapping of the original cursor.
- `MappingCursorLoader` with `Reverse Layer` -->
  {18, 15, 12, 9, 6, 3}

You can filter out of the original cursor.
- `MappingCursorLoader` with `Odd-Filter Layer` -->
  {3, 9, 15}

You can use a sequence of layers.
- `MappingCursorLoader` with `Reverse Layer` and `Odd-Filter Layer` -->
  {15, 9, 3}

You can add group header to the original cursor.
- `MappingCursorLoader` with `Every-10-Separator Layer` -->
  {'0~10 Group', 3, 6, 9, '11~20 Group', 12, 15, 18}

Feature 1 : Layer
-----------------
- A layer modifies the position mapping in `Layer.onMapping()`. 
  This is run in worker thread and deliver the result to main thread just like `CursorLoader`.

- Layers can be added or removed with `MappingCursorLoader.setLayers()/clearLayers()/addLayer()/removeLayer()`.
  First added layer is calculated first and the last added layer's mapping is the final result.

- Each layer has its mapping result(=cache) so it can be reused later
  unless the original cursor or layer order is changed.

Feature 2 : Group
-----------------
- Like `ExpandableListView`, you can add groups between cursor rows by
  `MappingCursorLoader.Layer.addGroup()`. This should be called in `MappingCursorLoader.Layer.onMapping()`.

- Like `ExpandableListView`, you can expand and collapse groups by
  `MappingCursorLoader.expandGroup()/collapseGroup()/expandAllGroup()`.
  (I think this is specially useful because `ExpandableListView` cannot be used with `CursorLoader`.) 

Examples
--------
I provide some basic layers for example. You can make your own layer or extend these layers.
Please run the TestActivity to see what these layers do.
- FilterLayer
- SortLayer
- SeparatorLayer
- ConditionalGroupsLayer

Dependencies
------------
- Android Support Package (android-support-v4.jar)
  Because of `android.support.v4.content.CursorLoader`

Release Notes
-------------
- v0.1.0 : Initial Release

Source
------
<a href="https://github.com/EaseTheWorld/MappingCursorLoader">https://github.com/EaseTheWorld/MappingCursorLoader</a>

Made by EaseTheWorld