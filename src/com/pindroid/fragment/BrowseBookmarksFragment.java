/*
 * PinDroid - http://code.google.com/p/PinDroid/
 *
 * Copyright (C) 2010 Matt Schmidt
 *
 * PinDroid is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 3 of the License,
 * or (at your option) any later version.
 *
 * PinDroid is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PinDroid; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA
 */

package com.pindroid.fragment;

import com.pindroid.R;
import com.pindroid.activity.FragmentBaseActivity;
import com.pindroid.listadapter.BookmarkViewBinder;
import com.pindroid.platform.BookmarkManager;
import com.pindroid.providers.BookmarkContent.Bookmark;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;
import android.support.v4.widget.SimpleCursorAdapter;

public class BrowseBookmarksFragment extends ListFragment 
	implements LoaderManager.LoaderCallbacks<Cursor> {
	
	private SimpleCursorAdapter mAdapter;
	private FragmentBaseActivity base;
	
	private String sortfield = Bookmark.Time + " DESC";

	private String tagname = null;
	private boolean unread = false;
	private Intent intent = null;
	String path = null;
	
	ListView lv;
	
	private OnBookmarkSelectedListener bookmarkSelectedListener;
	
	public interface OnBookmarkSelectedListener {
		public void onBookmarkView(Bookmark b);
		public void onBookmarkRead(Bookmark b);
		public void onBookmarkOpen(Bookmark b);
		public void onBookmarkAdd(Bookmark b);
		public void onBookmarkShare(Bookmark b);
		public void onBookmarkMark(Bookmark b);
		public void onBookmarkEdit(Bookmark b);
		public void onBookmarkDelete(Bookmark b);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState){
		super.onActivityCreated(savedInstanceState);
		
		base = (FragmentBaseActivity)getActivity();
		intent = base.getIntent();
		
		setHasOptionsMenu(true);
		
		mAdapter = new SimpleCursorAdapter(base, R.layout.bookmark_view, null, 
				new String[]{Bookmark.Description, Bookmark.Tags, Bookmark.ToRead, Bookmark.Shared}, 
				new int[]{R.id.bookmark_description, R.id.bookmark_tags, R.id.bookmark_unread, R.id.bookmark_private}, 0);
		
		setListAdapter(mAdapter);
		mAdapter.setViewBinder(new BookmarkViewBinder());

		if(base.mAccount != null) {				
			Uri data = intent.getData();
			
			if(data != null) {
				if(data.getUserInfo() != "") {
					base.username = data.getUserInfo();
				} else base.username = base.mAccount.name;
				
				path = data.getPath();
				tagname = data.getQueryParameter("tagname");
				unread = data.getQueryParameter("unread") != null;
				
		    	if(!data.getScheme().equals("content")) {
		    		openBookmarkInBrowser(new Bookmark(data.toString()));
		    		base.finish();
		    	}
			}
	
	    	getLoaderManager().initLoader(0, null, this);
	    	
			lv = getListView();
			lv.setTextFilterEnabled(true);
			lv.setFastScrollEnabled(true);
		
			lv.setOnItemClickListener(new OnItemClickListener() {
			    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					final Cursor c = (Cursor)lv.getItemAtPosition(position);
					Bookmark b = BookmarkManager.CursorToBookmark(c);
	
			    	if(base.defaultAction.equals("view")) {
			    		viewBookmark(b);
			    	} else if(base.defaultAction.equals("read")) {
			    		readBookmark(b);
			    	} else {
			    		openBookmarkInBrowser(b);
			    	}   	
			    }
			});
			
			/* Add Context-Menu listener to the ListView. */
			lv.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {
				public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
					menu.setHeaderTitle("Actions");
					MenuInflater inflater = base.getMenuInflater();

					inflater.inflate(R.menu.browse_bookmark_context_menu_self, menu);
				}
			});
		}
	}
	
	@Override
	public void onResume(){
		super.onResume();
		
		Uri data = base.getIntent().getData();
		if(data != null && data.getUserInfo() != null && data.getUserInfo() != "") {
			base.username = data.getUserInfo();
		} else if(base.getIntent().hasExtra("username")){
			base.username = base.getIntent().getStringExtra("username");
		} else base.username = base.mAccount.name;
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem aItem) {
		AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo) aItem.getMenuInfo();
		final Cursor c = (Cursor)lv.getItemAtPosition(menuInfo.position);
		Bookmark b = BookmarkManager.CursorToBookmark(c);
		
		switch (aItem.getItemId()) {
			case R.id.menu_bookmark_context_open:
				openBookmarkInBrowser(b);
				return true;
			case R.id.menu_bookmark_context_view:				
				viewBookmark(b);
				return true;
			case R.id.menu_bookmark_context_edit:
				bookmarkSelectedListener.onBookmarkEdit(b);
				return true;
			case R.id.menu_bookmark_context_delete:
				bookmarkSelectedListener.onBookmarkDelete(b);
				return true;
			case R.id.menu_bookmark_context_share:
				bookmarkSelectedListener.onBookmarkShare(b);
				return true;
			case R.id.menu_bookmark_context_read:
				readBookmark(b);
				return true;
			case R.id.menu_bookmark_context_markread:
				markBookmark(b);
				return true;
		}
		return false;
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		
		inflater.inflate(R.menu.browse_bookmark_menu, menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		boolean result = false;
		
	    switch (item.getItemId()) {
		    case R.id.menu_bookmark_sort_date_asc:
		    	sortfield = Bookmark.Time + " ASC";
				result = true;
				break;
		    case R.id.menu_bookmark_sort_date_desc:			
		    	sortfield = Bookmark.Time + " DESC";
		    	result = true;
		    	break;
		    case R.id.menu_bookmark_sort_description_asc:			
		    	sortfield = Bookmark.Description + " ASC";
		    	result = true;
		    	break;
		    case R.id.menu_bookmark_sort_description_desc:			
		    	sortfield = Bookmark.Description + " DESC";
		    	result = true;
		    	break;
		    case R.id.menu_bookmark_sort_url_asc:			
		    	sortfield = Bookmark.Url + " ASC";
		    	result = true;
		    	break;
		    case R.id.menu_bookmark_sort_url_desc:			
		    	sortfield = Bookmark.Url + " DESC";
		    	result = true;
		    	break;
	    }
	    
	    if(result) {
	    	getLoaderManager().restartLoader(0, null, this);
	    } else result = super.onOptionsItemSelected(item);
	    
	    return result;
	}
	
	private void openBookmarkInBrowser(Bookmark b) {
		bookmarkSelectedListener.onBookmarkOpen(b);
	}
	
	private void readBookmark(Bookmark b){
		if(base.markAsRead)
			markBookmark(b);
		bookmarkSelectedListener.onBookmarkRead(b);
	}
	
	private void markBookmark(Bookmark b){
		bookmarkSelectedListener.onBookmarkMark(b);
	}
	
	private void viewBookmark(Bookmark b) {
		bookmarkSelectedListener.onBookmarkView(b);
	}
	
	public boolean onSearchRequested() {

		Bundle contextData = new Bundle();
		contextData.putString("tagname", tagname);
		contextData.putString("username", base.username);
		contextData.putBoolean("unread", unread);
		base.startSearch(null, false, contextData, false);
		return true;
	}
    
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {

		if(Intent.ACTION_SEARCH.equals(intent.getAction())) {		
    		Bundle searchData = intent.getBundleExtra(SearchManager.APP_DATA);
    		
    		if(searchData != null) {
    			tagname = searchData.getString("tagname");
    			base.username = searchData.getString("username");
    			unread = searchData.getBoolean("unread");
    		}
    		
    		if(intent.hasExtra("username")) {
    			base.username = intent.getStringExtra("username");
    		}
    		
    		String query = intent.getStringExtra(SearchManager.QUERY);
    		
    		if(unread) {
    			base.setTitle(getString(R.string.unread_search_results_title, query));
    		} else base.setTitle(getString(R.string.bookmark_search_results_title, query));
    		
			return BookmarkManager.SearchBookmarks(query, tagname, unread, base.username, base);
		}  else if(path.equals("/bookmarks")) {

    		String title = "";
    		
    		if(unread && tagname != null && tagname != "") {
    			title = getString(R.string.browse_my_unread_bookmarks_tagged_title, tagname);
    		} else if(unread && (tagname == null || tagname.equals(""))) {
    			title = getString(R.string.browse_my_unread_bookmarks_title);
    		} else if(tagname != null && tagname != "") {
    			title = getString(R.string.browse_my_bookmarks_tagged_title, tagname);
    		} else {
    			title = getString(R.string.browse_my_bookmarks_title);
    		}
    		
			base.setTitle(title);

			return BookmarkManager.GetBookmarks(base.username, tagname, unread, sortfield, base);
		}
		return new CursorLoader(base);
	}
	
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
	    mAdapter.swapCursor(data);
	}
	
	public void onLoaderReset(Loader<Cursor> loader) {
	    mAdapter.swapCursor(null);
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			bookmarkSelectedListener = (OnBookmarkSelectedListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString() + " must implement OnTutSelectedListener");
		}
	}
}