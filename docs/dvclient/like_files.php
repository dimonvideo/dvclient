<?php
if( !defined( 'DATALIFEENGINE' ) ) {
	header( "HTTP/1.1 403 Forbidden" );
	header ( 'Location: ../../' );
	die( "Hacking attempt!" );
}

$id = intval($_GET['id']);
 $uid = intval($_GET['from_id']);
 $t = intval($_GET['t']);
 $unique = htmlspecialchars(strip_tags(addslashes(trim($_GET['u']))));
 $w = $db->super_query("SELECT lid,name,title FROM  " . PREFIX . "_" . $table . "_pic WHERE lid = '$id'");
 if (!empty($w['lid'])) {
  $check = $db->super_query("SELECT COUNT(*) AS count FROM " . PREFIX . "_frating WHERE name = '" . $unique . "' AND lid='$id' AND razdel = '$razdel' AND reason='plus'");
  $check2 = $db->super_query("SELECT COUNT(*) AS count FROM " . PREFIX . "_frating WHERE name = '" . $unique . "' AND lid='$id' AND razdel = '$razdel' AND reason='minus'");
  if ($t == 1) {
   if (intval($check['count']) == 0) {
    $db->query("INSERT  LOW_PRIORITY INTO " . PREFIX . "_showfilenew SET uid = '0',name = 'DVClient', razdel='$razdel', date = '" . time() . "', lid='$id'");
    $db->query("UPDATE " . PREFIX . "_fastdata SET hits=hits+1, plus=plus+1 WHERE lid = '$id' and razdel = '$table'");
    $db->query("INSERT INTO " . PREFIX . "_frating SET name = '" . $unique . "', lid='$id', razdel = '$razdel', reason='plus'");
   }
  } elseif ($t == 2) {

   if (intval($check['count']) > 0) {
    $db->query("UPDATE " . PREFIX . "_fastdata SET hits=hits+1, plus=plus-1 WHERE lid = '$id' and razdel = '$table'");
    $db->query("DELETE FROM " . PREFIX . "_frating WHERE name = '" . $unique . "' AND lid='$id' AND razdel = '$razdel' AND reason='plus'");
   }

  } elseif ($t == 3) {

  if (intval($check2['count']) == 0) {
   $db->query("INSERT  LOW_PRIORITY INTO " . PREFIX . "_showfilenew SET uid = '0',name = 'DVClient', razdel='$razdel', date = '" . time() . "', lid='$id'");
   $db->query("UPDATE " . PREFIX . "_fastdata SET hits=hits+1, minus=minus+1 WHERE lid = '$id' and razdel = '$table'");
   $db->query("INSERT INTO " . PREFIX . "_frating SET name = '" . $unique . "', lid='$id', razdel = '$razdel', reason='minus'");
  }
  
} elseif ($t == 4) {

    if (intval($check2['count']) > 0) {
     $db->query("UPDATE " . PREFIX . "_fastdata SET hits=hits+1, minus=minus-1 WHERE lid = '$id' and razdel = '$table'");
     $db->query("DELETE FROM " . PREFIX . "_frating WHERE name = '" . $unique . "' AND lid='$id' AND razdel = '$razdel' AND reason='minus'");
    }
 
  } 
 }

 if ($t == 5) { // members star

  $row = $db->super_query( "SELECT uid FROM " . PREFIX . "_ulogs WHERE (uid = ".$id." AND members = '".$unique."')" );

  if ($uid == $id) return;

  if(!$row['uid']) {

    $db->query( "UPDATE LOW_PRIORITY " . PREFIX . "_users set rating=rating+5, vote_num=vote_num+1 where user_id ='$id'" );      
      
    $db->query( "INSERT  INTO " . PREFIX . "_ulogs (uid, ip, members) values ('$id', 'dvclient', '$unique')" );
    
  }

 }

?>