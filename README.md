# scouter-plugin-server-alert-teams
### Scouter server plugin to send a alert via teams

- this project inspired by telegram plugin project of noces96. it is very similar.

- this project is  scouter server plugin project. this project goal is that send message to teams.
-  this project only support a sort of Alert.
    - CPU of Agent  (warning / fatal)
    - Memory of Agent (warning / fatal)
    - Disk of Agent (warning / fatal)
    - connected new Agent
    - disconnected Agent
    - reconnect Agent

### Properties (you can modify in conf/scouter.conf of scouter server home )
* **_ext\_plugin\_teams\_send\_alert_** : can send teams message or can'not  (true / false) - default : false
* **_ext\_plugin\_teams\_debug_** : can log message or can't  - default false
* **_ext\_plugin\_teams\_level_** : log level (0 : INFO, 1 : WARN, 2 : ERROR, 3 : FATAL) - default 0
* **_ext\_plugin\_teams\_webhook_url_** : Teams WebHook URL
* **_ext\_plugin\_teams\_channel_** : #Channel or @user_id
* **_ext\_plugin\_elapsed\_time\_threshold_** : Response time threshold (ms) - Default 0 , 0 means that the response time threshold is not checked.
* **_ext\_plugin\_gc\_time\_threshold_** : Threshold for GC Time (ms) - Default 0, 0 means that GC Time is not checked for exceeding the threshold.
* **_ext\_plugin\_thread\_count\_threshold_** : Threshold for Thread Count - Default 0, 0 means that Thread Count threshold is not checked.
* **_ext\_plugin\_teams\_xlog\_enabled_** : xlog message send (true / false) - default : false
* **_ext_plugin_teams_object_alert_enabled_** : object active/dead alert (true / false) - default : false
* **_ext_plugin_ignore_duplicate_alert_** : ignore duplicate notifications or not (true / false) - default : false


* Example
```
# External Interface (Teams)
ext_plugin_teams_send_alert=true
ext_plugin_teams_debug=true
ext_plugin_teams_level=1
ext_plugin_teams_webhook_url=https://abcd.webhook.office.com/webhookb2/234-9faaa-96sese11344c4e3/IncomingWebhook/asdfasdfasdfasdfasdfXXXXXXXXXXX
ext_plugin_teams_channel=#scouter
ext_plugin_teams_xlog_enabled=true
ext_plugin_teams_object_alert_enabled=true
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
    - Build Artifacts as a jar file
  
    
* Deploy
    - After build, an out directory is created under the project → Copy the scouter-plugin-server-alert-teams.jar created under the out folder and dependency library  →  save it to the lib/ folder under the scouter server installation path.
