package com.dimonvideo.client.util;

import com.dimonvideo.client.Config;

public class GetRazdelName {

    // получение данных по номеру раздела
    public static String getRazdelName(String razdel, int op) {

        String url = Config.COMMENTS_URL;
        String search_url = Config.COMMENTS_SEARCH_URL;
        String key = "comments";

        if (razdel != null) {
            if (razdel.equals("1") || razdel.equals("gallery")) {
                url = Config.GALLERY_URL;
                search_url = Config.GALLERY_SEARCH_URL;
                key = Config.GALLERY_RAZDEL;
            }
            if (razdel.equals("2") || razdel.equals("uploader")) {
                url = Config.UPLOADER_URL;
                search_url = Config.UPLOADER_SEARCH_URL;
                key = Config.UPLOADER_RAZDEL;

            }
            if (razdel.equals("3") || razdel.equals("vuploader")) {
                url = Config.VUPLOADER_URL;
                search_url = Config.VUPLOADER_SEARCH_URL;
                key = Config.VUPLOADER_RAZDEL;

            }
            if (razdel.equals("4") || razdel.equals("usernews")) {
                url = Config.NEWS_URL;
                search_url = Config.NEWS_SEARCH_URL;
                key = Config.NEWS_RAZDEL;

            }
            if (razdel.equals("5") || razdel.equals("muzon")) {
                url = Config.MUZON_URL;
                search_url = Config.MUZON_SEARCH_URL;
                key = Config.MUZON_RAZDEL;

            }
            if (razdel.equals("6") || razdel.equals("books")) {
                url = Config.BOOKS_URL;
                search_url = Config.BOOKS_SEARCH_URL;
                key = Config.BOOKS_RAZDEL;

            }
            if (razdel.equals("7") || razdel.equals("articles")) {
                url = Config.ARTICLES_URL;
                search_url = Config.ARTICLES_SEARCH_URL;
                key = Config.ARTICLES_RAZDEL;
            }
            if (razdel.equals("11") || razdel.equals("android")) {
                url = Config.ANDROID_URL;
                search_url = Config.ANDROID_SEARCH_URL;
                key = Config.ANDROID_RAZDEL;
            }
            if (razdel.equals("14") || razdel.equals("tracker")) {
                url = Config.TRACKER_URL;
                search_url = Config.TRACKER_SEARCH_URL;
                key = Config.TRACKER_RAZDEL;
            }
            if (razdel.equals("15") || razdel.equals("blog")) {
                url = Config.BLOG_URL;
                search_url = Config.BLOG_SEARCH_URL;
                key = Config.BLOG_RAZDEL;
            }
            if (razdel.equals("16") || razdel.equals("suploader")) {
                url = Config.SUPLOADER_URL;
                search_url = Config.SUPLOADER_SEARCH_URL;
                key = Config.SUPLOADER_RAZDEL;
            }
            if (razdel.equals("17") || razdel.equals("device")) {
                url = Config.DEVICE_URL;
                search_url = Config.DEVICE_SEARCH_URL;
                key = Config.DEVICE_RAZDEL;
            }
            if (razdel.equals("18") || razdel.equals("new")) {
                url = Config.NEW_FILES_URL;
                search_url = Config.UPLOADER_SEARCH_URL;
                key = Config.NEW_RAZDEL;
            }
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
