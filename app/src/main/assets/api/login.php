<?php
require 'db_connect.php';

$data = json_decode(file_get_contents('php://input'), true);

if (empty($data['username']) || empty($data['password'])) {
    json_response(['status' => 'error', 'message' => '用户名或密码不能为空'], 400);
}

$username = $data['username'];
$password = $data['password'];

$stmt = $conn->prepare("SELECT id, password FROM users WHERE username = ?");
$stmt->bind_param("s", $username);
$stmt->execute();
$stmt->store_result();
$stmt->bind_result($user_id, $hashed_password);

if ($stmt->num_rows > 0) {
    $stmt->fetch();
    if (password_verify($password, $hashed_password)) {
        $token = bin2hex(random_bytes(32));
        $token_expires_at = date('Y-m-d H:i:s', strtotime('+7 days'));

        $update_stmt = $conn->prepare("UPDATE users SET token = ?, token_expires_at = ? WHERE id = ?");
        $update_stmt->bind_param("ssi", $token, $token_expires_at, $user_id);
        $update_stmt->execute();

        json_response([
            'status' => 'success',
            'message' => '登录成功',
            'token' => $token,
            'userId' => $user_id,
            'username' => $username
        ]);
    } else {
        json_response(['status' => 'error', 'message' => '密码错误'], 401);
    }
} else {
    json_response(['status' => 'error', 'message' => '用户不存在'], 404);
}

$stmt->close();
$conn->close();