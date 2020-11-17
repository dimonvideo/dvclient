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
    public static final String ANDROID_RAZDEL = "android";
    public static final String TRACKER_RAZDEL = "tracker";
    public static final String BLOG_RAZDEL = "blog";

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
    public static final String ANDROID_URL = BASE_URL + "/apps/dvclient.php?op=1&razdel="+ANDROID_RAZDEL+"&min=";
    public static final String ANDROID_SEARCH_URL = BASE_URL + "/apps/dvclient.php?op=3&razdel="+ANDROID_RAZDEL+"&min=";
    public static final String TRACKER_URL = BASE_URL + "/apps/dvclient.php?op=1&razdel="+TRACKER_RAZDEL+"&min=";
    public static final String TRACKER_SEARCH_URL = BASE_URL + "/apps/dvclient.php?op=3&razdel="+TRACKER_RAZDEL+"&min=";
    public static final String BLOG_URL = BASE_URL + "/apps/dvclient.php?op=1&razdel="+BLOG_RAZDEL+"&min=";
    public static final String BLOG_SEARCH_URL = BASE_URL + "/apps/dvclient.php?op=3&razdel="+BLOG_RAZDEL+"&min=";

    public static final String FORUM_FEED_URL = BASE_URL + "/apps/dvclient.php?op=5&min=";
    public static final String FORUM_FEED_NO_POSTS_URL = BASE_URL + "/apps/dvclient.php?op=5&id=-1&min=";
    public static final String FORUM_CATEGORY_URL = BASE_URL + "/apps/dvclient.php?op=6&min=";
    public static final String FORUM_POSTS_URL = BASE_URL + "/apps/dvclient.php?op=7&min=";

    public static final String COMMENTS_READS_URL = BASE_URL + "/apps/dvclient.php?op=4&razdel=";
    public static final String CATEGORY_URL = BASE_URL + "/apps/dvclient.php?op=9&razdel=";

    public static final String CHECK_AUTH_URL = BASE_URL + "/apps/dvclient.php?op=10";
    public static final String PM_URL = BASE_URL + "/apps/dvclient.php?op=11&min=";
    public static final String REGISTRATION_URL = BASE_URL + "/apps/dvclient.php?op=12";

    public static final String LIKE_URL = BASE_URL + "/apps/dvclient.php?op=8&razdel=";
    public static final String LIKE_POST_URL = BASE_URL + "/apps/dvclient.php?op=13";

    //JSON TAGS
    public static final String TAG_ID = "lid";
    public static final String TAG_IMAGE_URL = "image";
    public static final String TAG_FAV = "fav";
    public static final String TAG_TITLE = "title";
    public static final String TAG_TEXT = "text";
    public static final String TAG_FULL_TEXT = "full_text";
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
    public static final String TAG_COUNT = "count";
    public static final String TAG_LAST_POSTER_NAME = "last_poster_name";
    public static final String TAG_STATE = "state";
    public static final String TAG_PINNED = "pinned";
    public static final String TAG_MIN = "min";
    public static final String TAG_PLUS = "plus";
    public static final String TAG_PM_UNREAD = "pm_unread";
    public static final String TAG_TOKEN = "token";
    public static final String TAG_NEW_TOPIC = "newtopic";
    public static final String TAG_TOPIC_ID = "topic_id";
    public static final String TAG_REP = "reputation";
    public static final String TAG_REG = "reg_date";

}

