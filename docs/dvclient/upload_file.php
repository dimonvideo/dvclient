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

		$pic = "pic";
		if (isset($_FILES['gif']['name'])) $pic = "gif";

		if (isset($_FILES[$pic]['name']) && isset($_POST['name'])) {

			//uploading file and storing it to database as well
			try {
				$valid_img = array("png", "gif");
				$path      = ROOT_DIR . "/files/uploadslinks/msg/api/" . $_POST['name'] . "/";
				$path_i      = ROOT_DIR . "/files/uploadslinks/msg/api/" . $_POST['name'] . "/thumbs/";
				$pathc     = mb_substr($path, 0, mb_strrpos($path, "/"));
				$pathi     = mb_substr($path_i, 0, mb_strrpos($path_i, "/"));

				if (is_uploaded_file($_FILES[$pic]['tmp_name'])) {
					$file_name = $_FILES[$pic]['tmp_name'];
					$size_file = filesize($file_name);
					$ext_file  = mb_substr($_FILES[$pic]['name'], 1 + mb_strrpos($_FILES[$pic]['name'], "."));
					$ext_file  = mb_strtolower($ext_file);

					if ($size_file > $maxsize) {
						$response['error']     = "true";
						$response['file_link'] = 'Размер изображения слишком велик';
					}

					if (!in_array($ext_file, $valid_img)) {
						$response['error']     = "true";
						$response['file_link'] = 'Это не изображение';
					}

					if ($size_file < 1) {
						$response['error']     = "true";
						$response['file_link'] = 'Ошибка сети, повторите загрузку';
					}

					if (!file_exists($pathc)) {
						umask(0);
						@mkdir($pathc, 0777);
					}

					if (!file_exists($pathi)) {
					  umask(0);
					  @mkdir($pathi, 0777);
					}

					$nameof = mb_substr($db->safeSQL($_FILES[$pic]['name']), 0, mb_strrpos($_FILES[$pic]['name'], "."));
					$new    = totranslit($nameof) . '_' . rand(0,999) . '.' . $ext_file;

					$img = 0;
					umask(0);
					$res = @move_uploaded_file($file_name, $path . '/' . $new);
					@chmod($path . '/' . $new, 0666);

					$maxside = 800;
					if (in_array($ext_file, $valid_img)) {
					  //  exec('/usr/bin/mogrify -resize "' . $maxside . '>" ' . $path . '/' . $new);
						$image = new Imagick($path . '/' . $new);
						$image->resizeImage(320,0,Imagick::FILTER_LANCZOS, 1, false);
						$image->writeImage($path_i . '/' . $new);
					}

					if (!$res) {
						$response['error']     = "true";
						$response['file_link'] = 'Ошибка загрузки изображения';
					}

				}

				if (isset($size_file)) {
					$response['error']     = "false";
					$response['file_link'] = $new;
				} 
				
			} catch (Exception $e) {
				$response['error']     = "true";
				$response['file_link'] = 'Ошибка '.$e;
			}

		} else {
			$response['error']     = "true";
			$response['file_link'] = "Required params not available";
		}

		break;
			  
		// delete file
		case 'delete':
		  if (isset($_POST['send_screen']) && isset($_POST['send_id'])) {

			@unlink(ROOT_DIR . "/files/uploadslinks/msg/api/" . $_POST['send_id'] . "/".$_POST['send_screen']);
			@unlink(ROOT_DIR . "/files/uploadslinks/msg/api/" . $_POST['send_id'] . "/thumbs/".$_POST['send_screen']);
			$response['error']     = "false";
			$response['file_link'] = "Успешно удалено ".$_POST['send_screen'];
			header('Content-Type: application/json');
			echo json_encode($response);
			exit();
		  
		  } else {
			$response['error']     = "true";
			$response['file_link'] = "Required params not available";
			header('Content-Type: application/json');
			echo json_encode($response);
			exit();
		  }


		break;

	default:
		$response['error']     = "true";
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