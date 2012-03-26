package rio.div2;

import rio.div2.Library;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.SurfaceHolder.Callback;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
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
    
    private Button captureButton;

    // 配列の宣言
    private String[] tokens;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mHandler = new Handler();
        
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        final CameraView ccd = new CameraView(this);
        // カメラ画面
        LinearLayout cameraView = new LinearLayout(this);
        WindowManager wm = (WindowManager)getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        cameraView.addView(ccd, new LinearLayout.LayoutParams(display.getWidth() - 100,
                                                              display.getHeight()));

        // ボタン画面
        LinearLayout buttonView = new LinearLayout(this);
        // buttonView.setOrientation(LinearLayout.VERTICAL);
        cameraView.addView(buttonView);
        setContentView(cameraView);

        captureButton = new Button(this);
        captureButton.setText(getResources().getString(R.string.main_label_capture));
        captureButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                captureButton.setEnabled(false);
                ccd.capture();
            }
        });
        buttonView.addView(captureButton, new LinearLayout.LayoutParams(100, display.getHeight()));
    }
    
    @Override
    public void onResume() {
        super.onResume();

        // トークンを読み込み
        tokens = Library.loadTokens(getApplicationContext());

        // Facebook用のインスタンスを生成
        mFacebook = new Facebook(Library.APPID_FOR_FACEBOOK);
        mFacebook.setAccessToken(tokens[Library.TOKEN_FOR_FACEBOOK]);
        mAsyncRunner = new AsyncFacebookRunner(mFacebook);
        
        destUri = null;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch(requestCode) {
        case Library.REQUEST_IMAGE_FROM_CAMERA:
            // カメラから画像を取得した場合
            if(resultCode == Activity.RESULT_OK) {
                Uri mUri = destUri;
                destUri = null;
                if(data != null && data.getData() != null) {
                    // data.getData()でUriが取得できた場合はそれを使う
                    postMessage(data.getData());
                }
                else {
                    // 取得できなければ用意しておいたUriを使う
                    postMessage(mUri);
                }
            }
            else if(resultCode == Activity.RESULT_CANCELED) {
                getContentResolver().delete(destUri, null, null);
            }

            break;
        case Library.REQUEST_IMAGE_FROM_GALLERY:
            // ギャラリーから画像を取得した場合
            if(resultCode == Activity.RESULT_OK) {
                postMessage(data.getData());
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
                Toast.makeText(getApplicationContext(), getResources().getString(R.string.error_make_directory_failed), Toast.LENGTH_SHORT).show();
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
        String fileName = "Tottepost_" + dateFormat.format(new Date()) + ".jpg";
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, fileName);
        values.put("_data", baseDir.getAbsolutePath() + "/" + fileName);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        destUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        
        Intent mIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        mIntent.putExtra(MediaStore.EXTRA_OUTPUT, destUri);
        startActivityForResult(mIntent, Library.REQUEST_IMAGE_FROM_CAMERA);
    }

    // ギャラリーを呼び出す
    public void callGallery() {
        Intent mIntent = new Intent();
        mIntent.setType("image/*");
        mIntent.setAction(Intent.ACTION_PICK);
        startActivityForResult(mIntent, Library.REQUEST_IMAGE_FROM_GALLERY);
    }

    // メッセージを投稿する
    public void postMessage(Uri inputUri) {
        // EditText messageBox = (EditText)findViewById(R.id.message);
        String message = ""; // messageBox.getText().toString();
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
            // messageBox.setText("");
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

                boolean complete = false;
                int count = 0;
                // 投稿に成功するか5回までリトライする
                while(!complete && count < 5) {
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
                        complete = true;
                    }
                    catch (TwitterException e) {
                        e.printStackTrace();
                        count++;
                    }
                }
                if(!complete) {
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

    // カメラ内部class
    public class CameraView extends SurfaceView implements Callback, PictureCallback {
        private Camera camera = null;
        private File baseDir;

        public CameraView(Context context) {
            super(context);
            SurfaceHolder holder = getHolder();
            holder.addCallback(this);
            holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
            
            //  保存先のディレクトリを作成
            baseDir = new File(Environment.getExternalStorageDirectory(), "Tottepost");
            try {
                if(!baseDir.exists() && !baseDir.mkdirs()) {
                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.error_make_directory_failed), Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
            catch(Exception e) {
                e.printStackTrace();
            }
        }

        // surface起動時の処理
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            try {
                camera = Camera.open();
                camera.setPreviewDisplay(holder);
                // camera.setDisplayOrientation(90);
            }
            catch(IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int f, int w, int h) {
            Camera.Parameters p = camera.getParameters();
            // p.setPreviewSize(w,h);
            camera.setParameters(p);
            camera.startPreview();
        }

        // surface終了時の処理
        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            camera.setPreviewCallback(null); 
            camera.stopPreview();
            camera.release();
            camera = null; 
        }

        // 撮影後処理と画像の保存
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            if(Library.isAnyServiceEnable(TottepostActivity.this)) {
                // 1つ以上のサービスが有効になっている場合は撮影した画像を投稿する
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddkkmmss");
                String fileName = "Tottepost_" + dateFormat.format(new Date()) + ".jpg";
                String destPath = baseDir.getAbsolutePath() + "/" + fileName;
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.TITLE, fileName);
                values.put("_data", destPath);
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                destUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                
                FileOutputStream fos = null;
                // SDカードへ出力
                try {
                    fos = new FileOutputStream(destPath);
                }
                catch(FileNotFoundException e) {
                    e.printStackTrace();
                }
    
                if(fos != null){
                    try {
                        fos.write(data);
                        fos.close();
                        fos = null;
                    }
                    catch(IOException e) {
                        getContentResolver().delete(destUri, null, null);
                        e.printStackTrace();
                    }
                }
                
                postMessage(destUri);
            }
            else {
                // 全てのサービスが無効になっている場合は撮影した画像を投稿しない
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), getResources().getString(R.string.error_no_service_selected), Toast.LENGTH_SHORT).show();
                    }
                });
            }

            // プレビュー再開
            camera.startPreview();
            captureButton.setEnabled(true);
        }

        // プレビュー画面をタッチしたときの動作
        // ハードの決定ボタン含みます
        @Override
        public boolean onTouchEvent(MotionEvent me) {
            if(me.getAction() == MotionEvent.ACTION_DOWN) {
                // xperiaのみ
                // 端末ごとにオートフォーカスの使用方法が違うため、オートフォーカスでエラーを吐く場合、
                // autoFocus();をコメントアウトしてください。近日中に改善予定。
                // autoFocus();
                // camera.takePicture(null, null, this);
            }
            return(true);
        }

        // main activityでの撮影ボタンの動作
        public boolean capture() {
            // xperiaのみ
            autoFocus();
            camera.takePicture(null, null, this);
            return(true);
        }

        // オートフォーカス
        public void autoFocus(){
            if(camera != null){
                camera.autoFocus(new Camera.AutoFocusCallback() {
                    @Override
                    public void onAutoFocus(boolean success, Camera camera){
                        camera.autoFocus(null);    
                    }
                });
            }
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