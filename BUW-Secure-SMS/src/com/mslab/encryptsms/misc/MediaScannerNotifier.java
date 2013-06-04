package com.mslab.encryptsms.misc;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.net.Uri;

/**
 * This class helps to detect changes on the storage.
 * @author Paul Kramer
 *
 */
public class MediaScannerNotifier implements MediaScannerConnectionClient {
	private Context mContext;
	private MediaScannerConnection mConnection;
	private String mPath;
	private String mMimeType;
	
	/**
	 * Constructor.
	 * @param context The application context.
	 * @param path The path to look for changes.
	 * @param mimeType The filetypes.
	 */
	public MediaScannerNotifier(Context context, String path, String mimeType) {
		mContext = context;
		mPath = path;
		mMimeType = mimeType;
		mConnection = new MediaScannerConnection(context, this);
		mConnection.connect();
	}
	
	/**
	 * Scans the path.
	 */
	public void onMediaScannerConnected() {
		mConnection.scanFile(mPath, mMimeType);
	}
	
	/**
	 * Action after the scann was completed.
	 */
	public void onScanCompleted(String path, Uri uri) { 
        // OPTIONAL: scan is complete, this will cause the viewer to render it 
        try { 
            
        } finally { 
            mConnection.disconnect(); 
            mContext = null; 
        } 
    }}
