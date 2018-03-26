package com.example.benjamin.taipeifaq;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteCursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.FilterQueryProvider;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.auth.FirebaseAuth;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class MainActivity extends AppCompatActivity {
    static int position;
    private ViewPager viewPager;
    private Toolbar toolbar;
    private MenuItem item;
    private AutoCompleteTextView autoCompleteTextView;
    private android.support.design.widget.TabLayout tabLayout;
    private FloatingActionButton fab;
    private final static int[] imageResId =
            {R.drawable.ic_menu_search, R.drawable.ic_menu_upload, R.drawable.ic_menu_recent_history};
    private Cursor cursor;
    private FirebaseAuth auth;
    private MyDBHelper helper;
    private final static String URLDATA = "http://data.taipei/opendata/datalist/" +
            "apiAccess?scope=resourceAquire&rid=06badd5c-e0bc-4b55-8646-9fcd4ea7096d&q=";
    private final static String PREFERENCES_NAME = "history";
    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //在setContentView()方法中，Android會自動在Layout文件的最外層再套一个FrameLayout
        setContentView(R.layout.activity_main);

        //確認系統會自動加一層父Layout
        //the parent of mainLayout is "android.support.v7.widget.ContentFrameLayout"
        //CoordinatorLayout mainLayout = (CoordinatorLayout) findViewById(R.id.main_layout);
        //ViewParent viewParent = mainLayout.getParent();
        //Log.d("TAG", "the parent of mainLayout is " + viewParent.toString());

        if(helper == null){
            helper = new MyDBHelper(this);
        }
        if(preferences == null){
            preferences = getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE);
        }
        initView();
        setTabLayoutIcon();
    }

    private void setTabLayoutIcon() {
        for(int position = 0; position < imageResId.length; position++){
            tabLayout.getTabAt(position).setIcon(imageResId[position]);
        }
    }

    private void initView(){
        toolbar = (Toolbar) findViewById(R.id.tool_bar);
        autoCompleteTextView = (AutoCompleteTextView) findViewById(R.id.search_textView);

        cursor = helper.getReadableDatabase()
                .query("Titles", null, null, null, null, null, null);

        final SimpleCursorAdapter adapter = new SimpleCursorAdapter(
                this,
                android.R.layout.simple_dropdown_item_1line,
                cursor,
                new String[]{"title"},
                new int[]{android.R.id.text1}, 0);

        adapter.setCursorToStringConverter(new SimpleCursorAdapter.CursorToStringConverter() {
            @Override
            public CharSequence convertToString(Cursor cursor) {
                final int index = cursor.getColumnIndex("title");  //該欄位的索引值
                final String str = cursor.getString(index); //取得字串
                return str;
            }
        });

        adapter.setFilterQueryProvider(new FilterQueryProvider() {
            @Override
            public Cursor runQuery(CharSequence constraint) {

                if(constraint == null){
                    return null;
                }

                String[] column = { "_id", "title" };
                String selection = "title LIKE ?"; // '?'表示可能有多個，因為是字串陣列
                String[] selectionArgs = {"%" + constraint + "%"};
                Cursor searchCursor = helper.getReadableDatabase()
                        .query("Titles",
                                column,
                                selection,
                                selectionArgs,
                                null,
                                null,
                                null);

                /* 比對字串第二種作法，用MatrixCursor自己造一個Table接收過濾後的資料並回傳
                MatrixCursor，這個類別可以讓你建立一個cursor而且自己建立裡面的data
                如果data並不是cursor但卻又有需要將它變成cursor的需求，就可以用Matrix Cursor

                MatrixCursor matrixCursor = new MatrixCursor(column);

                while (searchCursor.moveToNext()){
                    Log.d("runQuery_cursor: ", searchCursor.getString(1));
                    if(searchCursor.getString(1).toLowerCase().contains(constraint.toString().toLowerCase()))
                        matrixCursor.addRow(new Object[] {searchCursor.getInt(0), searchCursor.getString(1)});
                }
                */

                return searchCursor;
            }
        });

        autoCompleteTextView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                final String text = autoCompleteTextView.getText().toString();
                //Log.d("String: ", text);
                getFullData(text);
            }
        });
        autoCompleteTextView.setThreshold(1);

        autoCompleteTextView.setAdapter(adapter);
        autoCompleteTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                autoCompleteTextView.showDropDown();
            }
        });

        setSupportActionBar(toolbar);

        viewPager = (ViewPager) findViewById(R.id.viewpager);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int pos) {
                if(pos == 0){
                    autoCompleteTextView.setVisibility(View.VISIBLE);
                    item.setVisible(true);
                    fab.setVisibility(View.GONE);
                }
                else if(pos == 1){
                    autoCompleteTextView.setVisibility(View.GONE);
                    item.setVisible(false);
                    fab.setVisibility(View.GONE);
                    position = pos;
                }
                else{
                    autoCompleteTextView.setVisibility(View.GONE);
                    item.setVisible(false);
                    fab.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        viewPager.setAdapter(new ViewPagerAdapter(getSupportFragmentManager()));

        tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);

        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setVisibility(View.GONE);
        fab.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "清除搜尋紀錄", Toast.LENGTH_SHORT).show();
                preferences.edit().clear().apply(); //清空SharedPreferences
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        item = menu.findItem(R.id.action_delete);
        //取得刪除item，然後在viewPager.addOnPageChangeListener裡判斷是否要隱藏
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement

        switch(id){
            case R.id.action_delete:
                autoCompleteTextView.setText("");
                break;
            case R.id.action_settings:
                break;
            case R.id.action_logout:
                cursor.close();
                auth = FirebaseAuth.getInstance();
                auth.signOut();
                final Intent intentLogout = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(intentLogout);
                finish();
        }

        return super.onOptionsItemSelected(item);
    }

    public class ViewPagerAdapter extends FragmentPagerAdapter {

        public ViewPagerAdapter(android.support.v4.app.FragmentManager fm) {
            super(fm);
        }

        @Override
        public long getItemId(int position) {
            return super.getItemId(position);
        }

        /*
            getItem會創建當頁和下一頁的Fragment，其他更後面的Fragment不會馬上呼叫這個方法
            對於前一頁的作法，FragmentPagerAdapter會保留，只是銷毀了它的視圖，
            FragmentStatePagerAdapter則是連實體都銷毀(所以較適合用在大量分頁的場合)
            https://segmentfault.com/a/1190000003742057
         */
        @Override
        public Fragment getItem(int position) {
            return TabFragment.newInstance(position);
        }

        @Override
        public int getCount() {
            return imageResId.length;
        }

        @Override
        public int getItemPosition(Object object) {
            return super.getItemPosition(object);
        }
    }

    private void getFullData(final String title){
        StringBuffer url = new StringBuffer(URLDATA);
        url.append(title);

        RequestQueue mQueue;
        mQueue = Volley.newRequestQueue(this);
        final JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET,
                url.toString(), null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONArray data = response.getJSONObject("result").getJSONArray("results");
                            JSONObject jsonObject = data.getJSONObject(0); //只有一筆資料
                            final String postDate = jsonObject.getString("post_date");
                            final String deptName = jsonObject.getString("deptName");
                            final String newsFrom = jsonObject.getString("news_from");
                            final String newsContent = jsonObject.getString("news_content");
                            final String contact = jsonObject.getString("contact");
                            final String contactPhone = jsonObject.getString("contact_phone");

                            //儲存搜尋紀錄
                            String historyTitle = preferences.getString("newsTitle", "");
                            Log.d("historyTitle: ", historyTitle);
                            String[] arrayTitle = historyTitle.split("&");
                            StringBuilder sbTitle = new StringBuilder(title);

                            if((arrayTitle.length < 10)){
                                sbTitle.append("&" + historyTitle); //用"&"當各個title的分隔標記

                                preferences.edit()
                                        .putString("newsTitle", sbTitle.toString())
                                        .apply();
                            }
                            else{
                                //表示超過10個紀錄,用arraycopy控制複製長度(捨棄最舊的紀錄)到另一個新陣列
                                String[] newTitleArray = new String[10];
                                System.arraycopy(arrayTitle, 0, newTitleArray, 0, 10);
                                for(int i = 0; i < newTitleArray.length; i++){
                                    sbTitle.append("&" + newTitleArray[i]);
                                }
                            }

                            preferences.edit()
                                    .putString("newsTitle", sbTitle.toString())
                                    .apply();

                            Log.d("History: ", preferences.getString("newsTitle", ""));

                            Intent intent = new Intent(getApplicationContext() , ShowAllDataActivity.class);

                            Bundle bundle = new Bundle();
                            bundle.putString("deptName", deptName);
                            bundle.putString("newsFrom", newsFrom);
                            bundle.putString("newsContent", newsContent);
                            bundle.putString("contact", contact);
                            bundle.putString("contactPhone", contactPhone);
                            intent.putExtras(bundle);
                            startActivity(intent);

                        } catch (JSONException e){
                            Log.d("JSONException:" ,e.getMessage());
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("VolleyError: ", error.getMessage());
                    }
                }

        );
        mQueue.add(jsonObjectRequest);
    }

}
