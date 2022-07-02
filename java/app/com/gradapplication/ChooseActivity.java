package app.com.gradapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

public class ChooseActivity extends AppCompatActivity {

    ImageView numbersBtn;
    ImageView lettersBtn;
    ImageView wordsBtn;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose);

        numbersBtn = (ImageView) findViewById(R.id.numbers);
        lettersBtn = (ImageView) findViewById(R.id.letters);
        wordsBtn = (ImageView) findViewById(R.id.words);

        lettersBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ChooseActivity.this, GridActivity.class);
                startActivity(intent);
            }
        });
    }
}