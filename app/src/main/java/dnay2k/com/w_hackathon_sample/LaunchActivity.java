package dnay2k.com.w_hackathon_sample;

import android.app.Activity;
import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.widget.ImageView;
import com.squareup.picasso.Picasso;


/**
 * Created by dnay2 on 2016-07-22.
 */
public class LaunchActivity extends Activity {

    ImageView launchImg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        launchImg = (ImageView) findViewById(R.id.launchImg);
        Picasso.with(getApplicationContext()).load(R.drawable.ohfit_img).fit().into(launchImg);
        Thread t = new Thread(){
            @Override
            public void run() {
                SystemClock.sleep(2500);
                mHandler.sendEmptyMessage(0);
            }
        };
        t.start();
    }

    Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            Intent intent = new Intent(LaunchActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }
    };
}
