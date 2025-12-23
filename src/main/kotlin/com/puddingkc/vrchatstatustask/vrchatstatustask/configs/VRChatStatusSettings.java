package com.puddingkc.vrchatstatustask.vrchatstatustask.configs;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;

@Service(Service.Level.APP)
@State(name = "VRChatStatusConfig", storages = @Storage("VRChatStatusConfig.xml"))
public final class VRChatStatusSettings implements PersistentStateComponent<VRChatStatusSettings> {

    public boolean enabledOsc = true;
    public boolean textCropping = true;

    public String oscAddress = "127.0.0.1";
    public int oscPort = 9000;
    public int updateInterval = 5;

    public String messageTemplate = """
                                    [ IntelliJ Coding ]
                                    
                                    💻 工程 : {project}
                                    📝 编辑 : {file}
                                    
                                    {errors} Error | {warnings} Warn
                                    [ UPTime {uptime} ]
                                    """;

    public static VRChatStatusSettings getInstance() {
        return com.intellij.openapi.application.ApplicationManager.getApplication().getService(VRChatStatusSettings.class);
    }

    @Override
    public @NotNull VRChatStatusSettings getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull VRChatStatusSettings state) {
        XmlSerializerUtil.copyBean(state, this);
    }
}
