package rio.div2;

import rio.div2.Library;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.Toast;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.View.OnTouchListener;
import android.view.inputmethod.InputMethodManager;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.facebook.android.AsyncFacebookRunner;
import com.facebook.android.AsyncFacebookRunner.RequestListener;
import com.facebook.android.Facebook;
import com.facebook.android.FacebookError;

import twitter4j.StatusUpdate;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;

public class TottepostActivity extends Activity implements SurfaceHolder.Callback, Camera.PictureCallback {
    // 変数の宣言
    // mHandler - 他スレッドからのUIの更新に使用
    private Handler mHandler;
    // capturing - 撮影中かどうか
    // true:撮影中 false:非撮影中
    private boolean capturing;
    // focused - オートフォーカス済みかどうか
    // true:オートフォーカス済み false:オートフォーカス前
    private boolean focused;
    // focusing - オートフォーカス中かどうか
    // true:オートフォーカス中 false:非オートフォーカス中
    private boolean focusing;
    // uploading - アップロード中かどうか
    // true:アップロード中 false:非アップロード中
    private boolean uploading;
    // baseDir - 保存用ディレクトリ
    private File baseDir;
    // captureButton - 撮影ボタン
    private Button captureButton;
    // preview - プレビュー部分
    private SurfaceView preview;
    // mCamera - カメラのインスタンス
    private Camera mCamera;
    private LocationManager lManager;
    private Location mLocation;
    private LocationListener lListener;

    private Facebook mFacebook;
    private AsyncFacebookRunner mAsyncRunner;

    // 配列の宣言
    private String[] tokens;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mHandler = new Handler();

