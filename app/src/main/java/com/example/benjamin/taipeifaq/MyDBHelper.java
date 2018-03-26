package com.example.benjamin.taipeifaq;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.widget.ArrayAdapter;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by user on 2018/2/25.
 */

public class MyDBHelper extends SQLiteOpenHelper {
    private static final String TAG = "MyDBHelper";
    private static final String DB_NAME = "NewsTitles";
    private static final String TABLE_NAME = "Titles";
    private static final int DB_VERSION = 1;
    private static final String COL_id = "_id";
    private static final String COL_title = "title";
    private static final String TABLE_CREATE =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    COL_id + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COL_title + " TEXT " + "); ";
    private String urlData = "http://data.taipei/opendata/datalist/" +
            "apiAccess?scope=resourceAquire&rid=06badd5c-e0bc-4b55-8646-9fcd4ea7096d&sort=post_date desc";
    private Context context;
    private ArrayList<String> dataList;

    public MyDBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        dataList = new ArrayList<>();
        sqLiteDatabase.execSQL(TABLE_CREATE);
        getData(sqLiteDatabase);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(sqLiteDatabase);
    }


    private void getData(final SQLiteDatabase db){
        urlData = urlData.replaceAll(" ", "%20");
        final ContentValues values = new ContentValues();

        RequestQueue mQueue;
        mQueue = Volley.newRequestQueue(context);
        final JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET,
                urlData, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONArray data = response.getJSONObject("result").getJSONArray("results");
                            Log.d("Datalength: ", String.valueOf(data.length()));
                            for(int i = 0; i < data.length(); i++){
                                JSONObject jsonObject = data.getJSONObject(i);
                                dataList.add(jsonObject.getString("news_title"));
                                values.put(COL_title, jsonObject.getString("news_title"));
                                db.insert(TABLE_NAME, null, values);
                            }
                        } catch (JSONException e){
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("VolleyError: ", error.toString());
                    }
                }
        );
        mQueue.add(jsonObjectRequest);

    }

}
