<?php
/*
 * @Автор: Дмитрий Вороной (DimonVideo) 
 * @Email: dimon@dimonvideo.ru
 * @Создано: 16-12-2025 19:33:14 
 * @Изменено: 16-12-2025 19:33:14
 * @Описание: file:///home/dimonvideo.ru/html/apps/dvclient.php
 */

@error_reporting(E_ALL ^ E_WARNING ^ E_DEPRECATED ^ E_NOTICE);
@ini_set('error_reporting', E_ALL ^ E_WARNING ^ E_DEPRECATED ^ E_NOTICE);

@ini_set('display_errors', false);
@ini_set('html_errors', false);

define('DATALIFEENGINE', true);
define('ROOT_DIR', str_replace('apps', '', dirname(__FILE__)));
define('ENGINE_DIR', ROOT_DIR . '/engine');
define('API_ACCESS_KEY', 'AAAArtjh3Dc:APA91bHt3z_G0FG3Hi14bcH9H3QHkSfVu9xOBugy6kPAzH7hlRF-s69f0BRRCoUfBIfbrE0nL4Nkev5LZi3zj2-SZgWogWk3J4fmirtXnnkMKDErgCwyhEAXpwyu4YCRll4tH_FSxAmI');
$member_id = FALSE;
$is_logged = FALSE;
require_once ROOT_DIR . '/engine/init.php';

if (empty($_GET['op'])) {
  if ($is_logged)
    $data['state'] = 1;
  else
    $data['state'] = 0;
  echo json_encode($data);
}


include_once ROOT_DIR . '/apps/dvclient/funct.php';


$op = abs(intval($_GET['op']));
$page = abs(intval($_GET['min']));
$min = 0;

if ($page > 1) {
  // exit();
  $min = intval(floor(($page * 10) - 10));
}

$fo = 10;
$p = $page;

$razdel = $db->safeSQL($_GET['razdel']);
$category = $db->safeSQL($_GET['c']);
$table = tabhead($razdel, 0, 1);
$headers = tabhead($razdel, 1, 1);
$account = intval($_GET['u']);
$login_name = trim($db->safesql(strip_tags((string) $_GET['login_name'])));
$login_password = md5((string) $_GET['login_password']);
if (preg_match("/[\||\'|\<|\>|\"|\!|\?|\$|\@|\/|\\\|\&\~\*\+]/", $_GET['login_name'])) {
  $login_name = null;
}

require_once ENGINE_DIR . '/inc/parse.class.php';

$parse = new ParseFilter();
$parse->safe_mode = true;

