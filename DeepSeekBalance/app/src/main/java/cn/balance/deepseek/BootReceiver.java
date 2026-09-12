package cn.balance.deepseek;
import android.content.*;
public final class BootReceiver extends BroadcastReceiver {
    public void onReceive(Context c,Intent i) { BalanceJob.schedule(c); BalanceWidget.render(c); }
}
