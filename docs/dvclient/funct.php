<?php

if (!defined('DATALIFEENGINE')) {
  header("HTTP/1.1 403 Forbidden");
  header('Location: ../../');
  die("Hacking attempt!");
}

// отправка комментария
function send_post_comments($post = 'post', $client_id = array(), $lid = 0, $razdel = 'uploader', $table = 'uploader')
{
  global $db, $config, $parse, $_SERVER, $pmclass, $db;

  $stop = '';
  if ($client_id['confirmedemail'] == 0) {
    $stop .= "<li>Добавлять материалы разрешено участникам с подтвержденным адресом электронной почты!</li>";
  }
  if ($client_id['reputation'] < 0) {
    $stop .= "Добавлять комментарии разрешено участникам с положительной репутацией!";
  }
  if ($client_id['name'] == 'Read_only') {
    exit();
  }
  if ($client_id['user_group'] > 4) {
    $stop .= "Вам запрещено писать!";
  }
  if (!empty($client_id['restrict_post'])) {
    $restrict_post = explode(":", $client_id['restrict_post']);
    if ((($restrict_post[0] * 3600) + $restrict_post[2]) > time()) {
      $stop .= "<li>Ошибка - Вам запрещено отвечать</li>";
    }
  }

  if ($table == 'chat') {
    $action = '@' . $client_id['name'] . ": " . no_bb(strip_tags(stripslashes($post)));
    $action = str_ireplace('-------------', ' ', $action);
    @file_get_contents("https://api.telegram.org/bot497084192:AAEPyA-7clwebMld09buXe4vtcqDWmLMV9o/sendMessage?chat_id=@dimonvid&parse_mode=HTML&disable_web_page_preview=true&text=" . urlencode($action));
  }

  if ($stop) {
    die($stop);
  }
  $time = date("Y-m-d H:i:s", (time()));
  $headers = tabhead($razdel, 1, 1);

  // проверки ========================

  $p3 = $db->super_query("SELECT max(id) as lpid FROM " . PREFIX . "_" . $table . "_com WHERE post_id = $lid");
  $lpid = intval($p3['lpid']);
  $p2 = $db->super_query("SELECT * FROM " . PREFIX . "_" . $table . "_com WHERE id = $lpid");

  $pos = $db->safeSQL($parse->BB_Parse($p2['text'], false));
  $check1 = mb_strlen(trim($post));
  $check2 = mb_strlen(trim($parse->decodeBBCodes(stripslashes($p2['text']), false, 'YES')));

  if (($check1 != $check2) or ($p2['autor'] != $client_id['name'])) {

    $addtime = date("H.i", (time()));
    $ftime = date("Y-m-d H:i:s", ((time() - 6400)));
    if (($p2['autor'] == $client_id['name']) and ($p2['date'] > $ftime)) {

      $po = $pos . "[br]-------------[br][color=blue]Добавлено в $addtime:[/color] " . $post;

      $db->query("UPDATE LOW_PRIORITY " . PREFIX . "_" . $table . "_com SET
           text = '$po'
       WHERE id = $lpid");
    } else {

      // отправляем уведомление =====================================

      if (($table != 'vote') and ($table != 'userso')) {
        $row = $db->super_query("SELECT *  FROM " . PREFIX . "_" . $table . "_pic  WHERE lid = $lid");
      }

      $user = stripslashes($row['name']);
      $title = $db->safeSQL($parse->BB_Parse(stripslashes($parse->process(word_filter($row['title']))), false));

      if ($table == 'userso') {
        $name = $db->super_query("SELECT name FROM " . PREFIX . "_users WHERE user_id = '" . intval($lid) . "'");
        $user = $name['name'];
        $title = $user;
      }

      if ($table == 'vote') {
        $user = 'DimonVideo';
        $title = 'Опрос';
        $razdel = 'vote';
      }

      $s = '';

      $commclear = $post;

      if ($client_id['name'] != $user) {
        $s = $db->super_query("SELECT lastdate,citoff,sendkom,sendmail,sendmailu,email,name,confirmedemail FROM " . PREFIX . "_users  WHERE name='" . $db->safeSQL($user) . "'");

        if ($s['sendkom'] == '1') {
          if (($table != 'vote') and ($table != 'userso')) {
            $lid = intval($row['lid']);
          }

          $action = $headers . " - к файлу [b][url=https://dimonvideo.ru/$razdel/$lid]" . $title . "[/url][/b] прислан новый комментарий от [url=https://dimonvideo.ru/0/name/" . urlencode($client_id['name']) . "/]" . addslashes(($client_id['name'])) . "[/url]
   -------------
   $post
   ";

          $pmclass->sent_pm(0, 'Комментарий к Вашему файлу!', $action, $user, 0, $client_id['name'], $client_id['user_id'], $s['sendmail'], $s['sendmailu'], '', 2, 1, 0, 0, $lid, $razdel);

          $sendoldmail = false;
          if ($s['lastdate'] < (time() - 86400 * 7)) {
            $sendoldmail = true;
          }

          if ((intval($s['sendmail']) == '1') or ($sendoldmail)) {
            $post = stripslashes($parse->BB_Parse($action, false));
            $post = "Тема: Комментарий к Вашему файлу! Автор " . stripslashes($client_id['name']) . "<br><br>" . $post . "<br><br>-----------------<br>* для ответа, посетите Личные сообщения: <a href=https://dimonvideo.ru/pm/1/0>Основная</a> или <a href=https://m.dimonvideo.ru/pm/1/0>Смарт</a> версия.";

            $subject = "Комментарий к Вашему файлу!";
            if (intval($s['confirmedemail']) == '1') {
              phpmail($s['email'], $subject, $post, false, false, false, false, false, false);
            }
          }

          if (intval($s['sendmailu']) == '1') {
            $post = "Тема: Комментарий к Вашему файлу! Автор " . stripslashes($client_id['name']) . "<br><br>-----------------<br>* для ответа, посетите Личные сообщения: <a href=/pm/1/0>Основная</a> или <a href=https://m.dimonvideo.ru/pm/1/0>Смарт</a> версия.";

            $subject = "Комментарий к Вашему файлу от " . stripslashes($client_id['name']) . "!";
            if (intval($s['confirmedemail']) == '1') {
              phpmail($s['email'], $subject, $post, false, false, false, false, false, false);
            }
          }
        }
      }
      // ============
      $pm = $db->super_query("SELECT id FROM " . PREFIX . "_podp_kom WHERE (lid = '$lid') and (razdel='$razdel')");
      if ($pm['id'] > 0) {
        $pmm = $db->super_query("SELECT id FROM " . PREFIX . "_kom_pm WHERE (lid = '$lid') and (razdel='$razdel')");
        $actionpm = $headers . " - к файлу [b][url=https://dimonvideo.ru/$razdel/$lid]" . $title . "[/url][/b] прислан новый комментарий от [url=https://dimonvideo.ru/0/name/" . urlencode($client_id['name']) . "/]" . addslashes(($client_id['name'])) . "[/url]
   -------------
   $post
   ";

        if ($client_id['name'] != $user) {

          if ($pmm['id'] > 0) {
            $db->query("UPDATE  LOW_PRIORITY " . PREFIX . "_kom_pm SET lastname='" . addslashes($client_id['name']) . "',post = '$actionpm',date='" . time() . "',send=0 WHERE (lid = '$lid') and (razdel='$razdel')");
          } else {
            $db->query(" INSERT LOW_PRIORITY INTO  " . PREFIX . "_kom_pm SET lastname='" . addslashes($client_id['name']) . "', post = '$actionpm', date='" . time() . "', lid = '$lid',razdel='$razdel'");
          }
        }
      }
      //=============
      //echo $client_id['name'];
      $db->query("INSERT LOW_PRIORITY INTO " . PREFIX . "_" . $table . "_com SET  post_id='$lid',  date='$time', autor='" . $client_id['name'] . "', text='$commclear'");
      if (($table != 'vote') and ($table != 'userso')) {
        $db->query("UPDATE LOW_PRIORITY " . PREFIX . "_" . $table . "_pic set comments=comments+1 where lid ='$lid'");
      } else {
        $db->query("UPDATE LOW_PRIORITY " . PREFIX . "_vote set comments=comments+1 where id ='$lid'");
      }

      if ($table != 'userso') {
        $counts = $db->super_query("SELECT COUNT(*) as count FROM " . PREFIX . "_" . $table . "_com WHERE post_id = '$lid' "); // считаем всего файлов
        $count = intval($counts['count']);
        if ($table == 'vote') {
          $db->query("UPDATE LOW_PRIORITY " . PREFIX . "_vote set comments='$count' ");
        } else {
          $db->query("UPDATE LOW_PRIORITY " . PREFIX . "_" . $table . "_pic set comments='$count' where lid ='$lid'");
        }
      }
    }
  }
  // конец хака =======================================================================================

  // уведомление о цитировании
  $checkpost = mb_substr(trim($post), 0, 100);
  preg_match_all('#<b>(.+?)</b>#is', $checkpost, $reg);
  $checkpost = strip_tags($reg[1][0]);
  $action = $headers . "  Вас процитировал [url=https://dimonvideo.ru/0/name/" . urlencode($client_id['name']) . "/]" . addslashes(($client_id['name'])) . "[/url] в комментариях к файлу [b][url=https://dimonvideo.ru/$razdel/$lid]" . $title . "[/url][/b] [br]-------------[br]$post";
  $actionmail = $headers . "  Вас процитировал <a href=https://dimonvideo.ru/0/name/" . urlencode($client_id['name']) . "/>" . addslashes(($client_id['name'])) . "</a> в комментариях к файлу <b><a href=https://dimonvideo.ru/$razdel/$lid>" . $title . "</a></b> <br>-------------<br>$post";
  if ((mb_strlen($checkpost) > 2)) {
    $ss = $db->super_query("SELECT lastdate,email,citoff,sendmail,sendmailu,citchat,confirmedemail FROM " . PREFIX . "_users  WHERE name='" . $checkpost . "'");
    if (($table == 'chat') and ($ss['citchat'] != 1)) {
      $pmclass->sent_pm(0, 'Вас процитировали в чате!', $action, $checkpost, '', $client_id['name'], $client_id['user_id'], $ss['sendmail'], $ss['sendmailu'], '', 2, 1, 0, 0, $lid, $razdel);
    }

    if (($table != 'chat') and ($ss['citoff'] != 1)) {
      $pmclass->sent_pm(0, 'Вас процитировали в комментариях!', $action, $checkpost, '', $client_id['name'], $client_id['user_id'], $ss['sendmail'], $ss['sendmailu'], '', 2, 1, 0, 0, $lid, $razdel);
    }

    $subject = "Вас процитировали в комментариях!";

    $sendoldmail = false;

    if ($ss['lastdate'] < (time() - 86400 * 7)) {
      $sendoldmail = true;
    }

    if ((intval($ss['sendmail']) == 1) or ($sendoldmail)) {

      $subject = "Вас процитировали на DimonVideo.ru!";
      if (intval($ss['confirmedemail']) == 1) {
        phpmail($ss['email'], $subject, $actionmail, false, false, false, false, false, false);
      }
    }
    if (intval($ss['sendmailu']) == 1) {
      if (intval($ss['confirmedemail']) == 1) {
        phpmail($ss['email'], $subject, $actionmail, false, false, false, false, false, false);
      }
    }
  }
  // =======================

  $data[] = ["state" => 1];

  return json_encode($data, JSON_UNESCAPED_UNICODE);
}

// отправка ответа на форум в заданную тему
function send_post_forum($post = 'post', $client_id = array(), $pmid = 0)
{
  global $db, $config, $parse, $_SERVER, $pmclass, $db;

  $data = array();

  $stop = '';
  if ($client_id['confirmedemail'] == 0) {
    $stop .= "<li>Добавлять материалы разрешено участникам с подтвержденным адресом электронной почты!</li>";
  }
  if ($client_id['reputation'] < -1) {
    $stop .= "<li>Отвечать на форуме разрешено участникам с положительной репутацией!</li>";
  }

  if ($client_id['user_group'] > 4) {
    $stop .= "<li>Вам запрещено писать на форуме!</li>";
  }

  if ($pmid == 0) {
    $stop = "error";
  }

  $p = $db->super_query("SELECT * FROM " . PREFIX . "_topics WHERE tid = '$pmid'");
  $ip = filter_var($_SERVER['HTTP_X_FORWARDED_FOR'], FILTER_VALIDATE_IP) ? $_SERVER['HTTP_X_FORWARDED_FOR'] : $_SERVER['REMOTE_ADDR'];
  $ip = $db->safesql($ip);
  $user_id = intval($client_id['user_id']);
  $fullname = $client_id['name'];
  $f = intval($p['forum_id']);

  if ($p['icon_id'] > $client_id['reputation']) {
    $stop .= "<li><b>Просмотр невозможен. Ограничения по репутации - не меньше " . $p['icon_id'] . "</b></li>";
  }

  if ($p['state'] != 'open') {
    $stop .= "<li>Эта тема закрыта!</li>";
  }

  if ($stop) {
    die($stop);
  }

  $post = mb_substr(trim($post), 0, 30000);

  // отправлем уведомление =====================================
  if (($client_id['name'] != $p['starter_name']) and ($p['ispm'] == 0)) {

    $s = $db->super_query("SELECT lastdate,citoff,sendfor,sendmail,sendmailu,email,name,confirmedemail FROM " . PREFIX . "_users  WHERE name='" . $p['starter_name'] . "'");
    if ($s['sendfor'] == '1') {
      $action = "Форум - в Вашу тему: [b][url=https://dimonvideo.ru/forum/topic_" . $p['tid'] . "/$f/0]" . addslashes($p['title']) . "[/url][/b] добавлен новый ответ от [url=https://dimonvideo.ru/0/name/" . urlencode($client_id['name']) . "/]" . addslashes(($client_id['name'])) . "[/url][br]-------------[br]" . $post;
      $pmclass->sent_pm(0, 'Новый ответ в вашей теме!', $action, $p['starter_name'], $p['starter_id'], $client_id['name'], $client_id['user_id'], $s['sendmail'], $s['sendmailu'], '', 2, 1, $p['tid'], $f);
      $sendoldmail = false;
      if ($s['lastdate'] < (time() - 86400 * 7)) {
        $sendoldmail = true;
      }

      if ((intval($s['sendmail']) == '1') or ($sendoldmail)) {
        $postik = stripslashes($parse->BB_Parse($action, false));
        $postik = "Тема: Новый ответ в вашей теме! Автор " . stripslashes($client_id['name']) . "<br><br>" . $postik . "<br><br>-----------------<br>";
        $subject = "Новый ответ в вашей теме!";
        if (intval($s['confirmedemail']) == '1') {
          phpmail($s['email'], $subject, $postik, false, false, false, false, false, false);
        }
      }
      if (intval($s['sendmailu']) == '1') {
        $postik = "Тема: Новый ответ в вашей теме! Автор " . stripslashes($client_id['name']) . "<br><br>-----------------<br>";
        $subject = "Новый ответ в вашей теме от " . stripslashes($client_id['name']) . "!";
        if (intval($s['confirmedemail']) == '1') {
          phpmail($s['email'], $subject, $postik, false, false, false, false, false, false);
        }
      }
    }
  } // ============
  $pm = $db->super_query("SELECT id FROM " . PREFIX . "_podp_pm WHERE tid = '$pmid'");
  if ($pm['id'] > 0) {
    $pmm = $db->super_query("SELECT id FROM " . PREFIX . "_topics_pm WHERE tid = '$pmid'");
    $actionpm = "Форум - в тему: [b][url=https://dimonvideo.ru/forum/topic_" . $p['tid'] . "/$f/0]" . addslashes($p['title']) . "[/url][/b] ([url=https://dimonvideo.ru/forum/topic_" . $p['tid'] . "/$f/0]осн[/url]) добавлен новый ответ от [url=https://dimonvideo.ru/0/name/" . urlencode($client_id['name']) . "/]" . addslashes($client_id['name']) . "[/url][br]-------------[br]" . $post;
    if ($pmm['id'] > 0) {
      $db->query("UPDATE  LOW_PRIORITY " . PREFIX . "_topics_pm SET lastname='" . addslashes($client_id['name']) . "',post = '$actionpm',date='" . time() . "',send=0 WHERE tid = '" . $pmid . "'");
    } else {
      $db->query(" INSERT   INTO  " . PREFIX . "_topics_pm SET lastname='" . addslashes($client_id['name']) . "', post = '$actionpm', date='" . time() . "', tid = '$pmid',send=0");
    }
  } //=============

  $p3 = $db->super_query("SELECT max(pid) as lpid FROM " . PREFIX . "_posts WHERE topic_id = $pmid");
  $lpid = intval($p3['lpid']);
  $p2 = $db->super_query("SELECT new_topic,author_id, post, post_date FROM " . PREFIX . "_posts WHERE pid = $lpid");
  $time = time();
  $pdate = intval($p2['post_date']);
  $diff = $pdate + 700;
  $check1 = mb_strlen(trim($_POST['pm_text']));
  $check2 = mb_strlen(trim($parse->decodeBBCodes(stripslashes($p2['post']), false, 'YES')));
  if (($check1 != $check2) or (intval($p2['author_id']) != $client_id['user_id'])) {
    if ((intval($p2['author_id']) == $client_id['user_id']) and ($time < $diff) and ($p2['new_topic'] != 1)) {
      $addtime = date("H.i", ($time * 60));
      $po = "\r<br>-------------<br>добавлено в $addtime: " . $post;
      $db->query("UPDATE LOW_PRIORITY " . PREFIX . "_posts a set a.post=concat_ws('', a.post,' " . $po . "') WHERE a.pid = $lpid");
    } else {
      $db->query("INSERT  INTO " . PREFIX . "_posts SET
             edit_time = '',
             author_id = $user_id,
             author_name = '$fullname',
             ip_address = '$ip',
             post_date = $time,
             post = '$post',
             topic_id = '$pmid',
             mobile = '1',
             new_topic = 0 ");

      $db->query("UPDATE LOW_PRIORITY " . PREFIX . "_topics SET
             last_poster_id = $user_id,
             posts = posts+1,
             last_post = " . time() . ",
             last_poster_name = '$fullname'
             WHERE tid = $pmid");

      $db->query("UPDATE LOW_PRIORITY " . PREFIX . "_forums SET
             last_poster_id = $user_id,
             posts = posts+1,
             last_post = " . time() . ",
             last_poster_name = '$fullname',
             last_id = '$pmid'
             WHERE id = $f");
    }
  } // конец склейки дублей

  if ($pmid == 1728138369) {
    $action = no_bb(strip_tags(stripslashes($post)));
    file_get_contents("https://api.telegram.org/bot497084192:AAEPyA-7clwebMld09buXe4vtcqDWmLMV9o/sendMessage?chat_id=@dimonvideoru&parse_mode=HTML&disable_web_page_preview=true&text=" . urlencode($action));
  }
  $data[] = ["state" => 1];
  return json_encode($data, JSON_UNESCAPED_UNICODE);
}

function show_topics($where = '', $min = 0, $limit = 10, $count = 0, $result, $p = 1, $fav = 0)
{
  global $db, $config, $parse, $db;
  $i = 0;
  $data = array();

  if ($result && is_array($result['matches'])) {
    $ids = array_keys($result['matches']);
  }

  if ($count == 0) {
    $sql = $db->query("SELECT t.*, f.name FROM " . PREFIX . "_topics AS t LEFT JOIN " . PREFIX . "_forums AS f ON (t.forum_id = f.id) $where ORDER BY last_post DESC LIMIT $min, $limit");
  }

  if ($result && is_array($result['matches']) && $count > 0) {
    $sql = $db->query("SELECT t.*, f.name FROM " . PREFIX . "_topics AS t LEFT JOIN " . PREFIX . "_forums AS f ON (t.forum_id = f.id) WHERE tid in (" . implode(',', $ids) . ") ORDER BY FIELD (tid, " . implode(',', $ids) . ")");
  }

  while ($row = $db->get_row($sql)) {

    $tid = intval($row['tid']);

    $last_poster_name = stripslashes(html_entity_decode(no_bb(strip_tags($row['last_poster_name']))));
    $start_name = stripslashes(html_entity_decode(no_bb(strip_tags($row['starter_name']))));
    $title = stripslashes(html_entity_decode(no_bb(strip_tags($row['title']))));
    $desc = stripslashes(html_entity_decode(no_bb(strip_tags($row['description']))));
    $f_name = stripslashes(html_entity_decode(no_bb(strip_tags($row['name']))));
    $date = langdate($config['timestamp_active'], $row['last_post']);
    $state = stripslashes(html_entity_decode(no_bb(strip_tags($row['state']))));
    $pinned = intval($row['pinned']);
    $posts = intval($row['posts']);
    $views = intval($row['views']);

    $data[] = ["lid" => $tid, "last_poster_name" => $last_poster_name, "user" => $start_name, "title" => $title, "text" => $desc, "category" => $f_name, "date" => $date, "state" => $state, "pinned" => $pinned, "rating" => $posts, "time" => $row['last_post'], "views" => $views, "fav" => $fav];
    $i++;
  }

  if ($result && !is_array($result['matches']) && $count == 0) {
    $data = array();
    if ($p > 1) {
      exit();
    }

    $data[] = ["lid" => 0, "last_poster_name" => 'не найдено', "user" => '', "title" => 'не найдено', "text" => 'не найдено', "category" => 'не найдено', "date" => langdate($config['timestamp_active'], time()), "state" => 0, "pinned" => 0, "rating" => 0, "time" => time(), "views" => 0];
  }
  return json_encode($data, JSON_UNESCAPED_UNICODE);
}

function show_forums()
{
  global $db, $config, $parse, $db;
  $i = 0;
  $data = array();

  $sql_result = $db->query("SELECT a.id AS aid, b.*, b.id AS bid FROM " . PREFIX . "_forums a LEFT JOIN " . PREFIX . "_forums b ON a.id = b.parent_id  WHERE a.parent_id<1 ORDER by a.position ASC");
  while ($row = $db->get_row($sql_result)) {

    $tid = intval($row['bid']);
    $status = intval($row['parent_id']);
    $title = stripslashes(html_entity_decode(no_bb(strip_tags($row['name']))));
    $last_poster_name = stripslashes(html_entity_decode(no_bb(strip_tags($row['last_poster_name']))));
    $desc = stripslashes(html_entity_decode(no_bb(strip_tags($row['fdescription']))));
    if (!$row['last_post']) {
      $row['last_post'] = time();
    }

    $date = langdate($config['timestamp_active'], $row['last_post']);
    $f_name = stripslashes(html_entity_decode(no_bb(strip_tags($row['last_title']))));
    $start_name = stripslashes(html_entity_decode(no_bb(strip_tags($row['last_poster_name']))));

    $data[] = ["lid" => $tid, "last_poster_name" => $last_poster_name, "user" => $start_name, "title" => $title, "text" => $desc, "category" => $f_name, "date" => $date, "state" => $status, "pinned" => '0', "rating" => '0', "time" => $row['last_post'], "views" => '0'];
    $i++;
  }

  return json_encode($data, JSON_UNESCAPED_UNICODE);
}

function show_cats($razdel)
{
  global $db, $config, $parse, $db;
  $i = 0;
  $data = array();
  $table = tabhead($razdel, 0, 1);

  $sql_result = $db->query("
    SELECT
        a.*,
        b.pid   AS bpid,
        b.cid   AS bcid,
        b.title AS btitle,
        b.count_image AS bcount_image
    FROM " . PREFIX . "_categories a
    LEFT JOIN " . PREFIX . "_categories b
        ON (a.cid = b.pid) AND (a.razdel = b.razdel)
    WHERE a.pid = 0
      AND a.razdel = '" . $db->safesql($table) . "'
    ORDER BY a.title ASC, b.title ASC
");

  for ($i = 0; $myrow = $db->get_row($sql_result); $i = $myrow['cid']) {

    if ($myrow['cid'] != $i) {
      $data[] = [
        "lid" => $myrow['cid'],
        "title" => $myrow['title'],
        "count" => $myrow['count_image'],
        "razdel" => $razdel
      ];
    }

    if ($myrow['btitle']) {
      $data[] = [
        "lid" => $myrow['bcid'],
        "title" => $myrow['btitle'],
        "count" => $myrow['bcount_image'],   // <-- ВАЖНО
        "razdel" => $razdel
      ];
    }
  }

  $db->free($sql_result);

  if ($razdel == 'comments') {
    $data[0] = ["lid" => 1, "title" => 'Новости сайта', "count" => 5597, "razdel" => $razdel];
  }

  $output = json_encode($data, JSON_UNESCAPED_UNICODE);
  return $output;
}

function show_razdel($razdel = 'comments', $where = '', $min = 0, $limit = 10, $cid = 0, $fav = 0, $st = 1)
{
  global $db, $config, $parse, $page, $db;
  if ($page == 0) {
    $page = 1;
  }

  $status = " f.status >0 ";
  if ($st == 2)  $status = " f.status = 0 ";

  $i = 0;

  $table = tabhead($razdel, 0, 1);
  $headers = tabhead($razdel, 1, 1);

  $data = array();

  $sql_pic = $db->query("SELECT f.*, d.* FROM " . PREFIX . "_" . $table . "_pic  f LEFT JOIN " . PREFIX . "_fastdata d on (f.lid = d.lid) and (d.razdel = '" . $table . "')   WHERE {$status} {$where} ORDER BY f.date DESC LIMIT {$min}, {$limit}");

  if ($cid > 0) {
    $sql_pic = $db->query("SELECT f.lid as lid, f.*, c.cid, d.lid as dlid, d.hits, d.hits2, d.plus, d.minus, d.spas, d.rating, d.vote_num  FROM " . PREFIX . "_" . $table . "_pic f LEFT JOIN " . PREFIX . "_categories c on (f.cid = c.cid) and (c.razdel = '" . $table . "') LEFT JOIN " . PREFIX . "_fastdata d on (f.lid = d.lid) and (d.razdel = '" . $table . "') WHERE ({$status}  and ((c.pid = '$cid') or (f.cid = '$cid'))) ORDER BY f.date DESC LIMIT $min,$limit");
  }

  if ($razdel == 'tracker') {
    $sql_pic = $db->query("SELECT t.topic_first_post_id, t.topic_views AS hits, t.forum_id AS cid, t.topic_poster AS user_id, t.topic_title as title, t.topic_id as lid, t.topic_time as date, h.post_text AS listtext FROM bb_topics t LEFT JOIN bb_posts p ON t.topic_id = p.topic_id LEFT JOIN bb_posts_text h ON p.post_id = h.post_id WHERE t.topic_attachment = 1 ORDER BY topic_time  DESC LIMIT $min, $limit");

    if ($cid > 0) $sql_pic = $db->query("SELECT t.topic_first_post_id, t.topic_views AS hits, t.forum_id AS cid, t.topic_poster AS user_id, t.topic_title as title, t.topic_id as lid, t.topic_time as date, h.post_text AS listtext FROM bb_topics t LEFT JOIN bb_posts p ON t.topic_id = p.topic_id LEFT JOIN bb_posts_text h ON p.post_id = h.post_id WHERE t.topic_attachment = 1 AND t.forum_id = '$cid' ORDER BY topic_time  DESC LIMIT $min, $limit");
  }

  if ($st == 3) {

    $sql_pic = $db->query("SELECT status, lid, title, name, date, logourl, cid, cname, size, 'android' AS razdel, listtext, url, perenos, '' AS peren, server, server2, edittime FROM " . PREFIX . "_android_pic WHERE FROM_UNIXTIME(date,'%Y-%m-%d') = CURDATE() 
       UNION ALL SELECT status, lid, title, name, date, logourl, cid, cname, size, 'uploader' AS razdel, listtext, url, perenos, peren, server, server2, edittime   FROM " . PREFIX . "_uploader_pic WHERE FROM_UNIXTIME(date,'%Y-%m-%d') = CURDATE() 
       UNION ALL SELECT status, lid, title, name, date, logourl, cid, cname, size, 'vuploader' AS razdel, listtext, url, perenos, '' AS peren, server, server2, edittime   FROM " . PREFIX . "_vuploader_pic WHERE FROM_UNIXTIME(date,'%Y-%m-%d') = CURDATE()  
       UNION ALL SELECT status, lid, title, name, date, logourl, cid, cname, size, 'usernews' AS razdel, listtext, url, perenos, '' AS peren, server, server2, edittime   FROM " . PREFIX . "_usernews_pic WHERE FROM_UNIXTIME(date,'%Y-%m-%d') = CURDATE()  
       UNION ALL SELECT status, lid, title, name, date, logourl, cid, cname, size, 'gallery' AS razdel, listtext, url, perenos, '' AS peren, server, server2, edittime   FROM " . PREFIX . "_gallery_pic WHERE FROM_UNIXTIME(date,'%Y-%m-%d') = CURDATE()  
       UNION ALL SELECT status, lid, title, name, date, logourl, cid, cname, size, 'muzon' AS razdel, listtext, url, perenos, '' AS peren, server, server2, edittime   FROM " . PREFIX . "_muzon_pic WHERE FROM_UNIXTIME(date,'%Y-%m-%d') = CURDATE()  
       UNION ALL SELECT status, lid, title, name, date, logourl, cid, cname, size, 'articles' AS razdel, listtext, url, perenos, '' AS peren, server, server2, edittime   FROM " . PREFIX . "_articles_pic WHERE FROM_UNIXTIME(date,'%Y-%m-%d') = CURDATE()  
       UNION ALL SELECT status, lid, title, name, date, logourl, cid, cname, size, 'blog' AS razdel, listtext, url, perenos, '' AS peren, server, server2, edittime   FROM " . PREFIX . "_blog_pic WHERE FROM_UNIXTIME(date,'%Y-%m-%d') = CURDATE()  
       UNION ALL SELECT status, lid, title, name, date, logourl, 1 AS cid, 'Новости сайта' AS cname, 0 AS size, 'comments' AS razdel, short_story AS listtext, '' AS url, 0 AS perenos, 0 AS peren, 0 AS server, 0 AS server2, 0 AS edittime  FROM " . PREFIX . "_comments_pic WHERE FROM_UNIXTIME(date,'%Y-%m-%d') = CURDATE()  
       UNION ALL SELECT status, lid, title, name, date, logourl, cid, cname, size, 'book' AS razdel, listtext, url, perenos, '' AS peren, server, server2, edittime   FROM " . PREFIX . "_book_pic WHERE FROM_UNIXTIME(date,'%Y-%m-%d') = CURDATE()  ORDER by date DESC LIMIT $min, $limit;");
  }

  if ($razdel == 'members') {
    $sql_pic = $db->query("SELECT t.posts, t.banned, t.user_group, t.reputation, t.user_group, t.rating, t.reg_date, t.foto AS logourl, t.name as title, t.user_id as lid, t.lastdate as date FROM " . PREFIX . "_users t LEFT JOIN " . PREFIX . "_users_dop p ON t.user_id = p.user_id ORDER BY t.user_id  ASC LIMIT $min, $limit");
  }


  while ($row = $db->get_row($sql_pic)) {

    if ($st == 3) {
      $razdel = $row['razdel'];
      $headers = tabhead($razdel, 1, 1);
    }

    if ($razdel == 'comments') {
      $row['listtext'] = stripslashes($row['short_story']) . stripslashes($row['full_story']);
      $row['date'] = strtotime($row['date']);
    }
    if (($razdel == 'usernews')) {
      if ($row['istok'] == "https://dimonvideo.ru") continue;
    }
    if ($razdel == 'tracker') {
      $row['listtext'] = preg_replace("#\[align=(.+?)\](.+?)\[/align\]#is", "\\2", $row['listtext']);
      $row['listtext'] = preg_replace("#\[font=(.+?)\](.+?)\[/font\]#is", "\\2", $row['listtext']);
      $row['listtext'] = preg_replace("#\[spoiler=(.+?)\](.+?)\[/spoiler\]#is", "---", $row['listtext']);
      $row['listtext'] = preg_replace("#\[(url)\](\S.+?)\[/url\]#i", "\\2", $row['listtext']);
      $row['listtext'] = preg_replace("#\[(url)\s*=\s*\&quot\;\s*(\S+?)\s*\&quot\;\s*\](.*?)\[\/url\]#i", "\\2", $row['listtext']);
      $row['listtext'] = preg_replace("#\[(url)\s*=\s*(\S.+?)\s*\](.*?)\[\/url\]#i", "\\2", $row['listtext']);
      $row['listtext'] = preg_replace("#\[hr\]#is", "<br />\\1", $row['listtext']);
      $row['status'] = 1;
      $uid = intval($row['user_id']);
      $t_u = $db->super_query("SELECT name FROM " . PREFIX . "_users WHERE user_id = $uid");
      $row['name'] = $t_u['name'];
      $cid = intval($row['cid']);
      $f_c = $db->super_query("SELECT forum_name FROM bb_forums WHERE forum_id = $cid");
      $row['cname'] = $f_c['forum_name'];
      $aid = intval($row['topic_first_post_id']);
      $a_c = $db->super_query("SELECT attach_id FROM bb_attachments WHERE post_id = $aid");
      $torrent = intval($a_c['attach_id']);
    }


    $opisanie = (trim(((no_bb(html_entity_decode(strip_tags($row['listtext'])))))));
    $opisanie = str_replace("Перенесено из обменника", "", $opisanie);
    $opisanie = str_replace("Релиз подготовлен", "", $opisanie);
    $opisanie = str_replace("DimonVideo.ru ВКонтакте", "", $opisanie);
    $opisanie = str_replace("Описание:", "", $opisanie);
    $opisanie = str_replace("Описание", "", $opisanie);
    $opisanie = str_replace("Название:", "", $opisanie);
    $opisanie = preg_replace('/<img.*>/Uis', '', $opisanie);

    if ($razdel == 'gallery') {
      $opisanie = _substr(($opisanie), 30);
    } else {
      $opisanie = _substr(($opisanie), 70);
    }

    $opisanie = preg_replace('/<img.*>/Uis', '', $opisanie) . '...';



    $host = '';
    $text = stripslashes(html_entity_decode($parse->BB_Parse($row['listtext'], false)));
    $text = str_replace("https://m.dimonvideo.ru/go/?", "", $text);
    $text = str_replace("https://m.dimonvideo.ru/go?", "", $text);
    $text = str_replace("https://dimonvideo.ru/go/?", "", $text);
    $text = str_replace("https://dimonvideo.ru/go?", "", $text);
    $text = preg_replace('#<div class="title_spoiler">(.*?)</div>#s', '', $text);
    if ($row['istok']) {
      $host = parse_url($row['istok']);
      $host = stripslashes(no_bb($host['host']));
      //   $text = "Источник: <b>{$host}</b><br> ".$text; 
    }

    $com = intval($row['comments']);
    $lid = intval($row['lid']);
    $name = stripslashes(no_bb($row['name']));
    $cname = stripslashes(no_bb($row['cname']));

    $title = stripslashes(html_entity_decode(no_bb($row['title'])));
    $lid = intval($row['lid']);

    if (!empty($row['version'])) {
      $title .= " - v." . $row['version'];
    }

    $file = ftplinks($razdel, $row['url'], $row['perenos'], $row['peren'], $row['server'], $row['server2'], $row['logourl'], 1, $row['perenoss'], $row['edittime'], $row['date'], false, $row['oldlid']);
    if ($razdel == 'tracker') {
      $file = "https://dimonvideo.ru/torr/dl2.php?id=" . $torrent;
      $row['size'] = 8024;
      $row['logourl'] = "https://dimonvideo.ru/images/noavatar.png";
    }
    if (($razdel == 'usernews') or ($razdel == 'comments')) {
      $regex = "~/\111111/~";
    } else {
      $regex = "~/\d+/~";
    }

    $row['logourl'] = preg_replace($regex, "/", $row['logourl']);
    $logofullarr = explode('/', $row['logourl']);
    if (!empty($logofullarr[1])) {
      $imgfull = $logofullarr[0] . '/big_' . $logofullarr[1];
    }

    $logofull = ftplinks($razdel, $row['url'], $row['perenos'], $row['peren'], $row['server'], $row['server2'], $imgfull, 0, $row['perenoss'], $row['edittime'], $row['date'], false, $row['oldlid']);
    $logo = ftplinks($razdel, $row['url'], $row['perenos'], $row['peren'], $row['server'], $row['server2'], $row['logourl'], 0, $row['perenoss'], $row['edittime'], $row['date'], 320, $row['oldlid']);

    $date = langdate($config['timestamp_active'], $row['date']);
    $size = size(intval($row['size']));
    $user = html_entity_decode(strip_tags($row['name']));
    $views = intval($row['hits'] + $row['hits2']);
    $plus = intval($row['plus']);
    $minus = intval($row['minus']);
    $status = intval($row['status']);

    $mod = null;
    if (isset($row['modpath']) and strlen($row['modpath']) > 10) {
      $mod = "https://cdn.dimonv.ru/" . $row['modpath'];
    }
    if ($razdel == 'members') {
      $logo = "https://dimonvideo.ru/fotos/" . $row['logourl'];
      $opisanie = 'Нажмите, чтоб написать сообщение';
      $user = 'был на сайте';

      // ранг =================

      $rposts = abs(intval($row['posts']));
      $rbanned = $row['banned'];
      $ruser_group = $row['user_group'];
      $rreputation = $row['reputation'];
      $rlastdate = $row['date'];
      $rregistration = $row['reg_date'];
      $rat = intval($row['rating']);
      $text = strip_tags(user_level($rposts, $rbanned, $ruser_group, $rreputation, $rlastdate, $rregistration, $lid, $rat));
    }
    if ($row['logourl'] == null) $logo = "https://dimonvideo.ru/images/soon.jpg";

    if (strpos($row['logourl'], "https") !== false) {
      $logo = $row['logourl'];
    }

    $data[] = ["lid" => $lid, "post_id" => $lid, "status" => $status, "plus" => $plus, "minus" => $minus, "min" => $page, "views" => $views, "file_link" => $file, "mod" => $mod, "user" => $user, "size" => $size, "razdel" => $razdel, "headers" => $headers, "category" => $cname, "date" => $date, "time" => $row['date'], "title" => $title, "text" => $opisanie, "full_text" => $text, "image" => $logo, "rating" => $com, "fav" => $fav, "host" => $host, "istok" => $row['istok']];
    $i++;
  }
  return json_encode($data, JSON_UNESCAPED_UNICODE);
}

function strip_data($text)
{

  $quotes = array("\x60", "\t", "\n", "\r", ",", ";", ":", "[", "]", "{", "}", "=", "*", "^", "%", "$", "<", ">", "+", "-");
  $goodquotes = array("#", "'", '"', "&");
  $repquotes = array("\#", "\'", '\"', "&amp;");
  $text = stripslashes($text);
  $text = trim(strip_tags($text));
  $text = str_replace($quotes, '', $text);
  $text = str_replace($goodquotes, $repquotes, $text);

  return $text;
}

// build tree categories
function build_tree_cats($cats, $parent_id, $razdel = 'uploader')
{
  if (is_array($cats) and isset($cats[$parent_id])) {
    if (($razdel == 'android') || ($razdel == 'muzon') || ($razdel == 'suploader') || ($razdel == 'device') || ($razdel == 'tracker')) {
      $only_parent = true;
    } else {
      $only_parent = false;
    }

    if ($only_parent == false) {
      foreach ($cats[$parent_id] as $cat) {
        $tree[] = ["lid" => $cat['cid'], "title" => $cat['title'], "count" => $cat['count_image'], "razdel" => $razdel];
      }
    } elseif ($only_parent == true) {
      foreach ($cats[$parent_id] as $cat) {
        $tree[] = ["lid" => $cat['cid'], "title" => $cat['title'], "count" => $cat['count_image'], "razdel" => $razdel];
      }
    }
  }
  return $tree;
}
