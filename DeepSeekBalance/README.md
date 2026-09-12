# DeepSeek 余额 1.0.0

独立的原生 Android 工具，非 DeepSeek 官方应用。支持 Android 8.0 及以上，无第三方运行时依赖。

## 安装与使用

1. 在安卓手机上安装 `DeepSeekBalance-1.0.0.apk`。如系统询问，允许当前下载应用安装此 APK。
2. 打开「DeepSeek 余额」，粘贴 DeepSeek 官方 API Key，点「保存并查询」。不要填 `Bearer` 前缀。
3. 点击「添加桌面小部件」，按桌面提示确认添加。若桌面不支持此按钮，长按桌面空白处 → 小部件 / 添加工具 → DeepSeek 余额。
4. 点击小部件右下方「刷新余额」手动更新；点击余额区域打开 App。

官方密钥管理：https://platform.deepseek.com/api_keys

## 刷新与余额含义

- App 在前台时立即查询，随后每 60 秒刷新一次；离开 App 就停止前台轮询。
- 有小部件且已保存密钥时，默认向系统请求每 15 分钟后台更新，可改为 30、60 分钟或仅手动。
- 安卓 Doze、省电、网络状况和手机厂商后台限制可能推迟任务，不能保证秒级或精确每 15 分钟刷新。总是显示最近成功更新的日期与时间。
- 若后台刷新长期停止，可在系统应用管理中允许后台运行 / 自启动，并放宽电池限制；设置名称因机型而异。强行停止应用后需重新打开。
- GET `https://api.deepseek.com/user/balance` 查询整个账号的可用余额，非单个 API Key 的独立预算，也不是固定 token 数量。
- 人民币、美元钱包分别显示，不跨币种相加。显示总可用余额、充值余额、未过期赠送余额。
- 查询失败保留上次成功余额并显示失败状态，首次失败显示破折号；不会把失败显示成零余额。

## 隐私

密钥在本机通过 Android Keystore + AES-GCM 加密保存。应用只向 DeepSeek 官方 HTTPS 余额接口发送密钥，不跟随重定向，不调用聊天接口，不含广告、分析 SDK 或自建服务器。禁止系统应用数据备份。移除密钥时会清除本机余额缓存。查询结果明文保存在应用私有目录，以供桌面展示；桌面上的余额对能看到屏幕的人可见。

## 构建

常规方式：使用 Android Studio 打开目录；JDK 17、Android SDK 35、Android Gradle Plugin 8.7.3，构建 `app` 模块。需下载相应构建依赖。Gradle wrapper 未随包附带，由 Android Studio 使用兼容的 Gradle 8.9。

离线方式：准备 SDK platform 35、build-tools 35.0.0、Java 17、javac 或 Eclipse ECJ 编译器，然后运行：

```bash
export ANDROID_SDK_ROOT=/absolute/path/to/android-sdk
# 仅当没有 javac 时设置：
export ECJ_JAR=/absolute/path/to/ecj.jar
python3 build_local.py
```

脚本生成独立签名密钥到 `.signing/`，APK 输出到项目父目录的 `deliverables/`。发布更新前，务必恢复原 `.signing/`；否则新 APK 无法覆盖已安装版本。签名材料单独存于 `DeepSeekBalance-Signing-Backup.zip`，不要公开分享该备份。源码不包含签名私钥或 API Key。

## 验证与限制

已执行资源编译、Java 编译、DEX 构建、APK 签名验证，以及余额解析测试（官方示例、双币种、小数精度、零余额、负余额和异常响应）。

当前环境没有 Android 模拟器或连接的手机，也没有用户 API Key，因此未做真机安装、桌面宿主交互和真实账户联网验证。首次安装后请核对查询出的余额及小部件时间。后台运行效果取决于手机系统。

## 实现说明

`BalanceData` 使用 BigDecimal 保留金额精度；`Store` 保存加密密钥与缓存；`Repository` 处理 HTTPS、超时、错误和切换账户时的旧响应丢弃；`BalanceWidget` 与 `RefreshReceiver` 提供桌面渲染和点击刷新；`BalanceJob` 使用持久化 JobScheduler 周期任务。移除最后一个小部件后取消后台查询。

参考：
- https://api-docs.deepseek.com/zh-cn/api/get-user-balance/
- https://developer.android.com/develop/ui/views/appwidgets/advanced
