package com.example.benjamin.taipeifaq;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

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

import static android.content.Context.MODE_PRIVATE;

/**
 * Created by user on 2017/8/7.
 */
public class TabFragment extends Fragment {
    private ArrayList<FaqObject> myDataset, faqObjectList;
    private String[] history;
    private MyTabFragmentAdapter myTabFragmentAdapter;
    private static final String ARG_POSITION = "position";
    private static final String PREFERENCES_NAME = "history";
    private SharedPreferences preferences;
    private int position;
    private int offset = 30;
    private MyDBHelper helper;

    public static TabFragment newInstance(int position) {
        TabFragment tabFragment = new TabFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(ARG_POSITION, position);
        tabFragment.setArguments(bundle);
        return tabFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        position = getArguments().getInt(ARG_POSITION);
        Log.d(ARG_POSITION + "Tab:", String.valueOf(position));
        if(helper == null){
            helper = new MyDBHelper(getContext());
        }
        if(preferences == null){
            preferences = getActivity().getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.page_content, container, false);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //接收API的資料
        myDataset = new ArrayList<>();
        faqObjectList = new ArrayList<>();

        Bundle bundle = getActivity().getIntent().getExtras();
        for(int i = 0; i < bundle.getInt("dataSize"); i++ ){
            faqObjectList.add( (FaqObject) bundle.getSerializable("faqObject" + i));
        }
        myDataset.addAll(faqObjectList);
        faqObjectList.clear();  //清空List，等會若要加載更多資料可以重複使用這個List

        //擷取歷史紀錄
        history = preferences.getString("newsTitle", "目前沒有瀏覽紀錄").split("&");

        //將資料放進RecyclerView裡
        RecyclerView recyclerView = view.findViewById(R.id.recyclerView);
        final LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                //如果滑到底而且page是1的位置，則抓取更多資料
                if(!recyclerView.canScrollVertically(1) && MainActivity.position == 1){
                    RequestQueue mQueue;
                    String urlFAQ = "http://data.taipei/opendata/datalist/" +
                            "apiAccess?scope=resourceAquire&rid=06badd5c-e0bc-4b55-8646-9fcd4ea7096d&limit=30" +
                            "&sort=post_date desc&offset=";

                    urlFAQ = urlFAQ.replaceAll(" ", "%20");
                    urlFAQ = urlFAQ + offset;
                    Log.d("urlFAQ: ", urlFAQ);
                    mQueue = Volley.newRequestQueue(getContext());
                    final JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, urlFAQ, null,
                            new Response.Listener<JSONObject>() {
                                @Override
                                public void onResponse(JSONObject response) {
                                    try {
                                        JSONArray data = response.getJSONObject("result").getJSONArray("results");
                                        for(int i = 0; i < data.length(); i++ ){
                                            JSONObject jsonObject = data.getJSONObject(i);
                                            FaqObject faqObject = new FaqObject();
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

                                            faqObject.setType(0);
                                            faqObject.setID(offset + i + "");
                                            //faqObject.setChildFaqObject(faqObject);

                                            faqObjectList.add(faqObject);
                                        }
                                        offset = offset + data.length();

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
                    myTabFragmentAdapter.loadData(faqObjectList);
                    faqObjectList.clear();
                }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
            }
        });

        myTabFragmentAdapter = new MyTabFragmentAdapter(getActivity(), myDataset, history, position);
        recyclerView.setAdapter(myTabFragmentAdapter);
    }

    public static class MyTabFragmentAdapter extends RecyclerView.Adapter<MyTabFragmentAdapter.ViewHolder>{

        private Context context;
        private LayoutInflater layoutInflater;
        private ArrayList<FaqObject> faqObjects;
        private String[] hisTitle;
        private int pagePosition; //fragment頁面編號

        public class ViewHolder extends RecyclerView.ViewHolder{
            private TextView tvNewsTitle, tvPostDate, tvHisTitle, tvIntro;
            public ViewHolder(View itemView){
                super(itemView);
                tvIntro = itemView.findViewById(R.id.intro);
                tvNewsTitle = itemView.findViewById(R.id.news_title);
                tvPostDate = itemView.findViewById(R.id.post_date);
                tvHisTitle = itemView.findViewById(R.id.history_title);

            }
        }

        public MyTabFragmentAdapter(Context context, ArrayList<FaqObject> faqObjects, String[] hisTitle, int pagePosition){
            this.context = context;
            this.faqObjects = faqObjects;
            this.hisTitle = hisTitle;
            this.pagePosition = pagePosition;
            this.layoutInflater = LayoutInflater.from(context);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            //Log.d("onCreateViewHolder", "onCreateViewHolder");
            View itemView;
            switch (pagePosition){
                case 0:
                    itemView = layoutInflater.inflate(R.layout.page_content_intro, parent, false);
                    return new ViewHolder(itemView);
                case 1:
                    itemView = layoutInflater.inflate(R.layout.page_content_item, parent,false);
                    return new ViewHolder(itemView);
                case 2:
                    itemView = layoutInflater.inflate(R.layout.page_content_history, parent, false);
                    return new ViewHolder(itemView);
                default:
                    itemView = layoutInflater.inflate(R.layout.page_content_item, parent,false);
                    return new ViewHolder(itemView);
            }
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            switch (pagePosition){
                case 0:
                    holder.tvIntro.setText("上方可以搜尋資料庫內容，點兩下搜尋欄會顯示資料庫裡常見問題的標題，中間的分頁是可以從最新的資料查看，" + "第三個分頁是搜尋紀錄。");
                    break;
                case 1:
                    final FaqObject faqObject = faqObjects.get(position);
                    holder.tvPostDate.setText(faqObject.getPost_date());
                    holder.tvNewsTitle.setText(faqObject.getNews_title());
                    holder.itemView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Intent intent = new Intent(context, ShowAllDataActivity.class);
                            Bundle bundle = new Bundle();
                            bundle.putString("deptName", faqObject.getDeptName());
                            bundle.putString("newsFrom", faqObject.getNews_from());
                            bundle.putString("newsContent", faqObject.getNews_content());
                            bundle.putString("contact", faqObject.getContact());
                            bundle.putString("contactPhone", faqObject.getContact_phone());
                            intent.putExtras(bundle);
                            context.startActivity(intent);
                        }
                    });
                    break;
                case 2:
                    final String str = hisTitle[position];
                    holder.tvHisTitle.setText(str);
                    break;
            }
        }

        @Override
        public int getItemCount() {
            switch (pagePosition){
                case 0:
                    return 1;
                case 1:
                    return faqObjects.size();
                case 2:
                    return hisTitle.length;
                default:
                    return 0;
            }

        }

        public void loadData(ArrayList<FaqObject> faqObjects){
            Log.d("...", "loadData");
            this.faqObjects.addAll(faqObjects);
            notifyDataSetChanged();
        }

    }

}
