package rio.div2;

import java.util.ArrayList;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class Library {
    // 定数の宣言
    // リクエストコード
    public static final int REQUEST_IMAGE_FROM_CAMERA  = 0;
    public static final int REQUEST_IMAGE_FROM_GALLERY = 1;
    public static final int REQUEST_FACEBOOK_OAUTH = 10;
    public static final int REQUEST_TWITTER_OAUTH  = 11;
    // トークンの添字
    public static final int TOKEN_FOR_FACEBOOK = 0;
    public static final int TOKEN_FOR_TWITTER = 1;
    public static final int TOKEN_SECRET_FOR_TWITTER = 2;
    // Facebook用
    public static final int FACEBOOK = 0;
    public static final String APPID_FOR_FACEBOOK = "206421902773102";
    // Twitter用
    public static final int TWITTER = 1;
    public static final String CALLBACK_URL_FOR_TWITTER = "http://www.google.co.jp/";
    public static final String CS_KEY_FOR_TWITTER = "KZkiTK9pcyuGJFzZplIw";
    public static final String CS_SECRET_FOR_TWITTER = "Kn0pibiqjV4MsbXMTQJh4Muo5WbQalvelr0NsT5liXw";
    
    // コメントが有効であるかそうでないかを返す
    public static boolean isCommentEnable(Context context) {
        return(PreferenceManager.getDefaultSharedPreferences(context).getBoolean("setting_use_comment", false));
    }
    
    // 位置情報が有効であるかそうでないかをを返す
    public static boolean isLocationEnable(Context context) {
        return(PreferenceManager.getDefaultSharedPreferences(context).getBoolean("setting_send_location", false));
    }
    
    // 引数で指定されたサービスが有効であるかそうでないかをを返す
    public static boolean isServiceEnable(String inputServiceName, Context context) {
        return(PreferenceManager.getDefaultSharedPreferences(context).getBoolean(inputServiceName, false));
    }
    
    // 1つでも有効なサービスがあるかどうかを調べる
    public static boolean isAnyServiceEnable(Context context) {
        boolean retBool = false;
        
        String[] keyList = new String[] {"setting_use_facebook",
                                         "setting_use_twitter"};
        
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        for(int i = 0; !retBool && i < keyList.length; i++) {
            retBool = preferences.getBoolean(keyList[i], false);
        }
        
        return(retBool);
    }

    // データを保存する
    public static void saveData(String key, String value, Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putString(key, value).commit();
    }

    // データを読み出す
    public static String loadData(String key, Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        return(prefs.getString(key, ""));
    }
    
    // トークンを配列に格納して返す
    public static String[] loadTokens(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        ArrayList<String> retTokens = new ArrayList<String>();
        
        retTokens.add(prefs.getString("TokenForFacebook", ""));
        retTokens.add(prefs.getString("TokenForTwitter", ""));
        retTokens.add(prefs.getString("TokenSecretForTwitter", ""));
        
        return(retTokens.toArray(new String[0]));
    }
    
    private Library() {}
}