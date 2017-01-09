package es.schooleando.xkcdcomic;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.File;
import java.util.concurrent.ThreadLocalRandom;

public class ComicActivity extends AppCompatActivity implements BgResultReceiver.Receiver {
    private BgResultReceiver mResultReceiver;

    private int maxComic;

    private ProgressBar pb;
    private ImageView iv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comic);

        //recoger variables del layout
        pb = (ProgressBar)findViewById(R.id.progressBar);
        iv = (ImageView) findViewById(R.id.imageView);

        // creamos el BgResultReceiver
        mResultReceiver = new BgResultReceiver(new Handler());
        mResultReceiver.setReceiver(this);

        // Esto es gratis: al arrancar debemos cargar el cómic actual
        Intent intent = new Intent(this, DownloadIntentService.class);
        intent.putExtra("url", "http://xkcd.com/info.0.json");
        intent.putExtra("receiver", mResultReceiver);
        startService(intent);
    }


    @Override
    public void onReceiveResult(int resultCode, Bundle resultData) {
        // TODO: podemos recibir diferentes resultCodes del IntentService
        //      ERROR -> ha habido un problema de la conexión (Toast)
        //      PROGRESS -> nos estamos descargando la imagen (ProgressBar)
        //      OK -> nos hemos descargado la imagen correctamente. (ImageView)
        // Debeis controlar cada caso
        if (resultData.getBoolean("esElUltimo"))maxComic=resultData.getInt("comic");//si el comic es el último lo asignamos para obtener números aleatorios

        switch (resultCode){
            case DownloadIntentService.ERROR:
                String mensaje = resultData.getString("mensaje");
                pb.setVisibility(View.INVISIBLE);
                Toast.makeText(ComicActivity.this,mensaje,Toast.LENGTH_SHORT).show();
                break;

            case DownloadIntentService.PROGRESS:
                int progreso = resultData.getInt("progreso");
                pb.setIndeterminate(progreso < 0);
                pb.setProgress(progreso);
                break;

            case DownloadIntentService.OK:
                String ruta = resultData.getString("ruta");
                pb.setVisibility(View.INVISIBLE);
                File f =new File(ruta);
                if (f.exists()) {
                    iv.setImageBitmap(BitmapFactory.decodeFile(f.getAbsolutePath()));
                }else {
                    Toast.makeText(ComicActivity.this,"No existe el fichero descargado en " + ruta,Toast.LENGTH_SHORT).show();
                }
                break;
        }

    }

    // TODO: Falta un callback de ImageView para hacer click en la imagen y que se descargue otro comic aleatorio.
    public void imagenPulsada(View v){
        int rndNum = ThreadLocalRandom.current().nextInt(1, maxComic + 1);
        Intent intent = new Intent(this, DownloadIntentService.class);
        intent.putExtra("url", "http://xkcd.com/"+ rndNum +"/info.0.json");
        intent.putExtra("receiver", mResultReceiver);
        startService(intent);
    }
}
