**语言：** [English](README.md) | [简体中文](README.zh.md)

# Sertum（花环播放器）

Sertum 是一款完全离线、本地优先的 Android 音乐播放器，面向自己持有音乐文件库、希望经由 USB DAC 获得未经篡改的逐位精确（bit-perfect）回放的听众。

## 当前状态

规划阶段（pre-alpha），核心开发尚未开始。迭代记录见 `CHANGELOG.md`。

## 规划的 V1 功能

- 100% 离线：无账号、无遥测、不申请网络权限
- USB 独占 bit-perfect 输出（实现路径将由真机技术验证决定）
- 常规 Android 输出与蓝牙作为普通播放链路
- 曲库扫描：系统媒体索引 + 用户自选文件夹 + 可选全盘扫描
- 曲库视图：歌曲 / 专辑 / 艺术家，支持"艺术家 → 专辑 → 曲目"浏览
- 无损无缝播放（Gapless）与逐曲断点续播
- 为无内嵌封面的专辑补充封面
- 中英双语界面；深浅主题，近纯黑 + 暖金设计令牌
- 健壮性：DAC 热插拔、损坏文件、权限被撤销等场景的容错处理

## 规划的技术栈

- Kotlin、Jetpack Compose、Media3 ExoPlayer、Room、Hilt、Coil
- Android 10+（minSdk 29）、targetSdk 36，优先 arm64-v8a
- 自有代码使用 Apache License 2.0

## 仓库结构

- `app/` — Android 应用模块（未来）
- `gradle/` — Gradle Wrapper 与构建工具（未来）
- `CHANGELOG.md` — 人类可读的迭代记录
- `README.md` — 本文档的英文版
- `LICENSE` — Apache License 2.0

## 许可

自有代码采用 Apache License 2.0 许可。第三方组件保留其各自的许可证；首个代码提交时将一并加入完整的第三方许可声明文件。
