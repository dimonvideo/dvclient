<?php
if( !defined( 'DATALIFEENGINE' ) ) {
	header( "HTTP/1.1 403 Forbidden" );
	header ( 'Location: ../../' );
	die( "Hacking attempt!" );
}

$ua = mb_strtolower($_SERVER['HTTP_USER_AGENT']);
  $ip   = $db->safeSQL($_SERVER['REMOTE_ADDR']);
  $flag = 0;

  $nick = htmlspecialchars(strip_tags(addslashes(trim($_GET['u']))));
  $name = $nick;
  if (!isset($_GET['u']) OR ($_GET['u'] == "dvclient")) {
    $name = "аноним";$nick = $ip;
  }

  $op_vote = !empty($_POST['op_vote']) ? intval(abs($_POST['op_vote'])) : intval(abs($_GET['op_vote']));
  $stop     = false;
  $is_voted = false;
  $entry = "";
  $data = array();

  if (isset ($_REQUEST['vote_action'])) $vote_action = $_REQUEST['vote_action']; else $vote_action = "";
  if (isset ($_REQUEST['vote_id'])) $vote_id = intval($_REQUEST['vote_id']); else $vote_id = 0;
  if (isset ($_REQUEST['vote_check'])) $vote_check = intval($_REQUEST['vote_check']); else $vote_check = 0;

  if (empty($op_vote)){  
    $vote_info = get_vars ("vote");

    if (!$vote_info) {
      $vote_info = array ();
      $result_vote = $db->query("SELECT id, title, body, vote_num, comments FROM " . PREFIX . "_vote");  

	    while($row = $db->get_row($result_vote)){

	    $vote_info = array (
		    'id'        => $row['id'],
		    'title'     => $row['title'],
		    'body' 		=> $row['body'],
		    'vote_num'  => $row['vote_num'],
		    'comments' => $row['comments'],
		    );
	    }
	
      set_vars ("vote", $vote_info);
      $db->free($result_vote);
    }

 
    
    $rid = intval($vote_info['id']);    
    $title = stripslashes($vote_info['title']);
    $body  = stripslashes($vote_info['body']);
    $body  = explode("<br />", $body);
    $max   = $vote_info['vote_num'];
       
    if ( ( ( isset( $_COOKIE[ 'opros' ] )and( $_COOKIE[ 'opros' ] == $rid ) )and( ( $_COOKIE[ 'opros_user_id' ] ) == $nick ) ) ) {
      $vote_action == "results";
      $flag = 1;
      if (!isset($_GET['u']) OR ($_GET['u'] == "dvclient")) $flag = 0;
    }

    if ($vote_action == "vote") {
            
      $row  = $db->super_query("SELECT count(*) as count FROM ".PREFIX."_vote_result WHERE vote_id='$rid' AND name='$nick'");
      
      if ($row['count'] == 0) { 
        $is_voted = false;
      } else { 
        $is_voted = true;
        set_cookie ("opros", $rid, 31);
        set_cookie ("opros_user_id", $nick, 31);
      }

      $flag = 1;
      
      if ($is_voted == false) {
        
        $db->query("INSERT INTO ".PREFIX."_vote_result (ip, name, vote_id, answer) VALUES ('$ip', '$nick', '$rid', '$vote_check')");
        $db->query("UPDATE ".PREFIX."_vote set vote_num=vote_num+1 where id='$rid'");
        @unlink(ENGINE_DIR.'/cache/system/vote.php');
        @unlink(ENGINE_DIR.'/cache/system/voteresult.php');
        set_cookie ("opros", $rid, 31);
        set_cookie ("opros_user_id", $nick, 14);
        $max++;
      }
    }
    
    if ($vote_action == "results" OR $flag) {
      
      $answer = get_vars ("voteresult");
      
      if (!$answer){
        $result  = $db->query("SELECT answer, count(*) as count FROM ".PREFIX."_vote_result WHERE vote_id='$rid' GROUP BY answer");
        $flag = 1;
        $pn = 0;
        $answer = array ();
        
        while ($row = $db->get_row($result)) {
          $answer[$row['answer']]['count']  = $row['count'];
        }

        set_vars ("voteresult", $answer);
      }
    }
    
    switch ($flag) {
      
      case 0 :
        for ($i = 0; $i < sizeof($body); $i++) {
          if ($i == 0) { $sel = "checked"; } else {$sel = ""; };
          $entry .= "<tr><td width=\"10\"><input name=\"vote_check\" type=\"radio\" $sel value=\"$i\"></td><td class=\"vote\" width=\"100%\" style='padding-left: 7px;'>$body[$i]</td></tr>";
        }
        $entry = "<table cellpadding=\"3\" cellspacing=\"0\" width=\"100%\" border=0>$entry</table>";
     break;

     case 1:
      for ($i = 0; $i < sizeof($body); $i++) {
        ++$pn; if ($pn > 5) $pn = 1;
        $num = $answer[$i]['count'];
    
        if (!$num) $num = 0;
        if ($max != 0) $proc = (100 * $num) / $max; else $proc = 0;
        settype($proc, "integer");

        $entry .=
          "<table cellpadding=\"3\" cellspacing=\"0\" width=\"100%\" border=0><tr><td valign=\"middle\" >$body[$i] - $num ($proc%)</td></tr>
          <tr><td valign=\"middle\" height=\"10\" width=\"100%\">
          <img src=\"{$config['http_home_url']}templates/{$config['skin']}/dleimages/poll{$pn}.gif\" height=\"10\" width=\"$proc%\" style=\"border:1px solid black\">
          </td></tr></table>";
        }
        
        $entry = "<table cellpadding=\"0\" cellspacing=\"0\" width=\"95%\">$entry</table><br>Всего голосовало $max человек";
      //  $entry .= "<br><a href=/forum/topic_1728131216/6/0>Результаты опросов</a> | <a href=/forum/topic_1728149578/19/0>Предложи опрос!</a><div id=plus-".$rid."></div><br>";
     
        break;
      }
      
      $content .= <<<HTML

	      <form method="post" name="vote" action=''>
        $entry <br>
        <table width="100%">
        <tr><td>
		    <input type="hidden" name="vote_action" value="vote">
          <input type="hidden" name="vote_id" id="vote_id" value="{$rid}">
HTML;


if ($flag == 0) $content .= <<<HTML
<input type="submit"  class="btn btn-primary btn-sm" value="Голосовать как {$name}">
HTML;
$content .= <<<HTML

          </td></form></tr>
         </table>
HTML;

  }
      
  header('Content-Type: text/html; charset=utf-8',true);
  echo <<<HTML
  <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
    "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
  <html>
  <head>
<title>Опросы</title>
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
  <meta name="robots" content="noindex,nofollow">
  <META NAME="Document-state" CONTENT="Dynamic">
  <meta name="description" content="Votes">
  <meta name="generator" content="DimonVideo.ru">
  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=yes">
  <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css">
  <link href="https://dimonvideo.ru/templates/7/css.min.css?v=1" rel="stylesheet" type="text/css">
  </head>
<body>
<div class="container">
    <div class="row"><div class="col-sm-12"><br>
HTML;

      echo $content;
echo <<<HTML
<br><hr>
</div></div></div>
</body>
</html>
HTML;
$db->close ();
GzipOut ();
exit(); 

?>