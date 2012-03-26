package rio.div2;

import rio.div2.Library;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;

import com.facebook.android.DialogError;
import com.facebook.android.Facebook;
import com.facebook.android.FacebookError;

public class SettingActivity extends PreferenceActivity {
    // 変数の宣言
    private Facebook mFacebook;

    // 配列の宣言
    private String[] tokens;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preference);
        
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
    }
    
    @Override
    public void onResume() {
        super.onResume();

        tokens = new String[3];
        tokens[Library.TOKEN_FOR_FACEBOOK] = Library.loadData("TokenForFacebook", getApplicationContext());
        tokens[Library.TOKEN_FOR_TWITTER] = Library.loadData("TokenForTwitter", getApplicationContext());
        tokens[Library.TOKEN_SECRET_FOR_TWITTER] = Library.loadData("TokenSecretForTwitter", getApplicationContext());

        mFacebook = new Facebook(Library.APPID_FOR_FACEBOOK);
        mFacebook.setAccessToken(tokens[Library.TOKEN_FOR_FACEBOOK]);
        
        CheckBoxPreference checkBoxForFacebook = (CheckBoxPreference)findPreference("setting_use_facebook");
        if(tokens[Library.TOKEN_FOR_FACEBOOK].length() != 0) {
            // Facebookのアクセストークンがあればチェックボックスを有効に
            checkBoxForFacebook.setEnabled(true);
        }
        CheckBoxPreference checkBoxForTwitter = (CheckBoxPreference)findPreference("setting_use_twitter");
        if(tokens[Library.TOKEN_FOR_TWITTER].length() != 0 && tokens[Library.TOKEN_SECRET_FOR_TWITTER].length() != 0) {
            // Twitterのアクセストークンがあればチェックボックスを有効に
            checkBoxForTwitter.setEnabled(true);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch(requestCode) {
        case Library.REQUEST_FACEBOOK_OAUTH:
            if(resultCode == Activity.RESULT_OK) {
                mFacebook.authorizeCallback(requestCode, resultCode, data);
                Library.saveData("TokenForFacebook", data.getStringExtra(Facebook.TOKEN), getApplicationContext());
                tokens[Library.TOKEN_FOR_FACEBOOK] = Library.loadData("TokenForFacebook", getApplicationContext());
                mFacebook.setAccessToken(tokens[Library.TOKEN_FOR_FACEBOOK]);
                
                if(tokens[Library.TOKEN_FOR_FACEBOOK].length() != 0) {
                    // Facebookのアクセストークンがあればチェックボックスを有効に
                    CheckBoxPreference checkBoxForFacebook = (CheckBoxPreference)findPreference("setting_use_facebook");
                    checkBoxForFacebook.setEnabled(true);
                }
            }

            break;
        case Library.REQUEST_TWITTER_OAUTH:
            if(resultCode == Activity.RESULT_OK) {
                Library.saveData("TokenForTwitter", data.getStringExtra(OAuthActivity.TOKEN), getApplicationContext());
                Library.saveData("TokenSecretForTwitter", data.getStringExtra(OAuthActivity.TOKEN_SECRET), getApplicationContext());
                tokens[Library.TOKEN_FOR_TWITTER] = Library.loadData("TokenForTwitter", getApplicationContext());
                tokens[Library.TOKEN_SECRET_FOR_TWITTER] = Library.loadData("TokenSecretForTwitter", getApplicationContext());
                
                if(tokens[Library.TOKEN_FOR_TWITTER].length() != 0) {
                    // Twitterのアクセストークンがあればチェックボックスを有効に
                    CheckBoxPreference checkBoxForTwitter = (CheckBoxPreference)findPreference("setting_use_twitter");
                    checkBoxForTwitter.setEnabled(true);
                }
            }

            break;
        default:
            break;
        }
    }

    class LoginDialogListener implements Facebook.DialogListener {
        @Override
        public void onComplete(Bundle values) {
            Library.saveData("TokenForFacebook", values.getString(Facebook.TOKEN), getApplicationContext());
            tokens[Library.TOKEN_FOR_FACEBOOK] = values.getString(Facebook.TOKEN);
            mFacebook.setAccessToken(tokens[Library.TOKEN_FOR_FACEBOOK]);
        }

        @Override
        public void onFacebookError(FacebookError e) {}

        @Override
        public void onError(DialogError e) {}

        @Override
        public void onCancel() {}
    }
}