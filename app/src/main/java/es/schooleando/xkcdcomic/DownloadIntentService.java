package es.schooleando.xkcdcomic;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;

public class DownloadIntentService extends IntentService {
    private static final String TAG = DownloadIntentService.class.getSimpleName();
    private ResultReceiver mReceiver;

    public static final int PROGRESS = 0;
    public static final int OK = 1;
    public static final int ERROR = 2;

    public DownloadIntentService() {
        super("DownloadIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        mReceiver = intent.getParcelableExtra("receiver");
        Log.d(TAG, "onHandleIntent");
        String sUrl = intent.getStringExtra("url");
        Bundle b = new Bundle();

        b.putBoolean("esElUltimo",sUrl.equals("http://xkcd.com/info.0.json"));//avisamos que el número de comic enviado es el máximo o actual

        // TODO Aquí hacemos la conexión y accedemos a la imagen.

        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        if (ni != null && ni.isConnected()) {
            HttpURLConnection con = null;
            URL url;
            try {
                // TODO: Habrá que hacer 2 conexiones:
                //  1. Para descargar el resultado JSON para leer la URL.
                StringBuilder result = new StringBuilder();
                url = new URL(sUrl);
                con = (HttpURLConnection) url.openConnection();
                InputStream in = new BufferedInputStream(con.getInputStream());
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
                con.disconnect();
                //  2. Una vez tenemos la URL descargar la imagen en la carpeta temporal.
                JSONObject json = new JSONObject(result.toString());
                b.putInt("comic",json.getInt("num"));
                String imageUrl = json.getString("img");

                url = new URL(imageUrl);
                con = (HttpURLConnection) url.openConnection();
                con.setConnectTimeout(5000);
                con.setReadTimeout(5000);
                con.setRequestMethod("HEAD");
                con.connect();

                int size = con.getContentLength();
                in = url.openStream();

                ByteArrayOutputStream out = new ByteArrayOutputStream();

                byte[] by = new byte[1024];

                for (int i; (i = in.read(by)) != -1; ) {

                    out.write(by, 0, i);
                    if (size>0) {
                        b.putInt("progreso",out.size() * 100 / size);
                    }else{
                        b.putInt("progreso",i*-1);
                    }
                    mReceiver.send(PROGRESS, b);
                }

                File outputDir = getExternalCacheDir();
                String[] data= imageUrl.split("/");
                String[] f= data[data.length-1].split("\\.");
                File outputFile = File.createTempFile(f[0],"."+ f[1], outputDir);
                outputFile.deleteOnExit();

                FileOutputStream fos = new FileOutputStream(outputFile);
                fos.write(out.toByteArray());
                // TODO: Devolver la URI de la imagen si todo ha ido bien.
                b.putString("ruta",outputFile.getPath());
                mReceiver.send(OK, b);

                out.close();
                in.close();

                // TODO: Controlar los casos en los que no ha ido bien: excepciones en las conexiones, etc...
            } catch (MalformedURLException e) {
                b.putString("mensaje","url invalida: "+e.getMessage());
                mReceiver.send(ERROR, b);
            } catch (SocketTimeoutException e) {
                b.putString("mensaje","Tiempo excesivo: "+e.getMessage());
                mReceiver.send(ERROR, b);
            } catch (IOException e) {
                b.putString("mensaje","Error de lectura: "+e.getMessage());
                mReceiver.send(ERROR, b);
            } catch (JSONException e) {
                b.putString("mensaje","Json incorrecto: "+e.getMessage());
                mReceiver.send(ERROR, b);
            } catch (Exception e) {
                b.putString("mensaje","Excepción: "+e.getMessage());
                mReceiver.send(ERROR, b);
            } finally {
                if (con != null) con.disconnect();
            }
        } else {
            b.putString("mensaje","No hay conexión");
            mReceiver.send(ERROR, b);
        }
    }
}
