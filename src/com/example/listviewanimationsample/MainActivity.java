package com.example.listviewanimationsample;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnPreDrawListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class MainActivity extends Activity {

    public static final String TAG = "ListViewAnimationSample";
    private static final int ANIMATION_DURATION = 300;
    private ListView mListView;
    private MyArrayAdapter mAdapter;
    // Cached layout positions of items in listview prior to add/removal of alarm item
    private ConcurrentHashMap<Long, Integer> mItemIdTopMap = new ConcurrentHashMap<Long, Integer>();
    private String mAddedItem;
    
    private class MyArrayAdapter extends ArrayAdapter<String> {
        List<String> mObjects;
        public MyArrayAdapter(Context context, int resource, List<String> objects) {
            super(context, resource, objects);
            mObjects = objects;
        }
        
        @Override
        public long getItemId (int position) {
            return getItem(position).hashCode();
        }
        
        public void add(int position, String item) {
            mObjects.add(position, item);
            notifyDataSetChanged();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        mListView = (ListView) findViewById(R.id.list_view);
        mAdapter = new MyArrayAdapter(this, android.R.layout.simple_list_item_1, getData());
        mListView.setAdapter(mAdapter);
        mListView.setDivider(null);
    }

    public void onAddItemClicked(View view) {
        final ListView list = mListView;
        // The alarm list needs to be disabled until the animation finishes to prevent
        // possible concurrency issues.  It becomes re-enabled after the animations have
        // completed.
        mListView.setEnabled(false);

        // Store all of the current list view item positions in memory for animation.
        int firstVisiblePosition = list.getFirstVisiblePosition();
        for (int i=0; i<list.getChildCount(); i++) {
            View child = list.getChildAt(i);
            int position = firstVisiblePosition + i;
            long itemId = mAdapter.getItemId(position);
            mItemIdTopMap.put(itemId, child.getTop());
        }
        
        mAddedItem = "Added item " + mAdapter.getCount();
        mAdapter.add(0, mAddedItem);
        startAnimation();
    }
    
    private ArrayList<String> getData() {
        ArrayList<String> result = new ArrayList<String>();
        for (int i=0; i<2; ++i) {
            result.add("Hello item " + i);
        }
        return result;
    }
    
    private void startAnimation() {
        if (mItemIdTopMap.isEmpty() && mAddedItem == null) {
            return;
        }
        
        final ViewTreeObserver observer = mListView.getViewTreeObserver();
        final ListView list = mListView;
        observer.addOnPreDrawListener(new OnPreDrawListener() {
            private View mAddedView;
            @Override
            public boolean onPreDraw() {
                // Remove the pre-draw listener, as this only needs to occur once.
                if (observer.isAlive()) {
                    observer.removeOnPreDrawListener(this);
                }
                boolean firstAnimation = true;
                int firstVisiblePosition = list.getFirstVisiblePosition();

                // Iterate through the children to prepare the add/remove animation.
                for (int i = 0; i< list.getChildCount(); i++) {
                    final View child = list.getChildAt(i);

                    int position = firstVisiblePosition + i;
                    long itemId = mAdapter.getItemId(position);
                    
                    // If this is the added alarm, set it invisible for now, and animate later.
                    if (mAddedItem != null && TextUtils.equals(mAddedItem, mAdapter.getItem(position))) {
                        mAddedView = child;
                        mAddedView.setAlpha(0.0f);
                        continue;
                    }

                    // The cached starting position of the child view.
                    Integer startTop = mItemIdTopMap.get(itemId);
                    // The new starting position of the child view.
                    int top = child.getTop();

                    // If there is no cached starting position, determine whether the item has
                    // come from the top of bottom of the list view.
                    if (startTop == null) {
                        int childHeight = child.getHeight() + list.getDividerHeight();
                        startTop = top + (i > 0 ? childHeight : -childHeight);
                    }

                    Log.d(TAG, "Start Top: " + startTop + ", Top: " + top);
                    // If the starting position of the child view is different from the
                    // current position, animate the child.
                    if (startTop != top) {
                        int delta = startTop - top;
                        child.setTranslationY(delta);
                        child.animate().setDuration(ANIMATION_DURATION).translationY(0);
                        final View addedView = mAddedView;
                        if (firstAnimation) {

                            // If this is the first child being animated, then after the
                            // animation is complete, and animate in the added alarm (if one
                            // exists).
                            child.animate().withEndAction(new Runnable() {

                                @Override
                                public void run() {


                                    // If there was an added view, animate it in after
                                    // the other views have animated.
                                    if (addedView != null) {
                                        addedView.animate().alpha(1.0f)
                                            .setDuration(ANIMATION_DURATION)
                                            .withEndAction(new Runnable() {

                                                @Override
                                                public void run() {
                                                    // Re-enable the list after the add
                                                    // animation is complete.
                                                    list.setEnabled(true);
                                                }

                                            });
                                    } else {
                                        // Re-enable the list after animations are complete.
                                        list.setEnabled(true);
                                    }
                                }

                            });
                            firstAnimation = false;
                        }
                    }
                }

                // If there were no child views (outside of a possible added view)
                // that require animation...
                if (firstAnimation) {
                    if (mAddedView != null) {
                        // If there is an added view, prepare animation for the added view.
                        Log.d(TAG, "Animating added view...");
                        mAddedView.animate().alpha(1.0f)
                            .setDuration(ANIMATION_DURATION)
                            .withEndAction(new Runnable() {
                                @Override
                                public void run() {
                                    // Re-enable the list after animations are complete.
                                    list.setEnabled(true);
                                }
                            });
                    } else {
                        // Re-enable the list after animations are complete.
                        list.setEnabled(true);
                    }
                }
                mItemIdTopMap.clear();
                
                return true;
            }
        });
    }
}
