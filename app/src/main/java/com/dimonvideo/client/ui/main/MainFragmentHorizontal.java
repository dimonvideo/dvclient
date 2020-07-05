package com.dimonvideo.client.ui.main;

import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.dimonvideo.client.Config;
import com.dimonvideo.client.R;
import com.dimonvideo.client.adater.CardAdapter;
import com.dimonvideo.client.model.Feed;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@RequiresApi(api = Build.VERSION_CODES.M)
public class MainFragmentHorizontal extends Fragment implements RecyclerView.OnScrollChangeListener  {

    private List<Feed> listFeed;
    public RecyclerView recyclerView;
    public RecyclerView.Adapter adapter;

    private RequestQueue requestQueue;

    private int requestCount = 1;
    String razdel = "comments";
    String url = Config.COMMENTS_URL;
    String key = "comments";

    @RequiresApi(api = Build.VERSION_CODES.M)
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_home_horizontal, container, false);

        if (this.getArguments() != null) {
            razdel = getArguments().getString(Config.TAG_RAZDEL);

            assert razdel != null;
            if (razdel.equals(Config.GALLERY_RAZDEL)) {
                url = Config.GALLERY_URL;
                key = Config.GALLERY_RAZDEL;
            }
            if (razdel.equals(Config.UPLOADER_RAZDEL)) {
                url = Config.UPLOADER_URL;
                key = Config.UPLOADER_RAZDEL;

            }
            if (razdel.equals(Config.VUPLOADER_RAZDEL)) {
                url = Config.VUPLOADER_URL;
                key = Config.VUPLOADER_RAZDEL;

            }
            if (razdel.equals(Config.NEWS_RAZDEL)) {
                url = Config.NEWS_URL;
                key = Config.NEWS_RAZDEL;

            }
            if (razdel.equals(Config.MUZON_RAZDEL)) {
                url = Config.MUZON_URL;
                key = Config.MUZON_RAZDEL;

            }
            if (razdel.equals(Config.BOOKS_RAZDEL)) {
                url = Config.BOOKS_URL;
                key = Config.BOOKS_RAZDEL;

            }
            if (razdel.equals(Config.ARTICLES_RAZDEL)) {
                url = Config.ARTICLES_URL;
                key = Config.ARTICLES_RAZDEL;

            }

        }

        recyclerView = root.findViewById(R.id.recycler_view);
        LinearLayoutManager layoutManager
                = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setHasFixedSize(true);
        recyclerView.setOnScrollChangeListener(this);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(),
                layoutManager.getOrientation());
        recyclerView.addItemDecoration(dividerItemDecoration);

        listFeed = new ArrayList<>();
        requestQueue = Volley.newRequestQueue(requireActivity());

        // получение данных
        getData();
        adapter = new CardAdapter(listFeed, getContext());

        recyclerView.setAdapter(adapter);



        return root;
    }

    // запрос к серверу апи
    private JsonArrayRequest getDataFromServer(int requestCount) {

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        Set<String> selections = sharedPrefs.getStringSet("dvc_"+key+"_cat", null);
        String category_string = "all";
        if (selections != null) {
            String[] selected = selections.toArray(new String[]{});
            category_string = TextUtils.join(",", selected);
        }

        return new JsonArrayRequest(url + requestCount + "&c=placeholder," + category_string,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        for (int i = 0; i < response.length(); i++) {
                                Feed jsonFeed = new Feed();
                                JSONObject json;
                                try {
                                    json = response.getJSONObject(i);
                                    jsonFeed.setImageUrl(json.getString(Config.TAG_IMAGE_URL));
                                    jsonFeed.setTitle(json.getString(Config.TAG_TITLE));
                                    jsonFeed.setText(json.getString(Config.TAG_TEXT));
                                    jsonFeed.setDate(json.getString(Config.TAG_DATE));
                                    jsonFeed.setComments(json.getInt(Config.TAG_COMMENTS));
                                    jsonFeed.setHits(json.getInt(Config.TAG_HITS));
                                    jsonFeed.setRazdel(json.getString(Config.TAG_RAZDEL));
                                    jsonFeed.setLink(json.getString(Config.TAG_LINK));
                                    jsonFeed.setMod(json.getString(Config.TAG_MOD));
                                    jsonFeed.setCategory(json.getString(Config.TAG_CATEGORY));
                                    jsonFeed.setHeaders(json.getString(Config.TAG_HEADERS));
                                    jsonFeed.setUser(json.getString(Config.TAG_USER));
                                    jsonFeed.setSize(json.getString(Config.TAG_SIZE));
                                    jsonFeed.setTime(json.getLong(Config.TAG_TIME));
                                    jsonFeed.setId(json.getInt(Config.TAG_ID));
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                listFeed.add(jsonFeed);
                            }
                        adapter.notifyDataSetChanged();

                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(getContext(), getString(R.string.no_more), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // получение данных и увеличение номера страницы
    private void getData() {
        requestQueue.add(getDataFromServer(requestCount));
        requestCount++;
    }

    // опредление последнего элемента
    private boolean isLastItemDisplaying(RecyclerView recyclerView) {
        if (Objects.requireNonNull(recyclerView.getAdapter()).getItemCount() != 0) {
            int lastVisibleItemPosition = ((LinearLayoutManager) Objects.requireNonNull(recyclerView.getLayoutManager())).findLastCompletelyVisibleItemPosition();
            return lastVisibleItemPosition != RecyclerView.NO_POSITION && lastVisibleItemPosition == recyclerView.getAdapter().getItemCount() - 1;
        }
        return false;
    }

    // получение следующей страницы при скролле
    @Override
    public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
        if (isLastItemDisplaying(recyclerView)) {
            getData();
        }
    }


}