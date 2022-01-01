package com.github.sorusclient.client.hud;

import com.github.sorusclient.client.Sorus;
import com.github.sorusclient.client.adapter.Button;
import com.github.sorusclient.client.adapter.Key;
import com.github.sorusclient.client.adapter.MinecraftAdapter;
import com.github.sorusclient.client.event.EventManager;
import com.github.sorusclient.client.event.impl.KeyEvent;
import com.github.sorusclient.client.event.impl.MouseEvent;
import com.github.sorusclient.client.event.impl.RenderInGameEvent;
import com.github.sorusclient.client.hud.impl.armor.Armor;
import com.github.sorusclient.client.hud.impl.bossbar.BossBar;
import com.github.sorusclient.client.hud.impl.coordinates.Coordinates;
import com.github.sorusclient.client.hud.impl.experience.Experience;
import com.github.sorusclient.client.hud.impl.health.Health;
import com.github.sorusclient.client.hud.impl.hotbar.HotBar;
import com.github.sorusclient.client.hud.impl.hunger.Hunger;
import com.github.sorusclient.client.hud.impl.potionstatus.PotionStatus;
import com.github.sorusclient.client.hud.impl.sidebar.Sidebar;
import com.github.sorusclient.client.setting.Setting;
import com.github.sorusclient.client.setting.SettingContainer;
import com.github.sorusclient.client.setting.SettingManager;
import com.github.sorusclient.client.ui.Renderer;
import com.github.sorusclient.client.ui.UserInterface;
import com.github.sorusclient.client.util.Axis;
import com.github.sorusclient.client.util.Color;
import com.github.sorusclient.client.util.MathUtil;
import com.github.sorusclient.client.util.Pair;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HUDManager implements SettingContainer {

    private final List<HUDData> possibleHuds = new ArrayList<>();

    private final Map<String, HUDElement> elements = new HashMap<>();

    private long prevClickTime = 0;
    private HUDElement draggedHud;
    private InteractType interactType;
    private double initialMouseX;
    private double initialMouseY;
    private double initialHudX;
    private double initialHudY;
    private double initialScale;
    @SuppressWarnings("rawtypes")
    private Pair[] snapped = new Pair[0];

    private double[] prevScreenDimensions = new double[] {0, 0};

    private final Setting<Boolean> isShared;

    public HUDElement hudToOpenSettings = null;

    public HUDManager() {
        this.isShared = new Setting<>(false);

        this.initializePossibleElements();
        this.setupDefaultHud();
    }

    private void setupDefaultHud() {
        HotBar hotBar = new HotBar();
        hotBar.addAttached(null, new AttachType(0, 0, Axis.X));
        hotBar.addAttached(null, new AttachType(1, 1, Axis.Y));
        this.add(hotBar);

        Experience experience = new Experience();
        experience.addAttached(hotBar, new AttachType(0, 0, Axis.X));
        experience.addAttached(hotBar, new AttachType(1, -1, Axis.Y));
        this.add(experience);

        hotBar.addAttached(experience, new AttachType(0, 0, Axis.X));
        hotBar.addAttached(experience, new AttachType(-1, 1, Axis.Y));

        Health health = new Health();
        health.addAttached(experience, new AttachType(-1, -1, Axis.X));
        health.addAttached(experience, new AttachType(1, -1, Axis.Y));
        this.add(health);

        experience.addAttached(health, new AttachType(-1, -1, Axis.X));
        experience.addAttached(health, new AttachType(-1, 1, Axis.Y));

        Armor armor = new Armor();
        armor.addAttached(health, new AttachType(-1, -1, Axis.X));
        armor.addAttached(health, new AttachType(1, -1, Axis.Y));
        this.add(armor);

        health.addAttached(armor, new AttachType(-1, -1, Axis.X));
        health.addAttached(armor, new AttachType(-1, 1, Axis.Y));

        Hunger hunger = new Hunger();
        hunger.addAttached(experience, new AttachType(1, 1, Axis.X));
        hunger.addAttached(experience, new AttachType(1, -1, Axis.Y));
        this.add(hunger);

        experience.addAttached(hunger, new AttachType(1, 1, Axis.X));
        experience.addAttached(hunger, new AttachType(-1, 1, Axis.Y));

        BossBar bossBar = new BossBar();
        bossBar.addAttached(null, new AttachType(0, 0, Axis.X));
        bossBar.addAttached(null, new AttachType(-1, -1, Axis.Y));
        this.add(bossBar);

        Sidebar sideBar = new Sidebar();
        sideBar.addAttached(null, new AttachType(1, 1, Axis.X));
        sideBar.addAttached(null, new AttachType(0, 0, Axis.Y));
        this.add(sideBar);
    }

    public void initialize() {
        EventManager eventManager = Sorus.getInstance().get(EventManager.class);
        eventManager.register(RenderInGameEvent.class, this::render);
        eventManager.register(MouseEvent.class, this::onClick);
        eventManager.register(KeyEvent.class, this::onKey);

        Sorus.getInstance().get(SettingManager.class).register(this);
    }

    private void initializePossibleElements() {
        this.registerPossibleElement(Armor.class, "Armor", "yes yersy");
        this.registerPossibleElement(BossBar.class, "BossBar", "yes yersy");
        this.registerPossibleElement(Coordinates.class, "Coordinates", "yes yersy");
        this.registerPossibleElement(Experience.class, "Experience", "yes yersy");
        this.registerPossibleElement(Health.class, "Health", "yes yersy");
        this.registerPossibleElement(HotBar.class, "HotBar", "yes yersy");
        this.registerPossibleElement(Hunger.class, "Hunger", "yes yersy");
        this.registerPossibleElement(PotionStatus.class, "PotionStatus", "yes yersy");
        this.registerPossibleElement(Sidebar.class, "Sidebar", "yes yersy");
    }

    private void registerPossibleElement(Class<? extends HUDElement> hudClass, String name, String description) {
        this.possibleHuds.add(new HUDData(hudClass, name, description));
    }

    public void add(HUDElement hud) {
        String id = hud.getId() + "-" + (System.nanoTime() % 1000);
        this.elements.put(id, hud);
        hud.setInternalId(id);
    }

    public void remove(HUDElement hud) {
        this.elements.remove(hud.getInternalId());
    }

    public void render(RenderInGameEvent ignored) {
        MinecraftAdapter minecraftAdapter = Sorus.getInstance().get(MinecraftAdapter.class);
        Renderer renderer = Sorus.getInstance().get(Renderer.class);

        double[] screenDimensions = minecraftAdapter.getScreenDimensions();
        double[] mouseLocation = minecraftAdapter.getMouseLocation();

        if (screenDimensions[0] != this.prevScreenDimensions[0] || screenDimensions[1] != this.prevScreenDimensions[1]) {
            for (HUDElement element : this.elements.values()) {
                for (String attachedElementId : element.getAttached()) {
                    HUDElement attachedElement = this.getById(attachedElementId);
                    element.updatePosition(attachedElement, screenDimensions);
                }
            }
        }

        this.prevScreenDimensions = screenDimensions;

        boolean blendEnabled = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean textureEnabled = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);

        for (HUDElement element : this.elements.values()) {
            element.render();
        }

        if (Sorus.getInstance().get(UserInterface.class).isHudEditScreenOpen()) {
            for (HUDElement element : this.elements.values()) {
                double x = element.getX(screenDimensions[0]);
                double y = element.getY(screenDimensions[1]);
                double width = element.getScaledWidth();
                double height = element.getScaledHeight();

                renderer.drawRectangle(
                        x - width / 2,
                        y - height / 2,
                        width,
                        height,
                        Color.fromRGB(200, 200, 200, 50)
                );

                renderer.drawRectangle(x - width / 2 + 1, y - height / 2 + 1, 3, 3, Color.WHITE);
            }
        }

        if (draggedHud != null) {
            switch (this.interactType) {
                case MOVE:
                    List<Snap> xSnaps = new ArrayList<>();
                    List<Snap> ySnaps = new ArrayList<>();

                    double wantedX = MathUtil.clamp(initialHudX + (mouseLocation[0] - initialMouseX), draggedHud.getScaledWidth() / 2, screenDimensions[0] - draggedHud.getScaledWidth() / 2);
                    double wantedY = MathUtil.clamp(initialHudY + (mouseLocation[1] - initialMouseY), draggedHud.getScaledHeight() / 2, screenDimensions[1] - draggedHud.getScaledHeight() / 2);

                    Pair<HUDElement, AttachType> snappedX = null;
                    Pair<HUDElement, AttachType> snappedY = null;

                    Map<HUDElement, Pair<Pair<Double, Double>, Pair<Double, Double>>> possibleSnaps = new HashMap<>();
                    for (HUDElement hud : this.elements.values()) {
                        possibleSnaps.put(hud, new Pair<>(new Pair<>(hud.getX(screenDimensions[0]), hud.getScaledWidth()), new Pair<>(hud.getY(screenDimensions[1]), hud.getScaledHeight())));
                    }

                    possibleSnaps.put(null, new Pair<>(new Pair<>(screenDimensions[0] / 2, screenDimensions[0]), new Pair<>(screenDimensions[1] / 2, screenDimensions[1])));

                    for (Map.Entry<HUDElement, Pair<Pair<Double, Double>, Pair<Double, Double>>> elementSnap : possibleSnaps.entrySet()) {
                        if (elementSnap.getKey() == this.draggedHud) continue;

                        double otherX = elementSnap.getValue().getFirst().getFirst();
                        double otherWidth = elementSnap.getValue().getFirst().getSecond();
                        double otherY = elementSnap.getValue().getSecond().getFirst();
                        double otherHeight = elementSnap.getValue().getSecond().getSecond();

                        int[] possibleSides = new int[] {-1, 0, 1};
                        for (int selfSide : possibleSides) {
                            for (int otherSide : possibleSides) {
                                double snapLocationX = otherX + otherWidth / 2 * otherSide;
                                double snapOffsetX = Math.abs((wantedX - this.draggedHud.getScaledWidth() / 2 * -selfSide) - snapLocationX);
                                if (snapOffsetX < 5) {
                                    if (wantedY + draggedHud.getScaledHeight() / 2 + 5 >= otherY - otherHeight / 2 && wantedY - draggedHud.getScaledHeight() / 2 <= otherY + otherHeight / 2 + 5) {
                                        if (elementSnap.getKey() == null || !elementSnap.getKey().isAttachedTo(this.draggedHud)) {
                                            boolean isMinSnap = true;
                                            for (Snap snap : xSnaps) {
                                                if (Math.abs(snap.offset) <= Math.abs(snapOffsetX)) {
                                                    isMinSnap = false;
                                                    break;
                                                }
                                            }
                                            if (isMinSnap) {
                                                wantedX = snapLocationX - selfSide * draggedHud.getScaledWidth() / 2;
                                                for (Snap snap : new ArrayList<>(xSnaps)) {
                                                    if (snap.offset != snapOffsetX) {
                                                        xSnaps.remove(snap);
                                                        break;
                                                    }
                                                }

                                                xSnaps.add(new Snap(Axis.X, snapOffsetX, otherX + otherWidth / 2 * otherSide));
                                                snappedX = new Pair<>(elementSnap.getKey(), new AttachType(selfSide, otherSide, Axis.X));
                                            }
                                        }
                                    }
                                }

                                double snapLocationY = otherY + otherHeight / 2 * otherSide;
                                double snapOffsetY = Math.abs((wantedY - this.draggedHud.getScaledHeight() / 2 * -selfSide) - snapLocationY);
                                if (snapOffsetY < 5) {
                                    if (wantedX + draggedHud.getScaledWidth() / 2 + 5 >= otherX - otherWidth / 2 && wantedX - draggedHud.getScaledWidth() / 2 <= otherX + otherWidth / 2 + 5) {
                                        if (elementSnap.getKey() == null || !elementSnap.getKey().isAttachedTo(this.draggedHud)) {
                                            boolean isMinSnap = true;
                                            for (Snap snap : ySnaps) {
                                                if (Math.abs(snap.offset) <= Math.abs(snapOffsetY)) {
                                                    isMinSnap = false;
                                                    break;
                                                }
                                            }
                                            if (isMinSnap) {
                                                wantedY = snapLocationY - selfSide * draggedHud.getScaledHeight() / 2;
                                                for (Snap snap : new ArrayList<>(ySnaps)) {
                                                    if (snap.offset != snapOffsetY) {
                                                        ySnaps.remove(snap);
                                                        break;
                                                    }
                                                }

                                                ySnaps.add(new Snap(Axis.Y, snapOffsetY, otherY + otherHeight / 2 * otherSide));
                                                snappedY = new Pair<>(elementSnap.getKey(), new AttachType(selfSide, otherSide, Axis.Y));
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    displaySnaps(screenDimensions, xSnaps);
                    displaySnaps(screenDimensions, ySnaps);

                    draggedHud.setPosition(wantedX, wantedY, screenDimensions);

                    for (String hudId : draggedHud.getAttached()) {
                        HUDElement hud = this.getById(hudId);
                        if (hud == draggedHud || hud == null) continue;
                        hud.updatePosition(draggedHud, screenDimensions);
                    }

                    List<Pair<HUDElement, AttachType>> snapped = new ArrayList<>();
                    if (snappedX != null) {
                        snapped.add(snappedX);

                        if (snappedY == null) {
                            HUDElement hud = snappedX.getFirst();
                            double hudY = hud == null ? screenDimensions[1] / 2 : hud.getY(screenDimensions[1]);
                            double hudHeight = hud == null ? screenDimensions[1] : hud.getScaledHeight();

                            snapped.add(new Pair<>(hud, new AttachType(
                                    0,
                                    (((draggedHud.getY(screenDimensions[1]) - (hudY - hudHeight / 2)) / hudHeight) * 2) - 1,
                                    Axis.Y)));
                        }
                    }

                    if (snappedY != null) {
                        snapped.add(snappedY);

                        if (snappedX == null) {
                            HUDElement hud = snappedY.getFirst();
                            double hudX = hud == null ? screenDimensions[0] / 2 : hud.getX(screenDimensions[0]);
                            double hudWidth = hud == null ? screenDimensions[0] : hud.getScaledWidth();

                            snapped.add(new Pair<>(hud, new AttachType(
                                    0,
                                    (((draggedHud.getX(screenDimensions[0]) - (hudX - hudWidth / 2)) / hudWidth) * 2) - 1,
                                    Axis.X)));
                        }
                    }

                    this.snapped = snapped.toArray(new Pair[0]);
                    break;
                case RESIZE_BOTTOM_RIGHT: {
                    List<Snap> snaps = new ArrayList<>();

                    double wantedScale = MathUtil.clamp(initialScale + (mouseLocation[0] - initialMouseX) / draggedHud.getWidth(), 0.5, 2);

                    wantedScale = getWantedScale(screenDimensions, 1, 1, snaps, wantedScale);

                    draggedHud.setScale(wantedScale);
                    draggedHud.setPosition(
                            initialHudX + (wantedScale - initialScale) * draggedHud.getWidth() / 2,
                            initialHudY + (wantedScale - initialScale) * draggedHud.getHeight() / 2,
                            screenDimensions
                    );

                    for (String hudId : draggedHud.getAttached()) {
                        HUDElement hud = this.getById(hudId);
                        if (hud == draggedHud || hud == null) continue;
                        hud.updatePosition(draggedHud, screenDimensions);
                    }

                    displaySnaps(screenDimensions, snaps);
                    break;
                }
                case RESIZE_TOP_RIGHT: {
                    List<Snap> snaps = new ArrayList<>();

                    double wantedScale = MathUtil.clamp(initialScale + (mouseLocation[0] - initialMouseX) / draggedHud.getWidth(), 0.5, 2);

                    wantedScale = getWantedScale(screenDimensions, 1, -1, snaps, wantedScale);

                    draggedHud.setScale(wantedScale);
                    draggedHud.setPosition(
                            initialHudX + (wantedScale - initialScale) * draggedHud.getWidth() / 2,
                            initialHudY - (wantedScale - initialScale) * draggedHud.getHeight() / 2,
                            screenDimensions
                    );

                    for (String hudId : draggedHud.getAttached()) {
                        HUDElement hud = this.getById(hudId);
                        if (hud == draggedHud || hud == null) continue;
                        hud.updatePosition(draggedHud, screenDimensions);
                    }

                    displaySnaps(screenDimensions, snaps);
                    break;
                }
                case RESIZE_TOP_LEFT: {
                    List<Snap> snaps = new ArrayList<>();

                    double wantedScale = MathUtil.clamp(initialScale - (mouseLocation[0] - initialMouseX) / draggedHud.getWidth(), 0.5, 2);

                    wantedScale = getWantedScale(screenDimensions, -1, -1, snaps, wantedScale);

                    draggedHud.setScale(wantedScale);
                    draggedHud.setPosition(
                            initialHudX - (wantedScale - initialScale) * draggedHud.getWidth() / 2,
                            initialHudY - (wantedScale - initialScale) * draggedHud.getHeight() / 2,
                            screenDimensions
                    );

                    displaySnaps(screenDimensions, snaps);
                    break;
                }
                case RESIZE_BOTTOM_LEFT: {
                    List<Snap> snaps = new ArrayList<>();

                    double wantedScale = MathUtil.clamp(initialScale - (mouseLocation[0] - initialMouseX) / draggedHud.getWidth(), 0.5, 2);

                    wantedScale = getWantedScale(screenDimensions, -1, 1, snaps, wantedScale);

                    draggedHud.setScale(wantedScale);
                    draggedHud.setPosition(
                            initialHudX - (wantedScale - initialScale) * draggedHud.getWidth() / 2,
                            initialHudY + (wantedScale - initialScale) * draggedHud.getHeight() / 2,
                            screenDimensions
                    );

                    displaySnaps(screenDimensions, snaps);
                    break;
                }
            }

        }

        if (blendEnabled) {
            GL11.glEnable(GL11.GL_BLEND);
        } else {
            GL11.glDisable(GL11.GL_BLEND);
        }

        if (textureEnabled) {
            GL11.glEnable(GL11.GL_TEXTURE_2D);
        } else {
            GL11.glDisable(GL11.GL_TEXTURE_2D);
        }

        GL11.glColor4d(1, 1, 1, 1);
    }

    private void displaySnaps(double[] screenDimensions, List<Snap> snaps) {
        Renderer renderer2 = Sorus.getInstance().get(Renderer.class);

        for (Snap snap : snaps) {
            if (snap.axis == Axis.X) {
                renderer2.drawRectangle(snap.location - 0.25, 0, 0.5, screenDimensions[1], Color.fromRGB(255, 255, 255, 120));
            } else {
                renderer2.drawRectangle(0, snap.location - 0.25, screenDimensions[0], 0.5, Color.fromRGB(255, 255, 255, 120));
            }

        }
    }

    private double getWantedScale(double[] screenDimensions, double xSideInt, double ySideInt, List<Snap> snaps, double wantedScale) {
        for (HUDElement hud : this.elements.values()) {
            if (hud == this.draggedHud || draggedHud.isAttachedTo(hud)) continue;

            double otherHudX = hud.getX(screenDimensions[0]);
            double otherHudY = hud.getY(screenDimensions[1]);

            double xSide = initialHudX - draggedHud.getWidth() / 2 * initialScale * xSideInt + draggedHud.getWidth() * wantedScale * xSideInt;
            double ySide = initialHudY - draggedHud.getHeight() / 2 * initialScale * ySideInt + draggedHud.getHeight() * wantedScale * ySideInt;

            double xOpposite = initialHudX - draggedHud.getWidth() / 2 * initialScale * xSideInt;
            double yOpposite = initialHudY - draggedHud.getHeight() / 2 * initialScale * ySideInt;

            int[] possibleSides = new int[] {-1, 0, 1};
            for (int otherSide : possibleSides) {
                double snapOffsetX = Math.abs(xSide - (otherHudX + hud.getScaledWidth() / 2 * otherSide));
                if (snapOffsetX < 5) {
                    if (yOpposite + draggedHud.getHeight() * wantedScale + 5 >= otherHudY - hud.getScaledHeight() / 2 && ySide - draggedHud.getHeight() * wantedScale <= otherHudY + hud.getScaledHeight() / 2 + 5) {
                        boolean isMinSnap = true;
                        for (Snap snap : snaps) {
                            if (Math.abs(snap.offset) <= Math.abs(snapOffsetX)) {
                                isMinSnap = false;
                                break;
                            }
                        }
                        if (isMinSnap) {
                            wantedScale = Math.abs((otherHudX + hud.getScaledWidth() / 2 * otherSide) - xOpposite) / draggedHud.getWidth();
                            for (Snap snap : new ArrayList<>(snaps)) {
                                if (snap.offset != snapOffsetX || snap.axis != Axis.X) {
                                    snaps.remove(snap);
                                    break;
                                }
                            }

                            snaps.add(new Snap(Axis.X, snapOffsetX, otherHudX + hud.getScaledWidth() / 2 * otherSide));
                        }
                    }
                }

                double snapOffsetY = Math.abs(ySide - (otherHudY + hud.getScaledHeight() / 2 * otherSide));
                if (snapOffsetY < 5) {
                    if (xOpposite + draggedHud.getWidth() * wantedScale * xSideInt + 5 >= otherHudX - hud.getScaledWidth() / 2 && xSide - draggedHud.getWidth() * wantedScale * xSideInt <= otherHudX + hud.getScaledWidth() / 2 + 5) {
                        boolean isMinSnap = true;
                        for (Snap snap : snaps) {
                            if (Math.abs(snap.offset) <= Math.abs(snapOffsetY)) {
                                isMinSnap = false;
                                break;
                            }
                        }
                        if (isMinSnap) {
                            wantedScale = Math.abs((otherHudY + hud.getScaledHeight() / 2 * otherSide) - yOpposite) / draggedHud.getHeight();
                            for (Snap snap : new ArrayList<>(snaps)) {
                                if (snap.offset != snapOffsetX || snap.axis != Axis.Y) {
                                    snaps.remove(snap);
                                    break;
                                }
                            }

                            snaps.add(new Snap(Axis.Y, snapOffsetY, otherHudY + hud.getScaledHeight() / 2 * otherSide));
                        }
                    }
                }
            }
        }
        return wantedScale;
    }

    @SuppressWarnings("rawtypes")
    public void onClick(MouseEvent event) {
        if (event.getButton() != Button.PRIMARY) return;

        if (event.isPressed() && Sorus.getInstance().get(UserInterface.class).isHudEditScreenOpen()) {
            double[] screenDimensions = Sorus.getInstance().get(MinecraftAdapter.class).getScreenDimensions();

            for (HUDElement element : this.elements.values()) {
                double x = element.getX(screenDimensions[0]);
                double y = element.getY(screenDimensions[1]);

                double left = x - element.getScaledWidth() / 2;
                double right = x + element.getScaledWidth() / 2;
                double top = y - element.getScaledHeight() / 2;
                double bottom = y + element.getScaledHeight() / 2;

                boolean interacting = false;

                if (event.getX() > left + 1 && event.getX() < left + 4 && event.getY() > top + 1 && event.getY() < top + 4) {
                    this.hudToOpenSettings = element;
                } else if (distance(event.getX(), right, event.getY(), bottom) < 5) {
                    this.interactType = InteractType.RESIZE_BOTTOM_RIGHT;
                    interacting = true;
                } else if (distance(event.getX(), right, event.getY(), top) < 5) {
                    this.interactType = InteractType.RESIZE_TOP_RIGHT;
                    interacting = true;
                } else if (distance(event.getX(), left, event.getY(), top) < 5) {
                    this.interactType = InteractType.RESIZE_TOP_LEFT;
                    interacting = true;
                } else if (distance(event.getX(), left, event.getY(), bottom) < 5) {
                    this.interactType = InteractType.RESIZE_BOTTOM_LEFT;
                    interacting = true;
                } else if (event.getX() > left && event.getX() < right && event.getY() > top && event.getY() < bottom) {
                    this.interactType = InteractType.MOVE;
                    interacting = true;

                    if (System.currentTimeMillis() - this.prevClickTime < 400) {
                        element.detach();
                        interacting = false;
                    }
                }

                if (interacting) {
                    this.draggedHud = element;
                    this.initialMouseX = event.getX();
                    this.initialMouseY = event.getY();
                    this.initialHudX = x;
                    this.initialHudY = y;
                    this.initialScale = element.getScale();
                    this.prevClickTime = System.currentTimeMillis();
                }
            }
        } else {
            if (this.draggedHud == null) return;
            this.draggedHud.clearStaticAttached(new ArrayList<>());
            for (Pair hud : this.snapped) {
                this.draggedHud.addAttached((HUDElement) hud.getFirst(), (AttachType) hud.getSecond());

                if (hud.getFirst() != null) {
                    ((HUDElement) hud.getFirst()).clearStaticAttached(new ArrayList<>());
                    ((HUDElement) hud.getFirst()).addAttached(this.draggedHud, ((AttachType) hud.getSecond()).reverse());
                }
            }
            this.snapped = new Pair[0];
            this.draggedHud = null;
        }
    }

    public void onKey(KeyEvent event) {
        if (event.isPressed() && event.getKey() == Key.D && this.draggedHud != null) {
            this.draggedHud.delete(new ArrayList<>());
            this.draggedHud = null;
        }
    }

    public HUDElement getById(String id) {
        return this.elements.get(id);
    }

    private double distance(double x1, double x2, double y1, double y2) {
        return Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
    }

    @Override
    public String getId() {
        return "hud";
    }

    @SuppressWarnings("unchecked")
    @Override
    public void load(Map<String, Object> settings) {
        this.elements.clear();

        for (Map.Entry<String, Object> hud : settings.entrySet()) {
            Map<String, Object> hudSettings = (Map<String, Object>) hud.getValue();

            try {
                Class<?> hudClass = Class.forName((String) hudSettings.get("class"));
                HUDElement hudInstance = (HUDElement) hudClass.newInstance();
                hudInstance.load(hudSettings);

                this.elements.put(hud.getKey(), hudInstance);
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void loadForced(Map<String, Object> settings) {

    }

    @Override
    public void removeForced() {

    }

    @Override
    public Map<String, Object> save() {
        Map<String, Object> settingsMap = new HashMap<>();
        for (Map.Entry<String, HUDElement> setting : this.elements.entrySet()) {
            settingsMap.put(setting.getKey(), setting.getValue().save());
        }
        return settingsMap;
    }

    @Override
    public boolean isShared() {
        return this.isShared.getValue();
    }

    @Override
    public void setShared(boolean isShared) {
        this.isShared.setValue(isShared);
    }

    public List<HUDData> getPossibleHuds() {
        return this.possibleHuds;
    }

    private static class Snap {

        private final Axis axis;
        private final double offset;
        private final double location;

        private Snap(Axis axis, double offset, double location) {
            this.axis = axis;
            this.offset = offset;
            this.location = location;
        }

    }

    private enum InteractType {
        MOVE,
        RESIZE_BOTTOM_RIGHT,
        RESIZE_TOP_RIGHT,
        RESIZE_TOP_LEFT,
        RESIZE_BOTTOM_LEFT,
    }

}
