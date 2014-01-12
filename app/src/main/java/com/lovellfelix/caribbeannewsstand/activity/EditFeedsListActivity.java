package com.lovellfelix.caribbeannewsstand.activity;

import android.app.Activity;
import android.os.Bundle;
import android.view.MenuItem;

import com.lovellfelix.caribbeannewsstand.fragment.EditFeedsListFragment;
import com.lovellfelix.caribbeannewsstand.utils.UiUtils;

public class EditFeedsListActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        UiUtils.setPreferenceTheme(this);
        super.onCreate(savedInstanceState);

        getActionBar().setDisplayHomeAsUpEnabled(true);

        if (savedInstanceState == null) {
            EditFeedsListFragment fragment = new EditFeedsListFragment();
            getFragmentManager().beginTransaction().add(android.R.id.content, fragment, fragment.getClass().getName()).commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }

        return false;
    }
}
