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
    public static final String TAG_COUNT = "count";
    public static final String TAG_POSITION = "position";
    public static final String TAG_MIN = "min";
    public static final String TAG_JSON = "json";

    public static final String RESPONSE_COMMENTS = "[{\"lid\":4242,\"min\":1,\"views\":44,\"file_link\":\"https:\\/\\/dimonvideo.ru\\/files\\/screens.dimonvideo.ru\\/uploader\\/rabota-s-telefonami-pk\\/490149_screenshot0.jpg\",\"mod\":null,\"user\":\"c1cl0n\",\"size\":\"0 b\",\"razdel\":\"comments\",\"headers\":\"Новости\",\"category\":\"Новости\",\"date\":\"вчера в 13:01\",\"time\":1594288869,\"title\":\"Basic4android 10.00-beta\",\"text\":\"Basic4android - среда разработки приложений для ОС Android любой сложности, включает в себя весь необходимый инструмент...\",\"image\":\"https:\\/\\/dimonvideo.ru\\/files\\/screens.dimonvideo.ru\\/uploader\\/rabota-s-telefonami-pk\\/490149_screenshot0.jpg\",\"rating\":0},{\"lid\":4241,\"min\":1,\"views\":152,\"file_link\":\"https:\\/\\/dimonvideo.ru\\/files\\/screens.dimonvideo.ru\\/uploader\\/android-mody\\/468484_screenshot_20200602-103359_mx-player-pro.png\",\"mod\":null,\"user\":\"IM_V74\",\"size\":\"0 b\",\"razdel\":\"comments\",\"headers\":\"Новости\",\"category\":\"Новости\",\"date\":\"11:18 7 июл 2020\",\"time\":1594109932,\"title\":\"MX Player Pro 1.25.5\",\"text\":\"Мощный видеоплеер с аппаратным ускорением и поддержкой субтитров.Мод Инфо:Оптимизированная графика и очищенные ресурсы для быстр...\",\"image\":\"https:\\/\\/dimonvideo.ru\\/files\\/screens.dimonvideo.ru\\/uploader\\/android-mody\\/468484_screenshot_20200602-103359_mx-player-pro.png\",\"rating\":0},{\"lid\":4240,\"min\":1,\"views\":42,\"file_link\":\"\\/files\\/newsimg\\/382300\\/img.jpg\",\"mod\":null,\"user\":\"Combrig\",\"size\":\"0 b\",\"razdel\":\"comments\",\"headers\":\"Новости\",\"category\":\"Новости\",\"date\":\"23:58 6 июл 2020\",\"time\":1594069080,\"title\":\"FolderSync Pro 3.0.0 - анонс\",\"text\":\"Самый популярный файл Обменника прошедшего понедельника: FolderSync Pro в категории: Android - разные программы. от автора:...\",\"image\":\"\\/files\\/newsimg\\/382300\\/img.jpg\",\"rating\":0},{\"lid\":4239,\"min\":1,\"views\":93,\"file_link\":\"\\/files\\/newsimg\\/384373\\/img.jpg\",\"mod\":null,\"user\":\"zhenyatut\",\"size\":\"0 b\",\"razdel\":\"comments\",\"headers\":\"Новости\",\"category\":\"Новости\",\"date\":\"23:58 5 июл 2020\",\"time\":1593982680,\"title\":\"Crying Suns 1.4.2 - анонс\",\"text\":\"Самый популярный файл Обменника прошедшего воскресения: Crying Suns в категории: Android - стратегии. от автора:\\nCryin...\",\"image\":\"\\/files\\/newsimg\\/384373\\/img.jpg\",\"rating\":0},{\"lid\":4238,\"min\":1,\"views\":52,\"file_link\":\"https:\\/\\/dimonvideo.ru\\/files\\/screens.dimonvideo.ru\\/uploader\\/rabota-s-audio-pk\\/469199_00_proc.png\",\"mod\":null,\"user\":\"gr429842534\",\"size\":\"0 b\",\"razdel\":\"comments\",\"headers\":\"Новости\",\"category\":\"Новости\",\"date\":\"08:37 5 июл 2020\",\"time\":1593927436,\"title\":\"Балаболка 2.15.0.747\",\"text\":\"Программа предназначена для чтения вслух текстовых файлов.Для воспроизведения звуков голоса используются любые речевые синтезаторы, установленные ...\",\"image\":\"https:\\/\\/dimonvideo.ru\\/files\\/screens.dimonvideo.ru\\/uploader\\/rabota-s-audio-pk\\/469199_00_proc.png\",\"rating\":0},{\"lid\":4237,\"min\":1,\"views\":106,\"file_link\":\"\\/files\\/newsimg\\/468193\\/img.jpg\",\"mod\":null,\"user\":\"grazer08\",\"size\":\"0 b\",\"razdel\":\"comments\",\"headers\":\"Новости\",\"category\":\"Новости\",\"date\":\"23:58 4 июл 2020\",\"time\":1593896280,\"title\":\"3D Youtube Downloader 1.19.4 - анонс\",\"text\":\"Самый популярный файл Обменника прошедшей субботы: 3D Youtube Downloader в категории: Интернет - ПК. Программа предназн...\",\"image\":\"\\/files\\/newsimg\\/468193\\/img.jpg\",\"rating\":0},{\"lid\":4236,\"min\":1,\"views\":185,\"file_link\":\"\\/files\\/newsimg\\/384349\\/img.png\",\"mod\":null,\"user\":\"IM_V74\",\"size\":\"0 b\",\"razdel\":\"comments\",\"headers\":\"Новости\",\"category\":\"Новости\",\"date\":\"23:58 3 июл 2020\",\"time\":1593809880,\"title\":\"HDVideoBox+ 2.24 - анонс\",\"text\":\"Самый популярный файл Обменника прошедшей пятницы: HDVideoBox в категории: Android - мультимедиа. от автора:\\nТысячи фильмов, му...\",\"image\":\"\\/files\\/newsimg\\/384349\\/img.png\",\"rating\":0},{\"lid\":4235,\"min\":1,\"views\":89,\"file_link\":\"\\/files\\/newsimg\\/382604\\/img.png\",\"mod\":null,\"user\":\"zhenyatut\",\"size\":\"0 b\",\"razdel\":\"comments\",\"headers\":\"Новости\",\"category\":\"Новости\",\"date\":\"23:58 2 июл 2020\",\"time\":1593723480,\"title\":\"Jetpack Joyride 1.29.4 - анонс\",\"text\":\"Самый популярный файл Обменника прошедшего четверга: Jetpack Joyride в категории: Android - аркады. от автора:Пулеме...\",\"image\":\"\\/files\\/newsimg\\/382604\\/img.png\",\"rating\":0},{\"lid\":4234,\"min\":1,\"views\":80,\"file_link\":\"https:\\/\\/dimonvideo.ru\\/files\\/screens.dimonvideo.ru\\/uploader\\/android-multimedia\\/486228_screenshot_2019-10-19-19-18-31-904_com.videoeditorpro.android.png\",\"mod\":null,\"user\":\"Android\",\"size\":\"0 b\",\"razdel\":\"comments\",\"headers\":\"Новости\",\"category\":\"Новости\",\"date\":\"20:26 2 июл 2020\",\"time\":1593710773,\"title\":\"VivaCut Видеоредактор: монтаж видео и обработка 1.5.6\",\"text\":\"VivaCut для Android - это профессиональный видеоредактор. Создавайте видео истории с использованием многослойной временной ш...\",\"image\":\"https:\\/\\/dimonvideo.ru\\/files\\/screens.dimonvideo.ru\\/uploader\\/android-multimedia\\/486228_screenshot_2019-10-19-19-18-31-904_com.videoeditorpro.android.png\",\"rating\":0},{\"lid\":4233,\"min\":1,\"views\":84,\"file_link\":\"https:\\/\\/dimonvideo.ru\\/files\\/screens.dimonvideo.ru\\/uploader\\/android-multimedia\\/482445_screenshot_2020-03-08-21-12-00-353_mdmt.sabp.jpg\",\"mod\":null,\"user\":\"Android\",\"size\":\"0 b\",\"razdel\":\"comments\",\"headers\":\"Новости\",\"category\":\"Новости\",\"date\":\"08:26 2 июл 2020\",\"time\":1593667570,\"title\":\"Simple Audiobook Player 1.7.8\",\"text\":\"Простой, быстрый и функциональный проигрыватель аудиокниг.Возможности:•Закладки•Настраива...\",\"image\":\"https:\\/\\/dimonvideo.ru\\/files\\/screens.dimonvideo.ru\\/uploader\\/android-multimedia\\/482445_screenshot_2020-03-08-21-12-00-353_mdmt.sabp.jpg\",\"rating\":0}]";
}

