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
    public static final int REQUEST_INPUT_COMMENT = 2;
    public static final int REQUEST_CALL_SETTING = 3;
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

    /***
     * "コメントを投稿する"が有効かどうかを調べる
     *
     * @param context
     *     SharedPreferences取得用のコンテキスト
     * @return 有効ならばtrue、無効ならばfalseを返す
     */
    public static boolean isCommentEnable(Context context) {
        return(PreferenceManager.getDefaultSharedPreferences(context).getBoolean("setting_use_comment", false));
    }

    /***
     * "位置情報を送信する"が有効かどうかを調べる
     *
     * @param context
     *     SharedPreferences取得用のコンテキスト
     * @return 有効ならばtrue、無効ならばfalseを返す
     */
    public static boolean isLocationEnable(Context context) {
        return(PreferenceManager.getDefaultSharedPreferences(context).getBoolean("setting_send_location", false));
    }

    /***
     * "位置情報を送信する"の値を変更する
     *
     * @param inputBool
     *     新しい値
     * @param context
     *     SharedPreferences取得用のコンテキスト
     */
    public static void setLocationEnable(boolean inputBool, Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putBoolean("setting_send_location", inputBool).commit();
    }

    /***
     * 引数で指定されたサービスが有効かどうかを調べる
     *
     * @param inputServiceName
     *     対象とするサービスを定数で指定する
     * @param context
     *     SharedPreferences取得用のコンテキスト
     * @return 有効ならばtrue、無効ならばfalseを返す
     */
    public static boolean isServiceEnable(String inputServiceName, Context context) {
        return(PreferenceManager.getDefaultSharedPreferences(context).getBoolean(inputServiceName, false));
    }

    /***
     * 1つでも有効なサービスがあるかどうかを調べる
     *
     * @param context
     *     SharedPreferences取得用のコンテキスト
     * @return 1つでも有効ならばtrue、全て無効ならばfalseを返す
     */
    public static boolean isAnyServiceEnable(Context context) {
        boolean retBool = false;

        String[] keyList = new String[] {"setting_use_facebook",
                                         "setting_use_twitter"};

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        for(int i = 0; !retBool && i < keyList.length; i++) {
            retBool = prefs.getBoolean(keyList[i], false);
        }

        return(retBool);
    }

    /***
     * データを保存する
     *
     * @param key
     *     保存する値のキー
     * @param value
     *     保存する値
     * @param context
     *     SharedPreferences取得用のコンテキスト
     */
    public static void saveString(String key, String value, Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putString(key, value).commit();
    }

    /***
     * データを読み出す
     *
     * @param key
     *     読み出す値のキー
     * @param context
     *     SharedPreferences取得用のコンテキスト
     * @return keyに対応する値があればそれを返し、なければ空の文字列を返す
     */
    public static String loadString(String key, Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        return(prefs.getString(key, ""));
    }

    /***
     * トークンを配列に格納して返す
     *
     * @param context
     *     SharedPreferences取得用のコンテキスト
     * @return トークンを格納した文字列配列を返す
     */
    public static String[] loadTokens(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        ArrayList<String> retTokens = new ArrayList<String>();

        retTokens.add(prefs.getString("TokenForFacebook", ""));
        retTokens.add(prefs.getString("TokenForTwitter", ""));
        retTokens.add(prefs.getString("TokenSecretForTwitter", ""));

        return(retTokens.toArray(new String[0]));
    }

    /***
     * コンストラクタ
     * インスタンスが生成されないようにprivate宣言しておく
     */
    private Library() {}
}