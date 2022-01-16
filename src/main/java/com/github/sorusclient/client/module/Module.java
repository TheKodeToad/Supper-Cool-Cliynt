package com.github.sorusclient.client.module;

import com.github.sorusclient.client.setting.Setting;
import com.github.sorusclient.client.setting.ConfigurableData;
import com.github.sorusclient.client.setting.SettingContainer;
import com.github.sorusclient.client.setting.Util;

import java.util.*;

public class Module implements SettingContainer {

    private final String id;
    private final Map<String, Setting<?>> settings = new HashMap<>();

    protected final Setting<Boolean> isShared;

    public Module(String id) {
        this.id = id;

        this.isShared = new Setting<>(false);
    }

    protected void register(String id, Setting<?> setting) {
        this.settings.put(id, setting);
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public void load(Map<String, Object> settings) {
        for (Map.Entry<String, Object> setting : settings.entrySet()) {
            Setting<?> setting1 = this.settings.get(setting.getKey());
            if (setting1 != null) {
                setting1.setValueRaw(Util.toJava(setting1.getType(), setting.getValue()));
            }
        }
    }

    @Override
    public void loadForced(Map<String, Object> settings) {
        for (Map.Entry<String, Object> setting : settings.entrySet()) {
            Setting<?> setting1 = this.settings.get(setting.getKey());
            List<Object> forcedValues = new ArrayList<>();
            if (setting.getValue() instanceof List) {
                for (Object element : ((List<Object>) setting.getValue())) {
                    forcedValues.add(Util.toJava(setting1.getType(), element));
                }
            } else {
                forcedValues.add(Util.toJava(setting1.getType(), setting.getValue()));
            }
            setting1.setForcedValueRaw(forcedValues);
        }
    }

    @Override
    public void removeForced() {
        for (Setting<?> setting : this.settings.values()) {
            setting.setForcedValueRaw(null);
        }
    }

    @Override
    public Map<String, Object> save() {
        Map<String, Object> settingsMap = new HashMap<>();
        for (Map.Entry<String, Setting<?>> setting : this.settings.entrySet()) {
            settingsMap.put(setting.getKey(), Util.toData(setting.getValue().getRealValue()));
        }
        return settingsMap;
    }

    public void addSettings(List<ConfigurableData> settings) {
        settings.add(new ConfigurableData.Toggle("Shared", this.isShared));
    }

    @Override
    public boolean isShared() {
        return this.isShared.getValue();
    }

    @Override
    public void setShared(boolean isShared) {
        this.isShared.setValue(isShared);
    }

}
