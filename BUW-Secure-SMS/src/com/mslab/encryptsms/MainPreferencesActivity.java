package com.mslab.encryptsms;

import android.os.Bundle;
import android.preference.PreferenceActivity;

/**
 * This activity shows the preferences list.
 * @author Paul Kramer
 *
 */
public class MainPreferencesActivity extends PreferenceActivity {
	
	@SuppressWarnings("deprecation")
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.main_preferences);
    }
	
}
