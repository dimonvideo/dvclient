package com.dimonvideo.client.ui.uploader;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
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
import com.dimonvideo.client.adater.CardAdapter;
import com.dimonvideo.client.model.Feed;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class UploaderFragment extends Fragment implements RecyclerView.OnScrollChangeListener, SwipeRefreshLayout.OnRefreshListener  {

    private List<Feed> listFeed;

    private RecyclerView recyclerView;
    private RecyclerView.LayoutManager layoutManager;
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
        layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);

        listFeed = new ArrayList<>();
        requestQueue = Volley.newRequestQueue(requireActivity());

        progressBar = (ProgressBar) root.findViewById(R.id.progressbar);
        progressBar.setVisibility(View.VISIBLE);

        getData();

        recyclerView.setOnScrollChangeListener(this);
        adapter = new CardAdapter(listFeed, getContext());
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(), DividerItemDecoration.VERTICAL);
        dividerItemDecoration.setDrawable(Objects.requireNonNull(ContextCompat.getDrawable(getContext(), R.drawable.divider)));
        recyclerView.addItemDecoration(dividerItemDecoration);
        swipLayout = root.findViewById(R.id.swipe_layout);
        swipLayout.setOnRefreshListener(this);
        recyclerView.setAdapter(adapter);

        return root;
    }

    private JsonArrayRequest getDataFromServer(int requestCount) {



        //JsonArrayRequest of volley
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Config.UPLOADER_URL + requestCount,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        //Calling method parseData to parse the json response
                        progressBar.setVisibility(View.GONE);
                        parseData(response);

                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(getContext(), "No More Items Available", Toast.LENGTH_SHORT).show();
                    }
                });

        return jsonArrayRequest;
    }

    private void getData() {
        requestQueue.add(getDataFromServer(requestCount));
        requestCount++;
    }

    private void parseData(JSONArray array) {
        for (int i = 0; i < array.length(); i++) {
            Feed jsonFeed = new Feed();
            JSONObject json;
            try {
                json = array.getJSONObject(i);

                jsonFeed.setImageUrl(json.getString(Config.TAG_IMAGE_URL));
                jsonFeed.setTitle(json.getString(Config.TAG_TITLE));
                jsonFeed.setText(json.getString(Config.TAG_TEXT));
                jsonFeed.setDate(json.getString(Config.TAG_DATE));
                jsonFeed.setComments(json.getInt(Config.TAG_COMMENTS));
                jsonFeed.setRazdel(json.getString(Config.TAG_RAZDEL));
                jsonFeed.setId(json.getInt(Config.TAG_ID));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            listFeed.add(jsonFeed);
        }
        adapter.notifyDataSetChanged();
    }

    private boolean isLastItemDisplaying(RecyclerView recyclerView) {
        if (recyclerView.getAdapter().getItemCount() != 0) {
            int lastVisibleItemPosition = ((LinearLayoutManager) recyclerView.getLayoutManager()).findLastCompletelyVisibleItemPosition();
            if (lastVisibleItemPosition != RecyclerView.NO_POSITION && lastVisibleItemPosition == recyclerView.getAdapter().getItemCount() - 1)
                return true;
        }
        return false;
    }

    @Override
    public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
        if (isLastItemDisplaying(recyclerView)) {
            getData();
        }
    }

    @Override
    public void onRefresh() {
        requestCount = 1;
        getParentFragmentManager()
                .beginTransaction()
                .detach(com.dimonvideo.client.ui.uploader.UploaderFragment.this)
                .attach(com.dimonvideo.client.ui.uploader.UploaderFragment.this)
                .commit();
    }

}