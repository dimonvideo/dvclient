package com.dimonvideo.client.util;

import com.dimonvideo.client.Config;

public class GetRazdelName {

    // получение данных по номеру раздела
    public static String getRazdelName(int razdel, int op) {

        String url = Config.COMMENTS_URL;
        String search_url = Config.COMMENTS_SEARCH_URL;
        String key = "comments";

        if (razdel == 1) {
            url = Config.GALLERY_URL;
            search_url = Config.GALLERY_SEARCH_URL;
            key = Config.GALLERY_RAZDEL;
        }
        if (razdel == 2) {
            url = Config.UPLOADER_URL;
            search_url = Config.UPLOADER_SEARCH_URL;
            key = Config.UPLOADER_RAZDEL;

        }
        if (razdel == 3) {
            url = Config.VUPLOADER_URL;
            search_url = Config.VUPLOADER_SEARCH_URL;
            key = Config.VUPLOADER_RAZDEL;

        }
        if (razdel == 4) {
            url = Config.NEWS_URL;
            search_url = Config.NEWS_SEARCH_URL;
            key = Config.NEWS_RAZDEL;

        }
        if (razdel == 5) {
            url = Config.MUZON_URL;
            search_url = Config.MUZON_SEARCH_URL;
            key = Config.MUZON_RAZDEL;

        }
        if (razdel == 6) {
            url = Config.BOOKS_URL;
            search_url = Config.BOOKS_SEARCH_URL;
            key = Config.BOOKS_RAZDEL;

        }
        if (razdel == 7) {
            url = Config.ARTICLES_URL;
            search_url = Config.ARTICLES_SEARCH_URL;
            key = Config.ARTICLES_RAZDEL;
        }
        if (razdel == 11) {
            url = Config.ANDROID_URL;
            search_url = Config.ANDROID_SEARCH_URL;
            key = Config.ANDROID_RAZDEL;
        }
        if (razdel == 14) {
            url = Config.TRACKER_URL;
            search_url = Config.TRACKER_SEARCH_URL;
            key = Config.TRACKER_RAZDEL;
        }
        if (razdel == 15) {
            url = Config.BLOG_URL;
            search_url = Config.BLOG_SEARCH_URL;
            key = Config.BLOG_RAZDEL;
        }

        switch (op) {
            case 1:
                return search_url;
            case 2:
                return url;
            default:
                return key;
        }
    }
}
