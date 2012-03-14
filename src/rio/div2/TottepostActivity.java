package rio.div2;

import rio.div2.SettingActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;

public class TottepostActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
    }

    // 元のアクティビティに戻った際に設定内容を取得
    @Override
    public void onResume() {
        super.onResume();

        TextView myTextView = (TextView)findViewById(R.id.comment_state_text);
        if(SettingActivity.isCommentEnable(getApplicationContext())) {
            myTextView.setText(R.string.label_on);
        }
        else {
            myTextView.setText(R.string.label_off);
        }
        myTextView = (TextView)findViewById(R.id.location_state_text);
        if(SettingActivity.isLocationEnable(getApplicationContext())) {
            myTextView.setText(R.string.label_on);
        }
        else {
            myTextView.setText(R.string.label_off);
        }
        myTextView = (TextView)findViewById(R.id.facebook_state_text);
        if(SettingActivity.isServiceEnable(R.string.FACEBOOK_KEY, getApplicationContext())) {
            myTextView.setText(R.string.label_on);
        }
        else {
            myTextView.setText(R.string.label_off);
        }
        myTextView = (TextView)findViewById(R.id.twitter_state_text);
        if(SettingActivity.isServiceEnable(R.string.TWITTER_KEY, getApplicationContext())) {
            myTextView.setText(R.string.label_on);
        }
        else {
            myTextView.setText(R.string.label_off);
        }
    }

    // オプションメニューを作成
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean returnBool = super.onCreateOptionsMenu(menu);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);

        return(returnBool);
    }

    // オプションメニューの項目が選択された際の動作を設定
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean returnBool = super.onOptionsItemSelected(item);

        Intent intent = new Intent();
        switch(item.getItemId()) {
        case R.id.menu_setting:
            // 設定画面を開く
            intent.setClassName(getPackageName(), getPackageName() + ".SettingActivity");
            startActivity(intent);
            break;
        case R.id.menu_about:
            // About画面を開く
            intent.setClassName(getPackageName(), getPackageName() + ".AboutActivity");
            startActivity(intent);
            break;
        }

        return(returnBool);
    }
}