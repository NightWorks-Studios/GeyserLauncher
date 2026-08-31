package dev.lisfox.geyserlauncher.data

enum class ConfigCategory(val route: String, val title: String, val subtitle: String) {
    BEDROCK("bedrock", "基岩版监听", "地址、端口与 Bedrock 网络参数"),
    JAVA("java", "Java 服务器", "远端服务器、认证与转发"),
    MOTD("motd", "服务器信息", "MOTD、人数与 Ping 转发"),
    GAMEPLAY("gameplay", "游戏体验", "面向基岩版玩家的行为选项"),
    GENERAL("general", "通用", "语言、登录、更新与诊断"),
    ADVANCED("advanced", "高级选项", "压缩、代理、缓存与资源包")
}

enum class ConfigFieldType { TEXT, NUMBER, BOOLEAN, CHOICE, LIST }

data class ConfigField(
    val id: String,
    val category: ConfigCategory,
    val path: List<String>,
    val title: String,
    val description: String,
    val type: ConfigFieldType,
    val defaultValue: Any,
    val choices: List<String> = emptyList()
)

val GEYSER_CONFIG_FIELDS = listOf(
    field("bedrock_address", ConfigCategory.BEDROCK, "bedrock.address", "监听地址", "通常保持 0.0.0.0", ConfigFieldType.TEXT, "0.0.0.0"),
    field("bedrock_port", ConfigCategory.BEDROCK, "bedrock.port", "监听端口", "基岩版客户端连接的 UDP 端口", ConfigFieldType.NUMBER, 19132),
    field("bedrock_broadcast_port", ConfigCategory.BEDROCK, "advanced.bedrock.broadcast-port", "广播端口", "0 表示使用上方的监听端口", ConfigFieldType.NUMBER, 0),
    field("bedrock_compression", ConfigCategory.BEDROCK, "advanced.bedrock.compression-level", "压缩级别", "范围 -1 到 9，数值越高越省流量", ConfigFieldType.NUMBER, 6),
    field("bedrock_mtu", ConfigCategory.BEDROCK, "advanced.bedrock.mtu", "网络 MTU", "不确定时保持默认值", ConfigFieldType.NUMBER, 1400),
    field("bedrock_validate_login", ConfigCategory.BEDROCK, "advanced.bedrock.validate-bedrock-login", "校验基岩版登录", "验证 Bedrock 登录数据", ConfigFieldType.BOOLEAN, true),
    field("bedrock_haproxy", ConfigCategory.BEDROCK, "advanced.bedrock.use-haproxy-protocol", "接收 HAProxy 协议", "仅在前置代理明确启用时打开", ConfigFieldType.BOOLEAN, false),
    field("bedrock_haproxy_ips", ConfigCategory.BEDROCK, "advanced.bedrock.haproxy-protocol-whitelisted-ips", "HAProxy 白名单", "每行一个允许发送代理头的 IP 或网段", ConfigFieldType.LIST, emptyList<String>()),
    field("bedrock_waterdog", ConfigCategory.BEDROCK, "advanced.bedrock.use-waterdogpe-forwarding", "WaterdogPE 转发", "接收 WaterdogPE 的玩家信息转发", ConfigFieldType.BOOLEAN, false),

    field("java_address", ConfigCategory.JAVA, "java.address", "服务器地址", "Java 版服务器的 IP 地址或域名", ConfigFieldType.TEXT, "127.0.0.1"),
    field("java_port", ConfigCategory.JAVA, "java.port", "服务器端口", "Java 版服务器的 TCP 端口", ConfigFieldType.NUMBER, 25565),
    field("java_auth", ConfigCategory.JAVA, "java.auth-type", "认证方式", "与 Java 服务器的登录模式保持一致", ConfigFieldType.CHOICE, "online", listOf("online", "offline", "floodgate")),
    field("java_forward_hostname", ConfigCategory.JAVA, "java.forward-hostname", "转发主机名", "把 Bedrock 使用的主机名转发给 Java 端", ConfigFieldType.BOOLEAN, false),
    field("java_haproxy", ConfigCategory.JAVA, "advanced.java.use-haproxy-protocol", "发送 HAProxy 协议", "向支持的 Java 后端发送代理头", ConfigFieldType.BOOLEAN, false),

    field("motd_primary", ConfigCategory.MOTD, "motd.primary-motd", "主 MOTD", "服务器列表第一行文本", ConfigFieldType.TEXT, "Geyser"),
    field("motd_secondary", ConfigCategory.MOTD, "motd.secondary-motd", "副 MOTD", "服务器列表第二行文本", ConfigFieldType.TEXT, "Another Geyser server."),
    field("motd_passthrough", ConfigCategory.MOTD, "motd.passthrough-motd", "转发 Java MOTD", "优先显示 Java 服务器返回的描述", ConfigFieldType.BOOLEAN, true),
    field("motd_max_players", ConfigCategory.MOTD, "motd.max-players", "最大玩家数", "未转发人数时展示的容量", ConfigFieldType.NUMBER, 100),
    field("motd_counts", ConfigCategory.MOTD, "motd.passthrough-player-counts", "转发玩家数量", "显示 Java 服务器的在线人数", ConfigFieldType.BOOLEAN, true),
    field("motd_ping_interval", ConfigCategory.MOTD, "motd.ping-passthrough-interval", "Ping 间隔", "刷新服务器信息的秒数", ConfigFieldType.NUMBER, 3),

    field("game_server_name", ConfigCategory.GAMEPLAY, "gameplay.server-name", "服务器名称", "暂停菜单中显示的名称", ConfigFieldType.TEXT, "Geyser"),
    field("game_cooldown", ConfigCategory.GAMEPLAY, "gameplay.cooldown-type", "攻击冷却显示", "基岩版界面的 Java 攻击冷却提示", ConfigFieldType.CHOICE, "crosshair", listOf("crosshair", "hotbar", "disabled")),
    field("game_suggestions", ConfigCategory.GAMEPLAY, "gameplay.command-suggestions", "命令建议", "向基岩版客户端发送命令补全", ConfigFieldType.BOOLEAN, true),
    field("game_coordinates", ConfigCategory.GAMEPLAY, "gameplay.show-coordinates", "显示坐标", "不依赖世界设置显示坐标", ConfigFieldType.BOOLEAN, true),
    field("game_scaffolding", ConfigCategory.GAMEPLAY, "gameplay.disable-bedrock-scaffolding", "禁用基岩版搭桥", "使用 Java 版脚手架放置规则", ConfigFieldType.BOOLEAN, false),
    field("game_nether", ConfigCategory.GAMEPLAY, "gameplay.nether-roof-workaround", "下界顶部兼容", "避免基岩版在下界顶部出现异常", ConfigFieldType.BOOLEAN, false),
    field("game_emotes", ConfigCategory.GAMEPLAY, "gameplay.emotes-enabled", "允许表情动作", "把基岩版表情广播给其他玩家", ConfigFieldType.BOOLEAN, true),
    field("game_legacy", ConfigCategory.GAMEPLAY, "gameplay.block-legacy-codes", "阻止旧版颜色代码", "避免玩家提交旧式格式代码", ConfigFieldType.BOOLEAN, true),
    field("game_space_block", ConfigCategory.GAMEPLAY, "gameplay.unusable-space-block", "不可用空间方块", "用于替代无法映射的 Java 方块", ConfigFieldType.TEXT, "minecraft:barrier"),
    field("game_custom_content", ConfigCategory.GAMEPLAY, "gameplay.enable-custom-content", "自定义内容", "允许转换自定义物品和方块", ConfigFieldType.BOOLEAN, true),
    field("game_force_packs", ConfigCategory.GAMEPLAY, "gameplay.force-resource-packs", "强制资源包", "要求玩家接受所有资源包", ConfigFieldType.BOOLEAN, true),
    field("game_integrated_pack", ConfigCategory.GAMEPLAY, "gameplay.enable-integrated-pack", "内置资源包", "启用 Geyser 自带兼容资源包", ConfigFieldType.BOOLEAN, true),
    field("game_forward_ping", ConfigCategory.GAMEPLAY, "gameplay.forward-player-ping", "转发玩家延迟", "把基岩版玩家延迟报告给 Java 端", ConfigFieldType.BOOLEAN, false),
    field("game_achievements", ConfigCategory.GAMEPLAY, "gameplay.xbox-achievements-enabled", "Xbox 成就", "允许基岩版客户端解锁成就", ConfigFieldType.BOOLEAN, false),
    field("game_skulls", ConfigCategory.GAMEPLAY, "gameplay.max-visible-custom-skulls", "自定义头颅上限", "同时渲染的自定义头颅数量", ConfigFieldType.NUMBER, 128),
    field("game_skull_distance", ConfigCategory.GAMEPLAY, "gameplay.custom-skull-render-distance", "头颅渲染距离", "自定义头颅的方块渲染距离", ConfigFieldType.NUMBER, 32),

    field("general_locale", ConfigCategory.GENERAL, "default-locale", "默认语言", "system 表示跟随 Android 系统", ConfigFieldType.TEXT, "system"),
    field("general_log_ip", ConfigCategory.GENERAL, "log-player-ip-addresses", "记录玩家 IP", "在控制台日志中包含玩家 IP 地址", ConfigFieldType.BOOLEAN, true),
    field("general_saved_logins", ConfigCategory.GENERAL, "saved-user-logins", "保存登录的玩家", "每行一个需要缓存微软登录令牌的基岩版用户名", ConfigFieldType.LIST, listOf("ThisExampleUsernameShouldBeLongEnoughToNeverBeAnXboxUsername", "ThisOtherExampleUsernameShouldAlsoBeLongEnough")),
    field("general_auth_timeout", ConfigCategory.GENERAL, "pending-authentication-timeout", "登录等待时间", "微软账户授权允许等待的秒数", ConfigFieldType.NUMBER, 120),
    field("general_update", ConfigCategory.GENERAL, "notify-on-new-bedrock-update", "版本更新提醒", "新版 Bedrock 需要更新 Geyser 时提醒", ConfigFieldType.BOOLEAN, true),
    field("general_metrics", ConfigCategory.GENERAL, "enable-metrics", "匿名统计", "向 bStats 发送匿名运行统计", ConfigFieldType.BOOLEAN, true),
    field("general_debug", ConfigCategory.GENERAL, "debug-mode", "调试模式", "输出更详细的诊断日志", ConfigFieldType.BOOLEAN, false),

    field("advanced_cache", ConfigCategory.ADVANCED, "advanced.cache-images", "图片缓存天数", "0 表示不缓存下载的图片", ConfigFieldType.NUMBER, 0),
    field("advanced_scoreboard", ConfigCategory.ADVANCED, "advanced.scoreboard-packet-threshold", "计分板包阈值", "超过阈值时降低计分板更新频率", ConfigFieldType.NUMBER, 20),
    field("advanced_team", ConfigCategory.ADVANCED, "advanced.add-team-suggestions", "队伍名称建议", "为命令补全添加计分板队伍", ConfigFieldType.BOOLEAN, true),
    field("advanced_packs", ConfigCategory.ADVANCED, "advanced.resource-pack-urls", "资源包地址", "每行一个发送给基岩版玩家的 URL", ConfigFieldType.LIST, emptyList<String>()),
    field("advanced_floodgate", ConfigCategory.ADVANCED, "advanced.floodgate-key-file", "Floodgate 密钥文件", "相对于 Geyser 目录的密钥路径", ConfigFieldType.TEXT, "key.pem")
)

private fun field(
    id: String,
    category: ConfigCategory,
    path: String,
    title: String,
    description: String,
    type: ConfigFieldType,
    defaultValue: Any,
    choices: List<String> = emptyList()
) = ConfigField(id, category, path.split('.'), title, description, type, defaultValue, choices)
