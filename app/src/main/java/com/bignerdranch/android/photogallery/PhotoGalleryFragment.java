package com.bignerdranch.android.photogallery;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Layout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PhotoGalleryFragment extends Fragment {

	private static final String TAG = "PhotoGalleryFragment";

	private RecyclerView mPhotoRecyclerView;
	private List<GalleryItem> mItems = new ArrayList<>();
	private ThumbnailDownloader<PhotoHolder> mThumbnailDownloader;

	public static PhotoGalleryFragment newInstance() {
		return new PhotoGalleryFragment();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
		new FetchItemsTask().execute();

		Handler responseHandler = new Handler();
		mThumbnailDownloader = new ThumbnailDownloader<>(responseHandler);
		mThumbnailDownloader.setThumbnailDownloadListener(
				new ThumbnailDownloader.ThumbnailDownloadListener<PhotoHolder>() {
					@Override
					public void onThumbnailDownloaded(PhotoHolder target, Bitmap thumbnail) {
						Drawable drawable = new BitmapDrawable(getResources(), thumbnail);
						target.bindDrawable(drawable);
					}
				}
		);
		mThumbnailDownloader.start();
		mThumbnailDownloader.getLooper();
		Log.i(TAG, "Background thread started");
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_photo_gallery, container, false);
		mPhotoRecyclerView = (RecyclerView) v.findViewById(R.id.photo_recycler_view);
		mPhotoRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(), 3));

		setupAdapter();

		return v;
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		mThumbnailDownloader.clearQueue();
	}

	@Override
	public void onDestroy() {

		super.onDestroy();
		mThumbnailDownloader.quit();
		Log.i(TAG, "Background thread destroyed");
	}

	private class PhotoHolder extends RecyclerView.ViewHolder {
		private ImageView mItemImageView;

		public PhotoHolder(@NonNull View itemView) {
			super(itemView);
			mItemImageView = (ImageView)itemView.findViewById(R.id.item_image_view);
		}

		public void bindDrawable(Drawable drawable) {
			mItemImageView.setImageDrawable(drawable);
		}
	}

	private class PhotoAdapter extends RecyclerView.Adapter<PhotoHolder> {

		private List<GalleryItem> mGalleryItems;

		public PhotoAdapter(List<GalleryItem> galleryItems) {
			mGalleryItems = galleryItems;
		}

		@Override
		public PhotoHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
			LayoutInflater inflater = LayoutInflater.from(getActivity());
			View view = inflater.inflate(R.layout.list_item_gallery, viewGroup, false);
			return new PhotoHolder(view);
		}

		@Override
		public void onBindViewHolder(@NonNull PhotoHolder photoHolder, int position) {
			GalleryItem galleryItem = mGalleryItems.get(position);
			Drawable placeholder = getResources().getDrawable(R.drawable.bill_up_close);
			photoHolder.bindDrawable(placeholder);
			mThumbnailDownloader.queueThumbnail(photoHolder, galleryItem.getUrl());
		}

		@Override
		public int getItemCount() {
			return mGalleryItems.size();
		}
	}

	private class FetchItemsTask extends AsyncTask<Void, Void, List<GalleryItem>> {
		@Override
		protected List<GalleryItem> doInBackground(Void... params) {
			return new FlickrFetchr().fetchItems();
		}

		@Override
		protected void onPostExecute(List<GalleryItem> items) {
			Log.i(TAG, "onPostExecute() - items.size() = " + items.size());
			mItems = items;
			setupAdapter();
		}
	}

	private void setupAdapter() {

		if (isAdded()) {
			Log.i(TAG,"setupAdapter() - inside if clause = mItems.size() =" + mItems.size());
			mPhotoRecyclerView.setAdapter(new PhotoAdapter(mItems));
		}
	}
}

