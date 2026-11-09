# Gobang-pt

## 打包 APK

在项目根目录执行以下命令。

### Debug APK（可直接安装）

```bash
./build-apk.sh
```

也可以显式指定构建类型：

```bash
./build-apk.sh debug
```

产物路径：`dist/gobang-pt-debug.apk`。

### Release APK

```bash
./build-apk.sh release
```

产物路径：`dist/gobang-pt-release.apk`。

当前项目未配置正式签名，因此 Release APK 是未签名包，需要签名后才能安装到设备。
