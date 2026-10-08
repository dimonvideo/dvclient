<?php
if( !defined( 'DATALIFEENGINE' ) ) {
	header( "HTTP/1.1 403 Forbidden" );
	header ( 'Location: ../../' );
	die( "Hacking attempt!" );
}

$data = array();
  
  $lid = intval($_GET['lid']);
  $name = htmlspecialchars(strip_tags(addslashes(trim($_GET['u']))));
  $login_password = md5((string)$_GET['p']);
  $razdel = htmlspecialchars(strip_tags(addslashes(trim($_GET['razdel']))));
  $valid_razdel = array("uploader", "vuploader", "muzon", "usernews", "device", "articles", "gallery");
  
  if (!in_array($razdel, $valid_razdel)){
    $data[] = ["error" => true, "title" => "неверный раздел"];
    echo json_encode($data, JSON_UNESCAPED_UNICODE);
    die();
  }

  $user = $db->super_query("SELECT user_group, password FROM  " . PREFIX . "_users WHERE name LIKE '" . $name . "' and password='" . md5($login_password) . "'");

  if (($user['user_group'] < 3) AND ($user['password'] == md5($login_password))) {

    $w = $db->super_query("SELECT * FROM  " . PREFIX . "_".$razdel."_pic WHERE lid = '$lid'");
    $time = time();
    if ($w['approvedtime']>0) $time = $w['approvedtime']; 
    
    if ($w['status'] == '1') { 
      $data[] = ["error" => true, "title" => "Одобрено ранее"];
      echo json_encode($data, JSON_UNESCAPED_UNICODE);
      die();
    }
    
    $cid = $w['cid'];
    
    if (($w['odob']) and (($w['odob'] != $name) and ($name!='combrig'))) { 
      
      $data[] = ["error" => true, "title" => "Одобрение заблокировано"];
      echo json_encode($data, JSON_UNESCAPED_UNICODE);
      die();
    }


    $db->query("UPDATE " . PREFIX . "_".$razdel."_pic SET status = 1,  odob = '', approved = '".$name."', approvedtime = '".time()."', date = '".time()."' WHERE lid = '$lid'");
    
    $user = stripslashes(urldecode($w['name']));
    $log = "Файл одобрен!', Ваш файл ".addslashes($w['title'])."  одобрил ".$name."  ".date ("Y-m-d H:i:s",time()).". Это автоматическое уведомление. Отвечать на него не нужно.";

    $pmclass->sent_pm(0, 'Файл одобрен!', $log, $user, '', $name, 0, 0 ,0, '', 1, 1);

    $row  = $db->super_query("SELECT count(*) AS count FROM ".PREFIX."_frating WHERE lid='$lid' and razdel = '$razdel' AND name='".$name."'");
    
    if (($row['count'] == 0) AND ($user != $name)) {
      $db->query("UPDATE LOW_PRIORITY " . PREFIX . "_fastdata SET plus=plus+1 WHERE lid = '$lid' and razdel = '$razdel'");
      $db->query(" INSERT  LOW_PRIORITY INTO ".PREFIX."_frating SET name = '".$name."', lid='$lid', razdel = '$razdel', reason='plus'");
	  }

    $data[] = ["error" => false, "title" => "Одобрено успешно"];
    echo json_encode($data, JSON_UNESCAPED_UNICODE);
    die();

  } else {
    $data[] = ["error" => true, "title" => "неверный пользователь: ".$name];
    echo json_encode($data, JSON_UNESCAPED_UNICODE);
    die();
  }

?>