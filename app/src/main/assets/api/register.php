<?php
require 'db_connect.php';

$data = json_decode(file_get_contents('php://input'), true);

if (empty($data['username']) || empty($data['password'])) {
    json_response(['status' => 'error', 'message' => '用户名或密码不能为空'], 400);
}

$username = $data['username'];
$password = password_hash($data['password'], PASSWORD_DEFAULT);

$stmt = $conn->prepare("SELECT id FROM users WHERE username = ?");
$stmt->bind_param("s", $username);
$stmt->execute();
$stmt->store_result();

if ($stmt->num_rows > 0) {
    json_response(['status' => 'error', 'message' => '用户名已存在'], 409);
} else {
    $stmt = $conn->prepare("INSERT INTO users (username, password) VALUES (?, ?)");
    $stmt->bind_param("ss", $username, $password);
    if ($stmt->execute()) {
        json_response(['status' => 'success', 'message' => '注册成功']);
    } else {
        json_response(['status' => 'error', 'message' => '注册失败，请稍后重试'], 500);
    }
}

$stmt->close();
$conn->close();