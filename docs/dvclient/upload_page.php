<?php
if( !defined( 'DATALIFEENGINE' ) ) {
	header( "HTTP/1.1 403 Forbidden" );
	header ( 'Location: ../../' );
	die( "Hacking attempt!" );
}

$response = array();
  //if the call is an api call
  if (isset($_GET['api'])) {

      //switching the api call
      switch ($_GET['api']) {

      //if it is an upload call we will upload the image
      case 'upload':

        $send_user = htmlspecialchars(strip_tags(addslashes(trim($_POST['send_user']))));
        $send_razdel = htmlspecialchars(strip_tags(addslashes(trim($_POST['send_razdel']))));
        $send_category = intval(trim($_POST['send_category']));
        $send_title = htmlspecialchars(strip_tags(addslashes(trim($_POST['send_title']))));
        $send_desc = htmlspecialchars(trim($_POST['send_desc']));
        $send_screen = htmlspecialchars(strip_tags(addslashes(trim($_POST['send_screen']))));
        $send_logo = htmlspecialchars(strip_tags(addslashes(trim($_POST['send_logo']))));
        $send_catalog = intval(trim($_POST['send_catalog']));

        $valid_razdel = array("comments", "vote", "blog", "usernews", "articles", "gallery");
  
        if (!in_array($send_razdel, $valid_razdel)){
          $data[] = ["error" => true, "title" => "неверный раздел"];
          echo json_encode($data, JSON_UNESCAPED_UNICODE);
          die();
        }

        if ($send_category == 0) {
          $response['error']     = true;
          $response['file_link'] = 'Invalid category';
          header('Content-Type: application/json');
          echo json_encode($response);
          exit();
        }

        $table = tabhead($send_razdel, 0, 1);

        $w = $db->super_query("SELECT user_id, name FROM " . PREFIX . "_users where name LIKE '".$send_user."'");
        $uid = intval($w['user_id']);

        if ($uid == 0) {
          $response['error']     = true;
          $response['file_link'] = 'Invalid user';
          header('Content-Type: application/json');
          echo json_encode($response);
          exit();
        }

        // опрос
        if ($send_razdel == "vote") {

          $body = $db->safeSQL($parse->BB_Parse($parse->process($send_desc), false));
          $db->query("DELETE FROM ".PREFIX."_vote");
          $db->query("INSERT INTO ".PREFIX."_vote (date, title, body, approve) VALUES (CURRENT_DATE(), '$send_title', '$body', '1')");
          @unlink(ENGINE_DIR.'/cache/system/vote.php');
          @unlink(ENGINE_DIR.'/cache/system/voteresult.php');

          $response['error']     = false;
          $response['file_link'] = 'https://dimonvideo.ru/votes';
          header('Content-Type: application/json');
          echo json_encode($response);
          exit();
        }

        $ww = $db->super_query("SELECT title FROM " . PREFIX . "_categories WHERE cid=".$send_category." and razdel = '$table'");

        $cname = stripslashes($ww['title']);

        $user_name = stripslashes($w['name']);

        $status = 0;
        if ($user_name == 'DimonVideo') $status = 1;

        $url = '';
        $size = 0;
       
        if ( strpos( $send_screen, "null" ) !== false) {
          $send_screen = "https://dimonvideo.ru/images/soon.jpg";
        }
       
        if ($table == 'gallery'){
          $url = $send_screen;
          $size = 750000;
          if ((isset($_POST['send_logo'])) AND (strpos($_POST['send_logo'], "null" ) == false)) {
            $send_screen = $send_logo;
          }
        }

        $listtext = "[p]" . $db->safesql($parse->BB_Parse($parse->process($send_desc), false)) . "[/p]";
        
        if ($table == 'usernews') $ist = "https://dimonvideo.ru/0/name/".$user_name;
        
        if (($table == 'usernews') and ($user_name == 'DimonVideo')) {
          $listtext = $db->safesql($_POST['send_desc']);
          $ist = "https://dimonvideo.ru";
        }


        if ( strpos( $send_title, "Прикольная" ) !== false) {
          $r  = array('понедельника', 'вторника', 'среды', 'четверга', 'пятницы', 'субботы', 'воскресения');
          $f3 = array('1', '2', '3', '4', '5', '6', '7');
          $m  = str_replace($f3, $r, date("N", time()));
          $www = $db->super_query("SELECT max(lid) as lid FROM  " . PREFIX . "_".$table."_pic");    
          $lid = intval($www['lid']);
          $num = $lid+1;
          $send_title = preg_replace('/[0-9]+/', '', $send_title);
          $send_title = "Прикольная картинка ".$m." #".$num;
          $listtext = "Уникальный идентификатор: ". $num." ".$listtext;
        }

 

        $added_time = time();
        $origdate = date ("Y-m-d H:i:s", time());
        $date = $added_time;
        if ($send_razdel == 'comments') $date = $origdate;

        $db->query("INSERT INTO " . PREFIX . "_".$table."_pic (uid, cid, title, listtext, logourl, name, date, cname, status, istok, originaldate, mid, size, url) VALUES ('".$uid."','$send_category', '$send_title', '$listtext', '$send_screen', '".$user_name."', ".$date.", '$cname',  $status, '$ist', '".$origdate."', '".$send_catalog."', '".$size."', '".$url."')");
        $db->query("INSERT INTO " . PREFIX . "_fastdata (lid, razdel) VALUES ((select MAX(lid) FROM " . PREFIX . "_".$table."_pic), '".$table."')");
        $db->query("UPDATE " . PREFIX . "_categories set count_image=count_image+1 where (cid ='$cid' or pid = '$cid') and razdel = '$table'");

        $www = $db->super_query("SELECT max(lid) as lid FROM  " . PREFIX . "_".$table."_pic WHERE name = '".$user_name."'");    
        $lid = intval($www['lid']);
        $link = "https://dimonvideo.ru/".$send_razdel."/".$lid;
        $response['error']     = false;
        $response['file_link'] = $link;

          break;




      default:
          $response['error']     = true;
          $response['file_link'] = 'Invalid api call';
      }

  } else {

      http_response_code(404);
      exit();
  }

  //displaying the response in json
  header('Content-Type: application/json');
  echo json_encode($response);
  exit();

?>