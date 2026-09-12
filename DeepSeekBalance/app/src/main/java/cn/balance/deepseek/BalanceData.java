package cn.balance.deepseek;

import org.json.*;
import java.math.BigDecimal;
import java.util.*;

/** Parse monetary amounts as decimals, preserve separate currency wallets. */
public final class BalanceData {
    public static final class Wallet {
        public final String currency, total, granted, topped;
        Wallet(JSONObject o) throws JSONException {
            currency = o.getString("currency");
            if (!currency.equals("CNY") && !currency.equals("USD")) throw new JSONException("currency");
            total = money(o.getString("total_balance"));
            granted = money(o.getString("granted_balance"));
            topped = money(o.getString("topped_up_balance"));
        }
        public String label() { return (currency.equals("CNY") ? "¥ " : "$ ") + total; }
    }
    public final boolean available;
    public final List<Wallet> wallets = new ArrayList<Wallet>();
    public BalanceData(String raw) throws JSONException {
        JSONObject o = new JSONObject(raw);
        available = o.getBoolean("is_available");
        JSONArray a = o.getJSONArray("balance_infos");
        if (a.length() == 0 || a.length() > 2) throw new JSONException("wallet count");
        Set<String> seen = new HashSet<String>();
        for (int i=0;i<a.length();i++) {
            Wallet w = new Wallet(a.getJSONObject(i));
            if (!seen.add(w.currency)) throw new JSONException("duplicate currency");
            wallets.add(w);
        }
        Collections.sort(wallets, new Comparator<Wallet>() {
            public int compare(Wallet a, Wallet b) { return a.currency.compareTo(b.currency); }
        });
    }
    static String money(String s) throws JSONException {
        try {
            if (s.length() > 40) throw new NumberFormatException();
            BigDecimal b = new BigDecimal(s);
            if (Math.abs(b.scale()) > 12 || b.precision() > 24) throw new NumberFormatException();
            return b.stripTrailingZeros().scale() < 2 ? b.setScale(2).toPlainString() : b.stripTrailingZeros().toPlainString();
        } catch (NumberFormatException e) { throw new JSONException("invalid amount"); }
    }
    public String amounts() {
        StringBuilder s = new StringBuilder();
        for (Wallet w : wallets) { if (s.length()>0) s.append('\n'); s.append(w.label()); }
        return s.toString();
    }
}
