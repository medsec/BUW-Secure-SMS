package com.mslab.encryptsms;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.preference.PreferenceFragment;

/**
 * This fragment shows the preferences list.
 * @author Paul Kramer
 *
 */
@SuppressLint("NewApi")
public class MainPreferencesFragment extends PreferenceFragment {
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.main_preferences);
    }
	
}
