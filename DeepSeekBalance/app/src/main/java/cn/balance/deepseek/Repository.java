package cn.balance.deepseek;

import android.content.Context;
import android.os.*;
import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.net.ssl.HttpsURLConnection;

final class Repository {
    static final ExecutorService IO = Executors.newCachedThreadPool();
    static final ScheduledExecutorService TIMER = Executors.newSingleThreadScheduledExecutor();
    static final AtomicBoolean busy = new AtomicBoolean(false);
    static final Handler MAIN = new Handler(Looper.getMainLooper());
    static void refresh(final Context context, final Runnable done) {
        final Context c = context.getApplicationContext();
        IO.execute(new Runnable() { public void run() {
            if (!busy.compareAndSet(false,true)) { complete(done); return; }
            long generation;
            String key = "";
            synchronized (Store.LOCK) { generation = Store.prefs(c).getLong("generation",0); }
            String error = null, raw = null;
            HttpsURLConnection conn = null;
            ScheduledFuture<?> timeout = null;
            try {
                key = Store.key(c);
                if (key.isEmpty()) return;
                conn = (HttpsURLConnection)new URL("https://api.deepseek.com/user/balance").openConnection();
                final HttpsURLConnection active = conn;
                timeout = TIMER.schedule(new Runnable() { public void run() { active.disconnect(); } }, 7, TimeUnit.SECONDS);
                conn.setInstanceFollowRedirects(false);
                conn.setConnectTimeout(3000); conn.setReadTimeout(4000);
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization","Bearer " + key);
                conn.setRequestProperty("Accept","application/json");
                conn.setUseCaches(false);
                int code = conn.getResponseCode();
                if (code == 200) {
                    InputStream in = conn.getInputStream();
                    try {
                        ByteArrayOutputStream out = new ByteArrayOutputStream(); byte[] b = new byte[2048]; int n;
                        while ((n=in.read(b))!=-1) { out.write(b,0,n); if (out.size()>65536) throw new IOException(); }
                        raw = out.toString("UTF-8");
                        new BalanceData(raw);
                    } finally { in.close(); }
                } else if (code==401) error="API Key 无效或已撤销，请重新设置";
                else if (code==403) error="访问被拒绝，请检查账户权限";
                else if (code==429) error="查询过于频繁，请稍后刷新";
                else if (code>=500) error="DeepSeek 服务暂不可用";
                else error="查询失败（HTTP " + code + "）";
            } catch (org.json.JSONException e) { raw=null; error="余额数据格式异常，请稍后重试";
            } catch (SocketTimeoutException e) { raw=null; error="查询超时，点击重试";
            } catch (IOException e) { raw=null; error="连接失败，请检查网络后重试";
            } catch (Exception e) { raw=null; error="密钥读取失败，请重新保存 API Key";
            } finally {
                if (timeout!=null) timeout.cancel(false);
                if (conn!=null) conn.disconnect();
                synchronized (Store.LOCK) {
                    // Discard replies from an account whose key was changed or removed mid-request.
                    if (generation==Store.prefs(c).getLong("generation",0) && Store.hasKey(c)) {
                        if (raw!=null && error==null) Store.prefs(c).edit().putString("raw",raw).putLong("updated",System.currentTimeMillis()).remove("error").apply();
                        else if (error!=null) Store.prefs(c).edit().putString("error",error).apply();
                    }
                }
                busy.set(false);
                BalanceWidget.render(c);
                complete(done);
            }
        }});
    }
    private static void complete(Runnable done) { if (done!=null) MAIN.post(done); }
}
