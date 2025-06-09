<?php
header("Content-Type: application/json; charset=UTF-8");

set_exception_handler(function($exception) {
    json_response([
        'status' => 'error',
        'message' => 'Uncaught Exception: ' . $exception->getMessage(),
        'file' => $exception->getFile(),
        'line' => $exception->getLine()
    ], 500);
});

set_error_handler(function($severity, $message, $file, $line) {
    if (!(error_reporting() & $severity)) {
        return;
    }
    json_response([
        'status' => 'error',
        'message' => 'PHP Error: ' . $message,
        'file' => $file,
        'line' => $line,
        'severity' => $severity
    ], 500);
}, E_ALL);


$servername = "localhost";
$username = "bird_watcher";
$password = "PiNZ4tKcR7MhHfdP";
$dbname = "bird_watcher";

$conn = new mysqli($servername, $username, $password, $dbname);

if ($conn->connect_error) {
    json_response(['status' => 'error', 'message' => '数据库连接失败: ' . $conn->connect_error], 500);
}

$conn->set_charset("utf8mb4");

if (!function_exists('json_response')) {
    function json_response($data, $status_code = 200) {
        http_response_code($status_code);
        echo json_encode($data, JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT);
        exit();
    }
}
?>