package com.puddingkc.vrchatstatustask.vrchatstatustask;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class VRChatStatusTask implements ProjectActivity {

    @Override
    public @Nullable Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        VRChatStatusService service = project.getService(VRChatStatusService.class);
        if (service != null) {
            service.start();
        }
        return Unit.INSTANCE;
    }

}
