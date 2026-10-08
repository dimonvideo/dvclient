<?php
if( !defined( 'DATALIFEENGINE' ) ) {
	header( "HTTP/1.1 403 Forbidden" );
	header ( 'Location: ../../' );
	die( "Hacking attempt!" );
}

$data = array();
 $ip = filter_var($_SERVER['REMOTE_ADDR'], FILTER_VALIDATE_IP) ? $_SERVER['REMOTE_ADDR'] : '127.0.0.1';
 $login_name = trim($db->safesql(strip_tags((string)$_POST['userName'])));
 $login_password = md5((string)$_POST['userPassword']);
 if (preg_match("/[\||\'|\<|\>|\"|\!|\?|\$|\@|\/|\\\|\&\~\*\+]/", $login_name)) {
  $login_name = "";
 }

 $email = trim($db->safeSQL(strip_tags(urldecode($_POST['userEmail']))));
 if (empty($email) OR (mb_strlen($email) > 50) OR (filter_var($email, FILTER_VALIDATE_EMAIL) == false)) {
  $data['state'] = 4; // email неверно введен
  echo json_encode($data);
  exit();
 }

 if ($login_name) {

  $row = $db->super_query("SELECT COUNT(*) as count FROM " . PREFIX . "_users WHERE email = '$email'");
  if ($row['count']) {
   $data['state'] = 3; // email занят
   echo json_encode($data);
   exit();
  }
  $row = $db->super_query("SELECT COUNT(*) as count FROM " . PREFIX . "_users WHERE name = '$login_name'");
  if ($row['count']) {
   $data['state'] = 5; // nik занят
   echo json_encode($data);
   exit();
  }

  $login_name = $db->safeSQL(strip_tags(urldecode($login_name)));

  $client_id = $db->super_query("SELECT user_id, password FROM " . PREFIX . "_users WHERE name='" . $login_name . "' and password='" . md5($login_password) . "'");

  if ($client_id['user_id'] AND $client_id['password'] AND $client_id['password'] == md5($login_password)) {

   $data['state'] = 1; // уже зарегистрирован

  } else {

   $data['state'] = 2; // успешная регистрация
   $add_time = time() + ($date_adjust * 60);
   $regpassword = md5(md5($login_password));

   $db->query("INSERT INTO " . PREFIX . "_users (name, password, email, reg_date, lastdate, user_group,  ip_address) VALUES ('$login_name', '$regpassword', '$email', '$add_time', '$add_time', '4', '" . $ip . "')");

  }

 } else {
  $data['state'] = 0;
 }
// error

 echo json_encode($data);
 exit();
?>