package rio.div2;

import rio.div2.Library;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.widget.ListView;

import com.facebook.android.DialogError;
import com.facebook.android.Facebook;
import com.facebook.android.FacebookError;

public class SettingActivity extends PreferenceActivity {
    // 変数の宣言
    private Facebook mFacebook;
    private SharedPreferences.OnSharedPreferenceChangeListener listener;

    // 配列の宣言
    private String[] tokens;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preference);

        // Facebookの関連項目にリスナを登録する
        Preference mPreference = (Preference)findPreference("setting_regist_facebook");
        mPreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                // Facebookの認証を行う
                mFacebook.authorize(SettingActivity.this,
                                    new String[] {"offline_access", "publish_stream"},
                                    Library.REQUEST_FACEBOOK_OAUTH, new LoginDialogListener());
                
                return(true);
            }
        });
        mPreference = (Preference)findPreference("setting_delete_facebook");
        mPreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                // 登録情報を本当に削除するか尋ねる
                AlertDialog.Builder builder = new AlertDialog.Builder(SettingActivity.this);
                builder.setIcon(android.R.drawable.ic_dialog_alert);
                builder.setTitle(getString(R.string.setting_delete_title));
                builder.setMessage(getString(R.string.service_name_facebook)
                                   + getString(R.string.setting_delete_message));
                builder.setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Facebookのアクセストークンを削除する
                        Library.saveData("TokenForFacebook", "", getApplicationContext());
                        tokens[Library.TOKEN_FOR_FACEBOOK] = "";
    
                        updateUIForFacebook();
                    }
                });
                builder.setNegativeButton(getString(R.string.no), null);
                builder.setCancelable(true);
                builder.create().show();
                
                return(true);
            }
        });
        
        // Twitterの関連項目にリスナを追加する
        mPreference = (Preference)findPreference("setting_regist_twitter");
        mPreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                // Twitterの認証を行う
                Intent mIntent = new Intent(SettingActivity.this, OAuthActivity.class);
                mIntent.putExtra(OAuthActivity.CALLBACK, Library.CALLBACK_URL_FOR_TWITTER);
                mIntent.putExtra(OAuthActivity.CONSUMER_KEY, Library.CS_KEY_FOR_TWITTER);
                mIntent.putExtra(OAuthActivity.CONSUMER_SECRET, Library.CS_SECRET_FOR_TWITTER);
                startActivityForResult(mIntent, Library.REQUEST_TWITTER_OAUTH);
                
                return(true);
            }
        });
        mPreference = (Preference)findPreference("setting_delete_twitter");
        mPreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                // 登録情報を本当に削除するか尋ねる
                AlertDialog.Builder builder = new AlertDialog.Builder(SettingActivity.this);
                builder.setIcon(android.R.drawable.ic_dialog_alert);
                builder.setTitle(getString(R.string.setting_delete_title));
                builder.setMessage(getString(R.string.service_name_twitter)
                                   + getString(R.string.setting_delete_message));
                builder.setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Twitterのアクセストークンを削除する
                        Library.saveData("TokenForTwitter", "", getApplicationContext());
                        Library.saveData("TokenSecretForTwitter", "", getApplicationContext());
                        tokens[Library.TOKEN_FOR_TWITTER] = "";
                        tokens[Library.TOKEN_SECRET_FOR_TWITTER] = "";

                        updateUIForTwitter();
                    }
                });
                builder.setNegativeButton(getString(R.string.no), null);
                builder.setCancelable(true);
                builder.create().show();
                
                return(true);
            }
        });
        
        listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if(key.equals("setting_use_facebook")) {
                    updateUIForFacebook();
                }
                else if(key.equals("setting_use_twitter")) {
                    updateUIForTwitter();
                }
            }
        };
    }
    
    @Override
    public void onResume() {
        super.onResume();
        
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        prefs.registerOnSharedPreferenceChangeListener(listener);
        
        tokens = Library.loadTokens(getApplicationContext());
        
        mFacebook = new Facebook(Library.APPID_FOR_FACEBOOK);
        mFacebook.setAccessToken(tokens[Library.TOKEN_FOR_FACEBOOK]);

        updateUI();
    }
    
    @Override
    public void onPause() {
        super.onPause();
        
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        prefs.unregisterOnSharedPreferenceChangeListener(listener);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch(requestCode) {
        case Library.REQUEST_FACEBOOK_OAUTH:
            if(resultCode == Activity.RESULT_OK) {
                mFacebook.authorizeCallback(requestCode, resultCode, data);
                if(data.getStringExtra(Facebook.TOKEN) != null) {
                    Library.saveData("TokenForFacebook", data.getStringExtra(Facebook.TOKEN), getApplicationContext());
                    tokens[Library.TOKEN_FOR_FACEBOOK] = data.getStringExtra(Facebook.TOKEN);
                    mFacebook.setAccessToken(tokens[Library.TOKEN_FOR_FACEBOOK]);
                }
                else {
                    Library.saveData("TokenForFacebook", "", getApplicationContext());
                    tokens[Library.TOKEN_FOR_FACEBOOK] = "";
                }

                updateUIForFacebook();
            }

            break;
        case Library.REQUEST_TWITTER_OAUTH:
            if(resultCode == Activity.RESULT_OK) {
                if(data.getStringExtra(OAuthActivity.TOKEN) != null
                   && data.getStringExtra(OAuthActivity.TOKEN_SECRET) != null) {
                    Library.saveData("TokenForTwitter", data.getStringExtra(OAuthActivity.TOKEN), getApplicationContext());
                    Library.saveData("TokenSecretForTwitter", data.getStringExtra(OAuthActivity.TOKEN_SECRET), getApplicationContext());
                    tokens[Library.TOKEN_FOR_TWITTER] = data.getStringExtra(OAuthActivity.TOKEN);
                    tokens[Library.TOKEN_SECRET_FOR_TWITTER] = data.getStringExtra(OAuthActivity.TOKEN_SECRET);
                }
                else {
                    Library.saveData("TokenForTwitter", "", getApplicationContext());
                    Library.saveData("TokenSecretForTwitter", "", getApplicationContext());
                    tokens[Library.TOKEN_FOR_TWITTER] = "";
                    tokens[Library.TOKEN_SECRET_FOR_TWITTER] = "";
                }

                updateUIForTwitter();
            }

            break;
        default:
            break;
        }
    }
    
    /***
     * 各項目の表示を更新する
     */
    public void updateUI() {
        updateUIForFacebook();
        updateUIForTwitter();
    }
    
    /***
     * Facebookの関連項目の表示を更新する
     */
    public void updateUIForFacebook() {
        if(tokens[Library.TOKEN_FOR_FACEBOOK].length() != 0) {
            // Facebookのアクセストークンがある場合
            CheckBoxPreference mCheckBox = (CheckBoxPreference)findPreference("setting_use_facebook");
            mCheckBox.setEnabled(true);
            
            /***
             * summaryを動的に変更
             * 参考:Y.A.M の 雑記帳: Android&#12288;Preference の summary を動的に変更
             *      http://y-anz-m.blogspot.jp/2010/07/androidpreference-summary.html
             */
            Preference mPreference = (Preference)findPreference("setting_facebook");
            if(mCheckBox.isChecked()) {
                mPreference.setSummary(getString(R.string.setting_post_enable));
            }
            else {
                mPreference.setSummary(getString(R.string.setting_post_disable));
            }
            
            mPreference = (Preference)findPreference("setting_delete_facebook");
            mPreference.setEnabled(true);
        }
        else {
            // Facebookのアクセストークンがない場合
            CheckBoxPreference mCheckBox = (CheckBoxPreference)findPreference("setting_use_facebook");
            mCheckBox.setChecked(false);
            mCheckBox.setEnabled(false);

            Preference mPreference = (Preference)findPreference("setting_facebook");
            mPreference.setSummary(getString(R.string.setting_not_registed));
            
            mPreference = (Preference)findPreference("setting_delete_facebook");
            mPreference.setEnabled(false);
        }
        
        ListView view = this.getListView();
        view.invalidateViews();
    }
    
    /***
     * Twitterの関連項目の表示を更新する
     */
    public void updateUIForTwitter() {
        if(tokens[Library.TOKEN_FOR_TWITTER].length() != 0 && tokens[Library.TOKEN_SECRET_FOR_TWITTER].length() != 0) {
            // Twitterのアクセストークンがある場合
            CheckBoxPreference mCheckBox = (CheckBoxPreference)findPreference("setting_use_twitter");
            mCheckBox.setEnabled(true);
            
            Preference mPreference = (Preference)findPreference("setting_twitter");
            if(mCheckBox.isChecked()) {
                mPreference.setSummary(getString(R.string.setting_post_enable));
            }
            else {
                mPreference.setSummary(getString(R.string.setting_post_disable));
            }
            
            mPreference = (Preference)findPreference("setting_delete_twitter");
            mPreference.setEnabled(true);
        }
        else {
            // Twitterのアクセストークンがない場合
            CheckBoxPreference mCheckBox = (CheckBoxPreference)findPreference("setting_use_twitter");
            mCheckBox.setChecked(false);
            mCheckBox.setEnabled(false);

            Preference mPreference = (Preference)findPreference("setting_twitter");
            mPreference.setSummary(getString(R.string.setting_not_registed));
            
            mPreference = (Preference)findPreference("setting_delete_twitter");
            mPreference.setEnabled(false);
        }
        
        ListView view = this.getListView();
        view.invalidateViews();
    }

    /***
     * DialogActivityを使ってFacebookにログインする際のリスナクラス
     */
    class LoginDialogListener implements Facebook.DialogListener {
        @Override
        public void onComplete(Bundle values) {
            if(values.getString(Facebook.TOKEN) != null) {
                Library.saveData("TokenForFacebook", values.getString(Facebook.TOKEN), getApplicationContext());
                tokens[Library.TOKEN_FOR_FACEBOOK] = values.getString(Facebook.TOKEN);
                mFacebook.setAccessToken(tokens[Library.TOKEN_FOR_FACEBOOK]);
            }
            else {
                Library.saveData("TokenForFacebook", "", getApplicationContext());
                tokens[Library.TOKEN_FOR_FACEBOOK] = "";
            }

            updateUIForFacebook();
        }

        @Override
        public void onFacebookError(FacebookError e) {}

        @Override
        public void onError(DialogError e) {}

        @Override
        public void onCancel() {}
    }
}