package com.dimonvideo.client;

public class Config {

    public static final String BASE_URL = "https://dimonvideo.ru";

    //Data URL
    public static final String COMMENTS_URL = BASE_URL + "/apps/dvclient.php?op=1&razdel=comments&min=";
    public static final String COMMENTS_SEARCH_URL = BASE_URL + "/apps/dvclient.php?op=3&razdel=comments&min=";
    public static final String UPLOADER_URL = BASE_URL + "/apps/dvclient.php?op=1&razdel=uploader&min=";
    public static final String UPLOADER_SEARCH_URL = BASE_URL + "/apps/dvclient.php?op=3&razdel=uploader&min=";
    public static final String VUPLOADER_URL = BASE_URL + "/apps/dvclient.php?op=1&razdel=vuploader&min=";
    public static final String VUPLOADER_SEARCH_URL = BASE_URL + "/apps/dvclient.php?op=3&razdel=vuploader&min=";
    public static final String NEWS_URL = BASE_URL + "/apps/dvclient.php?op=1&razdel=usernews&min=";
    public static final String NEWS_SEARCH_URL = BASE_URL + "/apps/dvclient.php?op=3&razdel=usernews&min=";
    public static final String GALLERY_URL = BASE_URL + "/apps/dvclient.php?op=1&razdel=gallery&min=";
    public static final String GALLERY_SEARCH_URL = BASE_URL + "/apps/dvclient.php?op=3&razdel=gallery&min=";
    public static final String MUZON_URL = BASE_URL + "/apps/dvclient.php?op=1&razdel=muzon&min=";
    public static final String MUZON_SEARCH_URL = BASE_URL + "/apps/dvclient.php?op=3&razdel=muzon&min=";
    public static final String BOOKS_URL = BASE_URL + "/apps/dvclient.php?op=1&razdel=books&min=";
    public static final String BOOKS_SEARCH_URL = BASE_URL + "/apps/dvclient.php?op=3&razdel=books&min=";
    public static final String ARTICLES_URL = BASE_URL + "/apps/dvclient.php?op=1&razdel=articles&min=";
    public static final String ARTICLES_SEARCH_URL = BASE_URL + "/apps/dvclient.php?op=3&razdel=articles&min=";
    public static final String GAMES_URL = BASE_URL + "/apps/dvclient.php?op=1&razdel=online&min=";
    public static final String GAMES_SEARCH_URL = BASE_URL + "/apps/dvclient.php?op=3&razdel=online&min=";

    public static final String TEXT_URL = BASE_URL + "/apps/dvclient.php?op=2&razdel=";

    //JSON TAGS
    public static final String TAG_IMAGE_URL = "image";
    public static final String TAG_TITLE = "title";
    public static final String TAG_TEXT = "text";
    public static final String TAG_COMMENTS = "rating";
    public static final String TAG_DATE = "date";
    public static final String TAG_ID = "lid";
    public static final String TAG_RAZDEL = "razdel";
    public static final String TAG_CATEGORY = "category";
    public static final String TAG_HEADERS = "headers";
    public static final String TAG_USER = "user";
    public static final String TAG_SIZE = "size";
    public static final String TAG_HITS = "views";
    public static final String TAG_LINK = "file_link";
    public static final String TAG_MOD = "mod";
    public static final String TAG_RAZDEL_ID = "Category";
    public static final String TAG_STORY = "story";

}

