package scouter.plugin.server.alert.teams;

import scouter.server.Configure;

public class MonitoringGroupConfigure {
    private Configure conf;

    public MonitoringGroupConfigure(Configure config) {
        this.conf = config;
    }

    public String getValue(String key, String objType) {
        return this.getValue(key, objType, null);
    }

    public String getGroupKey(String originalKey, String objType) {
        if (originalKey == null) {
            return originalKey;
        }

        return objType + "." + originalKey;
    }

    public String getValue(String key, String objType, String defaultValue) {
        String groupKey = getGroupKey(key, objType);
        String value = conf.getValue(groupKey);
        if (value != null && value.trim().length() > 0) {
            return value;
        }
        // default key value
        value = conf.getValue(key);
        return value != null ? value : defaultValue;
    }

    public Boolean getBoolean(String key, final String objType, Boolean defaultValue) {
        String groupKey = getGroupKey(key, objType);
        Boolean value = toBoolean(conf.getValue(groupKey));
        if (value != null) {
            return value;
        }
        // default key value
        value = toBoolean(conf.getValue(key));
        return value != null ? value : defaultValue;
    }

    public int getInt(String key, String objType, int defaultValue) {
        String groupKey = getGroupKey(key, objType);
        Integer value = toInteger(conf.getValue(groupKey));
        if (value != null) {
            return value;
        }
        // default key value
        value = toInteger(conf.getValue(key));
        return value != null ? value : defaultValue;
    }

    public long getLong(String key, String objType, long defaultValue) {
        String groupKey = getGroupKey(key, objType);
        Long value = toLong(conf.getValue(groupKey));
        if (value != null) {
            return value;
        }
        // default key value
        value = toLong(conf.getValue(key));
        return value != null ? value : defaultValue;
    }


    private Long toLong(String value) {
        try {
            if (value != null) {
                return Long.parseLong(value);
            }
        } catch (Exception e) {
            // ignore exception
        }
        return null;
    }

    private Integer toInteger(String value) {
        try {
            if (value != null) {
                return Integer.parseInt(value);
            }
        } catch (Exception e) {
            // ignore exception
        }
        return null;
    }

    private Boolean toBoolean(String value) {
        try {
            if (value != null) {
                return Boolean.parseBoolean(value);
            }
        } catch (Exception e) {
            // ignore exception
        }
        return null;
    }
}
