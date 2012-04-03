package rio.div2;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.widget.ImageView;

public class BlankActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.blank);

        try {
            ContentResolver resolver = getContentResolver();
            Uri imageUri = getIntent().getData();
            /***
             * 画像を回転させる
             * 参考:Androidでカメラ撮影し画像を保存する方法 - DRY（日本やアメリカで働くエンジニア日記）
             *      http://d.hatena.ne.jp/ke-16/20110712/1310433427
             *      
             * 画像のサイズを画面サイズに合わせる
             * 参考:Android: Bitmapを画面サイズにリサイズする | 自転車で通勤しましょ♪ブログ
             *      http://319ring.net/blog/archives/1504
             */
            Bitmap srcBitmap = MediaStore.Images.Media.getBitmap(resolver, imageUri);
            int srcWidth  = srcBitmap.getWidth();
            int srcHeight = srcBitmap.getHeight();
            
            Matrix mMatrix = new Matrix();
            mMatrix.postRotate(90);
            DisplayMetrics metrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(metrics);
            float screenWidth  = (float)metrics.widthPixels;
            float screenHeight = (float)metrics.heightPixels;
            
            float widthScale  = screenWidth / srcWidth;
            float heightScale = screenHeight / srcHeight;
            
            if(widthScale > heightScale) {
                mMatrix.postScale(heightScale, heightScale);
            }
            else {
                mMatrix.postScale(widthScale, widthScale);
            }
            
            Bitmap mBitmap = Bitmap.createBitmap(srcBitmap, 0, 0, srcWidth, srcHeight, mMatrix, true);
            
            ImageView image = (ImageView)findViewById(R.id.image);
            image.setImageBitmap(mBitmap);
            
            Intent mIntent = new Intent(BlankActivity.this, DialogActivity.class);
            mIntent.setData(imageUri);
            startActivityForResult(mIntent, Library.REQUEST_INPUT_COMMENT);
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        setResult(resultCode, data);
        finish();
    }
}