// get main feed
if ($op == 1) {
  $where = '';

  if (isset($category)) {

    $catArray = explode(',', $category);
    $array = explode(",", $category);
    $all = array_search('all', $array);

    if (($razdel == 'uploader')) {

      $android = array_search('android', $array);
      $pc = array_search('pc', $array);
      $symbian = array_search('symbian', $array);
      $books = array_search('books', $array);
      $abooks = array_search('abooks', $array);
      $hbooks = array_search('hbooks', $array);
      $androidgames = array_search('androidgames', $array);
      $androidsoft = array_search('androidsoft', $array);

      $where = "AND cid IN(0,";

      if ($android > 0) {
        $cat = $db->query("SELECT * FROM  " . PREFIX . "_categories WHERE pid = '192' and razdel = 'uploader'");
        while ($cw = $db->get_row($cat)) {
          $where .= intval($cw['cid']) . ",";
        }
      }
      if ($pc > 0) {
        $cat = $db->query("SELECT * FROM  " . PREFIX . "_categories WHERE pid = '81' and razdel = 'uploader'");
        while ($cw = $db->get_row($cat)) {
          $where .= intval($cw['cid']) . ",";
        }
      }
      if ($symbian > 0) {
        $cat = $db->query("SELECT * FROM  " . PREFIX . "_categories WHERE pid = '28' and razdel = 'uploader'");
        while ($cw = $db->get_row($cat)) {
          $where .= intval($cw['cid']) . ",";
        }
      }
      if ($books > 0) {
        $cat = $db->query("SELECT * FROM  " . PREFIX . "_categories WHERE pid = '55' and razdel = 'uploader'");
        while ($cw = $db->get_row($cat)) {
          $where .= intval($cw['cid']) . ",";
        }

      }
      if ($abooks > 0) {
        $where .= "77,255,";
      }
      if ($hbooks > 0) {
        $where .= "9,";
      }
      if ($androidgames > 0) {
        $where .= "194,";
      }
      if ($androidsoft > 0) {
        $where .= "242,249,254,253,208,248,235,209,238,210,207,243,211,245,252,193,";
      }

      $where .= ")";
      $where = str_replace(",)", ")", $where);
    }

    if (($razdel == 'android')) {

      $soft = array_search('soft', $array);
      $games = array_search('games', $array);
      $themes = array_search('themes', $array);
      $android = array_search('android', $array);
      $pc = array_search('pc', $array);

      $where = "AND cid IN(0,";

      if (($soft > 0) or ($android > 0) or ($pc > 0)) {
        $cat = $db->query("SELECT * FROM  " . PREFIX . "_categories WHERE pid = '1' and razdel = 'android'");
        while ($cw = $db->get_row($cat)) {
          $where .= intval($cw['cid']) . ",";
        }
      }
      if ($games > 0) {
        $cat = $db->query("SELECT * FROM  " . PREFIX . "_categories WHERE pid = '2' and razdel = 'android'");
        while ($cw = $db->get_row($cat)) {
          $where .= intval($cw['cid']) . ",";
        }
      }
      if ($themes > 0) {
        $cat = $db->query("SELECT * FROM  " . PREFIX . "_categories WHERE pid = '3' and razdel = 'android'");
        while ($cw = $db->get_row($cat)) {
          $where .= intval($cw['cid']) . ",";
        }

      }

      $where .= ")";
      $where = str_replace(",)", ")", $where);
    }

    if (($razdel == 'muzon')) {

      $albums = array_search('albums', $array);
      $tracks = array_search('tracks', $array);
      $ringtone = array_search('ringtone', $array);
      $others = array_search('others', $array);

      $where = "AND cid IN(0,";

      if ($albums > 0) {
        $cat = $db->query("SELECT * FROM  " . PREFIX . "_categories WHERE pid = '137' and razdel = 'muzon'");
        while ($cw = $db->get_row($cat)) {
          $where .= intval($cw['cid']) . ",";
        }
      }
      if ($tracks > 0) {
        $cat = $db->query("SELECT * FROM  " . PREFIX . "_categories WHERE pid = '44' and razdel = 'muzon'");
        while ($cw = $db->get_row($cat)) {
          $where .= intval($cw['cid']) . ",";
        }
      }
      if ($ringtone > 0) {
        $cat = $db->query("SELECT * FROM  " . PREFIX . "_categories WHERE pid = '11' and razdel = 'muzon'");
        while ($cw = $db->get_row($cat)) {
          $where .= intval($cw['cid']) . ",";
        }
      }
      if ($others > 0) {
        $cat = $db->query("SELECT * FROM  " . PREFIX . "_categories WHERE pid = '65' and razdel = 'muzon'");
        while ($cw = $db->get_row($cat)) {
          $where .= intval($cw['cid']) . ",";
        }

      }

      $where .= ")";
      $where = str_replace(",)", ")", $where);
    }

    if (($razdel == 'vuploader')) {

      $movies = array_search('movies', $array);
      $serials = array_search('serials', $array);
      $clips = array_search('clips', $array);
      $humor = array_search('humor', $array);

      $where = "AND cid IN(0,";

      if ($movies > 0) {
        $cat = $db->query("SELECT * FROM  " . PREFIX . "_categories WHERE pid = '237' and razdel = 'vuploader'");
        while ($cw = $db->get_row($cat)) {
          $where .= intval($cw['cid']) . ",";
        }
      }
      if ($serials > 0) {
        $cat = $db->query("SELECT * FROM  " . PREFIX . "_categories WHERE pid = '13' and razdel = 'vuploader'");
        while ($cw = $db->get_row($cat)) {
          $where .= intval($cw['cid']) . ",";
        }
      }
      if ($clips > 0) {
        $cat = $db->query("SELECT * FROM  " . PREFIX . "_categories WHERE pid = '4' and razdel = 'vuploader'");
        while ($cw = $db->get_row($cat)) {
          $where .= intval($cw['cid']) . ",";
        }
      }
      if ($humor > 0) {
        $where .= "34,";
      }

      $where .= ")";
      $where = str_replace(",)", ")", $where);
    }

    if (($razdel == 'gallery')) {

      $humor = array_search('humor', $array);
      $wallpaper = array_search('wallpaper', $array);
      $others = array_search('others', $array);
      $animals = array_search('animals', $array);
      $where = "AND cid IN(0,";

      if ($humor > 0) {
        $where .= "6,";
      }
      if ($wallpaper > 0) {
        $where .= "170,8,";
      }
      if ($others > 0) {
        $where .= "9,22,17,21,11,10,7,";
      }
      if ($animals > 0) {
        $where .= "17,";
      }

      $where .= ")";
      $where = str_replace(",)", ")", $where);
    }
  }

  if (($razdel == 'usernews')) {

    $hardware = array_search('hardware', $array);
    $apple = array_search('apple', $array);
    $games = array_search('games', $array);
    $software = array_search('software', $array);
    $gadget = array_search('gadget', $array);
    $where = "AND cid IN(0,";

    if ($hardware > 0) {
      $where .= "11,";
    }
    if ($apple > 0) {
      $where .= "8,";
    }
    if ($games > 0) {
      $where .= "4,";
    }
    if ($software > 0) {
      $where .= "5,";
    }
    if ($gadget > 0) {
      $where .= "2,";
    }

    $where .= ")";
    $where = str_replace(",)", ")", $where);
  }

  if (($razdel == 'books')) {

    $audio = array_search('audio', $array);
    $fantastic = array_search('fantastic', $array);
    $fantasy = array_search('fantasy', $array);
    $others = array_search('others', $array);
    $where = "AND cid IN(0,";

    if ($audio > 0) {
      $where .= "191,8,";
    }
    if ($fantastic > 0) {
      $where .= "70,";
    }
    if ($fantasy > 0) {
      $where .= "71,";
    }
    if ($others > 0) {
      $where .= "197,69,193,73,109,185,187,78,195,188,196,72,108,192,75,186,9,";
    }

    $where .= ")";
    $where = str_replace(",)", ")", $where);
  }

  if ($all > 0) {
    $where = '';
  }

  $cid = intval($_GET['where']);
  $fav = intval($_GET['fav']);

  if ($fav > 0) {

    $where = '';
    $client_id = $db->super_query("SELECT user_id FROM " . PREFIX . "_users WHERE name='" . $login_name . "' ");

    if (intval($client_id['user_id']) > 0) {
      $row = $db->super_query("SELECT favorites_" . $razdel . ", user_id FROM " . PREFIX . "_favorites where user_id = '" . intval($client_id['user_id']) . "' and profile='4'");
      if ($row['favorites_' . $razdel]) {
        $where = " AND f.lid in(" . $row['favorites_' . $razdel] . ")";
      } else {
        $where = " AND f.lid in(0)";
      }

      $cid = 0;
    } else
      $where = " AND f.lid in(0)";

  }

  if ($razdel == 'comments') {
    $cid = 0;
  }

  $status = 1;
  if ($_GET['st'] > 1)
    $status = 2;
  if ($status == 2)
    $where = '';

  //if ($status != 2) $content = dle_cache("dvclient_" . $razdel . $where . $min . $fo . $cid . $fav . $status, 1); // кэшируем вывод категорий
// if (!$content) {
  $content = show_razdel($razdel, $where, $min, $fo, $cid, $fav, $status);
  //  create_cache("dvclient_" . $razdel . $where . $min . $fo . $cid . $fav . $status, $content, 1, 300); // кэшируем вывод категорий
  //}
  echo $content;
  exit();
}

