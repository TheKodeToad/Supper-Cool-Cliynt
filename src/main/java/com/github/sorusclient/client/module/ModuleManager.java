package com.github.sorusclient.client.module;

import com.github.sorusclient.client.Sorus;
import com.github.sorusclient.client.module.impl.blockoverlay.BlockOverlay;
import com.github.sorusclient.client.module.impl.enhancements.Enhancements;
import com.github.sorusclient.client.module.impl.fullbright.FullBright;
import com.github.sorusclient.client.module.impl.itemphysics.ItemPhysics;
import com.github.sorusclient.client.module.impl.oldanimations.OldAnimations;
import com.github.sorusclient.client.module.impl.environmentchanger.EnvironmentChanger;
import com.github.sorusclient.client.module.impl.perspective.Perspective;
import com.github.sorusclient.client.module.impl.togglesprintsneak.ToggleSprintSneak;
import com.github.sorusclient.client.module.impl.zoom.Zoom;
import com.github.sorusclient.client.setting.SettingManager;

import java.util.*;

public class ModuleManager {

    private final Map<Class<Module>, ModuleData> modules = new HashMap<>();

    public void initialize() {
        this.registerInternalModules();
    }

    private void registerInternalModules() {
        this.register(new BlockOverlay(), "Block Overlay", "test");
        this.register(new Enhancements(), "Enhancements", "test");
        this.register(new EnvironmentChanger(), "Environment Changer", "test");
        this.register(new FullBright(), "Fullbright", "test");
        this.register(new ItemPhysics(), "Item Physics", "test");
        this.register(new OldAnimations(), "Old Animations", "test");
        this.register(new Perspective(), "Perspective", "test");
        this.register(new ToggleSprintSneak(), "Toggle Sprint & Sneak", "test");
        this.register(new Zoom(), "Zoom", "test");
    }

    @SuppressWarnings("unchecked")
    public void register(Module module, String name, String description) {
        this.modules.put((Class<Module>) module.getClass(), new ModuleData(module, name, description));
        Sorus.getInstance().get(SettingManager.class).register(module);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(Class<T> moduleClass) {
        return (T) this.modules.get(moduleClass).getModule();
    }

    public Module get(String id) {
        Optional<ModuleData> optionalModuleData = this.modules.values().stream().filter(moduleData -> moduleData.getModule().getId().equals(id)).findFirst();
        return optionalModuleData.map(ModuleData::getModule).orElse(null);
    }

    public List<ModuleData> getModules() {
        return new ArrayList<>(this.modules.values());
    }

}
