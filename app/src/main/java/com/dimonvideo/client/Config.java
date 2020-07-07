package com.dimonvideo.client;

public class Config {

    public static final String BASE_URL = "https://dimonvideo.ru";
    // RAZDEL NAMES
    public static final String COMMENTS_RAZDEL = "comments";
    public static final String UPLOADER_RAZDEL = "uploader";
    public static final String VUPLOADER_RAZDEL = "vuploader";
    public static final String NEWS_RAZDEL = "usernews";
    public static final String GALLERY_RAZDEL = "gallery";
    public static final String MUZON_RAZDEL = "muzon";
    public static final String BOOKS_RAZDEL = "books";
    public static final String ARTICLES_RAZDEL = "articles";
    public static final String GAMES_RAZDEL = "online";

    //Data URL
    public static final String COMMENTS_URL = BASE_URL + "/apps/dvclient.php?op=1&razdel="+COMMENTS_RAZDEL+"&min=";
    public static final String COMMENTS_SEARCH_URL = BASE_URL + "/apps/dvclient.php?op=3&razdel="+COMMENTS_RAZDEL+"&min=";
    public static final String UPLOADER_URL = BASE_URL + "/apps/dvclient.php?op=1&razdel="+UPLOADER_RAZDEL+"&min=";
    public static final String UPLOADER_SEARCH_URL = BASE_URL + "/apps/dvclient.php?op=3&razdel="+UPLOADER_RAZDEL+"&min=";
    public static final String VUPLOADER_URL = BASE_URL + "/apps/dvclient.php?op=1&razdel="+VUPLOADER_RAZDEL+"&min=";
    public static final String VUPLOADER_SEARCH_URL = BASE_URL + "/apps/dvclient.php?op=3&razdel="+VUPLOADER_RAZDEL+"&min=";
    public static final String NEWS_URL = BASE_URL + "/apps/dvclient.php?op=1&razdel="+NEWS_RAZDEL+"&min=";
    public static final String NEWS_SEARCH_URL = BASE_URL + "/apps/dvclient.php?op=3&razdel="+NEWS_RAZDEL+"&min=";
    public static final String GALLERY_URL = BASE_URL + "/apps/dvclient.php?op=1&razdel="+GALLERY_RAZDEL+"&min=";
    public static final String GALLERY_SEARCH_URL = BASE_URL + "/apps/dvclient.php?op=3&razdel="+GALLERY_RAZDEL+"&min=";
    public static final String MUZON_URL = BASE_URL + "/apps/dvclient.php?op=1&razdel="+MUZON_RAZDEL+"&min=";
    public static final String MUZON_SEARCH_URL = BASE_URL + "/apps/dvclient.php?op=3&razdel="+MUZON_RAZDEL+"&min=";
    public static final String BOOKS_URL = BASE_URL + "/apps/dvclient.php?op=1&razdel="+BOOKS_RAZDEL+"&min=";
    public static final String BOOKS_SEARCH_URL = BASE_URL + "/apps/dvclient.php?op=3&razdel="+BOOKS_RAZDEL+"&min=";
    public static final String ARTICLES_URL = BASE_URL + "/apps/dvclient.php?op=1&razdel="+ARTICLES_RAZDEL+"&min=";
    public static final String ARTICLES_SEARCH_URL = BASE_URL + "/apps/dvclient.php?op=3&razdel="+ARTICLES_RAZDEL+"&min=";
    public static final String GAMES_URL = BASE_URL + "/apps/dvclient.php?op=1&razdel="+GAMES_RAZDEL+"&min=";
    public static final String GAMES_SEARCH_URL = BASE_URL + "/apps/dvclient.php?op=3&razdel="+GAMES_RAZDEL+"&min=";

    public static final String FORUM_FEED_URL = BASE_URL + "/apps/dvclient.php?op=5&min=";
    public static final String FORUM_FEED_NO_POSTS_URL = BASE_URL + "/apps/dvclient.php?op=5&id=-1&min=";
    public static final String FORUM_CATEGORY_URL = BASE_URL + "/apps/dvclient.php?op=6&min=";
    public static final String FORUM_POSTS_URL = BASE_URL + "/apps/dvclient.php?op=7&min=";

    public static final String TEXT_URL = BASE_URL + "/apps/dvclient.php?op=2&razdel=";
    public static final String COMMENTS_READS_URL = BASE_URL + "/apps/dvclient.php?op=4&razdel=";

    //JSON TAGS
    public static final String TAG_ID = "lid";
    public static final String TAG_IMAGE_URL = "image";
    public static final String TAG_TITLE = "title";
    public static final String TAG_TEXT = "text";
    public static final String TAG_COMMENTS = "rating";
    public static final String TAG_DATE = "date";
    public static final String TAG_RAZDEL = "razdel";
    public static final String TAG_CATEGORY = "category";
    public static final String TAG_HEADERS = "headers";
    public static final String TAG_USER = "user";
    public static final String TAG_SIZE = "size";
    public static final String TAG_HITS = "views";
    public static final String TAG_LINK = "file_link";
    public static final String TAG_MOD = "mod";
    public static final String TAG_STORY = "story";
    public static final String TAG_TIME = "time";
    public static final String TAG_LAST_POSTER_NAME = "last_poster_name";
    public static final String TAG_STATE = "state";
    public static final String TAG_PINNED = "pinned";



}

