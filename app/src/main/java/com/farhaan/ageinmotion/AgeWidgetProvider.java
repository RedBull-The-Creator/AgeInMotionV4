package com.farhaan.ageinmotion;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.widget.RemoteViews;
import java.time.Duration;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Calendar;
import java.util.Locale;

public class AgeWidgetProvider extends AppWidgetProvider {
    public static final String ACTION_TICK = "com.farhaan.ageinmotion.ACTION_TICK";
    private static final long MINUTE_MS = 60_000L;
    private static final int SEGMENTS = 32;

    @Override public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        for (int id : ids) update(context, manager, id);
        scheduleTick(context);
    }
    @Override public void onEnabled(Context context) { scheduleTick(context); }
    @Override public void onDisabled(Context context) { cancelTick(context); }
    @Override public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (ACTION_TICK.equals(intent.getAction())) { refreshAll(context); scheduleTick(context); }
    }

    static void update(Context c, AppWidgetManager m, int id) {
        long dobMillis = ConfigStore.dob(c, id);
        boolean progressMode = ConfigStore.progress(c, id);
        long nowMillis = System.currentTimeMillis();
        ZoneId zone = ZoneId.systemDefault();
        ZonedDateTime now = ZonedDateTime.now(zone);
        ZonedDateTime birth = CalendarToZoned(dobMillis, zone);
        double ageYears = birth.isAfter(now) ? 0.0 : Duration.between(birth, now).toMillis() / 31_557_600_000.0;

        RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget_age_in_motion);
        v.setTextViewText(R.id.age, String.format(Locale.US, "%.9f", ageYears));
        v.setTextViewText(R.id.unit, "YEARS");
        v.setTextViewText(R.id.detail, progressMode ? "TODAY IN MOTION" : "TIME IS MOVING");

        android.os.Bundle opts = m.getAppWidgetOptions(id);
        int widthDp = Math.max(opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 110), opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 110));
        boolean detailed = widthDp >= 150;
        v.setViewVisibility(R.id.age_detail_grid, detailed && !progressMode ? android.view.View.VISIBLE : android.view.View.GONE);
        v.setViewVisibility(R.id.progress_detail_grid, detailed && progressMode ? android.view.View.VISIBLE : android.view.View.GONE);
        v.setViewVisibility(R.id.progress_area, progressMode ? android.view.View.VISIBLE : android.view.View.GONE);
        v.setViewVisibility(R.id.percent, progressMode ? android.view.View.VISIBLE : android.view.View.GONE);

        if (progressMode) {
            double p = dayProgress(nowMillis);
            v.setTextViewText(R.id.percent, String.format(Locale.US, "%.2f%%", p * 100.0));
            v.removeAllViews(R.id.progress_segments);
            int filled = (int)Math.round(p * SEGMENTS);
            for (int i=0;i<SEGMENTS;i++) {
                RemoteViews seg = new RemoteViews(c.getPackageName(), R.layout.widget_segment);
                seg.setInt(R.id.segment, "setBackgroundResource", i < filled ? R.drawable.progress_segment_on : R.drawable.progress_segment_off);
                v.addView(R.id.progress_segments, seg);
            }
            setDayDetails(v, now);
        } else if (detailed) {
            setAgeDetails(v, birth, now);
        }
        m.updateAppWidget(id, v);
    }

    private static ZonedDateTime CalendarToZoned(long millis, ZoneId zone) {
        Calendar c = Calendar.getInstance(java.util.TimeZone.getTimeZone(zone));
        c.setTimeInMillis(millis);
        LocalDate d = LocalDate.of(c.get(Calendar.YEAR), c.get(Calendar.MONTH)+1, c.get(Calendar.DAY_OF_MONTH));
        return d.atStartOfDay(zone);
    }

    private static void setAgeDetails(RemoteViews v, ZonedDateTime birth, ZonedDateTime now) {
        if (birth.isAfter(now)) {
            v.setTextViewText(R.id.d1, "0"); v.setTextViewText(R.id.d2, "0"); v.setTextViewText(R.id.d3, "0"); v.setTextViewText(R.id.d4, "0");
            return;
        }
        Period p = Period.between(birth.toLocalDate(), now.toLocalDate());
        ZonedDateTime anchor = birth.plusYears(p.getYears()).plusMonths(p.getMonths()).plusDays(p.getDays());
        if (anchor.isAfter(now)) { p = p.minusDays(1); anchor = birth.plusYears(p.getYears()).plusMonths(p.getMonths()).plusDays(p.getDays()); }
        Duration d = Duration.between(anchor, now);
        long millis = d.toMillis();
        long hours = millis / 3_600_000L;
        v.setTextViewText(R.id.d1, String.valueOf(p.getYears()));
        v.setTextViewText(R.id.d2, String.valueOf(p.getMonths()));
        v.setTextViewText(R.id.d3, String.valueOf(p.getDays()));
        v.setTextViewText(R.id.d4, String.valueOf(hours));
    }

    private static void setDayDetails(RemoteViews v, ZonedDateTime now) {
        int h = now.getHour(), min = now.getMinute(), sec = now.getSecond(), ms = now.getNano()/1_000_000;
        v.setTextViewText(R.id.p1, String.valueOf(h)); v.setTextViewText(R.id.p2, String.valueOf(min)); v.setTextViewText(R.id.p3, String.valueOf(sec)); v.setTextViewText(R.id.p4, String.valueOf(ms));
    }

    private static double dayProgress(long millis) {
        ZoneId zone = ZoneId.systemDefault();
        ZonedDateTime now = ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(millis), zone);
        ZonedDateTime start = now.toLocalDate().atStartOfDay(zone);
        ZonedDateTime end = now.toLocalDate().plusDays(1).atStartOfDay(zone);
        double f = Duration.between(start, now).toMillis() / (double) Duration.between(start, end).toMillis();
        return Math.max(0.0, Math.min(1.0, f));
    }

    private static void scheduleTick(Context context) {
        AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarm.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime()+MINUTE_MS, MINUTE_MS, tickIntent(context));
    }
    private static void cancelTick(Context context) {
        AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarm.cancel(tickIntent(context));
    }
    private static PendingIntent tickIntent(Context context) {
        Intent i = new Intent(context, AgeWidgetProvider.class).setAction(ACTION_TICK);
        return PendingIntent.getBroadcast(context, 9381, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
    public static void refreshAll(Context c) {
        AppWidgetManager m = AppWidgetManager.getInstance(c);
        ComponentName n = new ComponentName(c, AgeWidgetProvider.class);
        for (int id : m.getAppWidgetIds(n)) update(c, m, id);
    }
    @Override public void onDeleted(Context c, int[] ids) {
        for (int id : ids) ConfigStore.remove(c, id);
        if (AppWidgetManager.getInstance(c).getAppWidgetIds(new ComponentName(c, AgeWidgetProvider.class)).length == 0) cancelTick(c);
    }
}
