package io.github.jasonli.mydaggerdemo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import javax.inject.Inject;

import io.github.jasonli.mydaggerdemo.components.*;
import io.github.jasonli.mydaggerdemo.modules.ActivityModule;
import io.github.jasonli.mydaggerdemo.components.DaggerActivityComponent;

public class MainActivity extends AppCompatActivity {
    private ActivityComponent mActivityComponent;

    @Inject UserModel userModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //UserModel user = new UserModel();
        mActivityComponent = DaggerActivityComponent.builder().activityModule(new ActivityModule()).build();
        mActivityComponent.inject(this);

        ((TextView) findViewById(R.id.user_desc_line)).setText(userModel.id + "\n" + userModel.name + "\n" + userModel.gender);
    }
}
