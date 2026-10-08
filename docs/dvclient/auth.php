<?php
if( !defined( 'DATALIFEENGINE' ) ) {
	header( "HTTP/1.1 403 Forbidden" );
	header ( 'Location: ../../' );
	die( "Hacking attempt!" );
}

$data = array();

if ($login_name) {

 $client_id = $db->super_query("SELECT * FROM " . PREFIX . "_users WHERE name LIKE '" . $login_name . "' and password='" . md5($login_password) . "'");

 if ($client_id['user_id'] AND $client_id['password'] AND $client_id['password'] == md5($login_password)) {

  $user_id = intval($client_id['user_id']);
  $user_name = stripslashes(html_entity_decode(no_bb(strip_tags($client_id['name']))));

  if ($user_id > 0) {

   $token = $db->safeSQL($_POST['token']);

   if ($client_id['foto']) {
    $foto = "https://dimonvideo.ru/fotos/" . $client_id['foto'];
   } else {
    $foto = "https://dimonvideo.ru/images/noavatar.png";
   }

   if (!$client_id['token']) {
    $client_id['token'] = 'error';
   }

   // ранг =================
   $rposts = abs(intval($client_id['posts']));
   $rbanned = $client_id['banned'];
   $ruser_group = $client_id['user_group'];
   $rreputation = intval($client_id['reputation']);
   $rlastdate = $client_id['lastdate'];
   $rregistration = $client_id['reg_date'];
   $rat = intval($client_id['rating']);

   $status = strip_tags(user_level($rposts, $rbanned, $ruser_group, $rreputation, $rlastdate, $rregistration, $user_id, $rat));
   $registration = langdate($config['timestamp_active'], $client_id['reg_date']);
   $lastdate = langdate($config['timestamp_active'], $client_id['lastdate']);

   $data['pm_unread'] = abs(intval($client_id['pm_unread']));
   $data['lid'] = $user_id;
   $data['user'] = $user_name;
   $data['token'] = $client_id['token'];
   $data['current_token'] = $token;
   $data['image'] = $foto;
   $data['headers'] = $status;
   $data['reputation'] = $rreputation;
   $data['reg_date'] = $registration;
   $data['rating'] = intval($client_id['vote_num']);
   $data['count'] = intval($client_id['posts']);
   $data['time'] = $lastdate;
   $data['state'] = 1;
   $data['user_id'] = $user_id;
   $data['user_group'] = intval($client_id['user_group']);

   if ($_POST['token']) {
    $db->query("UPDATE " . PREFIX . "_users SET token = '$token' WHERE user_id='" . $user_id . "'");
    $data['update'] = 1;
   }

   if ($_GET['addfav'] == 1) { // добавление в избранное

    $id = intval($_GET['id']);
    $row = $db->super_query("SELECT favorites_" . $razdel . ", user_id FROM " . PREFIX . "_favorites where user_id = '" . $user_id . "' and profile='4'");
    $list = explode(",", $row['favorites_' . $razdel]);
    foreach ($list as $daten) {
     if ($daten == $id) {
      die("error");
     }

    }
    $list[] = $id;
    $favorites = implode(",", $list);
    if ($row['favorites_' . $razdel] == "") {
     $favorites = $id;
    }

    $row['favorites_' . $razdel] = $favorites;
    if (!empty($row['user_id'])) {
     $db->query("UPDATE " . PREFIX . "_favorites set favorites_" . $razdel . "='$favorites' where user_id = '" . $user_id . "' and profile='4'");
    } else {
     $db->query("INSERT INTO " . PREFIX . "_favorites  (favorites_" . $razdel . ",user_id, profile) VALUES ('$favorites', '" . $user_id . "', '4')");
    }

   }

   if ($_GET['addfav'] == 2) { // удаление из избранного

    $id = intval($_GET['id']);
    $row = $db->super_query("SELECT favorites_" . $razdel . ", user_id FROM " . PREFIX . "_favorites where user_id = '" . $user_id . "' and profile='4'");
    $row['favorites_' . $razdel] = str_replace("," . $id, "", $row['favorites_' . $razdel]);
    $row['favorites_' . $razdel] = str_replace($id . ",", "", $row['favorites_' . $razdel]);
    $row['favorites_' . $razdel] = str_replace($id, "", $row['favorites_' . $razdel]);

    $db->query("UPDATE " . PREFIX . "_favorites set favorites_" . $razdel . "='" . $row['favorites_' . $razdel] . "' where user_id = '" . $user_id . "' and profile='4'");

   }

   if ($_GET['addfav'] == 3) { // добавление в friends

     $id = intval($_GET['id']);
     
     $name_to = $db->super_query("SELECT name FROM " . PREFIX . "_users where user_id = '".$id."'");
     $name_to = $name_to['name'];

     $row = $db->super_query("SELECT * FROM " . PREFIX . "_friends where user_id = '".$user_id."'");
   
     $names = explode(",", $row['friends_users']);
     
     foreach ($names as $name) {
     
       if ($name == $name_to) return;
     
     }
   
     $names[] = $name_to;

     $ignored_users = implode(",", $names);
   
     if ($row['friends_users'] == "") $ignored_users = $name_to;
   
     $row['friends_users'] = $ignored_users;
   
     if (!empty($row['user_id'])) {
       $db->query("UPDATE " . PREFIX . "_friends set friends_users='$ignored_users' where user_id = '".$user_id."'");
     } else {
       $db->query("INSERT INTO " . PREFIX . "_friends  (friends_users,user_id) VALUES ('$ignored_users', '".$user_id."')");
     }	

     // check ignor
     $row = $db->super_query("SELECT * FROM " . PREFIX . "_ignor where user_id = '".$user_id."'");
     $row['ignored_users'] = str_ireplace(",".$name_to, "", $row['ignored_users']);
     $row['ignored_users'] = str_ireplace($name_to.",", "", $row['ignored_users']);
     $row['ignored_users'] = str_ireplace($name_to, "", $row['ignored_users']);
     if (preg_match("/[0-9a-z_]/i", $name_to)) $db->query("UPDATE " . PREFIX . "_ignor set ignored_users='".$row['ignored_users']."' where user_id = '".$user_id."'");

    }

    
   if ($_GET['addfav'] == 4) { // добавление в ignor

     $id = intval($_GET['id']);
     
     $name_to = $db->super_query("SELECT name FROM " . PREFIX . "_users where user_id = '".$id."'");
     $name_to = $name_to['name'];

     $row = $db->super_query("SELECT * FROM " . PREFIX . "_ignor where user_id = '".$user_id."'");
   
     $names = explode(",", $row['ignored_users']);
     
     foreach ($names as $name) {
     
       if ($name == $name_to) return;
     
     }
   
     $names[] = $name_to;

     $ignored_users = implode(",", $names);
   
     if ($row['ignored_users'] == "") $ignored_users = $name_to;
   
     $row['ignored_users'] = $ignored_users;
   
     if (!empty($row['user_id'])) {
       $db->query("UPDATE " . PREFIX . "_ignor set ignored_users='$ignored_users' where user_id = '".$user_id."'");
     } else {
       $db->query("INSERT INTO " . PREFIX . "_ignor  (ignored_users,user_id) VALUES ('$ignored_users', '".$user_id."')");
     }	

     // check friends
     $row = $db->super_query("SELECT * FROM " . PREFIX . "_friends where user_id = '".$user_id."'");
       $row['friends_users'] = str_ireplace(",".$name_to, "", $row['friends_users']);
       $row['friends_users'] = str_ireplace($name_to.",", "", $row['friends_users']);
       $row['friends_users'] = str_ireplace($name_to, "", $row['friends_users']);
       if (preg_match("/[0-9a-z_]/i", $name_to)) $db->query("UPDATE " . PREFIX . "_friends set friends_users='".$row['friends_users']."' where user_id = '".$user_id."'");

    }

    if ($_GET['addfav'] == 5) { // delete all в ignor

     $id = intval($_GET['id']);
     
     $name_to = $db->super_query("SELECT name FROM " . PREFIX . "_users where user_id = '".$id."'");
     $name_to = $name_to['name'];

     // check friends
     $row = $db->super_query("SELECT * FROM " . PREFIX . "_friends where user_id = '".$user_id."'");
       $row['friends_users'] = str_ireplace(",".$name_to, "", $row['friends_users']);
       $row['friends_users'] = str_ireplace($name_to.",", "", $row['friends_users']);
       $row['friends_users'] = str_ireplace($name_to, "", $row['friends_users']);
       if (preg_match("/[0-9a-z_]/i", $name_to)) $db->query("UPDATE " . PREFIX . "_friends set friends_users='".$row['friends_users']."' where user_id = '".$user_id."'");

     // check ignor
     $row = $db->super_query("SELECT * FROM " . PREFIX . "_ignor where user_id = '".$user_id."'");
     $row['ignored_users'] = str_ireplace(",".$name_to, "", $row['ignored_users']);
     $row['ignored_users'] = str_ireplace($name_to.",", "", $row['ignored_users']);
     $row['ignored_users'] = str_ireplace($name_to, "", $row['ignored_users']);
     if (preg_match("/[0-9a-z_]/i", $name_to)) $db->query("UPDATE " . PREFIX . "_ignor set ignored_users='".$row['ignored_users']."' where user_id = '".$user_id."'"); 

    }

  } else {
   $data['state'] = 0;
  }

 } else {
  $data['state'] = 0;
 }

} else {
 $data['state'] = 0;
}

echo json_encode($data);
exit();

?>