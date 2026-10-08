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
$valid_razdel = array("uploader", "vuploader", "android", "usernews", "articles");

if (!in_array($razdel, $valid_razdel)){
  $data[] = ["error" => true, "title" => "неверный раздел"];
  echo json_encode($data, JSON_UNESCAPED_UNICODE);
  die();
}

$user = $db->super_query("SELECT user_group, password FROM  " . PREFIX . "_users WHERE name LIKE '" . $name . "' and password='" . md5($login_password) . "'");

if (($user['user_group'] == 1) AND ($user['password'] == md5($login_password))) {

  $row = $db->super_query("SELECT * FROM  " . PREFIX . "_".$razdel."_pic WHERE lid = '$lid'");
  $time = time();
  
  if (intval($row['lid']) == 0) { 
    $data[] = ["error" => true, "title" => "Файл не найден"];
    echo json_encode($data, JSON_UNESCAPED_UNICODE);
    die();
  }
  
  $cid = intval($row['cid']);
  
  if ( $lid != $row[ 'lid' ] ) {
    $data[] = ["error" => true, "title" => "Файл не найден"];
    echo json_encode($data, JSON_UNESCAPED_UNICODE);
    die();
  }

  $title = $row[ 'title' ] . " " . $row[ 'version' ];
  $logourl = '';

  if ( $row[ 'logourl' ] ) $logourl = ftplinks( $razdel, $row[ 'url' ], $row[ 'perenos' ], $row[ 'peren' ], $row[ 'server' ], $row[ 'server2' ], $row[ 'logourl' ], 0, $row[ 'perenoss' ], $row[ 'edittime' ], $row[ 'date' ], false,$row['oldlid'] );

  $listtext = addslashes($row[ 'listtext' ]);
  $date = date( "Y-m-d H:i:s", time() );
  $name = addslashes( $row[ 'name' ] );
  $uid = intval( $row[ 'uid' ] );

  $lin3 = " [URL=https://dimonvideo.ru/" . $razdel . "/" . $lid . "]" . $title . "[/URL] ";

  $listtext = $listtext . "
-----------------
Оставить комментарии и скачать файл можно здесь: " . $lin3;

  $db->query( "INSERT INTO " . PREFIX . "_comments_pic (uid, logourl, date, name, title, short_story, allow_comm) values ('".$uid."', '$logourl', '$date', '$name', '$title', '$listtext', 0)" );        
  $w = $db->super_query( "SELECT max(lid) as lid FROM  " . PREFIX . "_comments_pic" );
  $db->query( "INSERT INTO " . PREFIX . "_fastdata (lid, razdel) VALUES ('" . intval( $w[ 'lid' ] ) . "', 'comments')" );


  $action = "Анонс Вашего файла [b]" . $lin3 . "[/b] размещен на главной странице сайта. ";
  $pmclass->sent_pm( 0, 'Ваш файл на главной странице сайта!', $action, $name, '', 'DimonVideo', 1, '', '', '', 2, 1 );

  $data[] = ["error" => false, "title" => "Перенесено успешно"];
  echo json_encode($data, JSON_UNESCAPED_UNICODE);
  die();

} else {
  $data[] = ["error" => true, "title" => "неверный пользователь: ".$name];
  echo json_encode($data, JSON_UNESCAPED_UNICODE);
  die();
}
?>