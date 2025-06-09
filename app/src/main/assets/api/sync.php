<?php
require 'db_connect.php';

function sync_log($message) {
    $log_file = '/www/wwwroot/47.94.105.113/sync_debug.log';
    $formatted_message = date('[Y-m-d H:i:s] ') . print_r($message, true) . "\n";
    file_put_contents($log_file, $formatted_message, FILE_APPEND);
}

$headers = getallheaders();
$token = $headers['Authorization'] ?? null;
if (!$token) { json_response(['status' => 'error', 'message' => '未提供认证Token'], 401); }
$stmt = $conn->prepare("SELECT id, token_expires_at FROM users WHERE token = ?");
$stmt->bind_param("s", $token);
$stmt->execute();
$stmt->store_result();
$stmt->bind_result($user_id, $token_expires_at);
if ($stmt->num_rows == 0 || new DateTime() > new DateTime($token_expires_at)) { json_response(['status' => 'error', 'message' => 'Token无效或已过期'], 401); }
$stmt->fetch();
$stmt->close();

$data = json_decode(file_get_contents('php://input'), true);

if (json_last_error() !== JSON_ERROR_NONE) { json_response(['status' => 'error', 'message' => '无效的JSON请求体'], 400); }
$records = $data['records'] ?? [];
if (empty($records)) { json_response(['status' => 'success', 'message' => '没有需要同步的记录']); }

$upload_dir = '/www/wwwroot/47.94.105.113/uploads/';
if (!is_dir($upload_dir)) { if (!mkdir($upload_dir, 0775, true)) { json_response(['status' => 'error', 'message' => '无法创建上传目录: ' . $upload_dir], 500); } }
$base_image_url = 'http://47.94.105.113/uploads/';

$conn->begin_transaction();
$synced_client_ids = [];

try {
    foreach ($records as $record) {
        $clientId = $record['id'];

        $final_photo_urls = [];
        if (isset($record['photoUris']) && is_array($record['photoUris'])) {
            foreach($record['photoUris'] as $photo_data) {
                if (strpos($photo_data, 'data:image') === 0) {
                    @list($type, $data) = explode(';', $photo_data);
                    @list(, $data) = explode(',', $data);
                    if ($data === null) continue;
                    $decoded_data = base64_decode($data);
                    if ($decoded_data === false) continue;
                    $file_extension = strpos($type, 'jpeg') !== false ? '.jpg' : '.png';
                    $unique_filename = uniqid('img_' . $user_id . '_') . $file_extension;
                    $file_path = $upload_dir . $unique_filename;
                    if (file_put_contents($file_path, $decoded_data) === false) { continue; }
                    $final_photo_urls[] = $base_image_url . $unique_filename;
                } else { $final_photo_urls[] = $photo_data; }
            }
        }
        $photo_uris_json = json_encode($final_photo_urls);

        $title = $record['title'];
        $content = $record['content'] ?? null;
        $birdName = $record['birdName'];
        $scientificName = $record['scientificName'] ?? null;
        $latitude = $record['latitude'] ?? null;
        $longitude = $record['longitude'] ?? null;
        $detailedLocation = $record['detailedLocation'] ?? null;
        $audioUri = $record['audioUri'] ?? null;
        $recordDateTimestamp = $record['recordDateTimestamp'];

        $check_stmt = $conn->prepare("SELECT id FROM records WHERE user_id = ? AND client_id = ?");
        $check_stmt->bind_param("ii", $user_id, $clientId);
        $check_stmt->execute();
        $check_stmt->store_result();
        $record_exists = $check_stmt->num_rows > 0;
        $check_stmt->close();

        if ($record_exists) {
            $update_stmt = $conn->prepare(
                "UPDATE records SET title=?, content=?, bird_name=?, scientific_name=?, latitude=?, longitude=?, " .
                "detailed_location=?, photo_uris=?, audio_uri=?, record_date_timestamp=? " .
                "WHERE user_id = ? AND client_id = ?"
            );
            $update_stmt->bind_param("ssssddsssiis",
                $title, $content, $birdName, $scientificName, $latitude, $longitude,
                $detailedLocation, $photo_uris_json, $audioUri, $recordDateTimestamp,
                $user_id, $clientId
            );
            $update_stmt->execute();
            $update_stmt->close();
        } else {
            $insert_stmt = $conn->prepare(
                "INSERT INTO records (user_id, client_id, title, content, bird_name, scientific_name, latitude, longitude, " .
                "detailed_location, photo_uris, audio_uri, record_date_timestamp) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
            );
            $insert_stmt->bind_param("iissssddsssi",
                $user_id, $clientId, $title, $content, $birdName, $scientificName,
                $latitude, $longitude, $detailedLocation, $photo_uris_json, $audioUri, $recordDateTimestamp
            );
            $insert_stmt->execute();
            $insert_stmt->close();
        }

        $synced_client_ids[] = $clientId;
    }

    $conn->commit();
    json_response(['status' => 'success', 'message' => '同步成功', 'synced_client_ids' => $synced_client_ids]);

} catch (Exception $e) {
    $conn->rollback();
    json_response(['status' => 'error', 'message' => '数据库事务失败: ' . $e->getMessage()], 500);
}

$conn->close();
?>