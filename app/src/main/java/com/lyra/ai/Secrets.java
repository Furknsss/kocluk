package com.lyra.ai;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.security.KeyStore;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;

final class Secrets {
    private static final String ALIAS="lyra_ai_api_key_v1";
    private static final String PREFERENCES="lyra_private";
    private Secrets() {}
    private static SecretKey getKey() throws Exception {
        KeyStore store=KeyStore.getInstance("AndroidKeyStore");
        store.load(null);
        SecretKey present=(SecretKey)store.getKey(ALIAS,null);
        if(present!=null) return present;
        KeyGenerator generator=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(ALIAS,
            KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256).build());
        return generator.generateKey();
    }
    static void save(Context context,String text) throws Exception {
        if(text==null||text.trim().isEmpty()) {
            context.getSharedPreferences(PREFERENCES,Context.MODE_PRIVATE).edit().remove("key").apply();return;
        }
        Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE,getKey());
        byte[] iv=cipher.getIV(), encrypted=cipher.doFinal(text.trim().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        byte[] all=new byte[iv.length+encrypted.length];
        System.arraycopy(iv,0,all,0,iv.length);
        System.arraycopy(encrypted,0,all,iv.length,encrypted.length);
        context.getSharedPreferences(PREFERENCES,Context.MODE_PRIVATE).edit()
            .putString("key",Base64.encodeToString(all,Base64.NO_WRAP)).apply();
    }
    static String load(Context context) {
        try {
            String encoded=context.getSharedPreferences(PREFERENCES,Context.MODE_PRIVATE).getString("key",null);
            if(encoded==null) return "";
            byte[] bytes=Base64.decode(encoded,Base64.NO_WRAP);
            if(bytes.length<29) return "";
            byte[] iv=new byte[12];
            System.arraycopy(bytes,0,iv,0,12);
            byte[] data=new byte[bytes.length-12];
            System.arraycopy(bytes,12,data,0,data.length);
            Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE,getKey(),new GCMParameterSpec(128,iv));
            return new String(cipher.doFinal(data),java.nio.charset.StandardCharsets.UTF_8);
        } catch(Exception e) { return ""; }
    }
}