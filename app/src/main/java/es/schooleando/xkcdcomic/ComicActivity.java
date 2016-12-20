package es.schooleando.xkcdcomic;

import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class ComicActivity extends AppCompatActivity implements BgResultReceiver.Receiver {
    private BgResultReceiver mResultReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comic);

        // creamos el BgResultReceiver
        mResultReceiver = new BgResultReceiver(new Handler());
        mResultReceiver.setReceiver(this);

        // Esto es gratis: al arrancar debemos cargar el cómic actual
        Intent intent = new Intent(this, DownloadIntentService.class);
        intent.putExtra("url", "http://xkcd.com/info.0.json");
        intent.putExtra("receiver", mResultReceiver);
        startActivity(intent);
    }


    @Override
    public void onReceiveResult(int resultCode, Bundle resultData) {
        // TODO: Aquí obtenemos la URL del archivo y lo mostramos en el ImageView.

    }

    // TODO: Falta un callback de ImageView para hacer click en la imagen y que se descargue otro comic aleatorio.
}
