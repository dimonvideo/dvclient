<?php
/*
 * @Автор: Дмитрий Вороной (DimonVideo) 
 * @Email: dimon@dimonvideo.ru
 * @Создано: 16-12-2025 19:32:58 
 * @Изменено: 16-12-2025 19:32:58
 * @Описание: file:///home/dimonvideo.ru/html/apps/dvclient/pm.php?op=11
 */

if (!defined('DATALIFEENGINE')) {
  header("HTTP/1.1 403 Forbidden");
  header('Location: ../../');
  die("Hacking attempt!");
}

if ($login_name) {

  $client_id = $db->super_query("SELECT * FROM " . PREFIX . "_users WHERE name='" . $login_name . "' and password='" . md5($login_password) . "'");

  if ($client_id['user_id'] AND $client_id['password'] AND $client_id['password'] == md5($login_password)) {

    $user_id = intval($client_id['user_id']);
    $user_name = stripslashes(html_entity_decode(no_bb(strip_tags($client_id['name']))));

    $tip = intval($_GET['tip']);

    if ($user_id > 0) {

      $data = array();

      if ($_GET['pm'] == 10) { // delete or restore pm

        $pmid = intval($_GET['pm_id']);
        $delete_flag = intval($_GET['delete']);

        if ($delete_flag == 0) {
          $row = $db->super_query("SELECT * FROM " . PREFIX . "_pm where id= '$pmid'");

          if ($row['id'] == '') {
            $data[] = ["status" => 0];
          }

          $row['user'] = trim($row['user']);
          if ((mb_strtolower($row['user']) == mb_strtolower($user_name)) OR (mb_strtolower($row['user_from']) == mb_strtolower($user_name))) {

            $db->query("UPDATE LOW_PRIORITY " . PREFIX . "_pm set folder='deleted', ark='0' WHERE id='" . $row['id'] . "'");
            if ($row['att'] == '1') {
              umask(0);
              @unlink(ROOT_DIR . "/files/uploadslinks/msg/" . $row['file2']);
              @unlink(ROOT_DIR . "/files/uploadslinks/msg/" . $row['file']);
            }
            if ($row['pm_read'] != "yes") {
              $db->query("UPDATE LOW_PRIORITY " . PREFIX . "_users set pm_unread=pm_unread-1 where user_id='" . $user_id . "'");
            }
            if (($row['folder'] != "tasks") and ($row['ark'] == 0)) {
              $db->query("UPDATE LOW_PRIORITY " . PREFIX . "_users set pm_all=pm_all-1 where user_id='" . $user_id . "'");
            }
            $data[] = ["status" => 1];
            $pmclass->recount_pm($user_name, $user_id);

          }
        } elseif ($delete_flag == 1) {
          $row = $db->super_query("SELECT id, user, user_from, pm_read, folder FROM " . PREFIX . "_pm where id= '$pmid'");
          if ((mb_strtolower($row['user']) == mb_strtolower($user_name) AND $row['folder'] == "deleted") OR (mb_strtolower($row['user_from']) == mb_strtolower($user_name) AND $row['folder'] == "outbox")) {
            $delete_count++;
            $db->query("UPDATE LOW_PRIORITY " . PREFIX . "_pm set folder='inbox' where id='$row[id]'");
            $db->query("UPDATE LOW_PRIORITY " . PREFIX . "_users set pm_all=pm_all+1 where user_id='$user_id'");
          }
        } elseif ($delete_flag == 2) {
          $row = $db->super_query("SELECT id, user, user_from, pm_read, folder FROM " . PREFIX . "_pm where id= '$pmid'");

          if ((mb_strtolower($row['user']) == mb_strtolower($user_name)) OR (mb_strtolower($row['user_from']) == mb_strtolower($user_name))) {

            if ($row['pm_read'] != "yes") {
              $db->query("UPDATE LOW_PRIORITY " . PREFIX . "_users set pm_unread=pm_unread-1 where name='" . $user_name . "'");
            }
            $db->query("UPDATE LOW_PRIORITY " . PREFIX . "_pm set pm_read='yes', ark='1', folder='inbox' where id='" . $pmid . "'");

            if ($row['folder'] != 'tasks')
              $db->query("UPDATE LOW_PRIORITY " . PREFIX . "_users set pm_all=pm_all-1 where name='" . $user_name . "'");
            $data[] = ["status" => 1];

          }
        } elseif ($delete_flag == 3) {
          $row = $db->super_query("SELECT id, user, user_from, pm_read, folder FROM " . PREFIX . "_pm where id= '$pmid'");

          if ((mb_strtolower($row['user']) == mb_strtolower($user_name)) OR (mb_strtolower($row['user_from']) == mb_strtolower($user_name))) {

            if ($row['pm_read'] != "yes") {
              $db->query("UPDATE LOW_PRIORITY " . PREFIX . "_users set pm_unread=pm_unread-1 where name='" . $user_name . "'");
            }
            $db->query("UPDATE LOW_PRIORITY " . PREFIX . "_pm set pm_read='yes', ark='0', folder='inbox' where id='" . $pmid . "'");

            if ($row['folder'] != 'tasks')
              $db->query("UPDATE LOW_PRIORITY " . PREFIX . "_users set pm_all=pm_all-1 where name='" . $user_name . "'");
            $data[] = ["status" => 1];

          }
        }

        echo json_encode($data, JSON_UNESCAPED_UNICODE);
        exit();
      }

      if ($_GET['pm'] == 11) { // read state pm

        $pmid = intval($_GET['pm_id']);
        $delete = intval($_GET['delete']);

        $row = $db->super_query("SELECT * FROM " . PREFIX . "_pm where id= '$pmid'");

        if ($row['id'] == '') {
          $data[] = ["status" => 0];
        }

        $row['user'] = trim($row['user']);
        if ($row['user'] == $user_name AND $row['pm_read'] != "yes") {
          $db->query("UPDATE " . PREFIX . "_users set pm_unread=pm_unread-1  where user_id='" . $user_id . "'");
          $db->query("UPDATE " . PREFIX . "_pm set pm_read='yes',prior=2  where id='$pmid'");
        }
        echo json_encode($data, JSON_UNESCAPED_UNICODE);
        exit();
      }

      if ($_GET['pm'] == 12) { // reply pm =======================================================

        $pmid = intval($_GET['pm_id']);
        $uid = intval($_GET['uid']);
        $post = trim($db->safesql(strip_tags((string) $_POST['pm_text'])));
        $news_txt = substr(trim($post), 0, 4000);

        if ($uid > 0) { // send to user

          $rowu = $db->super_query("SELECT name  FROM " . PREFIX . "_users where user_id = '" . $uid . "'");
          $user = $rowu['name'];
          $pmclass->sent_pm(0, 'Новое сообщение', $news_txt, $user, '', $user_name, $user_id, 0, 0, '', 2);
          $data[] = ["state" => 1];
          echo json_encode($data);
          exit();

        }

        $delete = intval($_GET['delete']);

        if ($delete == 20) {

          send_post_comments($post, $client_id, $pmid, $razdel, $table);
          $data[] = ["state" => 1];
          echo json_encode($data, JSON_UNESCAPED_UNICODE);
          exit();
        }

        if ($delete == 2) {

          send_post_forum($post, $client_id, $pmid);

        }

        $r = $db->super_query("SELECT * FROM " . PREFIX . "_pm WHERE id = '$pmid'");
        $quote = $parse->decodeBBCodes(nl2br($r['text']), false, 'no');
        $qtxt = "" . $quote . " [br]======================[br]";
        $comm_update = $qtxt . "[b]" . $user_name . "[/b], " . date("Y-m-d H:i:s", time()) . ": " . $news_txt;
        $comm_update2 = $news_txt;

        if ($pmid != $r['id']) {
          $data[] = ["state" => 0];
          echo json_encode($data, JSON_UNESCAPED_UNICODE);
          exit();
        }

        $from = addslashes($r['user_from']);
        $user = $db->safeSQL($from);
        $subj = stripslashes($r['subj']);
        if (stristr($subj, "Re:")) {
          $thm = str_replace("Re:", "", $subj);
          $subj = "Re[1]: $thm";
        } elseif (stristr($subj, "Re[")) {
          $t1 = str_replace("Re[", "", $subj);
          $t1 = strtok($t1, "]");
          $t1 = $t1 + 1;
          $o = explode(" ", $subj);
          $thm = str_replace("$o[0]", "", $subj);
          $subj = "Re[$t1]:$thm";
        } else {
          $subj = "Re: $subj";
        }

        $rr = $db->super_query("SELECT u.lastdate, u.pm_ignor, u.drugls,u.name, u.user_id,u.email,u.sendmail,u.sendmailu,u.ogranpm,u.ipm,u.confirmedemail,i.ignored_users FROM " . PREFIX . "_users u LEFT JOIN " . PREFIX . "_ignor i ON u.user_id=i.user_id WHERE u.name='" . $user . "'");

        $ogranpm = intval($rr['ogranpm']);
        $opm1 = intval($se[0]);
        if (empty($opm1)) {
          $opm1 = '9';
        }
        $opm2 = intval($se[1]);
        if (empty($opm2)) {
          $opm2 = '23';
        }
        if (($ogranpm == '1') and ($member_id['user_group'] == '4')) {
          $checkdate = date("H");
          if (($checkdate < $opm1) or ($checkdate > $opm2)) {
            $data[] = ["state" => 0];
            echo json_encode($data, JSON_UNESCAPED_UNICODE);
            exit();
          }
        }

        $pmclass->sent_pm($outb, $subj, $comm_update, $user, '', $user_name, $user_id, $rr['sendmail'], $rr['sendmailu'], '', 2);

        if ($delete == 1) {
          $db->query("UPDATE LOW_PRIORITY " . PREFIX . "_pm set folder='deleted' WHERE id='$pmid' AND user = '" . $user_name . "'");

          $tid = intval($r['tid']);

          if ($tid > 0) { // отправка на форум из лички
            $post = "[b]" . $db->safeSQL($r['user_from']) . "[/b][br]" . $comm_update2;
            send_post_forum($post, $client_id, $tid);
          }

        }



        if ($pmid > 0) {
          $db->query("UPDATE LOW_PRIORITY " . PREFIX . "_pm set otv=1 where id='$pmid'");
        }

        $sendoldmail = false;
        if ($rr['lastdate'] < (time() - 86400 * 30)) {
          $sendoldmail = true;
        }

        if ((intval($rr['sendmail']) == '1') or ($sendoldmail)) {
          $post = stripslashes($parse->BB_Parse($comm_update, false));
          $post = "Тема $subj. Автор " . stripslashes($user_name) . "<br><br>" . $post . "<br>-----------------<br>* для ответа, посетите Личные сообщения: <a href=https://dimonvideo.ru/pm/1/0>Основная</a> или <a href=https://m.dimonvideo.ru/pm/1/0>Смарт</a> версия.<br><br>";
          $subject = "Вам личное сообщение с DimonVideo.ru!";

          if (intval($rr['confirmedemail']) == 1) {
            phpmail($rr['email'], $subject, $post, false, false, false, false, false, false);
          }

        } elseif (intval($rr['sendmailu']) == '1') {
          $post = "Тема $subj. Автор " . stripslashes($user_name) . "<br>-----------------<br>* для ответа, посетите Личные сообщения: <a href=https://dimonvideo.ru/pm/1/0>Основная</a> или <a href=https://m.dimonvideo.ru/pm/1/0>Смарт</a> версия.<br><br>-----------------<br>";

          $subject = "Вам личное сообщение от " . stripslashes($user_name);

          if (intval($rr['confirmedemail']) == 1) {
            phpmail($rr['email'], $subject, $post, false, false, false, false, false, false);
          }

        }

        $data[] = ["state" => 1];
        echo json_encode($data);
        exit();
      }

      if ($_GET['pm'] == 6) { // friends
        $row = $db->super_query("SELECT friends_users FROM " . PREFIX . "_friends where user_id = '" . $user_id . "'");
        $names = explode(",", $row['friends_users']);
        @natcasesort($names);
        if ($p > 1)
          exit();
        foreach ($names as $name) {
          if (strlen($name) > 2) {

            $rowu = $db->super_query("SELECT *  FROM " . PREFIX . "_users where name = '" . $name . "'");
            $id = intval($rowu['user_id']);

            // ранг =================

            $rposts = abs(intval($rowu['posts']));
            $rbanned = $rowu['banned'];
            $ruser_group = $rowu['user_group'];
            $rreputation = $rowu['reputation'];
            $rlastdate = $rowu['lastdate'];
            $rregistration = $rowu['reg_date'];
            $rat = intval($rowu['rating']);
            $text = strip_tags(user_level($rposts, $rbanned, $ruser_group, $rreputation, $rlastdate, $rregistration, $id, $rat));

            if ($rowu['foto']) {
              $foto = "https://dimonvideo.ru/fotos/" . $rowu['foto'] . "";
            } else {
              $foto = "https://dimonvideo.ru/images/noavatar.png";
            }

            $date = langdate($config['timestamp_active'], ($rowu['lastdate']));

            $data[] = ["lid" => $id, "user" => 'был на сайте', "title" => $name, "full_text" => $text, "text" => 'Нажмите, чтоб написать сообщение', "image" => $foto, "date" => $date, "state" => 0, "pinned" => 0, "rating" => 0, "time" => $rowu['lastdate'], "views" => 0];
          }
        }





        echo json_encode($data, JSON_UNESCAPED_UNICODE);
        exit();
      }

      if ($_GET['pm'] == 7) { // ignor
        $row = $db->super_query("SELECT ignored_users FROM " . PREFIX . "_ignor where user_id = '" . $user_id . "'");
        $names = explode(",", $row['ignored_users']);
        @natcasesort($names);
        if ($p > 1)
          exit();
        foreach ($names as $name) {
          if (strlen($name) > 2) {

            $rowu = $db->super_query("SELECT *  FROM " . PREFIX . "_users where name = '" . $name . "'");
            $id = intval($rowu['user_id']);

            // ранг =================

            $rposts = abs(intval($rowu['posts']));
            $rbanned = $rowu['banned'];
            $ruser_group = $rowu['user_group'];
            $rreputation = $rowu['reputation'];
            $rlastdate = $rowu['lastdate'];
            $rregistration = $rowu['reg_date'];
            $rat = intval($rowu['rating']);
            $text = strip_tags(user_level($rposts, $rbanned, $ruser_group, $rreputation, $rlastdate, $rregistration, $id, $rat));

            if ($rowu['foto']) {
              $foto = "https://dimonvideo.ru/fotos/" . $rowu['foto'] . "";
            } else {
              $foto = "https://dimonvideo.ru/images/noavatar.png";
            }

            $date = langdate($config['timestamp_active'], ($rowu['lastdate']));

            $data[] = ["lid" => $id, "user" => 'был на сайте', "title" => $name, "full_text" => $text, "text" => 'Нажмите, чтоб написать сообщение', "image" => $foto, "date" => $date, "state" => 0, "pinned" => 0, "rating" => 0, "time" => $rowu['lastdate'], "views" => 0];
          }
        }





        echo json_encode($data, JSON_UNESCAPED_UNICODE);
        exit();
      }


      $pmclass->recount_pm($user_name, $user_id);


      $sql = $db->query("SELECT p.*,u.foto,u.lastdate FROM " . PREFIX . "_pm p LEFT JOIN " . PREFIX . "_users u ON p.user_from = u.name WHERE user = '" . strtolower($user_name) . "' AND folder = 'inbox' AND ark = '0' order by DATE DESC limit $min,$fo");
      if ($_GET['pm'] == 2) { // архив
        $sql = $db->query("SELECT * FROM " . PREFIX . "_pm WHERE user = '" . strtolower($user_name) . "' AND folder != 'deleted' AND ark = '1' order by date desc LIMIT $min,$fo");
      }

      if ($_GET['pm'] == 5) { // корзина
        $sql = $db->query("SELECT p.*,u.foto,u.lastdate FROM " . PREFIX . "_pm p LEFT JOIN " . PREFIX . "_users u ON p.user_from = u.name WHERE (user_from = '" . strtolower($user_name) . "' AND folder = 'deleted') or (user = '" . strtolower($user_name) . "' AND folder = 'deleted') order by date desc LIMIT  $min,$fo");
      }

      if ($_GET['pm'] == 4) { // задачи
        $db->query($sql = "SELECT p.*,u.foto,u.lastdate FROM " . PREFIX . "_pm p LEFT JOIN " . PREFIX . "_users u ON p.user_from = u.name WHERE user = '" . strtolower($user_name) . "' AND folder = 'tasks' order by date desc LIMIT  $min,$fo");
      }

      if ($_GET['pm'] == 1) { // отправленные
        $sql = $db->query("SELECT p.*,u.foto,u.lastdate FROM " . PREFIX . "_pm p LEFT JOIN " . PREFIX . "_users u ON p.user = u.name WHERE user_from = '" . strtolower($user_name) . "' AND folder = 'outbox' AND ark = '0' order by date desc LIMIT $min,$fo");
      }

      if ($_GET['pm'] == 3) { // исходящие
        $sql = $db->query("SELECT p.*,u.foto,u.lastdate FROM " . PREFIX . "_pm p LEFT JOIN " . PREFIX . "_users u ON p.user = u.name WHERE user_from = '" . strtolower($user_name) . "'  AND folder = 'inbox' AND pm_read = 'no' AND tid=0 AND f=0 AND lid = 0 order by date desc LIMIT $min,$fo");
      }



      while ($row = $db->get_row($sql)) {

        $id = intval($row['id']);
        $user_from = stripslashes(html_entity_decode(no_bb($row['user_from'])));
        $user = stripslashes(html_entity_decode(no_bb($row['user'])));
        $system = intval($row['fsystem']);
        $pri = intval($row['prior']);
        $att = 0;
        if ($row['pm_read'] != "yes") {
          $att = 1;
        }

        $otv = intval($row['otv']);
        $date = langdate($config['timestamp_active'], ($row['date']));
        $subj = stripslashes(html_entity_decode(no_bb(strip_tags($row['subj']))));
        if ($row['foto']) {
          $foto = "https://dimonvideo.ru/fotos/" . $row['foto'] . "";
        } else {
          $foto = "https://dimonvideo.ru/images/noavatar.png";
        }

        $text = preg_replace("#[\n]+#", "\n", $row['text']);
        $text = ($parse->BB_Parse(stripslashes(nl2br($text)), true));
        $text = stripslashes(html_entity_decode($text));
        $opisanie = (trim(((no_bb(html_entity_decode(strip_tags($row['text'])))))));
        $opisanie = preg_replace('/<img.*>/Uis', '', $opisanie);
        $opisanie = _substr(($opisanie), 50);

        $data[] = ["lid" => $id, "last_poster_name" => $user_from, "user" => $user, "title" => $subj, "full_text" => $opisanie, "text" => $text, "category" => $foto, "date" => $date, "state" => $system, "pinned" => $pri, "rating" => $otv, "time" => $row['date'], "views" => $att];
      }

    }

  }

}
echo json_encode($data, JSON_UNESCAPED_UNICODE);
exit();

?>