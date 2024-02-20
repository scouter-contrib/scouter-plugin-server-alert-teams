package scouter.plugin.server.alert.teams;

import com.google.gson.Gson;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import scouter.lang.AlertLevel;
import scouter.lang.TextTypes;
import scouter.lang.TimeTypeEnum;
import scouter.lang.counters.CounterConstants;
import scouter.lang.pack.*;
import scouter.lang.plugin.PluginConstants;
import scouter.lang.plugin.annotation.ServerPlugin;
import scouter.net.RequestCmd;
import scouter.server.Configure;
import scouter.server.CounterManager;
import scouter.server.Logger;
import scouter.server.core.AgentManager;
import scouter.server.db.TextRD;
import scouter.server.netio.AgentCall;
import scouter.util.DateUtil;
import scouter.util.HashUtil;
import scouter.util.IPUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class TeamsPlugin {

    final Configure conf = Configure.getInstance();

    private final MonitoringGroupConfigure groupConf;

    private static AtomicInteger ai = new AtomicInteger(0);
    private static List<Integer> javaeeObjHashList = new ArrayList<Integer>();
    private static AlertPack lastPack;
    private static long lastSentTimeStamp;

    public TeamsPlugin() {
        groupConf = new MonitoringGroupConfigure(conf);

        if (ai.incrementAndGet() == 1) {
            ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

            // thread count check
            executor.scheduleAtFixedRate(new Runnable() {
                                             @Override
                                             public void run() {
                                                 if (conf.getInt("ext_plugin_thread_count_threshold", 0) == 0) {
                                                     return;
                                                 }
                                                 for (int objHash : javaeeObjHashList) {
                                                     try {
                                                         if (AgentManager.isActive(objHash)) {
                                                             ObjectPack objectPack = AgentManager.getAgent(objHash);
                                                             MapPack mapPack = new MapPack();
                                                             mapPack.put("objHash", objHash);

                                                             mapPack = AgentCall.call(objectPack, RequestCmd.OBJECT_THREAD_LIST, mapPack);

                                                             int threadCountThreshold = groupConf.getInt("ext_plugin_thread_count_threshold", objectPack.objType, 0);
                                                             int threadCount = mapPack.getList("name").size();

                                                             if (threadCountThreshold != 0 && threadCount > threadCountThreshold) {
                                                                 AlertPack ap = new AlertPack();

                                                                 ap.level = AlertLevel.WARN;
                                                                 ap.objHash = objHash;
                                                                 ap.title = "Thread count exceed a threshold.";
                                                                 ap.message = objectPack.objName + "'s Thread count(" + threadCount + ") exceed a threshold.";
                                                                 ap.time = System.currentTimeMillis();
                                                                 ap.objType = objectPack.objType;

                                                                 alert(ap);
                                                             }
                                                         }
                                                     } catch (Exception e) {
                                                         // ignore
                                                     }
                                                 }
                                             }
                                         },
                    0, 5, TimeUnit.SECONDS);
        }
    }

    @ServerPlugin(PluginConstants.PLUGIN_SERVER_ALERT)
    public void alert(final AlertPack pack) {
        if (groupConf.getBoolean("ext_plugin_teams_send_alert", pack.objType, false)) {
            int level = groupConf.getInt("ext_plugin_teams_level", pack.objType, 0);
            // Get log level (0 : INFO, 1 : WARN, 2 : ERROR, 3 : FATAL)
            if (level <= pack.level) {
                new Thread() {
                    public void run() {
                        try {
                            String webhookURL = groupConf.getValue("ext_plugin_teams_webhook_url", pack.objType);
                            String channel = groupConf.getValue("ext_plugin_teams_channel", pack.objType);

                            assert webhookURL != null;

                            // Get the agent Name
                            String name = AgentManager.getAgentName(pack.objHash) == null ? "N/A" : AgentManager.getAgentName(pack.objHash);

                            if (name.equals("N/A") && pack.message.endsWith("connected.")) {
                                int idx = pack.message.indexOf("connected");
                                if (pack.message.indexOf("reconnected") > -1) {
                                    name = pack.message.substring(0, idx - 6);
                                } else {
                                    name = pack.message.substring(0, idx - 4);
                                }
                            }

                            String title = pack.title;
                            String msg = pack.message;
                            if (title.equals("INACTIVE_OBJECT")) {
                                title = "An object has been inactivated.";
                                msg = pack.message.substring(0, pack.message.indexOf("OBJECT") - 1);
                            } else if (title.equals("ACTIVATED_OBJECT")) {
                                title = "An object is activated now!!! ";
                                msg = pack.message.substring(0, pack.message.indexOf("OBJECT") - 1);
                            }

                            String finalMsg = makeMessage(name, pack.objType.toUpperCase(), title, msg);

                            if (groupConf.getBoolean("ext_plugin_ignore_duplicate_alert", pack.objType,false) && lastPack != null){
                                long diff = System.currentTimeMillis() - lastSentTimeStamp;
                                if (lastPack.objHash == pack.objHash && lastPack.title.equals(pack.title) && diff < DateUtil.MILLIS_PER_HOUR) {
                                    println("ignored continuous duplicate alert for an hour  : " + pack.title);
                                    return;
                                }
                            }

                            if (groupConf.getBoolean("ext_plugin_teams_debug", pack.objType, false)) {
                                println("WebHookURL : " + webhookURL);
                                println("param : " + finalMsg);
                            }

                            HttpPost post = new HttpPost(webhookURL);
                            post.addHeader("Content-Type", "application/json");
                            // charset set utf-8
                            post.setEntity(new StringEntity(finalMsg, "utf-8"));

                            CloseableHttpClient client = HttpClientBuilder.create().build();

                            // send the post request
                            HttpResponse response = client.execute(post);

                            // save the last pack info to ignore continuous duplicate alert
                            lastSentTimeStamp = System.currentTimeMillis();
                            lastPack = pack;

                            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                                println("Teams message sent to [Channel : " + channel + "] successfully.");
                            } else {
                                println("Teams message sent failed. Verify below information.");
                                println("[WebHookURL] : " + webhookURL);
                                println("[Message] : " + finalMsg);
                                println("[Reason] : " + EntityUtils.toString(response.getEntity(), "UTF-8"));
                            }
                        } catch (Exception e) {
                            println("[Error] : " + e.getMessage());
                            if (conf._trace) {
                                e.printStackTrace();
                            }
                        }
                    }

                }.start();
            }
        }
    }

    @ServerPlugin(PluginConstants.PLUGIN_SERVER_OBJECT)
    public void object(ObjectPack pack) {
        if (!conf.getBoolean("ext_plugin_teams_object_alert_enabled", false)) {
            return;
        }

        if (pack.version != null && pack.version.length() > 0) {
            AlertPack ap = null;
            ObjectPack op = AgentManager.getAgent(pack.objHash);

            if (op == null && pack.wakeup == 0L) {
                // in case of new agent connected
                ap = new AlertPack();
                ap.level = AlertLevel.INFO;
                ap.objHash = pack.objHash;
                ap.title = "An object has been activated.";
                ap.message = pack.objName + " is connected.";
                ap.time = System.currentTimeMillis();

                if (AgentManager.getAgent(pack.objHash) != null) {
                    ap.objType = AgentManager.getAgent(pack.objHash).objType;
                } else {
                    ap.objType = "scouter";
                }

                alert(ap);
            } else if (op.alive == false) {
                // in case of agent reconnected
                ap = new AlertPack();
                ap.level = AlertLevel.INFO;
                ap.objHash = pack.objHash;
                ap.title = "An object has been activated.";
                ap.message = pack.objName + " is reconnected.";
                ap.time = System.currentTimeMillis();
                ap.objType = AgentManager.getAgent(pack.objHash).objType;

                alert(ap);
            }
            // inactive state can be handled in alert() method.
        }
    }

    @ServerPlugin(PluginConstants.PLUGIN_SERVER_XLOG)
    public void xlog(XLogPack pack) {
        if (!conf.getBoolean("ext_plugin_teams_xlog_enabled", false)) {
            return;
        }

        String objType = AgentManager.getAgent(pack.objHash).objType;
        if (groupConf.getBoolean("ext_plugin_teams_xlog_enabled", objType, true)) {
            if (pack.error != 0) {
                String date = DateUtil.yyyymmdd(pack.endTime);
                String service = TextRD.getString(date, TextTypes.SERVICE, pack.service);

                AlertPack ap = new AlertPack();
                ap.level = AlertLevel.ERROR;
                ap.objHash = pack.objHash;
                ap.title = "xlog Error";
                ap.message = "URL  :  "+service + " \r\n\r\n Error_Message  :  " + TextRD.getString(date, TextTypes.ERROR, pack.error);
                ap.time = System.currentTimeMillis();
                ap.objType = objType;
                alert(ap);
            }

            try {
                int elapsedThreshold = groupConf.getInt("ext_plugin_elapsed_time_threshold", objType, 0);

                if (elapsedThreshold != 0 && pack.elapsed > elapsedThreshold) {
                    String serviceName = TextRD.getString(DateUtil.yyyymmdd(pack.endTime), TextTypes.SERVICE, pack.service);

                    AlertPack ap = new AlertPack();

                    ap.level = AlertLevel.WARN;
                    ap.objHash = pack.objHash;
                    ap.title = "Elapsed Time Exceed a threshold.";
                    ap.message = "[" + AgentManager.getAgentName(pack.objHash) + "] "
                            + "  [URL : "+ serviceName + "] "
                            + "  Elapsed Time(" + pack.elapsed + " ms) exceed a threshold.";
                    ap.time = System.currentTimeMillis();
                    ap.objType = objType;

                    alert(ap);
                }

            } catch (Exception e) {
                Logger.printStackTrace(e);
            }
        }
    }


    @ServerPlugin(PluginConstants.PLUGIN_SERVER_COUNTER)
    public void counter(PerfCounterPack pack) {
        String objName = pack.objName;
        int objHash = HashUtil.hash(objName);
        String objType = null;
        String objFamily = null;

        if (AgentManager.getAgent(objHash) != null) {
            objType = AgentManager.getAgent(objHash).objType;
        }

        if (objType != null) {
            objFamily = CounterManager.getInstance().getCounterEngine().getObjectType(objType).getFamily().getName();
        }

        try {
            // in case of objFamily is javaee
            if (CounterConstants.FAMILY_JAVAEE.equals(objFamily)) {
                // save javaee type's objHash
                if (!javaeeObjHashList.contains(objHash)) {
                    javaeeObjHashList.add(objHash);
                }

                if (pack.timetype == TimeTypeEnum.REALTIME) {
                    long gcTimeThreshold = groupConf.getLong("ext_plugin_gc_time_threshold", objType, 0);
                    long gcTime = pack.data.getLong(CounterConstants.JAVA_GC_TIME);

                    if (gcTimeThreshold != 0 && gcTime > gcTimeThreshold) {
                        AlertPack ap = new AlertPack();

                        ap.level = AlertLevel.WARN;
                        ap.objHash = objHash;
                        ap.title = "GC time exceed a threshold.";
                        ap.message = objName + "'s GC time(" + gcTime + " ms) exceed a threshold.";
                        ap.time = System.currentTimeMillis();
                        ap.objType = objType;

                        alert(ap);
                    }
                }
            }
        } catch (Exception e) {
            Logger.printStackTrace(e);
        }
    }

    private void println(Object o) {
        if (conf.getBoolean("ext_plugin_teams_debug", false)) {
            System.out.println(o);
            Logger.println(o);
        }
    }

    private String makeMessage(String serverName, String type, String title, String msg) {
        StringBuilder template = new StringBuilder();
        template.append("{");
        template.append("\"@type\": \"MessageCard\",");
        template.append("\"@context\": \"https://schema.org/extensions\",");
        template.append("\"title\": \"Scouter Alert\",");
        template.append("\"text\": \"")
                .append("[SERVER] : " + serverName).append("\n")
                .append("\n[TYPE] : " + type).append("\n")
                .append("\n[TITLE] : " + title).append("\n")
                .append("\n[MESSAGE] : \n").append("\n")
                .append("\n"+msg+"\n")
                .append("\",");
        template.append("}");
        return template.toString();
    }
}
