package cn.balance.deepseek;

import android.app.job.*;
import android.content.*;

public final class BalanceJob extends JobService {
    private JobParameters running;
    static void schedule(Context c) {
        JobScheduler s = (JobScheduler)c.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        int minutes = Store.prefs(c).getInt("minutes",15);
        if (!Store.hasKey(c) || minutes==0 || BalanceWidget.ids(c).length==0) { s.cancel(8101); return; }
        JobInfo old = s.getPendingJob(8101);
        if (old!=null && old.getIntervalMillis()==minutes*60000L) return;
        s.schedule(new JobInfo.Builder(8101,new ComponentName(c,BalanceJob.class))
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).setPeriodic(minutes*60000L)
            .setPersisted(true).build());
    }
    public boolean onStartJob(final JobParameters p) {
        running=p;
        Repository.refresh(this,new Runnable() { public void run() { if (running==p) { running=null; jobFinished(p,false); } } });
        return true;
    }
    public boolean onStopJob(JobParameters p) { running=null; return true; }
}
