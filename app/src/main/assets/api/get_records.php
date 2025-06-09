<?php
require 'db_connect.php';

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

$query = "SELECT client_id, title, content, bird_name, scientific_name, latitude, longitude, detailed_location, photo_uris, audio_uri, record_date_timestamp FROM records WHERE user_id = ? ORDER BY record_date_timestamp DESC";
$stmt = $conn->prepare($query);
$stmt->bind_param("i", $user_id);
$stmt->execute();
$result = $stmt->get_result();

$records = [];
if ($result->num_rows > 0) {
    while($row = $result->fetch_assoc()) {
        $client_record = [
            'id' => (int)$row['client_id'],
            'title' => $row['title'],
            'content' => $row['content'],
            'birdName' => $row['bird_name'],
            'scientificName' => $row['scientific_name'],
            'latitude' => (float)$row['latitude'],
            'longitude' => (float)$row['longitude'],
            'detailedLocation' => $row['detailed_location'],
            'photoUris' => json_decode($row['photo_uris'], true) ?: [],
            'audioUri' => $row['audio_uri'],
            'recordDateTimestamp' => (int)$row['record_date_timestamp']
        ];
        $records[] = $client_record;
    }
}
$stmt->close();

json_response(['status' => 'success', 'records' => $records]);

$conn->close();
?>