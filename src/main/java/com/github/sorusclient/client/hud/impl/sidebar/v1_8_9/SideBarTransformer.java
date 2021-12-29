package com.github.sorusclient.client.hud.impl.sidebar.v1_8_9;

import com.github.glassmc.loader.GlassLoader;
import com.github.glassmc.loader.Listener;
import com.github.glassmc.loader.loader.ITransformer;
import com.github.glassmc.loader.util.Identifier;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;

public class SideBarTransformer implements Listener, ITransformer {

    private final String IN_GAME_HUD = Identifier.parse("v1_8_9/net/minecraft/client/gui/hud/InGameHud").getClassName();

    @Override
    public void run() {
        GlassLoader.getInstance().registerTransformer(SideBarTransformer.class);
    }

    @Override
    public boolean canTransform(String name) {
        return name.equals(IN_GAME_HUD);
    }

    @Override
    public byte[] transform(String name, byte[] data) {
        ClassNode classNode = new ClassNode();
        ClassReader classReader = new ClassReader(data);
        classReader.accept(classNode, 0);

        this.transformInGameHud(classNode);

        ClassWriter classWriter = new ClassWriter(0);
        classNode.accept(classWriter);
        return classWriter.toByteArray();
    }

    private void transformInGameHud(ClassNode classNode) {
        Identifier renderScoreboardObjective = Identifier.parse("v1_8_9/net/minecraft/client/gui/hud/InGameHud#renderScoreboardObjective(Lv1_8_9/net/minecraft/scoreboard/ScoreboardObjective;Lv1_8_9/net/minecraft/client/util/Window;)V");

        for (MethodNode methodNode : classNode.methods) {
            if (methodNode.name.equals(renderScoreboardObjective.getMethodName()) && methodNode.desc.equals(renderScoreboardObjective.getMethodDesc())) {
                methodNode.instructions.insert(new InsnNode(Opcodes.RETURN));
            }
        }
    }

}
