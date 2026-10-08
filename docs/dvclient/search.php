<?php
if( !defined( 'DATALIFEENGINE' ) ) {
	header( "HTTP/1.1 403 Forbidden" );
	header ( 'Location: ../../' );
	die( "Hacking attempt!" );
}

$story = $db->safeSQL(stripslashes($_GET['story']));
$story = filter_var($story, FILTER_SANITIZE_STRING);
$story = str_replace("'", '', $story);
$story = str_replace("&#39;", "", $story);

$story = strip_data(rawurldecode(trim($story)));
$story = (mb_substr($story, 0, 90));

$vr = 'data';
$datelimit = time() - 86400 * 365 * 20;
$story = mb_strtolower(str_replace("'", "", $story));
$story = str_replace("-", " ", $story);
$story = stripslashes($story);
require_once ROOT_DIR . "/sphinxapi.php";
$sphinx = new SphinxClient();
$sphinx->SetServer('127.0.0.1', 9312);
$sphinx->SetMatchMode(SPH_MATCH_ANY);
$sphinx->SetSortMode(SPH_SORT_RELEVANCE);
$d1 = time();

$w = array('listtext' => 10, 'title' => 100);

$sphinx->SetLimits(($p - 1) * $fo, $fo, 5000);

if (($razdel != 'members')) {
 $sphinx->SetFieldWeights($w);
 $sphinx->setFilterRange($vr, $datelimit, $d1);
 }

if ($razdel == 'members') {
   $index = 'users';
 } else $index = 'dimonvideo_' . $table;

$result = $sphinx->Query('*' . $sphinx->escapeString($story) . '*', $index);
$count = intval($result['total']);
$data = array();

if ($count == 0) {
 if ($p > 1) {
  exit();
 }

 $data[] = ["lid" => 0, "status" => 1, "plus" => 1, "min" => 1, "views" => 0, "file_link" => 0, "mod" => 0, "user" => 0, "size" => 0, "razdel" => $razdel, "headers" => $headers, "category" => 0, "date" => langdate($config['timestamp_active'], time()), "time" => time(), "title" => "Не найдено", "text" => "Ничего не найдено по вашему запросу", "full_text" => "Ничего не найдено по вашему запросу", "image" => 'https://dimonvideo.ru/images/soon.jpg', "rating" => 0, "category" => "не найдено", "fav" => 0];
 echo json_encode($data, JSON_UNESCAPED_UNICODE);
 exit();
}

if ($result && is_array($result['matches'])) {

 $ids = array_keys($result['matches']);

 $sql_pic = $db->query("SELECT f.*, d.*  FROM " . PREFIX . "_" . $table . "_pic f LEFT JOIN " . PREFIX . "_fastdata d on (f.lid = d.lid) and (d.razdel = '" . $table . "') WHERE f.lid in (" . implode(',', $ids) . ") ORDER BY FIELD (f.lid, " . implode(',', $ids) . ")");

 if ($razdel == 'members') $sql_pic = $db->query("SELECT posts, banned, user_group, reputation, rating, reg_date, name AS title, name AS listtext, user_id AS lid, lastdate AS date, foto AS logourl FROM " . PREFIX . "_users WHERE user_id in (".implode(',', $ids).") ORDER BY user_id DESC  LIMIT 0, 35");

 while ($row = $db->get_row($sql_pic)) {

  $row['listtext'] = stripslashes($row['listtext']);
  $docs = array(strip_tags($row['listtext']));

  if ($razdel == 'comments') {
   $docs = array(stripslashes($row['short_story']));
  }

  if ($razdel == 'comments') {
   $row['date'] = strtotime($row['date']);
  }


  $opts = array(
   "before_match" => "",
   "after_match" => "",
   "chunk_separator" => " ... ",
   "limit" => 200,
   "around" => 4,
  );

  $pres = $sphinx->BuildExcerpts($docs, 'dimonvideo_' . $table, $story, $opts);

  if ($pres && is_array($pres)) {

   foreach ($pres as $k => $p) {
    $text = stripslashes(html_entity_decode($parse->BB_Parse($row['listtext'], false)));
    $text = str_replace("https://m.dimonvideo.ru/go/?", "", $text);
    $text = str_replace("https://m.dimonvideo.ru/go?", "", $text);
    $text = str_replace("https://dimonvideo.ru/go/?", "", $text);
    $text = str_replace("https://dimonvideo.ru/go?", "", $text);
    $opisanie = no_bb(stripslashes($p));
    $date = langdate($config['timestamp_active'], $row['date']);
    $status = false;
    $file = ftplinks($razdel, $row['url'], $row['perenos'], $row['peren'], $row['server'], $row['server2'], $row['logourl'], 1, $row['perenoss'], $row['edittime'], $row['date'], false,$row['oldlid']);

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
    if (!empty($logofullarr[2])) $imgfull = $logofullarr[0].'/'.$logofullarr[1].'/big_'.$logofullarr[2];

    $logofull = ftplinks($razdel, $row['url'], $row['perenos'], $row['peren'], $row['server'], $row['server2'], $imgfull, 0, $row['perenoss'], $row['edittime'], $row['date'], false,$row['oldlid']);
    $logo = ftplinks($razdel, $row['url'], $row['perenos'], $row['peren'], $row['server'], $row['server2'], $row['logourl'], 0, $row['perenoss'], $row['edittime'], $row['date'], 320,$row['oldlid']);
    if (($razdel == 'gallery') and (!empty($logofullarr[1]))) {
     $file = 'https://dimonvideo.ru/files/screens.dimonvideo.ru/gallery/' . $imgfull;
    }

    $mod = null;
    if (isset($row['modpath']) and strlen($row['modpath']) > 10) {
     $mod = "https://".$global_url."/" . $row['modpath'];
    }

    $title = stripslashes(stripslashes($row['title'])); // 0 имя
    $size = size($row['size']); // 9 размер

    $views = (int)$row['hits'] + (int)$row['hits2'];
    $com = intval($row['comments']);
    $lid = intval($row['lid']);
    $user = html_entity_decode(strip_tags($row['name']));
    $cname = stripslashes(no_bb($row['cname']));

    if ($razdel == 'members') {
     $logo = "https://dimonvideo.ru/fotos/" . $row[ 'logourl' ];
     $opisanie = 'Нажмите, чтоб написать сообщение';
     $user = 'был на сайте';
         
     // ранг =================
       
     $rposts = abs( intval( $row[ 'posts' ] ) );
     $rbanned = $row[ 'banned' ];
     $ruser_group = $row[ 'user_group' ];
     $rreputation = $row[ 'reputation' ];
     $rlastdate = $row[ 'date' ];
     $rregistration = $row[ 'reg_date' ];
     $rat = intval( $row[ 'rating' ] );
     $text = strip_tags(user_level( $rposts, $rbanned, $ruser_group, $rreputation, $rlastdate, $rregistration, $lid, $rat ));
     
   }
   if ($row['logourl'] == null) $logo = "https://dimonvideo.ru/images/soon.jpg";


    $data[] = ["lid" => $lid, "status" => 1, "plus" => 0, "min" => $page, "views" => $views, "file_link" => $file, "mod" => $mod, "user" => $user, "size" => $size, "razdel" => $razdel, "headers" => $headers, "category" => $cname, "date" => $date, "time" => $row['date'], "title" => $title, "text" => $opisanie, "full_text" => $text, "image" => $logo, "rating" => $com, "fav" => 0];
   }
  }
 }

}
echo json_encode($data, JSON_UNESCAPED_UNICODE);
exit();
?>