// search
if ($op == 3) {
  include_once ROOT_DIR . '/apps/dvclient/search.php';
}

// comments
if ($op == 4) {
  include_once ROOT_DIR . '/apps/dvclient/comments.php';
}

// forum
if ($op == 5) {
  $where = false;
  $story = $db->safeSQL(stripslashes(trim($_GET['story'])));
  $count = 0;

  if ($story) {
    $story = filter_var($story, FILTER_SANITIZE_STRING);
    $story = str_replace("'", '', $story);
    $story = str_replace("&#39;", "", $story);

    $story = strip_data(rawurldecode(($story)));
    $story = (mb_substr($story, 0, 90));

    $vr = 'last_post';
    $datelimit = time() - 86400 * 365 * 20;
    $story = mb_strtolower(str_replace("'", "", $story));
    $story = str_replace("-", " ", $story);
    $story = stripslashes($story);

    require_once ROOT_DIR . "/sphinxapi.php";
    $sphinx = new SphinxClient();
    $sphinx->SetServer('127.0.0.1', 9312);
    $sphinx->SetMatchMode(SPH_MATCH_ANY);
    $sphinx->SetSortMode(SPH_SORT_RELEVANCE);
    $d1 = time();
    $sphinx->setFilterRange($vr, $datelimit, $d1);
    $sphinx->SetLimits(($p - 1) * $fo, $fo, 5000);
    $result = $sphinx->Query($sphinx->escapeString('*' . $story . '*'), 'dimonvideo_topics');
    $count = intval($result['total']);
  }

  $where = intval($_GET['id']);
  if ($where > 0) {
    $where = " WHERE f.id = " . $where;
  } else {
    $where = '';
  }

  if ($_GET['id'] == -1) {
    $where = " WHERE ((t.status>0) and (t.posts = 1) and (t.forum_id!=73) and (t.forum_id!=82))  ";
  }

  $fav = intval($_GET['fav']);

  if ($fav > 0) {

    $where = '';
    $client_id = $db->super_query("SELECT user_id FROM " . PREFIX . "_users WHERE name='" . $login_name . "' ");
    if (intval($client_id['user_id']) > 0) {
      $row = $db->super_query("SELECT favorites_forum, user_id FROM " . PREFIX . "_favorites where user_id = '" . intval($client_id['user_id']) . "' and profile='4'");
      if ($row['favorites_forum']) {
        $where = " WHERE t.tid in(" . $row['favorites_forum'] . ")";
      } else {
        $where = " WHERE t.tid in(0)";
      }
    }
    $orderby = "t.last_post DESC";

  }
  echo show_topics($where, $min, $fo, $count, $result, $p, $fav);
  exit();

}

