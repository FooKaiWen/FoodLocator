package home.com.googlemap;

import android.content.Intent;
import android.net.Uri;
import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class LoadingPage extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.loadingpage);
        final Intent intent = new Intent(this, MapsActivity.class);
        new CountDownTimer(3000, 1000) {
            public void onFinish() {
                startActivity(intent);
            }
            public void onTick(long millisUntilFinished) {
                millisUntilFinished = 3000;
            }
        }.start();
    }
}
