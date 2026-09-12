package cn.balance.deepseek;

import android.content.*;
import android.security.keystore.*;
import android.util.Base64;
import java.security.KeyStore;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;

final class Store {
    static final Object LOCK = new Object();
    private static final String ALIAS = "deepseek_balance_api_key_v1";
    static SharedPreferences prefs(Context c) { return c.getSharedPreferences("balance", Context.MODE_PRIVATE); }
    static boolean hasKey(Context c) { return prefs(c).contains("secret"); }
    private static SecretKey master() throws Exception {
        KeyStore ks = KeyStore.getInstance("AndroidKeyStore"); ks.load(null);
        if (!ks.containsAlias(ALIAS)) {
            KeyGenerator g = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
            g.init(new KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());
            g.generateKey();
        }
        return (SecretKey)ks.getKey(ALIAS, null);
    }
    static String key(Context c) throws Exception {
        synchronized (LOCK) {
            String encrypted = prefs(c).getString("secret", "");
            if (encrypted.isEmpty()) return "";
            Cipher x = Cipher.getInstance("AES/GCM/NoPadding");
            x.init(Cipher.DECRYPT_MODE, master(), new GCMParameterSpec(128, Base64.decode(prefs(c).getString("iv", ""), Base64.NO_WRAP)));
            return new String(x.doFinal(Base64.decode(encrypted, Base64.NO_WRAP)), "UTF-8");
        }
    }
    static void saveKey(Context c, String key) throws Exception {
        synchronized (LOCK) {
            Cipher x = Cipher.getInstance("AES/GCM/NoPadding"); x.init(Cipher.ENCRYPT_MODE, master());
            String encrypted = Base64.encodeToString(x.doFinal(key.getBytes("UTF-8")), Base64.NO_WRAP);
            SharedPreferences p = prefs(c);
            if (!p.edit().putString("secret",encrypted).putString("iv",Base64.encodeToString(x.getIV(),Base64.NO_WRAP))
                .putLong("generation", p.getLong("generation",0)+1).remove("raw").remove("updated").remove("error").commit()) throw new Exception("storage");
        }
    }
    static void clear(Context c) {
        synchronized (LOCK) {
            SharedPreferences p = prefs(c);
            p.edit().remove("secret").remove("iv").remove("raw").remove("updated").remove("error")
                .putLong("generation",p.getLong("generation",0)+1).commit();
        }
    }
    static BalanceData data(Context c) {
        try { return new BalanceData(prefs(c).getString("raw","")); } catch (Exception e) { return null; }
    }
}
