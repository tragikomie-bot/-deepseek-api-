package cn.balance.deepseek;

import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.widget.RemoteViews;
import java.text.SimpleDateFormat;
import java.util.*;

public final class BalanceWidget extends AppWidgetProvider {
    static int[] ids(Context c) { return AppWidgetManager.getInstance(c).getAppWidgetIds(new ComponentName(c,BalanceWidget.class)); }
    static String time(Context c) {
        long t = Store.prefs(c).getLong("updated",0);
        return t==0 ? "尚未更新" : "更新于 " + new SimpleDateFormat("MM-dd HH:mm",Locale.CHINA).format(new Date(t));
    }
    static void render(Context c) {
        BalanceData d = Store.data(c);
        String error = Store.prefs(c).getString("error","");
        String status = !Store.hasKey(c) ? "打开 App 设置 API Key" : time(c);
        if (Store.hasKey(c)) {
            if (!error.isEmpty()) status = "刷新失败 · " + time(c);
            else if (d!=null && !d.available) status = "账户额度不可用 · " + time(c);
        }
        for (int id : ids(c)) {
            RemoteViews v = new RemoteViews(c.getPackageName(),R.layout.balance_widget);
            v.setTextViewText(R.id.widget_amount,d==null ? "—" : d.amounts());
            v.setTextViewText(R.id.widget_status,status);
            v.setOnClickPendingIntent(R.id.widget_root,PendingIntent.getActivity(c,0,new Intent(c,MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
            Intent refresh = new Intent(c,RefreshReceiver.class).setAction("cn.balance.deepseek.REFRESH").addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
            v.setOnClickPendingIntent(R.id.widget_refresh,PendingIntent.getBroadcast(c,1,refresh,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
            AppWidgetManager.getInstance(c).updateAppWidget(id,v);
        }
    }
    public void onUpdate(Context c,AppWidgetManager m,int[] ids) {
        render(c); BalanceJob.schedule(c); RefreshReceiver.fetch(c,goAsync());
    }
    public void onEnabled(Context c) { BalanceJob.schedule(c); }
    public void onDisabled(Context c) { BalanceJob.schedule(c); }
    public void onAppWidgetOptionsChanged(Context c,AppWidgetManager m,int id,android.os.Bundle options) { render(c); }
}
