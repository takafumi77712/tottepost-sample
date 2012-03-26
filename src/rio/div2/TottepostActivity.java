package rio.div2;

import rio.div2.Library;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.facebook.android.AsyncFacebookRunner;
import com.facebook.android.Facebook;
import com.facebook.android.FacebookError;

import twitter4j.StatusUpdate;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;

public class TottepostActivity extends Activity {
    // 変数の宣言
    private Handler mHandler;
    private Uri destUri;

    private Facebook mFacebook;
    private AsyncFacebookRunner mAsyncRunner;

    // 配列の宣言
    private String[] tokens;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mHandler = new Handler();

        Button postFromCameraButton = (Button)findViewById(R.id.post_from_camera);
        postFromCameraButton.setOnClickListener(new AdapterView.OnClickListener() {
           @Override
           public void onClick(View v) {
               closeInputPanel(v);
               callCamera();
           }
        });

        Button postFromGalleryButton = (Button)findViewById(R.id.post_from_gallery);
        postFromGalleryButton.setOnClickListener(new AdapterView.OnClickListener() {
           @Override
           public void onClick(View v) {
               closeInputPanel(v);
               callGallery();
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
        mAsyncRunner = new AsyncFacebookRunner(mFacebook);
        
        if(Library.isAnyServiceEnable(getApplicationContext())) {
            Button postFromCameraButton = (Button)findViewById(R.id.post_from_camera);
            postFromCameraButton.setEnabled(true);

            Button postFromGalleryButton = (Button)findViewById(R.id.post_from_gallery);
            postFromGalleryButton.setEnabled(true);
        }
        else {
            Button postFromCameraButton = (Button)findViewById(R.id.post_from_camera);
            postFromCameraButton.setEnabled(false);

            Button postFromGalleryButton = (Button)findViewById(R.id.post_from_gallery);
            postFromGalleryButton.setEnabled(false);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch(requestCode) {
        case Library.REQUEST_IMAGE:
            if(resultCode == Activity.RESULT_OK) {
                if(data != null && data.getData() != null) {
                    postMessage(data.getData());
                }
                else {
                    postMessage(destUri);
                }
            }

            break;
        default:
            break;
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

    // 入力パネルを閉じる
    public void closeInputPanel(View v) {
        InputMethodManager mInputMethodManager = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        mInputMethodManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
    }

    // カメラを呼び出す
    public void callCamera() {
        // 保存先のディレクトリを作成
        File baseDir = new File(Environment.getExternalStorageDirectory(), "Tottepost");
        try {
            if(!baseDir.exists() && !baseDir.mkdirs()) {
                Toast.makeText(getApplicationContext(), "保存用ディレクトリの作成に失敗しました。", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        
        /***
         * カメラで撮影した画像の保存先を明示する
         * 参考:カメラやギャラリーピッカーからの画像取得周りまとめ2｜いろいろ備忘録
         *     http://ameblo.jp/yolluca/entry-10895298488.html
         */
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddkkmmss");
        String destName = baseDir.getAbsolutePath() + "/Tottepost_" + dateFormat.format(new Date()) + ".jpg";
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, destName);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        destUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        
        Intent mIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        mIntent.putExtra(MediaStore.EXTRA_OUTPUT, destUri);
        startActivityForResult(mIntent, Library.REQUEST_IMAGE);
    }

    // ギャラリーを呼び出す
    public void callGallery() {
        Intent mIntent = new Intent();
        mIntent.setType("image/*");
        mIntent.setAction(Intent.ACTION_PICK);
        startActivityForResult(mIntent, Library.REQUEST_IMAGE);
    }

    // メッセージを投稿する
    public void postMessage(Uri inputUri) {
        EditText messageBox = (EditText)findViewById(R.id.message);
        String message = messageBox.getText().toString();
        if(inputUri != null) {
            Toast.makeText(getApplicationContext(), getResources().getString(R.string.post_message_before), Toast.LENGTH_SHORT).show();
            if(Library.isServiceEnable("setting_use_facebook", getApplicationContext())) {
                // Facebook用の処理
                if(tokens[Library.TOKEN_FOR_FACEBOOK].length() != 0) {
                    // アクセストークンの取得に成功したら投稿を行う
                    ImagePostTask imagePost = new ImagePostTask(message, inputUri, Library.FACEBOOK);
                    imagePost.execute();
                }
                else {
                    Toast.makeText(getApplicationContext(),
                                   getResources().getString(R.string.service_name_facebook) + getResources().getString(R.string.post_login_failed),
                                   Toast.LENGTH_SHORT).show();
                }
            }
            if(Library.isServiceEnable("setting_use_twitter", getApplicationContext())) {
                // Twitter用の処理
                if(tokens[Library.TOKEN_FOR_TWITTER].length() != 0 && tokens[Library.TOKEN_SECRET_FOR_TWITTER].length() != 0) {
                    // アクセストークンの取得に成功したら投稿を行う
                    ImagePostTask imagePost = new ImagePostTask(message, inputUri, Library.TWITTER);
                    imagePost.execute();
                }
                else {
                    Toast.makeText(getApplicationContext(),
                                   getResources().getString(R.string.service_name_twitter) + getResources().getString(R.string.post_login_failed),
                                   Toast.LENGTH_SHORT).show();
                }
            }
            messageBox.setText("");
        }
        else {
            Toast.makeText(getApplicationContext(), getResources().getString(R.string.post_image_empty), Toast.LENGTH_SHORT).show();
        }
    }

    class ImagePostTask extends AsyncTask<Void, Void, Void> {
        private String message;
        private Uri imageUri;
        private int target;

        public ImagePostTask(String inputMessage, Uri inputUri, int inputTarget) {
            message = inputMessage;
            imageUri = inputUri;
            target = inputTarget;
        }

        @Override
        public Void doInBackground(Void... params) {
            ContentResolver resolver = getContentResolver();
            switch(target) {
            case Library.FACEBOOK:
                /***
                 * Facebookに画像を投稿する
                 * 参考:インドＩＴ留学メモ: AndroidでFacebook
                 *     http://blog-indiait.blogspot.jp/2011/06/androidfacebook.html
                 */
                byte[] picture = null;
                try {
                    Bitmap mBitmap = MediaStore.Images.Media.getBitmap(resolver, imageUri);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    String type = resolver.getType(imageUri);
                    if(type.equals("image/png")) {
                        mBitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
                    }
                    else {
                        mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                    }
                    picture = baos.toByteArray();
                    baos.close();
                }
                catch(Exception e) {
                    e.printStackTrace();
                }
                Bundle mBundle = new Bundle();
                mBundle.putString("method", "photos.upload");
                mBundle.putByteArray("photo", picture);
                mBundle.putString("caption", message);
                mAsyncRunner.request(null, mBundle, "POST", new ImagePostRequestListener(), null);

                break;
            case Library.TWITTER:
                /***
                 * Twitterにメッセージを投稿する
                 * 参考:androidとか日記: Twitter4j-2.2.xを使ったツイートのコーディング例
                 *      http://blog.kyosuke25.com/2011/12/twitter4j-22x.html
                 *
                 * Twitterに画像を投稿する
                 * 参考:Twitter4Jで画像をアップロード << ぜんのホームページ
                 *     http://zenjiro.wordpress.com/2011/11/28/upload-image-with-twitter4j/
                 */
                Twitter mTwitter = new TwitterFactory().getInstance();
                mTwitter.setOAuthConsumer(Library.CS_KEY_FOR_TWITTER, Library.CS_SECRET_FOR_TWITTER);
                mTwitter.setOAuthAccessToken(new AccessToken(tokens[Library.TOKEN_FOR_TWITTER], tokens[Library.TOKEN_SECRET_FOR_TWITTER]));
                StatusUpdate status = new StatusUpdate(message);
                Cursor mCursor = resolver.query(imageUri, null, null, null, null);
                mCursor.moveToFirst();
                File image = new File(mCursor.getString(mCursor.getColumnIndex(MediaStore.MediaColumns.DATA)));
                status.media(image);

                try {
                    mTwitter.updateStatus(status);
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(),
                                           getResources().getString(R.string.service_name_twitter) + getResources().getString(R.string.post_message_after),
                                           Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                catch (TwitterException e) {
                    e.printStackTrace();
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(),
                                    getResources().getString(R.string.service_name_twitter) + getResources().getString(R.string.post_message_post_failed),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                break;
            default:
                break;
            }

            return(null);
        }
    }

    class ImagePostRequestListener implements AsyncFacebookRunner.RequestListener {
        @Override
        public void onComplete(String response, Object state) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(),
                                   getResources().getString(R.string.service_name_facebook) + getResources().getString(R.string.post_message_after),
                                   Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public void onMalformedURLException(MalformedURLException e, Object state) {}

        @Override
        public void onIOException(IOException e, Object state) {}

        @Override
        public void onFileNotFoundException(FileNotFoundException e, Object state) {}

        @Override
        public void onFacebookError(FacebookError e, Object state) {}
    }
}