        /***
         * スリープを無効にする
         * 参考:画面をスリープ状態にさせないためには - 逆引きAndroid入門
         *      http://www.adakoda.com/android/000207.html
         */
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.main);

        /***
         * Keep Aliveを無効にする
         * 参考:Broken pipe exception - Twitter4J J | Google グループ
         *      http://groups.google.com/group/twitter4j-j/browse_thread/thread/56b18baac1846ab2?pli=1
         */
        System.setProperty("http.keepAlive", "false");
        System.setProperty("https.keepAlive", "false");

        capturing = false;
        focused = false;
        focusing = false;
        uploading = false;

        // 保存用ディレクトリの作成
        baseDir = new File(Environment.getExternalStorageDirectory(), "tottepost");
        try {
            if(!baseDir.exists() && !baseDir.mkdirs()) {
                Toast.makeText(getApplicationContext(), getString(R.string.error_make_directory_failed), Toast.LENGTH_SHORT).show();
                finish();
            }
        }
        catch(Exception e) {
            e.printStackTrace();
        }

        captureButton = (Button)findViewById(R.id.capture);
        captureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!capturing) {
                    capturing = true;
                    captureButton.setEnabled(false);
                    mCamera.takePicture(null, null, null, TottepostActivity.this);
                    /*
                    if(focused) {
                        // オートフォーカス済みであればそのまま撮影
                        mCamera.takePicture(TottepostActivity.this, null, null, TottepostActivity.this);
                    }
                    else {
                        // オートフォーカス前であればオートフォーカス後に撮影
                        mCamera.autoFocus(new Camera.AutoFocusCallback() {
                            @Override
                            public void onAutoFocus(boolean success, Camera camera) {
                                // 一部端末では、プレビューを止めた状態での撮影が行えないようなので、
                                // プレビューを止めずに撮影する
                                // mCamera.stopPreview();
                                try {
                                    Thread.sleep(300);
                                }
                                catch(Exception e) {
                                    e.printStackTrace();
                                }
                                mCamera.takePicture(TottepostActivity.this, null, null, TottepostActivity.this);
                            }
                        });
                    }
                    */
                }
            }
        });

        preview = (SurfaceView)findViewById(R.id.preview);
        preview.getHolder().addCallback(TottepostActivity.this);
        preview.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        preview.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // 画面がタッチされたらオートフォーカスを実行
                if(!focusing) {
                    // オートフォーカス中でなければオートフォーカスを実行
                    // フラグを更新
                    focusing = true;
                    
                    captureButton.setEnabled(false);
                    mCamera.autoFocus(new Camera.AutoFocusCallback() {
                        @Override
                        public void onAutoFocus(boolean success, Camera camera) {
                            focusing = false;
                            captureButton.setEnabled(true);
                        }
                    });
                    focused = true;
                }

                return(false);
            }
        });
        
        lListener = new MyLocationListener();
        lManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        mLocation = lManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
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

        surfaceChanged(preview.getHolder(), 0, 0, 0);
        
        if(!Library.isAnyServiceEnable(getApplicationContext())) {
            /***
             * 投稿先のサービスが1つも選択されていなければ、撮影ボタンを無効にしてダイアログを表示する
             * 参考:アラートダイアログ(AlertDialog)を使用するには - 逆引きAndroid入門
             *      http://www.adakoda.com/android/000083.html
             */
            captureButton.setEnabled(false);
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setIcon(android.R.drawable.ic_dialog_alert);
            builder.setTitle(getString(R.string.error_title));
            builder.setMessage(getString(R.string.error_no_service_selected));
            builder.setPositiveButton(getString(R.string.ok), null);
            builder.setCancelable(true);
            builder.create().show();
        }
        else {
            captureButton.setEnabled(true);
        }
        
        lManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000, 0, lListener);
    }

    @Override
    public void onPause() {
        super.onPause();

        if(mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
        
        lManager.removeUpdates(lListener);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch(requestCode) {
        case Library.REQUEST_IMAGE_FROM_GALLERY:
            // ギャラリーから画像を取得した場合
            if(resultCode == Activity.RESULT_OK) {
                postMessage("", data.getData());
            }

            break;
        case Library.REQUEST_INPUT_COMMENT:
            if(resultCode == Activity.RESULT_OK) {
                String message = data.getStringExtra("message");
                Uri imageUri = data.getData();
                postMessage(message, imageUri);
            }
            this.onResume();

            break;
        default:
            break;
        }
    }

    /***
     * オプションメニューを作成
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean returnBool = super.onCreateOptionsMenu(menu);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);

        return(returnBool);
    }

    /***
     * オプションメニューの項目が選択された際の動作を設定
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean returnBool = super.onOptionsItemSelected(item);

        Intent mIntent;
        switch(item.getItemId()) {
        case R.id.menu_setting:
            // 設定画面を開く
            mIntent = new Intent(TottepostActivity.this, SettingActivity.class);
            startActivity(mIntent);
            break;
        case R.id.menu_about:
            // About画面を開く
            mIntent = new Intent(TottepostActivity.this, AboutActivity.class);
            startActivity(mIntent);
            break;
        }

        return(returnBool);
    }

    /***
     * 入力パネルを閉じる
     * 
     * @param v
     */
    public void closeInputPanel(View v) {
        InputMethodManager mInputMethodManager = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        mInputMethodManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
    }

    /***
     * 写真撮影時のコールバックメソッド
     */
    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        // ファイル名を生成
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddkkmmss");
        String fileName = "tottepost_" + dateFormat.format(new Date()) + ".jpg";
        File destFile = new File(baseDir, fileName);

        // 生成したファイル名で新規ファイルを登録
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, fileName);
        values.put("_data", destFile.getAbsolutePath());
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        Uri destUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        
        try {
            FileOutputStream fos = new FileOutputStream(destFile.getAbsolutePath());
            fos.write(data);
            fos.flush();
            fos.close();
            
            if(Library.isCommentEnable(getApplicationContext())) {
                Intent mIntent = new Intent(TottepostActivity.this, BlankActivity.class);
                mIntent.setData(destUri);
                startActivityForResult(mIntent, Library.REQUEST_INPUT_COMMENT);
            }
            else {
                postMessage("", destUri);
            }
        }
        catch(FileNotFoundException e) {
            getContentResolver().delete(destUri, null, null);
            e.printStackTrace();
        }
        catch(IOException e) {
            getContentResolver().delete(destUri, null, null);
            e.printStackTrace();
        }

        try {
            /***
             * 画像の向きを書き込む
             * 参考:AndroidでExif情報編集 – Android | team-hiroq
             *      http://team-hiroq.com/blog/android/android_jpeg_exif.html
             *
             *      [AIR][Android] CameraUIで撮影した写真の回転が、機種によってバラバラなのをExifで補整する！  |    R o m a t i c A : Blog  : Archive
             *      http://blog.romatica.com/2011/04/04/air-for-android-cameraui-exif/
             *
             * 画面の向きを検出する
             * 参考:Androidアプリ開発メモ027：画面の向き: ぷ～ろぐ
             *      http://into.cocolog-nifty.com/pulog/2011/10/android027-9b2b.html
             */
            ExifInterface ei = new ExifInterface(destFile.getAbsolutePath());
            WindowManager wm = (WindowManager)getSystemService(Context.WINDOW_SERVICE);
            Display mDisplay = wm.getDefaultDisplay();
            switch(mDisplay.getRotation()) {
            case Surface.ROTATION_0:
                ei.setAttribute(ExifInterface.TAG_ORIENTATION, "6");

                break;
            case Surface.ROTATION_90:
                ei.setAttribute(ExifInterface.TAG_ORIENTATION, "1");

                break;
            case Surface.ROTATION_270:
                ei.setAttribute(ExifInterface.TAG_ORIENTATION, "3");

                break;
            default:
                break;
            }
            ei.saveAttributes();
        }
        catch(IOException e) {
            e.printStackTrace();
        }

        capturing = false;
        focused = false;
        mCamera.cancelAutoFocus();
        if(!Library.isCommentEnable(getApplicationContext())) {
            mCamera.startPreview();
        }
        captureButton.setEnabled(true);
    }

    /***
     * SurfaceViewのサイズなどが変更された際に呼び出される
     */
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        try {
            if(mCamera != null) {
                mCamera.stopPreview();
            }
            else {
                mCamera = Camera.open();
            }
    
            // 各種パラメータの設定
            Camera.Parameters params = mCamera.getParameters();
            // 保存する画像サイズを決定
            List<Camera.Size> pictureSizes = params.getSupportedPictureSizes();
            Camera.Size picSize = pictureSizes.get(0);
            for(int i = 1; i < pictureSizes.size(); i++) {
                Camera.Size temp = pictureSizes.get(i);
                if(picSize.width * picSize.height > 1920 * 1080 || picSize.width * picSize.height < temp.width * temp.height) {
                    // 1920x1080以下で一番大きな画像サイズを選択
                    picSize = temp;
                }
            }
            params.setPictureSize(picSize.width, picSize.height);
            
            // 画像サイズを元にプレビューサイズを決定
            List<Camera.Size> previewSizes = params.getSupportedPreviewSizes();
            Camera.Size preSize = previewSizes.get(0);
            for(int i = 1; i < previewSizes.size(); i++) {
                Camera.Size temp = previewSizes.get(i);
                if(Math.abs((double)picSize.width / (double)picSize.height - (double)preSize.width / (double)preSize.height)
                   >= Math.abs((double)picSize.width / (double)picSize.height - (double)temp.width / (double)temp.height)) {
                    if(preSize.width * preSize.height < temp.width * temp.height) {
                        // 一番保存サイズに近くてかつ一番大きなプレビューサイズを選択
                        preSize = temp;
                    }
                }
            }
            params.setPreviewSize(preSize.width, preSize.height);
            
            // プレビューサイズを元にSurfaceViewのサイズを決定
            // プレビューサイズとSurfaceViewのサイズで縦横の関係が逆になっている
            WindowManager manager = (WindowManager)getSystemService(WINDOW_SERVICE);
            Display mDisplay = manager.getDefaultDisplay();
            ViewGroup.LayoutParams lParams = preview.getLayoutParams();
            lParams.width  = mDisplay.getWidth();
            lParams.height = mDisplay.getHeight();
            if((double)preSize.width / (double)preSize.height
               < (double)mDisplay.getHeight() / (double)mDisplay.getWidth()) {
                // 横の長さに合わせる
                lParams.height = preSize.width * mDisplay.getWidth() / preSize.height;
            }
            else if((double)preSize.width / (double)preSize.height
                    > (double)mDisplay.getHeight() / (double)mDisplay.getWidth()) {
                // 縦の長さに合わせる
                lParams.width  = preSize.height * mDisplay.getHeight() / preSize.width;
            }
            preview.setLayoutParams(lParams);
            
            mCamera.setParameters(params);
            
            switch(mDisplay.getRotation()) {
            case Surface.ROTATION_0:
                mCamera.setDisplayOrientation(90);
    
                break;
            case Surface.ROTATION_90:
                mCamera.setDisplayOrientation(0);
    
                break;
            case Surface.ROTATION_270:
                mCamera.setDisplayOrientation(180);
    
                break;
            default:
                break;
            }

            mCamera.setPreviewDisplay(preview.getHolder());
            mCamera.startPreview();
        }
        catch(Exception e) {
            Toast.makeText(getApplicationContext(), getString(R.string.error_launch_camera_failed), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    /***
     * SurfaceViewが生成された際に呼び出される
     */
    @Override
    public void surfaceCreated(SurfaceHolder holder) {}

    /***
     * SurfaceViewが破棄される際に呼び出される
     */
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {}

    /***
     * ギャラリーを呼び出す
     */
    public void callGallery() {
        Intent mIntent = new Intent();
        mIntent.setType("image/*");
        mIntent.setAction(Intent.ACTION_PICK);
        startActivityForResult(mIntent, Library.REQUEST_IMAGE_FROM_GALLERY);
    }

    /***
     * メッセージを投稿する
     * 
     * @param inputMessage
     *     投稿するメッセージ
     * @param inputUri
     *     投稿する画像のUri
     */
    public void postMessage(String inputMessage, Uri inputUri) {
        if(inputUri != null) {
            Toast.makeText(getApplicationContext(), getString(R.string.post_message_before), Toast.LENGTH_LONG).show();
            if(Library.isServiceEnable("setting_use_facebook", getApplicationContext())) {
                // Facebook用の処理
                if(tokens[Library.TOKEN_FOR_FACEBOOK].length() != 0) {
                    // アクセストークンの取得に成功したら投稿を行う
                    ImagePostTask imagePost = new ImagePostTask(inputMessage, inputUri, Library.FACEBOOK);
                    imagePost.execute();
                }
                else {
                    Toast.makeText(getApplicationContext(),
                                   getString(R.string.service_name_facebook) + getString(R.string.post_login_failed),
                                   Toast.LENGTH_SHORT).show();
                }
            }
            if(Library.isServiceEnable("setting_use_twitter", getApplicationContext())) {
                // Twitter用の処理
                if(tokens[Library.TOKEN_FOR_TWITTER].length() != 0 && tokens[Library.TOKEN_SECRET_FOR_TWITTER].length() != 0) {
                    // アクセストークンの取得に成功したら投稿を行う
                    ImagePostTask imagePost = new ImagePostTask(inputMessage, inputUri, Library.TWITTER);
                    imagePost.execute();
                }
                else {
                    Toast.makeText(getApplicationContext(),
                                   getString(R.string.service_name_twitter) + getString(R.string.post_login_failed),
                                   Toast.LENGTH_SHORT).show();
                }
            }
        }
        else {
            Toast.makeText(getApplicationContext(), getString(R.string.post_image_empty), Toast.LENGTH_SHORT).show();
        }
    }

    /***
     * 投稿をバックグラウンドで行うためのクラス
     */
    class ImagePostTask extends AsyncTask<Void, Void, Void> {
        // 変数の宣言
        private String message;
        private Uri imageUri;
        private int target;

        /***
         * コンストラクタ
         * 
         * @param inputMessage
         *     投稿するメッセージ
         * @param inputUri
         *     投稿する画像のUri
         * @param inputTarget
         *     投稿先を定数で指定する
         */
        public ImagePostTask(String inputMessage, Uri inputUri, int inputTarget) {
            message = inputMessage;
            imageUri = inputUri;
            target = inputTarget;
        }

        /***
         * アップロードをバックグランドで行う
         */
        @Override
        public Void doInBackground(Void... params) {
            while(uploading) {
                // 他のファイルをアップロード中であれば待機する
                try {
                    Thread.sleep(500);
                }
                catch(Exception e) {
                    e.printStackTrace();
                }
            }
            uploading = true;
            
            switch(target) {
            case Library.FACEBOOK:
                /***
                 * Facebookに画像を投稿する
                 * 参考:インドＩＴ留学メモ: AndroidでFacebook
                 *     http://blog-indiait.blogspot.jp/2011/06/androidfacebook.html
                 */
                byte[] picture = null;
                try {
                    ContentResolver resolver = getContentResolver();
                    Bitmap mBitmap = MediaStore.Images.Media.getBitmap(resolver, imageUri);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    String type = resolver.getType(imageUri);
                    if(type.equals("image/png")) {
                        mBitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
                    }
                    else {
                        mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                    }
                    mBitmap.recycle();
                    picture = baos.toByteArray();
                    baos.close();
                }
                catch(Exception e) {
                    e.printStackTrace();
                }
                if(Library.isLocationEnable(getApplicationContext())) {
                    Bundle mBundle = new Bundle();
                    mBundle.putString("type", "place");
                    mBundle.putString("center", mLocation.getLatitude() + "," + mLocation.getLongitude());
                    mBundle.putString("distance", "1000");
                    mAsyncRunner.request("search", mBundle, new FetchPlaceRequestListener(message, picture));
                }
                else {
                    Bundle mBundle = new Bundle();
                    mBundle.putString("method", "photos.upload");
                    mBundle.putString("caption", message);
                    mBundle.putByteArray("photo", picture);
                    mAsyncRunner.request(null, mBundle, "POST", new ImagePostRequestListener(), null);
                }

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
                ContentResolver resolver = getContentResolver();
                Cursor mCursor = resolver.query(imageUri, null, null, null, null);
                mCursor.moveToFirst();
                File image = new File(mCursor.getString(mCursor.getColumnIndex(MediaStore.MediaColumns.DATA)));
                status.media(image);

                boolean complete = false;
                int count = 0;
                if(!complete && count < 5) {
                    // 成功するか5回失敗するまで投稿
                    try {
                        mTwitter.updateStatus(status);
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getApplicationContext(),
                                               getString(R.string.service_name_twitter) + getString(R.string.post_message_after),
                                               Toast.LENGTH_SHORT).show();
                            }
                        });
                        complete = true;
                        uploading = false;
                    }
                    catch (TwitterException e) {
                        count++;
                        e.printStackTrace();
                    }
                }
                if(!complete) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(),
                                    getString(R.string.service_name_twitter) + getString(R.string.post_message_post_failed),
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

    /***
     * Facebookに画像を投稿する際のリスナクラス
     */
    class ImagePostRequestListener implements AsyncFacebookRunner.RequestListener {
        @Override
        public void onComplete(String response, Object state) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(),
                                   getString(R.string.service_name_facebook) + getString(R.string.post_message_after),
                                   Toast.LENGTH_SHORT).show();
                }
            });
            uploading = false;
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
    
    class MyLocationListener implements LocationListener {
        @Override
        public void onLocationChanged(Location location) {
            mLocation = location;
        }

        @Override
        public void onProviderDisabled(String provider) {}

        @Override
        public void onProviderEnabled(String provider) {}

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {}
    }
    
    class FetchPlaceRequestListener implements AsyncFacebookRunner.RequestListener {
        private String message;
        private byte[] picture;
        
        public FetchPlaceRequestListener(String inputMessage, byte[] inputPicture) {
            message = inputMessage;
            picture = inputPicture;
        }
        
        @Override
        public void onComplete(String response, Object state) {
            try {
                JSONArray array = new JSONObject(response).getJSONArray("data");
                if(array != null) {
                    Bundle mBundle = new Bundle();
                    mBundle.putString("method", "photos.upload");
                    mBundle.putString("caption", message);
                    mBundle.putByteArray("photo", picture);
                    JSONObject temp = array.getJSONObject(0);
                    mBundle.putString("place", temp.getString("id"));
                    mAsyncRunner.request(null, mBundle, "POST", new ImagePostRequestListener(), null);
                }
            }
            catch(JSONException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onIOException(IOException e, Object state) {}

        @Override
        public void onFileNotFoundException(FileNotFoundException e, Object state) {}

        @Override
        public void onMalformedURLException(MalformedURLException e, Object state) {}

        @Override
        public void onFacebookError(FacebookError e, Object state) {}
    }
}