package rio.div2;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

public class DialogActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog);
        
        Button postButton = (Button)findViewById(R.id.post);
        postButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeInputPanel(v);
                EditText messageBox = (EditText)findViewById(R.id.message);
                String message = messageBox.getText().toString();
                
                Intent mIntent = new Intent();
                mIntent.putExtra("message", message);
                mIntent.setData(getIntent().getData());
                setResult(RESULT_OK, mIntent);
                finish();
            }
        });
    }

    // 入力パネルを閉じる
    public void closeInputPanel(View v) {
        InputMethodManager mInputMethodManager = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        mInputMethodManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
    }
}