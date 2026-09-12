package cn.balance.deepseek;

import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.text.InputType;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;

public final class MainActivity extends Activity implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final int INK=0xff182443, MUTED=0xff68768e, BLUE=0xff5268ed, BG=0xfff4f6fb;
    private LinearLayout content, wallets;
    private TextView amount, state, stamp, keyState;
    private Button refresh;
    private EditText keyInput;
    private boolean visible;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable tick = new Runnable() { public void run() {
        if (visible) { fetch(); handler.postDelayed(this,60000); }
    }};
    public void onCreate(Bundle stateBundle) {
        super.onCreate(stateBundle);
        if (Build.VERSION.SDK_INT>=30) getWindow().setDecorFitsSystemWindows(false);
        final ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(BG); scroll.setFillViewport(true);
        content = new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(22),dp(24),dp(22),dp(28));
        scroll.addView(content);
        if (Build.VERSION.SDK_INT>=30) scroll.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            public WindowInsets onApplyWindowInsets(View v,WindowInsets insets) {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.ime());
                scroll.setPadding(bars.left,bars.top,bars.right,bars.bottom); return insets;
            }
        });
        setContentView(scroll);
        TextView eyebrow = text("DEEPSEEK  /  BALANCE",12,BLUE); eyebrow.setLetterSpacing(.12f); content.addView(eyebrow);
        TextView title = text("余额，一眼就知道。",27,INK); title.setTypeface(null,Typeface.BOLD); add(title,8);
        add(text("你的 API 账户余额与桌面小部件",14,MUTED),6);

        LinearLayout hero = card(INK); add(hero,24);
        hero.addView(text("总可用余额",14,0xffbbc9ea));
        amount = text("—",40,Color.WHITE); amount.setTypeface(null,Typeface.BOLD);
        amount.setAutoSizeTextTypeUniformWithConfiguration(20,40,1,android.util.TypedValue.COMPLEX_UNIT_SP);
        amount.setMinHeight(dp(66)); hero.addView(amount);
        state = text("添加 API Key 后开始查询",13,0xffb8f2da); hero.addView(state);
        stamp = text("尚未更新",12,0xffbbc9ea); LinearLayout.LayoutParams tp=wrap();tp.topMargin=dp(12);hero.addView(stamp,tp);
        refresh = button("↻  立即刷新",BLUE,Color.WHITE); add(refresh,12);
        refresh.setOnClickListener(new View.OnClickListener(){public void onClick(View v){fetch();}});
        wallets = new LinearLayout(this);wallets.setOrientation(LinearLayout.VERTICAL); add(wallets,4);

        LinearLayout setup = card(Color.WHITE); add(setup,20);
        setup.addView(bold("连接账户",18));
        keyState=text("尚未连接",12,MUTED); setup.addView(keyState);
        keyInput = new EditText(this);
        keyInput.setHint("粘贴 DeepSeek 官方 API Key"); keyInput.setTextSize(14);
        keyInput.setSingleLine(true); keyInput.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
        keyInput.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
        keyInput.setSaveEnabled(false);keyInput.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_DONE);
        keyInput.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(512)});
        setup.addView(keyInput,new LinearLayout.LayoutParams(-1,dp(56)));
        Button save=button("保存并查询",BLUE,Color.WHITE);setup.addView(save);
        save.setOnClickListener(new View.OnClickListener(){public void onClick(View v){saveKey();}});
        Button remove=button("移除密钥与本地余额",0xffedf0f8,MUTED);LinearLayout.LayoutParams rp=wrap();rp.topMargin=dp(8);setup.addView(remove,rp);
        remove.setOnClickListener(new View.OnClickListener(){public void onClick(View v){
            new AlertDialog.Builder(MainActivity.this).setTitle("移除账户？").setMessage("将删除本机保存的密钥和余额，桌面小部件会停止查询。")
                .setNegativeButton("取消",null).setPositiveButton("移除",new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int w){
                    Store.clear(MainActivity.this);keyInput.setText("");BalanceJob.schedule(MainActivity.this);BalanceWidget.render(MainActivity.this);draw();
                }}).show();
        }});
        TextView privacy=text("密钥使用 Android Keystore 在本机加密保存，仅发送至 api.deepseek.com 查询余额。应用无广告、无统计 SDK。",12,MUTED);
        LinearLayout.LayoutParams pp=wrap();pp.topMargin=dp(12);setup.addView(privacy,pp);

        LinearLayout widget=card(Color.WHITE);add(widget,16);widget.addView(bold("放到桌面，随时看",18));
        widget.addView(text("点小部件的「刷新余额」立即查询，点余额区域打开 App。",13,MUTED));
        Button pin=button("＋  添加桌面小部件",0xffedf0f8,BLUE);LinearLayout.LayoutParams ap=wrap();ap.topMargin=dp(12);widget.addView(pin,ap);
        pin.setOnClickListener(new View.OnClickListener(){public void onClick(View v){
            AppWidgetManager manager=AppWidgetManager.getInstance(MainActivity.this);
            if (manager.isRequestPinAppWidgetSupported()) {
                boolean ok=manager.requestPinAppWidget(new ComponentName(MainActivity.this,BalanceWidget.class),null,null);
                if(!ok) widgetHelp();
            } else widgetHelp();
        }});
        widget.addView(text("桌面自动刷新间隔",13,INK));
        Spinner interval=new Spinner(this);
        final int[] values={15,30,60,0};
        ArrayAdapter<String> adapter=new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"15 分钟（默认）","30 分钟","60 分钟","仅手动刷新"});
        interval.setAdapter(adapter);int current=Store.prefs(this).getInt("minutes",15);
        for(int n=0;n<values.length;n++)if(values[n]==current)interval.setSelection(n);
        widget.addView(interval,new LinearLayout.LayoutParams(-1,dp(52)));
        interval.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            public void onNothingSelected(AdapterView<?> p){}
            public void onItemSelected(AdapterView<?> p,View v,int position,long id){
                Store.prefs(MainActivity.this).edit().putInt("minutes",values[position]).apply();BalanceJob.schedule(MainActivity.this);
            }
        });
        widget.addView(text("App 在前台每 60 秒刷新。桌面刷新受安卓省电策略影响，可能延迟；请以「更新于」时间为准。若长时间不更新，可在系统设置中允许本应用后台运行。",12,MUTED));
        add(text("显示整个账号的余额，非单个 Key 的独立额度。\n仅支持 DeepSeek 官方密钥，不支持第三方中转站。\nDeepSeek 余额 1.0.0 · 独立工具，非官方应用",12,MUTED),20);
        draw();
    }
    private void saveKey() {
        final String key=keyInput.getText().toString().trim();
        if(key.length()<8 || !key.matches("[A-Za-z0-9_.-]+")){keyInput.setError("请粘贴完整的 API Key，不要添加 Bearer 前缀");return;}
        if(Repository.busy.get()){toast("正在查询，请稍等片刻再保存");return;}
        try {
            Store.saveKey(this,key);keyInput.setText("");keyInput.clearFocus();
            ((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(keyInput.getWindowToken(),0);
            BalanceWidget.render(this);BalanceJob.schedule(this);fetch();
        } catch(Exception e){toast("无法加密保存密钥，请重试");}
    }
    private void widgetHelp(){new AlertDialog.Builder(this).setTitle("添加桌面小部件").setMessage("返回手机桌面，长按空白处 → 小部件（或添加工具）→ 找到「DeepSeek 余额」→ 拖到桌面。不同桌面的入口名称可能不同。").setPositiveButton("知道了",null).show();}
    private void fetch(){
        if(!Store.hasKey(this)){draw();return;}
        if(Repository.busy.get())return;
        refresh.setEnabled(false);refresh.setText("正在查询…");
        Repository.refresh(this,new Runnable(){public void run(){if(!isDestroyed())draw();}});
    }
    private void draw(){
        BalanceData d=Store.data(this);boolean has=Store.hasKey(this);
        amount.setText(d==null?"—":d.amounts());
        String error=Store.prefs(this).getString("error","");
        state.setText(!has?"添加 API Key 后开始查询":!error.isEmpty()?error:d==null?"等待首次查询":d.available?"账户额度可用":"账户额度不可用");
        state.setTextColor(error.isEmpty()?0xffb8f2da:0xffffc3ac);
        stamp.setText(BalanceWidget.time(this)+(d!=null&&!error.isEmpty()?" · 上次成功的余额":""));
        keyState.setText(has?"密钥已加密保存 · 输入新密钥可切换账户":"尚未连接 · 密钥只需保存一次");
        boolean busy=Repository.busy.get();refresh.setEnabled(has&&!busy);refresh.setText(busy?"正在查询…":"↻  立即刷新");
        wallets.removeAllViews();
        if(d!=null)for(BalanceData.Wallet w:d.wallets){
            LinearLayout row=card(Color.WHITE);LinearLayout.LayoutParams p=wrap();p.topMargin=dp(8);wallets.addView(row,p);
            row.addView(bold(w.currency.equals("CNY")?"人民币 · CNY":"美元 · USD",14));
            row.addView(text("充值余额  "+w.topped+"    /    赠送余额  "+w.granted,13,MUTED));
        }
    }
    public void onSharedPreferenceChanged(SharedPreferences p,String key){if(visible)draw();}
    protected void onResume(){super.onResume();visible=true;Store.prefs(this).registerOnSharedPreferenceChangeListener(this);draw();handler.post(tick);}
    protected void onPause(){visible=false;handler.removeCallbacks(tick);Store.prefs(this).unregisterOnSharedPreferenceChangeListener(this);super.onPause();}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    private TextView text(String s,int sp,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(sp);t.setTextColor(color);t.setLineSpacing(dp(3),1);return t;}
    private TextView bold(String s,int sp){TextView t=text(s,sp,INK);t.setTypeface(null,Typeface.BOLD);return t;}
    private LinearLayout card(int color){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setPadding(dp(20),dp(18),dp(20),dp(18));GradientDrawable bg=new GradientDrawable();bg.setColor(color);bg.setCornerRadius(dp(22));l.setBackground(bg);return l;}
    private Button button(String s,int bg,int fg){Button b=new Button(this);b.setText(s);b.setTextSize(14);b.setAllCaps(false);b.setTextColor(fg);b.setMinHeight(dp(50));b.setPadding(dp(12),dp(8),dp(12),dp(8));GradientDrawable d=new GradientDrawable();d.setColor(bg);d.setCornerRadius(dp(14));b.setBackground(d);b.setStateListAnimator(null);return b;}
    private LinearLayout.LayoutParams wrap(){return new LinearLayout.LayoutParams(-1,-2);}
    private void add(View v,int gap){LinearLayout.LayoutParams p=wrap();p.topMargin=dp(gap);content.addView(v,p);}
}
