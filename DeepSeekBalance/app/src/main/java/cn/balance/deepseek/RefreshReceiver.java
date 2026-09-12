package cn.balance.deepseek;

import android.content.*;
import android.appwidget.AppWidgetManager;
import android.widget.RemoteViews;

public final class RefreshReceiver extends BroadcastReceiver {
    public void onReceive(Context c,Intent i) {
        RemoteViews v = new RemoteViews(c.getPackageName(),R.layout.balance_widget);
        v.setTextViewText(R.id.widget_status,"正在查询…");
        AppWidgetManager.getInstance(c).partiallyUpdateAppWidget(BalanceWidget.ids(c),v);
        fetch(c,goAsync());
    }
    static void fetch(Context c,final PendingResult result) {
        // Finish receiver lifetime promptly even if a platform network stack ignores timeouts.
        final java.util.concurrent.atomic.AtomicBoolean finished = new java.util.concurrent.atomic.AtomicBoolean();
        final Runnable finish = new Runnable() { public void run() { if (finished.compareAndSet(false,true)) result.finish(); } };
        Repository.MAIN.postDelayed(finish,8500);
        Repository.refresh(c,new Runnable() { public void run() { Repository.MAIN.removeCallbacks(finish); finish.run(); } });
    }
}
