package myau.ui;

import myau.ATALOSS;
import myau.module.legit.CustomCape;
import myau.module.legit.CustomSkin;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/** 衣柜的公共逻辑：列出 png、套用、清空、查当前生效项。两套 GUI 共用。 */
public final class WardrobeContent {
    public static final int MAX_ROWS = 12;

    private WardrobeContent() {
    }

    public static List<File> capes() {
        return listPng(new File(Minecraft.getMinecraft().mcDataDir, "config/ATALOSS/capes"));
    }

    public static List<File> skins() {
        return listPng(new File(Minecraft.getMinecraft().mcDataDir, "config/ATALOSS/skins"));
    }

    public static String currentCape() {
        CustomCape module = (CustomCape) ATALOSS.moduleManager.modules.get(CustomCape.class);
        return module == null || !module.isEnabled() ? null : module.path.getValue();
    }

    public static String currentSkin() {
        CustomSkin module = (CustomSkin) ATALOSS.moduleManager.modules.get(CustomSkin.class);
        return module == null || !module.isEnabled() ? null : module.path.getValue();
    }

    public static void applyCape(File file) {
        CustomCape module = (CustomCape) ATALOSS.moduleManager.modules.get(CustomCape.class);
        if (module == null) {
            return;
        }
        module.path.setValue(relative(true, file));
        if (!module.isEnabled()) {
            module.setEnabled(true);
        }
    }

    public static void applySkin(File file) {
        CustomSkin module = (CustomSkin) ATALOSS.moduleManager.modules.get(CustomSkin.class);
        if (module == null) {
            return;
        }
        module.path.setValue(relative(false, file));
        if (!module.isEnabled()) {
            module.setEnabled(true);
        }
    }

    public static void clearCape() {
        CustomCape module = (CustomCape) ATALOSS.moduleManager.modules.get(CustomCape.class);
        if (module != null) {
            module.setEnabled(false);
        }
    }

    public static void clearSkin() {
        CustomSkin module = (CustomSkin) ATALOSS.moduleManager.modules.get(CustomSkin.class);
        if (module != null) {
            module.setEnabled(false);
        }
    }

    public static String relative(boolean cape, File file) {
        return "config/ATALOSS/" + (cape ? "capes/" : "skins/") + file.getName();
    }

    private static List<File> listPng(File dir) {
        List<File> out = new ArrayList<>();
        File[] files = dir.listFiles();
        if (files == null) {
            return out;
        }
        Arrays.sort(files, Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER));
        for (File file : files) {
            if (file.isFile() && file.getName().toLowerCase().endsWith(".png")) {
                out.add(file);
            }
        }
        return out;
    }
}
