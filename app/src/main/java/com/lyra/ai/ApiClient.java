package com.lyra.ai;

import android.graphics.Bitmap;
import android.util.Base64;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class ApiClient {
    interface Callback { void done(String answer); }
    private static final ExecutorService background=Executors.newSingleThreadExecutor();
    private static final String[] MODELS={"gemini-4-hydrogen","gemini-3.8-flash"};
    private static final String SYSTEM_PROMPT=
        "Sen Lyra'sın. Türkçe konuşan, YKS (TYT/AYT) odaklı dikkatli ve verimli bir öğretmensin. "+
        "Matematik, geometri, fizik, kimya, biyoloji ve Türkçede soru kökünü, şekilleri ve şıkları "+
        "tek tek kontrol et. Yanıtı kısa bir sonuç, adım adım gerekçeli çözüm ve sınav püf noktası "+
        "olarak sun. Bilmediğin şeyi uydurma. Okunmayan görselleri belirt. "+
        "MEB kazanımlarını gözet, gereksiz uzun açıklamalar verme. "+
        "Uygulama verilerine erişimin olduğunu iddia etme; yalnızca kullanıcı tarafından paylaşılanları kullan.";
    private ApiClient() {}

    static void ask(String apiKey, String question, Bitmap image, Callback callback) {
        background.execute(() -> {
            String lastError="Yanıt alınamadı.";
            for(String model: MODELS) {
                try {
                    String answer=call(apiKey,model,question,image);
                    callback.done(answer);
                    return;
                } catch (ApiError e) {
                    lastError=e.getMessage();
                    if(e.status==401 || e.status==403) break;
                } catch(Exception e) { lastError="Bağlantı hatası: "+e.getClass().getSimpleName(); }
            }
            callback.done(lastError+"\n\nGemini 4 Hydrogen veya Gemini 3.8 Flash modelinin API erişimini Google AI Studio üzerinden kontrol et.");
        });
    }
    private static String call(String key,String model,String question,Bitmap image) throws Exception {
        URL url=new URL("https://generativelanguage.googleapis.com/v1beta/models/"+model+":generateContent");
        HttpURLConnection connection=(HttpURLConnection)url.openConnection();
        try {
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(12000);
            connection.setReadTimeout(65000);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type","application/json; charset=UTF-8");
            connection.setRequestProperty("x-goog-api-key",key);
            JSONArray parts=new JSONArray();
            parts.put(new JSONObject().put("text",question));
            if(image!=null) {
                Bitmap bitmap=image;
                if(bitmap.getWidth()>1600) {
                    bitmap=Bitmap.createScaledBitmap(bitmap,1600,
                        Math.max(1,(int)(bitmap.getHeight()*1600f/bitmap.getWidth())),true);
                }
                ByteArrayOutputStream output=new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG,85,output);
                parts.put(new JSONObject().put("inline_data",new JSONObject()
                    .put("mime_type","image/jpeg")
                    .put("data",Base64.encodeToString(output.toByteArray(),Base64.NO_WRAP))));
                if(bitmap!=image) bitmap.recycle();
            }
            JSONObject request=new JSONObject()
                .put("systemInstruction",new JSONObject().put("parts",new JSONArray()
                    .put(new JSONObject().put("text",SYSTEM_PROMPT))))
                .put("contents",new JSONArray().put(new JSONObject()
                    .put("role","user").put("parts",parts)))
                .put("generationConfig",new JSONObject()
                    .put("temperature",0.15)
                    .put("maxOutputTokens",4096));
            try(OutputStream out=connection.getOutputStream()) {
                out.write(request.toString().getBytes(StandardCharsets.UTF_8));
            }
            int status=connection.getResponseCode();
            InputStream body=status<400?connection.getInputStream():connection.getErrorStream();
            String raw="";
            if(body!=null) try(BufferedReader reader=new BufferedReader(
                new InputStreamReader(body,StandardCharsets.UTF_8))) {
                StringBuilder b=new StringBuilder();String line;
                while((line=reader.readLine())!=null) b.append(line);
                raw=b.toString();
            }
            if(status<200||status>299) {
                String detail="";
                try { detail=new JSONObject(raw).getJSONObject("error").optString("message",""); }
                catch(Exception ignored) {}
                String message=(status==429?"Ücretsiz kullanım kotası veya hız sınırı doldu. ":
                    status==401||status==403?"API anahtarını veya model erişimini kontrol et. ":
                    status==404||status==400?"Model bu API anahtarında kullanılamıyor. ":"API hatası ("+status+"). ");
                throw new ApiError(status,message+detail);
            }
            JSONObject object=new JSONObject(raw);
            JSONArray candidates=object.optJSONArray("candidates");
            if(candidates==null||candidates.length()==0) throw new ApiError(0,"Model yanıt üretmedi.");
            JSONArray responses=candidates.getJSONObject(0).optJSONObject("content").optJSONArray("parts");
            StringBuilder response=new StringBuilder();
            if(responses!=null)for(int i=0;i<responses.length();i++)
                response.append(responses.getJSONObject(i).optString("text",""));
            if(response.toString().trim().isEmpty()) throw new ApiError(0,"Boş yanıt geldi.");
            return response.toString().trim();
        } finally { connection.disconnect(); }
    }
    private static class ApiError extends Exception {
        final int status;
        ApiError(int code,String message) {super(message);status=code;}
    }
}