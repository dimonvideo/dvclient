package com.dimonvideo.client.ui.main;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.StringRequest;
import com.dimonvideo.client.Config;
import com.dimonvideo.client.MainActivity;
import com.dimonvideo.client.R;
import com.dimonvideo.client.databinding.FragmentAddBinding;
import com.dimonvideo.client.model.FeedCats;
import com.dimonvideo.client.util.AppController;
import com.dimonvideo.client.util.GetRazdelName;
import com.dimonvideo.client.util.MessageEvent;
import com.dimonvideo.client.util.ProgressHelper;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainFragmentAddFile extends Fragment {

    private String razdel = "usernews";
    private FragmentAddBinding binding;
    private List<String> listFeed, listFeedRazdel, listFeedCategory;
    private Spinner categoryList;
    private EditText desc, catalog, ist, title;
    private FrameLayout screenFrame;
    private RelativeLayout istFrame;
    private ImageButton imgBtn;
    private TextView selectScreen, note;
    private Button btnClose, btnSend;
    private String login_name;
    private String screenName;
    private String categoryId;
    private AlertDialog alert;
     AlertDialog.Builder builder;
    private ClipboardManager myClipboard;
    private ClipData myClip;
    private boolean doubleBackToExitPressedOnce = false;

    public MainFragmentAddFile() {

    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEvent event) {
        Bitmap bitmap = event.bitmap;
        screenName = Config.THUMB_URL + login_name + File.separator + event.image_uploaded;

        // если скриншот загружен на сервер
        if (bitmap != null) {
            BitmapDrawable bitmapDrawable = new BitmapDrawable(getResources(), bitmap);
            imgBtn.setBackground(bitmapDrawable);
            selectScreen.setVisibility(View.GONE);
            desc.setVisibility(View.VISIBLE);
            btnSend.setVisibility(View.VISIBLE);
            btnClose.setVisibility(View.VISIBLE);

            if (login_name.equals("DimonVideo")) catalog.setVisibility(View.VISIBLE);

            if (listFeedRazdel.get(0).equals(Config.NEWS_RAZDEL)) {
                istFrame.setVisibility(View.VISIBLE);
                ist.setText(Config.BASE_URL);
            }

            note.setVisibility(View.GONE);

            // кнопка Закрыть
            btnClose.setOnClickListener(v -> {

                builder = new AlertDialog.Builder(requireContext());
                builder.setTitle(getString(R.string.warning));
                builder.setMessage(getString(R.string.warning_message));
                builder.setPositiveButton(getString(R.string.ok), (dialog, which) -> {
                    dialog.dismiss();
                    MainActivity.navController.navigate(R.id.nav_home);
                });
                builder.setNegativeButton(getString(R.string.no), (dialog, which) -> dialog.dismiss());
                alert = builder.create();
                alert.show();

            });

            // кнопка Отправить
            btnSend.setOnClickListener(v -> {

                builder = new AlertDialog.Builder(requireContext());
                builder.setTitle(getString(R.string.warning));
                builder.setMessage(getString(R.string.check_message)+"\n\n"+getString(R.string.signature_title)+": "+login_name);
                builder.setPositiveButton(getString(R.string.ok), (dialog, which) -> {
                    dialog.dismiss();
                    ProgressHelper.showDialog(requireContext(), getString(R.string.please_wait));
                    StringRequest stringRequest = new StringRequest(Request.Method.POST, Config.UPLOAD_FILE_URL, response -> {

                        JSONObject obj;
                        try {
                            obj = new JSONObject(response);

                            String msg = obj.getString(Config.TAG_LINK);
                            String err = obj.getString("error");

                            if (err.equals("false")) {
                                Toast.makeText(requireContext(), requireContext().getString(R.string.success_send_file), Toast.LENGTH_LONG).show();
                                MainActivity.navController.navigate(R.id.nav_home);
                                try {
                                    myClipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
                                    myClip = ClipData.newPlainText("text", msg);
                                    myClipboard.setPrimaryClip(myClip);
                                } catch (Throwable ignored) {
                                }
                            }
                            if (err.equals("true")) Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
                            ProgressHelper.dismissDialog();


                        } catch (Exception ignored) {
                            ProgressHelper.dismissDialog();
                        }
                    }, Throwable::printStackTrace) {

                        @Override
                        protected Map<String, String> getParams() {
                            Map<String, String> postMap = new HashMap<>();
                            postMap.put("send_user", login_name);
                            postMap.put("send_razdel", listFeedRazdel.get(0));
                            postMap.put("send_category", categoryId);
                            postMap.put("send_title", title.getText().toString());
                            postMap.put("send_desc", desc.getText().toString());
                            postMap.put("send_screen", screenName);
                            postMap.put("send_catalog", catalog.getText().toString());
                            postMap.put("send_ist", ist.getText().toString());
                            return postMap;
                        }
                    };

                    AppController.getInstance().addToRequestQueue(stringRequest);

                });
                builder.setNegativeButton(getString(R.string.no), (dialog, which) -> dialog.dismiss());
                alert = builder.create();
                alert.show();
            });
        }
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAddBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }

        EventBus.getDefault().postSticky(new MessageEvent("0", null, null, null, null, null));

        // проверка имени пользователя
        login_name = AppController.getInstance().userName(null);
        if (login_name == null) MainActivity.navController.navigate(R.id.nav_home);

        Spinner razdelList = (Spinner) binding.razdel;
        razdelList.setVisibility(View.GONE);
        title = binding.title;

        ist = binding.ist;

        istFrame = binding.istLayout;
        istFrame.setVisibility(View.GONE);

        screenFrame = binding.screen;
        screenFrame.setVisibility(View.GONE);

        selectScreen = binding.screenText;

        btnClose = binding.dismiss;
        btnSend = binding.save;

        btnSend.setVisibility(View.GONE);
        btnClose.setVisibility(View.GONE);

        imgBtn = binding.imgBtn;

        // название файла
        title.setOnEditorActionListener(
                (v, actionId, event) -> {
                    if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                            actionId == EditorInfo.IME_ACTION_DONE ||
                            event != null &&
                                    event.getAction() == KeyEvent.ACTION_DOWN &&
                                    event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                        if (event == null || !event.isShiftPressed()) {

                            if (TextUtils.isEmpty(title.getText().toString().trim())){
                                Toast.makeText(requireContext(), getString(R.string.title_retry), Toast.LENGTH_SHORT).show();
                            }else {
                                razdelList.setVisibility(View.VISIBLE);
                            }

                            return true;
                        }
                    }
                    return false;
                }
        );


        categoryList = (Spinner) binding.categories;
        categoryList.setVisibility(View.GONE);

        desc = binding.desc;
        catalog = binding.catalog;
        desc.setVisibility(View.GONE);

        note = binding.note;
        note.setMovementMethod(LinkMovementMethod.getInstance());

        // выбор раздела
        ArrayAdapter<CharSequence> razdelListAdapter = ArrayAdapter
                .createFromResource(requireContext(), R.array.add_razdel,
                        android.R.layout.simple_spinner_item);
        razdelListAdapter
                .setDropDownViewResource(android.R.layout.select_dialog_singlechoice);
        razdelList.setAdapter(razdelListAdapter);
        razdelList.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                if (position > 0) {
                    listFeedRazdel = new ArrayList<>();
                    categoryList.setVisibility(View.VISIBLE);
                    if (position == 1) {
                        razdel = Config.NEWS_RAZDEL;
                        listFeedRazdel.add(razdel);
                    }
                    if (position == 2) {
                        razdel = Config.GALLERY_RAZDEL;
                        istFrame.setVisibility(View.GONE);
                        listFeedRazdel.add(razdel);
                    }
                    if (position == 3) {
                        razdel = Config.BLOG_RAZDEL;
                        listFeedRazdel.add(razdel);
                        istFrame.setVisibility(View.GONE);
                    }
                    if (position == 4) {
                        razdel = Config.ARTICLES_RAZDEL;
                        istFrame.setVisibility(View.GONE);
                        listFeedRazdel.add(razdel);
                    }
                    getData();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });


        // перехват кнопки назад.
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {

                if (doubleBackToExitPressedOnce) {
                    MainActivity.navController.navigate(R.id.nav_home);
                    return;
                }

                doubleBackToExitPressedOnce = true;
                Toast.makeText(requireContext(), getString(R.string.press_twice), Toast.LENGTH_SHORT).show();
                new Handler(Looper.getMainLooper()).postDelayed(() -> doubleBackToExitPressedOnce=false, 2000);

            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), callback);
    }

    private JsonArrayRequest getDataFromServer() {

        ProgressHelper.showDialog(requireContext(), getString(R.string.please_wait));
        String key = GetRazdelName.getRazdelName(razdel, 0);
        String url = Config.CATEGORY_URL;
        listFeed = new ArrayList<>();
        listFeedCategory = new ArrayList<>();
        listFeed.add(requireContext().getString(R.string.choose_category));
        listFeedCategory.add("0");
        return new JsonArrayRequest(url + key,
                response -> {
                    for (int i = 0; i < response.length(); i++) {
                        FeedCats jsonFeed = new FeedCats();
                        JSONObject json;
                        try {
                            json = response.getJSONObject(i);
                            jsonFeed.setTitle(json.getString(Config.TAG_TITLE));
                            jsonFeed.setCid(json.getInt(Config.TAG_ID));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        listFeed.add(String.valueOf(jsonFeed.getTitle()));
                        listFeedCategory.add(String.valueOf(jsonFeed.getCid()));

                        // выбор подкатегории
                        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item , listFeed);
                        categoryAdapter.setDropDownViewResource(
                                android.R.layout.simple_spinner_dropdown_item );
                        categoryList.setAdapter(categoryAdapter);
                        ProgressHelper.dismissDialog();
                        categoryList.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                            @Override
                            public void onItemSelected(AdapterView<?> parent, View view,
                                                       int position, long id) {
                                if (position > 0) {
                                    screenFrame.setVisibility(View.VISIBLE);

                                    categoryId = listFeedCategory.get(position);

                                    if (listFeedRazdel.get(0).equals(Config.NEWS_RAZDEL)) {
                                        istFrame.setVisibility(View.VISIBLE);
                                    } else istFrame.setVisibility(View.GONE);

                                    // загрузка скриншота
                                    imgBtn.setOnClickListener(v -> {
                                        MainActivity.pickMedia.launch(new PickVisualMediaRequest.Builder()
                                                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                                                .build());
                                    });
                                    selectScreen.setOnClickListener(v -> {
                                        MainActivity.pickMedia.launch(new PickVisualMediaRequest.Builder()
                                                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                                                .build());
                                    });
                                }
                            }

                            @Override
                            public void onNothingSelected(AdapterView<?> parent) {
                            }
                        });
                    }
                },
                error -> {
                    ProgressHelper.dismissDialog();
                });
    }

    private void getData() {
        AppController.getInstance().addToRequestQueue(getDataFromServer());
    }

    @Override
    public void onDestroy() {
        if (EventBus.getDefault().isRegistered(this)) EventBus.getDefault().unregister(this);
        if (ProgressHelper.isDialogVisible()) ProgressHelper.dismissDialog();
        super.onDestroy();
        binding = null;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (EventBus.getDefault().isRegistered(this)) EventBus.getDefault().unregister(this);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

}