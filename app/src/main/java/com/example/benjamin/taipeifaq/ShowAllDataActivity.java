package com.example.benjamin.taipeifaq;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.widget.TextView;

/**
 * Created by user on 2018/3/11.
 */

public class ShowAllDataActivity extends AppCompatActivity {

    private TextView deptName, deptNameContent, newsFrom, newsFromContent, newsContent, newsContent2,
                contact, contactContent, contactPhone, contactPhoneContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_showalldata);
        initView();
        showResults();
    }

    private void initView(){
        deptName = (TextView) findViewById(R.id.deptName);
        deptNameContent = (TextView) findViewById(R.id.deptName_content);
        newsFrom = (TextView) findViewById(R.id.news_from);
        newsFromContent = (TextView) findViewById(R.id.news_from_content);
        newsContent = (TextView) findViewById(R.id.news_content);
        newsContent2 = (TextView) findViewById(R.id.news_content2);
        contact = (TextView) findViewById(R.id.contact);
        contactContent = (TextView) findViewById(R.id.contact_content);
        contactPhone = (TextView) findViewById(R.id.contact_phone);
        contactPhoneContent = (TextView) findViewById(R.id.contact_phone_content);
    }

    private void showResults(){
        Bundle bundle = getIntent().getExtras();

        deptName.setText(R.string.deptName);
        deptName.setBackgroundColor(Color.GRAY);
        newsFrom.setText(R.string.newsFrom);
        newsFrom.setBackgroundColor(Color.GRAY);
        newsContent.setText(R.string.newsContent);
        newsContent.setBackgroundColor(Color.GRAY);
        contact.setText(R.string.contact);
        contact.setBackgroundColor(Color.GRAY);
        contactPhone.setText(R.string.contactPhone);
        contactPhone.setBackgroundColor(Color.GRAY);

        deptNameContent.setText(bundle.getString("deptName"));
        newsFromContent.setText(bundle.getString("newsFrom"));
        newsContent2.setText(bundle.getString("newsContent"));
        contactContent.setText(bundle.getString("contact"));
        contactPhoneContent.setText(bundle.getString("contactPhone"));
    }
}
