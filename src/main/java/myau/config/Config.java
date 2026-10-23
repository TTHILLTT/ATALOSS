package myau.config;

import com.google.gson.*;
import myau.ATALOSS;
import myau.mixin.IAccessorMinecraft;
import myau.module.Module;
import myau.util.ChatUtil;
import myau.property.Property;
import net.minecraft.client.Minecraft;

import java.io.*;
import java.util.ArrayList;

public class Config {
    public static Minecraft mc = Minecraft.getMinecraft();
    public static Gson gson = new GsonBuilder().setPrettyPrinting().create();
    /** 配置目录（改名后） */
    public static final File CONFIG_DIR = new File("./config/ATALOSS/");
    /** 改名前的旧目录：只读兼容，用于自动迁移旧配置，不再写入 */
    public static final File LEGACY_CONFIG_DIR = new File("./config/OpenMyau/");
    public String name;
    public File file;
    /** 旧目录下的同名文件，新目录没有该配置时回退读取 */
    public File legacyFile;

    public static String lastConfig;

    /** 列出/打开配置时使用的目录：优先新目录，其次旧目录（迁移期） */
    public static File activeDir() {
        if (CONFIG_DIR.exists()) {
            return CONFIG_DIR;
        }
        return LEGACY_CONFIG_DIR.exists() ? LEGACY_CONFIG_DIR : CONFIG_DIR;
    }

    public Config(String name, boolean newConfig) {
        this.name = name;
        lastConfig = name;
        if (name.equals("!") || name.equals("default")) {
            this.name = "default";
        }
        this.file = new File(CONFIG_DIR, String.format("%s.json", this.name));
        this.legacyFile = new File(LEGACY_CONFIG_DIR, String.format("%s.json", this.name));
        try {
            file.getParentFile().mkdirs();
            if (newConfig) {
                ((IAccessorMinecraft) mc).getLogger().info(String.format("Created: %s", this.file.getName()));
            }
        } catch (Exception e) {
            ((IAccessorMinecraft) mc).getLogger().error(e.getMessage());
        }
    }

    public void load() {
        // 新目录没有、旧目录有 → 读取旧文件，之后保存仍写入新目录（等效自动迁移）
        File source = this.file.exists()
                ? this.file
                : (this.legacyFile != null && this.legacyFile.exists() ? this.legacyFile : this.file);
        try {

            if (!source.exists()) {
                ChatUtil.sendFormatted(String.format("%sConfig file not found (&c&o%s&r). Creating default config...&r", ATALOSS.clientName, source.getName()));
                save();
                return;
            }

            JsonElement parsed = new JsonParser().parse(new BufferedReader(new FileReader(source)));
            if (parsed == null || !parsed.isJsonObject()) {
                ChatUtil.sendFormatted(String.format("%sInvalid config format (&c&o%s&r)&r", ATALOSS.clientName, source.getName()));
                return;
            }

            JsonObject jsonObject = parsed.getAsJsonObject();
            for (Module module : ATALOSS.moduleManager.modules.values()) {
                JsonElement moduleObj = jsonObject.get(module.getName());
                if (moduleObj != null && moduleObj.isJsonObject()) {
                    JsonObject object = moduleObj.getAsJsonObject();

                    ArrayList<Property<?>> list = ATALOSS.propertyManager.properties.get(module.getClass());
                    if (list != null) {
                        for (Property<?> property : list) {
                            if (object.has(property.getName())) {
                                try {
                                    property.read(object);
                                } catch (Exception e) {
                                    ((IAccessorMinecraft) mc).getLogger().warn(String.format("Failed to load property %s for module %s", property.getName(), module.getName()));
                                }
                            }
                        }
                    }

                    if (object.has("toggled")) {
                        JsonElement toggled = object.get("toggled");
                        if (toggled != null && toggled.isJsonPrimitive()) {
                            module.setEnabled(toggled.getAsBoolean());
                        }
                    }

                    if (object.has("key")) {
                        JsonElement key = object.get("key");
                        if (key != null && key.isJsonPrimitive()) {
                            module.setKey(key.getAsInt());
                        }
                    }

                    if (object.has("hidden")) {
                        JsonElement hidden = object.get("hidden");
                        if (hidden != null && hidden.isJsonPrimitive()) {
                            module.setHidden(hidden.getAsBoolean());
                        }
                    }
                }
            }
            if (source != this.file) {
                ChatUtil.sendFormatted(String.format("%sConfig has been loaded (&a&o%s&r, migrated from &o%s&r)&r", ATALOSS.clientName, file.getName(), LEGACY_CONFIG_DIR.getPath()));
            } else {
                ChatUtil.sendFormatted(String.format("%sConfig has been loaded (&a&o%s&r)&r", ATALOSS.clientName, file.getName()));
            }
        } catch (FileNotFoundException e) {
            ChatUtil.sendFormatted(String.format("%sConfig file not found (&c&o%s&r)&r", ATALOSS.clientName, source.getName()));
        } catch (JsonSyntaxException e) {
            ChatUtil.sendFormatted(String.format("%sConfig has invalid JSON syntax (&c&o%s&r)&r", ATALOSS.clientName, source.getName()));
            ((IAccessorMinecraft) mc).getLogger().error("JSON Syntax Error: " + e.getMessage());
        } catch (Exception e) {
            ((IAccessorMinecraft) mc).getLogger().error("Error loading config: " + e.getMessage());
            ChatUtil.sendFormatted(String.format("%sConfig couldn't be loaded (&c&o%s&r)&r", ATALOSS.clientName, source.getName()));
        }
    }

    public void save() {
        try {
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }

            JsonObject object = new JsonObject();
            for (Module module : ATALOSS.moduleManager.modules.values()) {
                JsonObject moduleObject = new JsonObject();
                moduleObject.addProperty("toggled", module.isEnabled());
                moduleObject.addProperty("key", module.getKey());
                moduleObject.addProperty("hidden", module.isHidden());

                ArrayList<Property<?>> list = ATALOSS.propertyManager.properties.get(module.getClass());
                if (list != null) {
                    for (Property<?> property : list) {
                        try {
                            property.write(moduleObject);
                        } catch (Exception e) {
                            ((IAccessorMinecraft) mc).getLogger().warn(String.format("Failed to save property %s for module %s", property.getName(), module.getName()));
                        }
                    }
                }
                object.add(module.getName(), moduleObject);
            }

            PrintWriter printWriter = new PrintWriter(new FileWriter(file));
            printWriter.println(gson.toJson(object));
            printWriter.close();
            ChatUtil.sendFormatted(String.format("%sConfig has been saved (&a&o%s&r)&r", ATALOSS.clientName, file.getName()));
        } catch (IOException e) {
            ((IAccessorMinecraft) mc).getLogger().error("Error saving config: " + e.getMessage());
            ChatUtil.sendFormatted(String.format("%sConfig couldn't be saved (&c&o%s&r)&r", ATALOSS.clientName, file.getName()));
        }
    }
}