// forum
if ($op == 6) {
  if ($p > 1) {
    exit();
  }

  echo show_forums();
  exit();
}

// posts
if ($op == 7) {
  $where = false;
  $story = $db->safeSQL(stripslashes(trim($_GET['story'])));

  if ($p > 1) {
    //exit();
  }

  if ($story) {
    $story = filter_var($story, FILTER_SANITIZE_STRING);
    $story = str_replace("'", '', $story);
    $story = str_replace("&#39;", "", $story);

    $story = strip_data(rawurldecode(($story)));
    $story = (mb_substr($story, 0, 90));

    $vr = 'post_date';
    $datelimit = time() - 86400 * 365 * 20;
    $story = mb_strtolower(str_replace("'", "", $story));
    $story = str_replace("-", " ", $story);
    $story = stripslashes($story);

    require_once ROOT_DIR . "/sphinxapi.php";
    $sphinx = new SphinxClient();
    $sphinx->SetServer('127.0.0.1', 9312);
    $sphinx->SetMatchMode(SPH_MATCH_ANY);
    $sphinx->SetSortMode(SPH_SORT_RELEVANCE);
    $d1 = time();
    $sphinx->setfilter('tid', array(intval($_GET['id'])));
    $w = array('posts' => 20, 'title' => 2300, 'description' => 220);
    $sphinx->SetFieldWeights($w);
    $sphinx->setFilterRange($vr, $datelimit, $d1);
    $sphinx->SetLimits(($p - 1) * $fo, $fo, 8000);

    $result = $sphinx->Query($sphinx->escapeString('*' . $story . '*'), 'dimonvideo');
    $count = intval($result['total']);
    if ($result && is_array($result['matches'])) {
      $ids = array_keys($result['matches']);
    } else {
      $data[] = ["topic_id" => 0, "lid" => 0, "image" => "/images/noavatar.png", "newtopic" => 1, "last_poster_name" => 0, "user" => 0, "title" => "Ничего не найдено", "text" => "Ничего не найдено по Вашему запросу", "category" => "поиск", "date" => 0, "state" => 0, "pinned" => 0, "rating" => 0, "time" => time(), "views" => 0, "min" => 0];

      echo json_encode($data, JSON_UNESCAPED_UNICODE);
      exit();

    }

  }

  $where = intval($_GET['id']);
  if ($where > 0) {
    $where = " WHERE topic_id = " . intval($where);
  } else {
    $where = ' WHERE topic_id = 1728145189';
  }

  $data = array();

  $sql = $db->query("SELECT p.*, u.foto, u.posts, u.banned, u.user_group, u.reputation, u.lastdate, u.reg_date, u.rating  FROM " . PREFIX . "_posts p LEFT JOIN " . PREFIX . "_users u ON p.author_id = u.user_id $where ORDER BY new_topic DESC, post_date DESC LIMIT $min, $fo");

  if ($story) {
    $id_list1 = implode(" union SELECT * FROM " . PREFIX . "_posts WHERE pid=", $ids);
    $sql = $db->query("SELECT *  FROM " . PREFIX . "_posts WHERE pid=" . $id_list1);
  }

  while ($row = $db->get_row($sql)) {

    $tid = intval($row['pid']);
    $topic_id = intval($row['topic_id']);

    $row['post'] = preg_replace("#<!--smile:(.+?)-->(.+?)<!--/smile-->#is", '', $row['post']);
    $last_poster_name = stripslashes(html_entity_decode(no_bb(strip_tags($row['author_name']))));
    $start_name = stripslashes(html_entity_decode(no_bb(strip_tags($row['starter_name']))));
    $title = stripslashes(html_entity_decode(no_bb(strip_tags($row['title']))));
    $text = stripslashes(html_entity_decode($parse->BB_Parse($row['post'], false)));
    $text = str_replace("https://m.dimonvideo.ru/go/?", "", $text);
    $text = str_replace("https://m.dimonvideo.ru/go?", "", $text);
    $text = str_replace("https://dimonvideo.ru/go/?", "", $text);
    $text = str_replace("https://dimonvideo.ru/go?", "", $text);


    $date = langdate($config['timestamp_active'], $row['post_date']);
    $state = stripslashes(html_entity_decode(no_bb(strip_tags($row['state']))));
    $pinned = intval($row['pinned']);
    $posts = intval($row['posts']);
    $newtopic = intval($row['new_topic']);
    $views = intval($row['views']);
    if ($row['foto']) {
      $logo = "/fotos/" . $row['foto'];
    } else {
      $logo = "/images/noavatar.png";
    }

    // ранг пользователя =================================
    $posts = $row['posts'];
    $banned = $row['banned'];
    $user_group = $row['user_group'];
    $rposts = $row['posts'];
    $rbanned = $row['banned'];
    $ruser_group = $row['user_group'];
    $rreputation = $row['reputation'];
    $rlastdate = $row['lastdate'];
    $rregistration = $row['reg_date'];
    $rat = intval($row['rating']);
    $status = user_level($rposts, $rbanned, $ruser_group, $rreputation, $rlastdate, $rregistration, $row['user_id'], $rat);
    $f_name = stripslashes(html_entity_decode(no_bb(strip_tags($status))));

    $data[] = ["topic_id" => $topic_id, "lid" => $tid, "image" => $logo, "newtopic" => $newtopic, "last_poster_name" => $last_poster_name, "user" => $start_name, "title" => $title, "text" => $text, "category" => $f_name, "date" => $date, "state" => $state, "pinned" => $pinned, "rating" => $posts, "time" => $row['post_date'], "views" => $views, "min" => $min];
    $i++;
  }

  echo json_encode($data, JSON_UNESCAPED_UNICODE);
  exit();

}

