package com.farhaan.ageinmotion;

import android.content.Context;
import java.util.Calendar;

public final class ConfigStore {
    private static final String PREF = "config";
    static void save(Context c, int id, long dob, int lifespan, boolean progress) {
        c.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putLong("dob_"+id,dob).putInt("life_"+id,lifespan).putBoolean("progress_"+id,progress).apply();
    }
    static long dob(Context c,int id){ return c.getSharedPreferences(PREF,0).getLong("dob_"+id, defaultDob()); }
    static int lifespan(Context c,int id){ return c.getSharedPreferences(PREF,0).getInt("life_"+id,80); }
    static boolean progress(Context c,int id){ return c.getSharedPreferences(PREF,0).getBoolean("progress_"+id,false); }
    static void remove(Context c,int id){ c.getSharedPreferences(PREF,0).edit().remove("dob_"+id).remove("life_"+id).remove("progress_"+id).apply(); }
    private static long defaultDob(){ Calendar x=Calendar.getInstance(); x.add(Calendar.YEAR,-27); return x.getTimeInMillis(); }
}
