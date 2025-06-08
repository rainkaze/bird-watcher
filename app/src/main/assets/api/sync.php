<?php
require 'db_connect.php';

// 验证Token
$headers = getallheaders();
$token = $headers['Authorization'] ?? null;

if (!$token) {
    json_response(['status' => 'error', 'message' => '未提供认证Token'], 401);
}

$stmt = $conn->prepare("SELECT id, token_expires_at FROM users WHERE token = ?");
$stmt->bind_param("s", $token);
$stmt->execute();
$stmt->store_result();
$stmt->bind_result($user_id, $token_expires_at);

if ($stmt->num_rows == 0 || new DateTime() > new DateTime($token_expires_at)) {
    json_response(['status' => 'error', 'message' => 'Token无效或已过期'], 401);
}
$stmt->fetch();
$stmt->close();

$data = json_decode(file_get_contents('php://input'), true);
$records = $data['records'] ?? [];

if (empty($records)) {
    json_response(['status' => 'success', 'message' => '没有需要同步的记录']);
}

// 定义图片上传目录，确保这个目录存在且有写入权限
$upload_dir = '/www/wwwroot/47.94.105.113/uploads/'; // 请确保这个目录存在
if (!is_dir($upload_dir)) {
    mkdir($upload_dir, 0755, true);
}
// 网站的公开URL前缀
$base_image_url = 'http://47.94.105.113/uploads/';


$conn->begin_transaction();

$insert_stmt = $conn->prepare(
    "INSERT INTO records (user_id, client_id, title, content, bird_name, scientific_name, latitude, longitude, detailed_location, photo_uris, audio_uri, record_date_timestamp) " .
    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " .
    "ON DUPLICATE KEY UPDATE " .
    "title=VALUES(title), content=VALUES(content), bird_name=VALUES(bird_name), scientific_name=VALUES(scientific_name), latitude=VALUES(latitude), longitude=VALUES(longitude), detailed_location=VALUES(detailed_location), photo_uris=VALUES(photo_uris), audio_uri=VALUES(audio_uri), record_date_timestamp=VALUES(record_date_timestamp)"
);

$synced_client_ids = [];

try {
    foreach ($records as $record) {
        $final_photo_urls = [];
        if (isset($record['photoUris']) && is_array($record['photoUris'])) {
            foreach($record['photoUris'] as $photo_data) {
                // 判断是Base64数据还是已经是HTTP URL
                if (strpos($photo_data, 'data:image') === 0) {
                    // 是Base64数据，需要解码并保存
                    list($type, $data) = explode(';', $photo_data);
                    list(, $data)      = explode(',', $data);
                    $decoded_data = base64_decode($data);
                    
                    $file_extension = strpos($type, 'jpeg') !== false ? '.jpg' : '.png';
                    $unique_filename = uniqid('img_') . $file_extension;
                    $file_path = $upload_dir . $unique_filename;
                    
                    file_put_contents($file_path, $decoded_data);
                    
                    $final_photo_urls[] = $base_image_url . $unique_filename;
                } else {
                    // 已经是URL，直接使用
                    $final_photo_urls[] = $photo_data;
                }
            }
        }

        $photo_uris_json = json_encode($final_photo_urls);

        $insert_stmt->bind_param("iisssssddssi",
            $user_id,
            $record['id'], 
            $record['title'],
            $record['content'],
            $record['birdName'],
            $record['scientificName'],
            $record['latitude'],
            $record['longitude'],
            $record['detailedLocation'],
            $photo_uris_json, // 存储公开的URL JSON数组
            $record['audioUri'],
            $record['recordDateTimestamp']
        );
        $insert_stmt->execute();
        $synced_client_ids[] = $record['id'];
    }
    $conn->commit();
    json_response(['status' => 'success', 'message' => '同步成功', 'synced_client_ids' => $synced_client_ids]);
} catch (Exception $e) {
    $conn->rollback();
    json_response(['status' => 'error', 'message' => '数据库操作失败: ' . $e->getMessage()], 500);
}

$insert_stmt->close();
$conn->close();