// like files & members
if ($op == 8) {
  include_once ROOT_DIR . '/apps/dvclient/like_files.php';
}

// cats
if ($op == 9) {
  if ($p > 1) {
    exit();
  }
 // $content = dle_cache("dvclient_cats_" . $razdel, 1); // кэшируем вывод категорий
  if (!$content) {
    $content = show_cats($razdel);
    create_cache("dvclient_cats_" . $razdel, 1, 360); // кэшируем вывод категорий
  }
  echo $content;
  exit();
}

// auth check
if ($op == 10) {
  include_once ROOT_DIR . '/apps/dvclient/auth.php';
}

// pm
if ($op == 11) {
  include_once ROOT_DIR . '/apps/dvclient/pm.php';
}

// register
if ($op == 12) {
  include_once ROOT_DIR . '/apps/dvclient/register.php';
}

// like posts
if ($op == 13) {
  include_once ROOT_DIR . '/apps/dvclient/like_post.php';
}

// file upload
if ($op == 14) {
  include_once ROOT_DIR . '/apps/dvclient/upload_file.php';
}

// file page upload
if ($op == 15) {
  include_once ROOT_DIR . '/apps/dvclient/upload_page.php';
}

// vote
if ($op == 16) {

  $data = array();

  $result_vote = $db->super_query("SELECT title FROM " . PREFIX . "_vote");
  $oprostitle = stripslashes($result_vote['title']);

  $data[] = ["title" => $oprostitle];

  echo json_encode($data, JSON_UNESCAPED_UNICODE);
  die();
}

