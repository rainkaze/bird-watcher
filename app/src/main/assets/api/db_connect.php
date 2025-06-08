<?php
header("Content-Type: application/json; charset=UTF-8");

$servername = "localhost";
$username = "bird_watcher";
$password = "PiNZ4tKcR7MhHfdP";
$dbname = "bird_watcher";

// 创建连接
$conn = new mysqli($servername, $username, $password, $dbname);

// 检查连接
if ($conn->connect_error) {
    echo json_encode(['status' => 'error', 'message' => '数据库连接失败: ' . $conn->connect_error]);
    exit();
}

$conn->set_charset("utf8mb4");

function_exists('json_response') or define('JSON_RESPONSE', true);

function json_response($data, $status_code = 200) {
    http_response_code($status_code);
    echo json_encode($data);
    exit();
}