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

// 获取记录
$records = [];
$result = $conn->query("SELECT * FROM records WHERE user_id = $user_id ORDER BY record_date_timestamp DESC");

if ($result->num_rows > 0) {
    while($row = $result->fetch_assoc()) {
        // 将驼峰命名法转换为客户端需要的格式
        $row['id'] = (int)$row['client_id']; // 使用客户端ID
        $row['birdName'] = $row['bird_name'];
        $row['scientificName'] = $row['scientific_name'];
        $row['detailedLocation'] = $row['detailed_location'];
        $row['photoUris'] = json_decode($row['photo_uris'], true) ?: [];
        $row['audioUri'] = $row['audio_uri'];
        $row['recordDateTimestamp'] = (int)$row['record_date_timestamp'];

        // 删除不需要的字段
        unset($row['user_id'], $row['client_id'], $row['bird_name'], $row['scientific_name'], $row['detailed_location'], $row['audio_uri'], $row['created_at'], $row['updated_at']);

        $records[] = $row;
    }
}

json_response(['status' => 'success', 'records' => $records]);

$conn->close();