// vote
if ($op == 17) {
  include_once ROOT_DIR . '/apps/dvclient/show_vote.php';
}

// images
if ($op == 18) {

  $nick = htmlspecialchars(strip_tags(addslashes(trim($_GET['u']))));

  $dirname = ROOT_DIR . "/files/uploadslinks/msg/api/" . $nick . "/thumbs/";
  $images = glob($dirname . "*.png");

  foreach ($images as $image) {
    $image = str_replace(ROOT_DIR, '', $image);
    echo '<img src="' . $image . '" /><br /><hr><br>';
  }

}

// odob
if ($op == 19) {
  include_once ROOT_DIR . '/apps/dvclient/odob.php';
}

// show single file
if ($op == 20) {

  $lid = intval($_GET['lid']);
  echo show_razdel($razdel, " AND f.lid = " . $lid, 0, 1, 0, 0, 1);
  exit();

}

// show single topic
if ($op == 21) {

  $tid = intval($_GET['tid']);
  echo show_topics(" WHERE t.tid = '" . $tid . "'", 0, 1, 0, 0, 1, 0);
  exit();

}


// put to news
if ($op == 22) {
  include_once ROOT_DIR . '/apps/dvclient/put_to_news.php';
}

// latest
if ($op == 23) {

  $content = dle_cache("dvclient_latest_" . $razdel . $where . $min . $fo . $cid . $fav . $status, 1); // кэшируем вывод категорий
  if (!$content) {
    $content = show_razdel('', '', $min, $fo, 0, 0, 3);
    create_cache("dvclient_latest_" . $razdel . $where . $min . $fo . $cid . $fav . $status, $content, 1, 100); // кэшируем вывод категорий
  }
  echo $content;
  exit();

}

if ($op == 74) { // сохранение настроек

  $status = "error";
  $json = trim($db->safesql($_POST['json']));
  $date_create = "---";
  if (($account > 0) AND (json_validator($_POST['json']))) {

    $backup = $db->super_query("SELECT id FROM dvclient_prefs WHERE account = '$account'");
    $id = intval($backup['id']);
    $date_create = date("d-m-Y H:i", time());
    if ($id > 0)
      $db->query("UPDATE dvclient_prefs SET json = '{$json}', date_create = now()  WHERE account = '$account'");
    else
      $db->query("INSERT INTO dvclient_prefs SET json = '{$json}', account = '$account'");

    $status = "ok";
  }


  $data['status'] = $status;
  $data['account'] = $account;
  $data['date'] = $date_create;
  $data['json'] = "error";
  echo json_encode($data);
  exit();
}

if ($op == 75) { // проверка даты бэкапы

  $status = "error";
  $date_create = "---";
  if ($account > 0) {

    $backup = $db->super_query("SELECT id, date_create FROM dvclient_prefs WHERE account = '$account'");
    $id = intval($backup['id']);
    $date_create = stripslashes($backup['date_create']);

    $status = "ok";
  }


  $data['status'] = $status;
  $data['account'] = $account;
  $data['date'] = $date_create;
  $data['json'] = "error";
  echo json_encode($data);
  exit();
}

if ($op == 76) { // restore настроек

  $status = "error";
  $json = "error";
  $date_create = "---";

  if ($account > 0) {

    $backup = $db->super_query("SELECT id, date_create, json FROM dvclient_prefs WHERE account = '$account'");
    $id = intval($backup['id']);
    $date_create = stripslashes($backup['date_create']);
    $json = stripslashes(stripslashes($backup['json']));
    $status = "ok";
  }


  $data['status'] = $status;
  $data['account'] = $account;
  $data['date'] = $date_create;
  $data['json'] = $json;
  echo json_encode($data);
  exit();
}

// =============================================================================================================================== //

function json_validator($data)
{
  if (!empty($data)) {
    return is_string($data) && is_array(json_decode($data, true)) ? true : false;
  }
  return false;
}

?>