<?php
if( !defined( 'DATALIFEENGINE' ) ) {
	header( "HTTP/1.1 403 Forbidden" );
	header ( 'Location: ../../' );
	die( "Hacking attempt!" );
}

$id = intval($_GET['id']);
 $ttt = intval($_GET['t']);
 $unique = htmlspecialchars(strip_tags(addslashes(trim($_GET['u']))));
 $member_id = $db->super_query("SELECT user_id, name FROM " . PREFIX . "_users WHERE name LIKE '" . $unique . "'");
 $unique = $member_id['name'];
 $mname = totranslit($unique);
 $p = $db->super_query("SELECT author_name,author_id,topic_id FROM " . PREFIX . "_posts WHERE pid = '$id'");
 if ($p['author_name'] == $unique) {
  die("error");
 }

 $t = $db->super_query("SELECT forum_id,tid,title FROM " . PREFIX . "_topics WHERE tid = '" . $p['topic_id'] . "'");
 $row7 = $db->super_query("SELECT text FROM " . PREFIX . "_spasibo WHERE pid = '$id'");
 $row8 = $db->super_query("SELECT twitter FROM " . PREFIX . "_users WHERE name = '" . $p['author_name'] . "'");
 if (preg_match("/" . totranslit($unique) . "/i", $row7['text'])) {die("error");}
 $spcount = 1;
 $sp = explode(" ", $row7['text']);
 $spcount = intval($sp[0]) + 1;
 $spc = word_filter($sp[4] . strip_tags($mname));
 $sps = "$spcount посетителей выразили благодарность: $spc,";
 $list = explode(",", $row7['text']);
 $subj = stripslashes(no_bb($t['title']));
 $f = intval($t['forum_id']);
 $t = intval($t['tid']);

 $number = $db->super_query("SELECT SUM(number) as sum FROM " . PREFIX . "_posts WHERE (topic_id = $t) and (pid < $id)");
 $number = intval($number['sum']);
 $number = intval($number) + 1;
 $mins = floor($number / 20);
 $mins = $mins * 20;

 $tema = "[url=https://dimonvideo.ru/forum/topic_$t/$f/0]" . $subj . "[/url] пост [url=https://m.dimonvideo.ru/forum/post_$id]номер " . $number . "[/url]";
 if ($spcount != 1) {
  $db->query("UPDATE LOW_PRIORITY " . PREFIX . "_spasibo SET text='$sps' WHERE pid = '$id'");
  $db->query("UPDATE LOW_PRIORITY " . PREFIX . "_posts SET sps=sps+1 WHERE pid = '$id'");

  if ($row8['twitter'] == 0) {
   $pmclass->sent_pm(0, 'Благодарность', 'Спасибо за ответ в теме ' . $tema, $p['author_name'], '', $unique, '', '', '', '', '', 1, $t, $f, 0, '', '');
  }

  $db->query("UPDATE LOW_PRIORITY " . PREFIX . "_users SET sps=sps+1 WHERE name = '" . $p['author_name'] . "'");

 } else {
  $db->query("INSERT LOW_PRIORITY INTO " . PREFIX . "_spasibo SET text='$sps', pid='$id'");
  $db->query("UPDATE LOW_PRIORITY " . PREFIX . "_posts SET sps=sps+1 WHERE pid = '$id'");
  if ($row8['twitter'] == 0) {
   $pmclass->sent_pm(0, 'Благодарность', 'Спасибо за ответ в теме ' . $tema, $p['author_name'], '', $unique, '', '', '', '', '', 1, $t, $f, 0, '', '');
  }

  $db->query("UPDATE LOW_PRIORITY " . PREFIX . "_users SET sps=sps+1 WHERE name = '" . $p['author_name'] . "'");
 }

//===== репа
 $dr = time() - 86400 * 7;
 $ww = $db->super_query("SELECT user_id, name, allow_rep FROM " . PREFIX . "_users where name = '" . $p['author_name'] . "'");
 if (($ww['allow_rep'] == 0) and ($ww['user_id'] > 0)) {
  $count = $db->super_query("SELECT COUNT(*) as count FROM " . PREFIX . "_reputation WHERE (created > $dr) and (from_user = '" . $unique . "')");
  $count = intval($count['count']);
  $k = 500;
  $counts = $db->super_query("SELECT COUNT(*) as count FROM " . PREFIX . "_reputation WHERE (created > $dr) and (to_user = '" . $ww['user_id'] . "') and (from_user = '" . $unique . "') ");
  $counts = intval($counts['count']);
  if (($counts == 0) and ($count <= $k)) {
   $rep_from = $unique;
   if ($unique == $ww['name']) {die("self destruction!");}
   $action = "$rep_from [b][color=red]увеличил[/color][/b] Вашу репутацию, указав следующую причину: Спасибо за ответ в теме: " . $tema . "";

   $pmclass->sent_pm(0, 'Изменение репутации', $action, $p['author_name'], '', $rep_from, '', 0, 0, '', 2, 1);

   $db->query("INSERT LOW_PRIORITY INTO " . PREFIX . "_reputation (from_user, to_user, forum, topic, post, created, message, rating, anonym, fsystem) VALUES ('" . intval($member_id['user_id']) . "', '" . $ww['user_id'] . "', '0', '0','0','" . time() . "', 'Спасибо за ответ в теме: " . $tema . "', '1', 0, 1)");
   $db->query("UPDATE LOW_PRIORITY " . PREFIX . "_users set reputation = reputation +1 where name='" . $p['author_name'] . "'");
  }
 }
?>