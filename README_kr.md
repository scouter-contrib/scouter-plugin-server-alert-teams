# scouter-plugin-server-alert-teams
### Scouter server plugin to send a alert via teams

- noces96님의 telegram plugin project를 토대로 만들었습니다. 매우 흡사합니다 ^^

- 본 프로젝트는 스카우터 서버 플러그인으로써 서버에서 발생한 Alert 메시지를 teams 로 전송하는 역할을 한다.
- 현재 지원되는 Alert의 종류는 다음과 같다.
    - Agent의 CPU (warning / fatal)
    - Agent의 Memory (warning / fatal)
    - Agent의 Disk (warning / fatal)
    - 신규 Agent 연결
    - Agent의 연결 해제
    - Agent의 재접속

### Properties (스카우터 서버 설치 경로 하위의 conf/scouter.conf)
* **_ext\_plugin\_teams\_send\_alert_** : Teams 메시지 발송 여부 (true / false) - 기본 값은 false
* **_ext\_plugin\_teams\_debug_** : 로깅 여부 - 기본 값은 false
* **_ext\_plugin\_teams\_level_** : 수신 레벨(0 : INFO, 1 : WARN, 2 : ERROR, 3 : FATAL) - 기본 값은 0
* **_ext\_plugin\_teams\_webhook_url_** : Teams WebHook URL
* **_ext\_plugin\_teams\_channel_** : 채널명(#Channel)
* **_ext\_plugin\_elapsed\_time\_threshold_** : 응답시간의 임계치 (ms) - 기본 값은 0으로, 0일때 응답시간의 임계치 초과 여부를 확인하지 않는다.
* **_ext\_plugin\_gc\_time\_threshold_** : GC Time의 임계치 (ms) - 기본 값은 0으로, 0일때 GC Time의 임계치 초과 여부를 확인하지 않는다.
* **_ext\_plugin\_thread\_count\_threshold_** : Thread Count의 임계치 - 기본 값은 0으로, 0일때 Thread Count의 임계치 초과 여부를 확인하지 않는다.
* **_ext\_plugin\_teams\_xlog\_enabled_** : xlog maasege send (true / false) - default : false
* **_ext_plugin_teams_object_alert_enabled_** : object active/dead alert (true / false) - default : false
* **_ext_plugin_ignore_duplicate_alert_** : 연속된 중복 알림을 무시할 지 여부 (true / false) - default : false


* Example
```
# External Interface (Teams)
ext_plugin_teams_send_alert=true
ext_plugin_teams_debug=true
ext_plugin_teams_level=1
ext_plugin_teams_webhook_url=https://abcd.webhook.office.com/webhookb2/234-9faaa-96sese11344c4e3/IncomingWebhook/asdfasdfasdfasdfasdfXXXXXXXXXXX
ext_plugin_teams_channel=#scouter
ext_plugin_teams_xlog_enabled=true
ext_plugin_teams_object_alert_enabled=false
ext_plugin_ignore_duplicate_alert=true

ext_plugin_elapsed_time_threshold=5000
ext_plugin_gc_time_threshold=5000
ext_plugin_thread_count_threshold=300
```

### Dependencies
* Project
    - scouter.common
    - scouter.server
* Library
    - commons-codec-1.9.jar
    - commons-logging-1.2.jar
    - gson-2.6.2.jar
    - httpclient-4.5.2.jar
    - httpcore-4.4.4.jar

### Build & Deploy
* Build
    - 프로젝트를 jar 파일로 Build Artifacts


* Deploy
  - 빌드 후 프로젝트 하위에 out 디렉토리가 생성됨 → out 폴더 하위에 생성된 scouter-plugin-server-alert-teams.jar 과 디펜던시 라이브러리를 복사하여 스카우터 서버 설치 경로 하위의 lib/ 폴더에 저장
