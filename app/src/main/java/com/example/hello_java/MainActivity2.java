package com.example.hello_java;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity2 extends AppCompatActivity implements View.OnClickListener{

    private EditText editText;

    private ImageView imageView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.my_layout);

        Button button = (Button) findViewById(R.id.button);
        button.setOnClickListener(this);

        editText = (EditText)  findViewById(R.id.edit_text);
        imageView = (ImageView) findViewById(R.id.ImageView);

        imageView.setImageResource(R.drawable.ic_launcher_foreground);


//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
//            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
//            return insets;
//        });
    }
    @Override
    public void onClick(View v) {
        // 处理点击事件
        if (v.getId() == R.id.button)
            Toast.makeText(MainActivity2.this , "test" , Toast.LENGTH_LONG).show();
        // 好像是新版Android studio的坑
//        switch (v.getId()) {
//            case R.id.button:
//                // 执行按钮点击操作
//                break;
//            // 处理其他点击事件
//        }
    }
}
