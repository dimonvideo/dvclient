package com.dimonvideo.client.ui.home;

import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
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
import com.dimonvideo.client.adater.CardAdapter;
import com.dimonvideo.client.model.Feed;
import com.dimonvideo.client.util.getMainData;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@RequiresApi(api = Build.VERSION_CODES.M)
public class HomeFragment extends Fragment implements RecyclerView.OnScrollChangeListener, SwipeRefreshLayout.OnRefreshListener  {


    private List<Feed> listFeed;
    private RecyclerView recyclerView;
    private RecyclerView.Adapter adapter;
    SwipeRefreshLayout swipLayout;
    private TextView tvEmptyView;

    private RequestQueue requestQueue;

    private int requestCount = 1;
    private ProgressBar progressBar, ProgressBarBottom;
    int razdel = 0;
    String url = Config.COMMENTS_URL;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_home, container, false);

        if (this.getArguments() != null) {
            razdel = getArguments().getInt("Category");

            if (razdel == 1) url = Config.GALLERY_URL;
            if (razdel == 2) url = Config.UPLOADER_URL;
            if (razdel == 3) url = Config.VUPLOADER_URL;
            if (razdel == 4) url = Config.NEWS_URL;
            if (razdel == 5) url = Config.MUZON_URL;
            if (razdel == 6) url = Config.BOOKS_URL;
            if (razdel == 7) url = Config.ARTICLES_URL;

        }

        recyclerView = root.findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);

        listFeed = new ArrayList<>();
        requestQueue = Volley.newRequestQueue(requireActivity());

        progressBar = root.findViewById(R.id.progressbar);
        progressBar.setVisibility(View.VISIBLE);
        ProgressBarBottom = root.findViewById(R.id.ProgressBarBottom);
        ProgressBarBottom.setVisibility(View.GONE);
        // получение данных
        getData();

        recyclerView.setOnScrollChangeListener(this);
        adapter = new CardAdapter(listFeed, getContext());

        // разделитель позиций
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(), DividerItemDecoration.VERTICAL);
        dividerItemDecoration.setDrawable(Objects.requireNonNull(ContextCompat.getDrawable(requireContext(), R.drawable.divider)));
        recyclerView.addItemDecoration(dividerItemDecoration);

        // pull to refresh
        swipLayout = root.findViewById(R.id.swipe_layout);
        swipLayout.setOnRefreshListener(this);

        recyclerView.setAdapter(adapter);
        tvEmptyView = (TextView) root.findViewById(R.id.empty_view);

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
        return new JsonArrayRequest(url + requestCount + "&c=placeholder," + category,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        progressBar.setVisibility(View.GONE);
                        ProgressBarBottom.setVisibility(View.GONE);
                        getMainData.parseData(response, listFeed, adapter); // парсинг данных

                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        progressBar.setVisibility(View.GONE);
                        ProgressBarBottom.setVisibility(View.GONE);
                        tvEmptyView.setVisibility(View.VISIBLE);
                        Toast.makeText(getContext(), getString(R.string.no_more), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // получение данных и увеличение номера страницы
    private void getData() {
        ProgressBarBottom.setVisibility(View.VISIBLE);
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
                .detach(HomeFragment.this)
                .attach(HomeFragment.this)
                .commit();
    }

public ProgressDialog ProgressDialogShow() {
        ProgressDialog progressDialog;
        progressDialog = new ProgressDialog(getContext());
        progressDialog.setTitle("Write Title here");
        progressDialog.setMessage("Loading...");

        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setCancelable(false);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setIndeterminate(true);
        return progressDialog;
    }

}