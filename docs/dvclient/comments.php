<?php
if( !defined( 'DATALIFEENGINE' ) ) {
	header( "HTTP/1.1 403 Forbidden" );
	header ( 'Location: ../../' );
	die( "Hacking attempt!" );
}

$data = array();
$lid = intval($_GET['lid']);
$st = intval($_GET['st']);
$i = 1;
$find = 0;

$sql ="SELECT c.*, u.*, u.reg_date as regdate FROM " . PREFIX . "_" . $table . "_com c LEFT JOIN " . PREFIX . "_users u ON c.autor = u.name  WHERE post_id='$lid' order by date DESC LIMIT $min, $fo";

if ($lid == 0)  $sql = "SELECT c.*, u.*, u.reg_date as regdate, f.title as title FROM " . PREFIX . "_" . $table . "_com c LEFT JOIN " . PREFIX . "_users u ON c.autor = u.name LEFT JOIN " . PREFIX . "_" . $table . "_pic f ON f.lid = c.post_id order by date DESC LIMIT $min, $fo";

if ($st == 3) $sql = "SELECT c.post_id, c.text, 'uploader' AS razdel, c.date, f.title as title  FROM " . PREFIX . "_uploader_com c LEFT JOIN dle_uploader_pic f ON f.lid = c.post_id  WHERE DATE(c.date) = CURDATE() 
UNION ALL SELECT c.post_id, c.text, 'android' AS razdel, c.date, f.title as title   FROM " . PREFIX . "_android_com  c LEFT JOIN dle_android_pic f ON f.lid = c.post_id  WHERE DATE(c.date) = CURDATE() 
UNION ALL SELECT c.post_id, c.text, 'vuploader' AS razdel, c.date, f.title as title   FROM " . PREFIX . "_vuploader_com c LEFT JOIN dle_vuploader_pic f ON f.lid = c.post_id  WHERE DATE(c.date) = CURDATE()  
UNION ALL SELECT c.post_id, c.text, 'comments' AS razdel, c.date, f.title as title   FROM " . PREFIX . "_comments_com c LEFT JOIN dle_comments_pic f ON f.lid = c.post_id  WHERE DATE(c.date) = CURDATE() 
UNION ALL SELECT c.post_id, c.text, 'usernews' AS razdel, c.date, f.title as title   FROM " . PREFIX . "_usernews_com c LEFT JOIN dle_usernews_pic f ON f.lid = c.post_id  WHERE DATE(c.date) = CURDATE() 
UNION ALL SELECT c.post_id, c.text, 'muzon' AS razdel, c.date, f.title as title   FROM " . PREFIX . "_muzon_com c LEFT JOIN dle_muzon_pic f ON f.lid = c.post_id  WHERE DATE(c.date) = CURDATE() 
UNION ALL SELECT c.post_id, c.text, 'articles' AS razdel, c.date, f.title as title   FROM " . PREFIX . "_articles_com c LEFT JOIN dle_articles_pic f ON f.lid = c.post_id  WHERE DATE(c.date) = CURDATE() 
UNION ALL SELECT c.post_id, c.text, 'gallery' AS razdel, c.date, f.title as title   FROM " . PREFIX . "_gallery_com c LEFT JOIN dle_gallery_pic f ON f.lid = c.post_id  WHERE DATE(c.date) = CURDATE() ORDER BY date DESC LIMIT $min, $fo
";

$sql_result = $db->query($sql);

while ($row = $db->get_row($sql_result)) {

 if ($st == 3) $razdel = $row['razdel'];
 $find = 1;
 $row['date'] = strtotime($row['date']);
 $date = langdate($config['timestamp_active'], $row['date']);

 $title = stripslashes(html_entity_decode(no_bb(strip_tags($row['title']))));
 $text = stripslashes(html_entity_decode($parse->BB_Parse($row['text'], false)));
 $text = str_replace("https://m.dimonvideo.ru/go/?", "", $text);
 $text = str_replace("https://m.dimonvideo.ru/go?", "", $text);
 $text = str_replace("https://dimonvideo.ru/go/?", "", $text);
 $text = str_replace("https://dimonvideo.ru/go?", "", $text);
 $text = preg_replace('~\s*,\s*~', ', ', $text);
 $name = (stripslashes($row['autor']));
 $id = intval($row['id']);
 $post_id = intval($row['post_id']);
 if ($row['foto']) {
  $logo = "/fotos/" . $row['foto'];
 } else {
  $logo = "/images/noavatar.png";
 }
 // ранг пользователя =================================
 $posts = $row['posts'];
 $banned = $row['banned'];
 $user_group = $row['user_group'];
 // ранг =================
 $rposts = $row['posts'];
 $rbanned = $row['banned'];
 $ruser_group = $row['user_group'];
 $rreputation = $row['reputation'];
 $rlastdate = $row['lastdate'];
 $rregistration = $row['reg_date'];
 $rat = intval($row['rating']);
 $status = strip_tags(user_level($rposts, $rbanned, $ruser_group, $rreputation, $rlastdate, $rregistration, $row['user_id'], $rat));

 $data[] = ["topic_id" => $id, "lid" => $lid, "post_id" => $post_id, "image" => $logo, "newtopic" => $newtopic, "user" => $name, "title" => $title, "text" => $text, "category" => $status, "date" => $date, "razdel" => $razdel, "pinned" => $pinned, "rating" => $posts, "time" => $row['date'], "views" => $views, "min" => $min];
 $i++;
}

if ($find == 0) {

 $data[] = ["topic_id" => 0, "lid" => $lid, "post_id" => $post_id, "image" => "/images/noavatar.png", "newtopic" => $newtopic, "user" => "-", "title" => $sql, "text" => "Комментариев еще нет", "category" => "---", "date" => langdate($config['timestamp_active'], time()), "razdel" => $razdel, "pinned" => $pinned, "rating" => $posts, "time" => time(), "views" => $st, "min" => 0];
 if ($min > 0) {
  exit();
 }

 echo json_encode($data, JSON_UNESCAPED_UNICODE);
 exit();
}
echo json_encode($data, JSON_UNESCAPED_UNICODE);
exit();
?>