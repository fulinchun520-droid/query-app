# SN查詢 App（Android）

包了公司內部 SN 過站查詢網頁的 WebView 殼 App，用途：
- 手機桌面圖示可直接開啟查詢頁面
- 自動放行相機權限（用來掃碼）
- 只信任 AppConfig.TRUSTED_HOST 這個內部伺服器的自簽憑證，其餘網站正常安全檢查

## 修改網址

網址寫在 `app/src/main/java/com/mfg/snquery/MainActivity.kt` 最上面的 `AppConfig` 物件：

```kotlin
object AppConfig {
    const val START_URL = "https://10.41.40.38:8091/sn_query"
    const val TRUSTED_HOST = "10.41.40.38"
}
```

換伺服器 IP 只要改這兩行，改完 push 上 GitHub 就會自動重新編譯。

## 用 GitHub Actions 雲端編譯（不需要在自己電腦裝任何軟體）

1. 到 https://github.com 註冊一個免費帳號（如果還沒有）
2. 新增一個 Repository（右上角 + → New repository），名稱隨意，例如 `sn-query-app`，設成 **Private**（不想被外人看到程式碼的話）
3. 把這個資料夾裡的所有檔案上傳上去：
   - 網頁版做法：進到剛建立的 repo 頁面，點 "uploading an existing file"，把這個資料夾裡所有檔案跟資料夾拖進去（要保留資料夾結構），點 Commit
4. 上傳完成後，點上方的 **Actions** 分頁
5. 應該會看到一個叫 **"Build APK"** 的 workflow 正在跑（或剛跑完），點進去
6. 跑完之後（通常 3-5 分鐘），畫面下方會有一個 **Artifacts** 區塊，裡面有一個 **`SN查詢-apk`**，點下去下載，解壓縮後就是 `app-debug.apk`
7. 把這個 `.apk` 傳到手機上（Email、LINE、USB 傳輸、或請 Claude 幫忙加到查詢系統的下載頁面都可以），手機上點開安裝（會問「允許安裝不明來源應用程式」，同意即可）

## 之後要改版本

修改完程式碼（例如改網址），一樣用「uploading an existing file」把改過的檔案重新上傳覆蓋，或用網頁版的檔案編輯功能直接改，每次改完存檔，GitHub Actions 都會自動重新編譯一次新的 apk，去 Actions 分頁抓最新的下載即可。
