package com.lovellfelix.caribbeannewsstand.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import com.lovellfelix.caribbeannewsstand.Constants;
import net.fred.feedex.R;
import com.lovellfelix.caribbeannewsstand.fragment.EntryFragment;
import com.lovellfelix.caribbeannewsstand.utils.UiUtils;

public class EntryActivity extends BaseActivity {

    private EntryFragment mEntryFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        UiUtils.setPreferenceTheme(this);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_entry);

        mEntryFragment = (EntryFragment) getFragmentManager().findFragmentById(R.id.entry_fragment);
        if (savedInstanceState == null) { // Put the data only the first time (the fragment will save its state)
            mEntryFragment.setData(getIntent().getData());
        }

        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            Bundle b = getIntent().getExtras();
            if (b != null && b.getBoolean(Constants.INTENT_FROM_WIDGET, false)) {
                Intent intent = new Intent(this, HomeActivity.class);
                startActivity(intent);
            }
            finish();
            return true;
        }

        return false;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        mEntryFragment.setData(intent.getData());
    }
}