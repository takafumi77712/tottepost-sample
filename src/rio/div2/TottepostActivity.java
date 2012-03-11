package rio.div2;

import rio.div2.SettingActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
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
        if(SettingActivity.isCommentEnable(getBaseContext())) {
            myTextView.setText(R.string.label_on);
        }
        else {
            myTextView.setText(R.string.label_off);
        }
        myTextView = (TextView)findViewById(R.id.location_state_text);
        if(SettingActivity.isLocationEnable(getBaseContext())) {
            myTextView.setText(R.string.label_on);
        }
        else {
            myTextView.setText(R.string.label_off);
        }
        myTextView = (TextView)findViewById(R.id.facebook_state_text);
        if(SettingActivity.isServiceEnable(R.string.FACEBOOK_KEY, getBaseContext())) {
            myTextView.setText(R.string.label_on);
        }
        else {
            myTextView.setText(R.string.label_off);
        }
        myTextView = (TextView)findViewById(R.id.twitter_state_text);
        if(SettingActivity.isServiceEnable(R.string.TWITTER_KEY, getBaseContext())) {
            myTextView.setText(R.string.label_on);
        }
        else {
            myTextView.setText(R.string.label_off);
        }
    }

    // オプションメニューを作成
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean ret = super.onCreateOptionsMenu(menu);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);

        return(ret);
    }

    // オプションメニューの項目が選択された際の動作を設定
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
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

        return(super.onOptionsItemSelected(item));
    }
}