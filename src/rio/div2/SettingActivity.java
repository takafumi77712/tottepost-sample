package rio.div2;

import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class SettingActivity extends PreferenceActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preference);
    }

    // コメントが有効であるかそうでないかを返す
    public static boolean isCommentEnable(Context context) {
        return(PreferenceManager.getDefaultSharedPreferences(context).getBoolean("setting_use_comment", false));
    }

    // 位置情報が有効であるかそうでないかをを返す
    public static boolean isLocationEnable(Context context) {
        return(PreferenceManager.getDefaultSharedPreferences(context).getBoolean("setting_send_location", false));
    }

    // 引数で指定されたサービスが有効であるかそうでないかをを返す
    public static boolean isServiceEnable(int serviceNameID, Context context) {
        String serviceName = "";

        serviceName = context.getResources().getString(serviceNameID);

        return(PreferenceManager.getDefaultSharedPreferences(context).getBoolean(serviceName, false));
    }
}