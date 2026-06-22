# 我们的秘密基地 Android 首页

这是一个基于 `Kotlin + Jetpack Compose` 的原生 Android 首页原型，按你给的设计稿、HTML 参考和需求文档搭好了首屏结构。

## 现在已经包含

- 顶部问候语、情侣名称、消息/设置入口
- 主视觉插画区
- 恋爱天数与周年倒计时卡片
- 双人今日心情卡片与本地保存
- 快捷记录输入区
- 六宫格核心功能区
- 最近动态区
- 固定底部导航

## 配置入口

背景图、头像和图标都走配置，不直接写死在页面代码里：

- 视觉资源配置：
  [app/src/main/assets/homepage_visual_config.json](/C:/Users/Administrator/Documents/yyhlj/app/src/main/assets/homepage_visual_config.json)
- 首页内容配置：
  [app/src/main/assets/homepage_content.json](/C:/Users/Administrator/Documents/yyhlj/app/src/main/assets/homepage_content.json)

你后面想换图时，通常只需要：

1. 把新图片放进 `app/src/main/res/drawable-nodpi/`
2. 修改 `homepage_visual_config.json` 里的资源名

## 资源来源

当前接入了你提供的参考图片，并拆成了首页使用的本地资源：

- 主插画：`home_hero_illustration.png`
- 角色头像：`avatar_sheep.png`、`avatar_chick.png`
- 功能图标：`ic_*`

## 说明

当前工作区里没有可直接调用的 `java` / `gradle` / Android SDK，因此这次我把工程和页面代码完整搭好了，但没法在这里实际编译运行。建议你下一步用 Android Studio 打开这个目录做一次同步和预览。
