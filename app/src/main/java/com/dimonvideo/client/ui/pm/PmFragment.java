package com.dimonvideo.client.ui.pm;

import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.dimonvideo.client.Config;
import com.dimonvideo.client.R;
import com.dimonvideo.client.adater.MainAdapter;
import com.dimonvideo.client.model.Feed;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@RequiresApi(api = Build.VERSION_CODES.M)
public class PmFragment extends Fragment implements RecyclerView.OnScrollChangeListener, SwipeRefreshLayout.OnRefreshListener  {

    private List<Feed> listFeed;

    private RecyclerView recyclerView;
    private RecyclerView.Adapter adapter;
    SwipeRefreshLayout swipLayout;

    private RequestQueue requestQueue;

    private int requestCount = 1;
    private ProgressBar progressBar;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_home, container, false);

        recyclerView = (RecyclerView) root.findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);

        listFeed = new ArrayList<>();
        requestQueue = Volley.newRequestQueue(requireActivity());

        progressBar = (ProgressBar) root.findViewById(R.id.progressbar);
        progressBar.setVisibility(View.VISIBLE);

        // получение данных
        getData();

        recyclerView.setOnScrollChangeListener(this);
        adapter = new MainAdapter(listFeed, getContext());

        // разделитель позиций
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(), DividerItemDecoration.VERTICAL);
        dividerItemDecoration.setDrawable(Objects.requireNonNull(ContextCompat.getDrawable(requireContext(), R.drawable.divider)));
        recyclerView.addItemDecoration(dividerItemDecoration);

        // pull to refresh
        swipLayout = root.findViewById(R.id.swipe_layout);
        swipLayout.setOnRefreshListener(this);

        recyclerView.setAdapter(adapter);

        return root;
    }

    // запрос к серверу апи
    private JsonArrayRequest getDataFromServer(int requestCount) {

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        Set<String> selections = sharedPrefs.getStringSet("dvc_news_cat", null);
        String category = "all";
        if (selections != null) {
            String[] selected = selections.toArray(new String[]{});
            category = TextUtils.join(",", selected);
        }
        //    Toast.makeText(getContext(), category, Toast.LENGTH_SHORT).show();

        return new JsonArrayRequest(Config.NEWS_URL + requestCount + "&c=placeholder," + category,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        progressBar.setVisibility(View.GONE);
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
                                jsonFeed.setId(json.getInt(Config.TAG_ID));
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            listFeed.add(jsonFeed);
                        }
                        adapter.notifyDataSetChanged();                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        progressBar.setVisibility(View.GONE);
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

    // обновление
    @Override
    public void onRefresh() {
        requestCount = 1;
        getParentFragmentManager()
                .beginTransaction()
                .detach(PmFragment.this)
                .attach(PmFragment.this)
                .commit();
    }

}