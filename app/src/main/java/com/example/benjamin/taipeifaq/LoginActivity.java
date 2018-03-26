package com.example.benjamin.taipeifaq;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by user on 2017/11/6.
 */

public class LoginActivity extends AppCompatActivity{
    private RequestQueue mQueue;
    private ImageView imageView;
    private FaqObject faqObject;
    private String urlFAQ = "http://data.taipei/opendata/datalist/" +
            "apiAccess?scope=resourceAquire&rid=06badd5c-e0bc-4b55-8646-9fcd4ea7096d&limit=30" +
            "&sort=post_date desc";

    private FirebaseAuth auth;
    private FirebaseAuth.AuthStateListener authStateListener;
    private String userUID;

    //呼叫addAuthStateListener方法加入傾聽器屬性，
    //每次LoginActivity首次顯示或是從背景返回前景時都會自動開始傾聽事件。
    @Override
    protected void onStart() {
        super.onStart();
        auth.addAuthStateListener(authStateListener);
    }

    //呼叫removeAuthStateListener方法移除傾聽事件，
    //每次LoginActivity進入背景或結束時都會停止傾聽事件。
    @Override
    protected void onStop() {
        super.onStop();
        auth.removeAuthStateListener(authStateListener);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        urlFAQ = urlFAQ.replaceAll(" ", "%20"); //路徑中有空格，需要將空格替換成%20，否則會報錯
        auth = FirebaseAuth.getInstance();
        authStateListener = new FirebaseAuth.AuthStateListener(){
            @Override
            public void onAuthStateChanged(@NonNull final FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();

                if(user != null){   //假如已登入
                    Log.d("onAuthStateChanged", "登入:"+
                            user.getUid());
                    userUID =  user.getUid();
                    setContentView(R.layout.activity_checklogin);
                    imageView = (ImageView) findViewById(R.id.checkloginimage);
                    Animation animation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.mytransition);
                    imageView.startAnimation(animation);

                    final Intent intent = new Intent(LoginActivity.this, MainActivity.class);

                    final Thread startTimer = new Thread(){
                        @Override
                        public void run() {
                            try {
                                initData(intent); //URL連線取得市政府資料
                                sleep(3000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            finally {
                                startActivity(intent);
                                finish();
                            }
                        }
                    };
                    startTimer.start();
                }
                else{
                    // before login
                    Log.d("onAuthStateChanged", "已登出");
                    setContentView(R.layout.activity_login);
                    Button button_login = (Button) findViewById(R.id.button_login);
                    final EditText textEmail = (EditText) findViewById(R.id.email);
                    final EditText textPassword = (EditText) findViewById(R.id.password);
                    //隱藏密碼
                    textPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());

                    button_login.setOnClickListener(new View.OnClickListener(){
                        @Override
                        public void onClick(View view) {

                            String email = textEmail.getText().toString();
                            String password = textPassword.getText().toString();
                            Log.d("AUTH", email+"/"+password);

                            if(email.isEmpty()){
                                Toast.makeText(LoginActivity.this, R.string.email_empty,
                                        Toast.LENGTH_SHORT).show();
                            }
                            else if(password.isEmpty()){
                                Toast.makeText(LoginActivity.this, R.string.password_empty,
                                        Toast.LENGTH_SHORT).show();
                            }
                            else
                                login(email, password);
                        }
                    });

                    final Button button_register = (Button) findViewById(R.id.button_register);
                    button_register.setOnClickListener(new View.OnClickListener(){
                        @Override
                        public void onClick(View view) {
                            LayoutInflater inflater = LayoutInflater.from(getApplicationContext());
                            final View layout = inflater.inflate(R.layout.dialog_register,
                                    (ViewGroup) findViewById(R.id.dialog_register_layout));

                            final EditText dialog_textEmail = layout.findViewById(R.id.email_register);
                            final EditText dialog_textPassword = layout.findViewById(R.id.password_register);
                            final EditText dialog_double_check = layout.findViewById(R.id.double_check_register);

                            new AlertDialog.Builder(LoginActivity.this)
                                    .setTitle("輸入欲註冊的電子信箱與密碼")
                                    .setView(layout)
                                    .setPositiveButton("確定", new DialogInterface.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    String stringEmail = dialog_textEmail.getText().toString();
                                    String stringPassword = dialog_textPassword.getText().toString();
                                    String stringDoubleCheck = dialog_double_check.getText().toString();

                                    if(stringEmail.isEmpty()){
                                        Toast.makeText(LoginActivity.this, R.string.email_empty,
                                                Toast.LENGTH_SHORT).show();
                                    }
                                    else if(stringPassword.isEmpty()){
                                        Toast.makeText(LoginActivity.this, R.string.password_empty,
                                                Toast.LENGTH_SHORT).show();
                                    }
                                    else if(stringDoubleCheck.isEmpty() |
                                            !stringDoubleCheck.equals(stringPassword)){
                                        Toast.makeText(LoginActivity.this, R.string.password_wrong,
                                                Toast.LENGTH_SHORT).show();
                                    }
                                    else
                                        createUser(stringEmail, stringPassword);
                                }
                            })
                            .setNegativeButton("取消", null)
                            .show();
                        }
                    });

                }
            }
        };

    }

    public void login(final String email, final String password){

        //呼叫FirebaseAuth類別的signInWithEmailAndPassword方法進行帳號與密碼的登入。
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {

                    //當使用者輸入Email與密碼後，假設該Email尚未是會員（不存在此用戶）時，
                    //顯示詢問使用者是否註冊

                    //登入工作完成後會被自動執行的Callback方法
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(!task.isSuccessful()){
                            //Log.d("onComplete: ", task.getException().getMessage());
                            final String password_wrong =
                                    "The password is invalid or the user does not have a password.";
                            if(task.getException().getMessage().equals(password_wrong)){
                                Toast.makeText(LoginActivity.this, R.string.password_wrong,
                                        Toast.LENGTH_SHORT).show();
                            }
                            else
                                register(email, password);
                        }
                    }
                });
    }

    private void register(final String email, final String password) {
        new AlertDialog.Builder(LoginActivity.this)
                .setTitle("登入問題")
                .setMessage("無此帳號，是否要以此帳號與密碼註冊?")
                .setPositiveButton("註冊",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                createUser(email, password);
                            }
                        })
                .setNegativeButton("取消", null)
                .show();

    }

    private void createUser(final String email, final String password) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(

                        //假設該Email不是會員，詢問使用者是否用此帳號註冊
                        //添加OnCompleteListener監聽
                        new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if(!task.isSuccessful()){
                                    final String accountExist = "The email address is already in " +
                                            "use by another account.";
                                    //如果帳號已經有人申請過了，則跳出提示訊息。
                                    if(task.getException().getMessage().equals(accountExist)){
                                        Toast.makeText(LoginActivity.this, R.string.account_exist,
                                                Toast.LENGTH_SHORT).show();
                                    }
                                }
                                if(task.isSuccessful()){
                                    Toast.makeText(LoginActivity.this, R.string.signin_successful,
                                            Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
    }

    private void initData(final Intent intent){
        /*
            You don't need to run Volley request on async task.
            Why:
            They manage all network related task on separate thread.
            If you look closely at library project they did not picture the async task.
            But they intelligently handle all network related task efficiently.
            Check RequestQueue.java class in Volley's main package
         */
        mQueue = Volley.newRequestQueue(getApplicationContext());
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, urlFAQ, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            //ArrayList<String> listNewTitle = new ArrayList();
                            //ArrayList<String> listPostDate = new ArrayList();

                            //在切換成MainActivity之前把資料放進bundle裡，好讓MainActivity接收資料
                            Bundle bundle = new Bundle();

                            JSONArray data = response.getJSONObject("result").getJSONArray("results");
                            int dataLength = data.length();
                            bundle.putInt("dataSize", data.length());

                            for(int i = 0; i < dataLength; i++ ){
                                JSONObject jsonObject = data.getJSONObject(i);

                                faqObject = new FaqObject();
                                faqObject.setDeptName(jsonObject.getString("deptName"));
                                faqObject.setPost_date(jsonObject.getString("post_date"));
                                faqObject.setNews_from(jsonObject.getString("news_from"));
                                faqObject.setContact(jsonObject.getString("contact"));
                                faqObject.setContact_phone(jsonObject.getString("contact_phone"));
                                faqObject.setNews_title(jsonObject.getString("news_title"));
                                faqObject.setNews_content(jsonObject.getString("news_content"));
                                faqObject.setStart_date(jsonObject.getString("start_date"));
                                faqObject.setEnd_date(jsonObject.getString("end_date"));
                                faqObject.setData_id(jsonObject.getString("data_id"));

                                bundle.putSerializable("faqObject" + i, faqObject);

                            }
                            intent.putExtras(bundle);

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("VolleyError: ", error.toString());
                    }
                });

        mQueue.add(jsonObjectRequest);
    }

}
