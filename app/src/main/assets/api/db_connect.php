<?php
header("Content-Type: application/json; charset=UTF-8");

// --- 新增开始: 全局错误和异常处理器 ---
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
        // This error code is not included in error_reporting
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
// --- 新增结束 ---


$servername = "localhost";
$username = "bird_watcher";
$password = "PiNZ4tKcR7MhHfdP";
$dbname = "bird_watcher";

// 创建连接
$conn = new mysqli($servername, $username, $password, $dbname);

// 检查连接
if ($conn->connect_error) {
    // 直接调用 json_response 函数，它会 exit
    json_response(['status' => 'error', 'message' => '数据库连接失败: ' . $conn->connect_error], 500);
}

$conn->set_charset("utf8mb4");

// 确保 json_response 函数只定义一次
if (!function_exists('json_response')) {
    function json_response($data, $status_code = 200) {
        http_response_code($status_code);
        echo json_encode($data, JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT);
        exit();
    }
}
?>