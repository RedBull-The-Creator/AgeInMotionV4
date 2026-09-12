package com.farhaan.ageinmotion;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import java.util.Calendar;

public class ConfigureActivity extends Activity {
    private int widgetId;
    private DatePicker dob;
    private Spinner lifespan;
    private RadioGroup mode;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_configure);
        widgetId = getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) { finish(); return; }

        dob = findViewById(R.id.dob);
        lifespan = findViewById(R.id.lifespan);
        mode = findViewById(R.id.mode);

        String[] vals = {"70 years", "75 years", "80 years", "85 years", "90 years", "95 years", "100 years"};
        lifespan.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, vals));
        int savedLife = ConfigStore.lifespan(this, widgetId);
        lifespan.setSelection(Math.max(0, Math.min(vals.length - 1, (savedLife - 70) / 5)));

        Calendar x = Calendar.getInstance();
        x.setTimeInMillis(ConfigStore.dob(this, widgetId));
        dob.updateDate(x.get(Calendar.YEAR), x.get(Calendar.MONTH), x.get(Calendar.DAY_OF_MONTH));
        if (ConfigStore.progress(this, widgetId)) ((RadioButton)findViewById(R.id.progress_mode)).setChecked(true);
        findViewById(R.id.save).setOnClickListener(v -> save());
    }

    private void save() {
        Calendar x = Calendar.getInstance();
        x.clear();
        x.set(dob.getYear(), dob.getMonth(), dob.getDayOfMonth(), 0, 0, 0);
        int life = 70 + lifespan.getSelectedItemPosition() * 5;
        boolean prog = mode.getCheckedRadioButtonId() == R.id.progress_mode;
        ConfigStore.save(this, widgetId, x.getTimeInMillis(), life, prog);
        Intent result = new Intent();
        result.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
        setResult(RESULT_OK, result);
        AgeWidgetProvider.update(this, AppWidgetManager.getInstance(this), widgetId);
        finish();
    }
}
