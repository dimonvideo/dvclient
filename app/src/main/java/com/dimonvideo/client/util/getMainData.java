package com.dimonvideo.client.util;

import androidx.recyclerview.widget.RecyclerView;

import com.dimonvideo.client.Config;
import com.dimonvideo.client.model.Feed;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class getMainData {

    // разбор полученных данных
    public static void parseData(JSONArray array, List<Feed> listFeed, RecyclerView.Adapter adapter) {
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
                jsonFeed.setCategory(json.getString(Config.TAG_CATEGORY));
                jsonFeed.setHeaders(json.getString(Config.TAG_HEADERS));
                jsonFeed.setId(json.getInt(Config.TAG_ID));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            listFeed.add(jsonFeed);
        }
        adapter.notifyDataSetChanged();
    }

}
