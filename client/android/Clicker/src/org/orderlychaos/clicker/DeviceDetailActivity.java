package org.orderlychaos.clicker;


import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;

public class DeviceDetailActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_detail);

        getActionBar().setDisplayHomeAsUpEnabled(true);

        if (savedInstanceState == null) {
            Bundle arguments = new Bundle();
            arguments.putString(DeviceDetailFragment.ARG_ITEM_ID,
                    getIntent().getStringExtra(DeviceDetailFragment.ARG_ITEM_ID));
            DeviceDetailFragment fragment = new DeviceDetailFragment();
            fragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.device_detail_container, fragment)
                    .commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            NavUtils.navigateUpTo(this, new Intent(this, DeviceListActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
