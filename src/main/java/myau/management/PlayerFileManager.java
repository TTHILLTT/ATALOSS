package myau.management;

import net.minecraft.client.Minecraft;

import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.stream.Collectors;

public abstract class PlayerFileManager {
    public static Minecraft mc = Minecraft.getMinecraft();
    public ArrayList<String> players;
    public File file;
    /** 改名前的旧文件路径：只读兼容，新文件不存在时回退读取，不再写入 */
    public File legacyFile;
    public Color color;

    public PlayerFileManager(File file, Color color) {
        this(file, null, color);
    }

    public PlayerFileManager(File file, File legacyFile, Color color) {
        this.players = new ArrayList<>();
        this.file = file;
        this.legacyFile = legacyFile;
        this.color = color;
    }

    public void load() {
        // 新路径没有、旧路径有 → 读取旧文件；之后 save() 仍写入新路径（等效自动迁移）
        File source = file.exists() ? file : (legacyFile != null && legacyFile.exists() ? legacyFile : file);
        if (!source.exists()) {
            try {
                if ((file.getParentFile().exists() || file.getParentFile().mkdirs()) && file.createNewFile()) {
                    System.out.printf("File created: %s%n", file.getName());
                }
            } catch (IOException e) {
                System.err.println("Error creating file: " + e.getMessage());
            }
        } else if (source != file) {
            System.out.printf("Migrating %s -> %s%n", source.getPath(), file.getPath());
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(source))) {
            players.clear();
            players.addAll(reader.lines().map(String::trim).collect(Collectors.toList()));
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }
    }

    public void save() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            writer.print(String.join("\n", players));
        } catch (IOException e) {
            System.err.println("Error saving file: " + e.getMessage());
        }
    }

    public String add(String name) {
        if (isFriend(name)) {
            return null;
        }
        players.add(name);
        save();
        return name;
    }

    public String remove(String name) {
        for (String player : players) {
            if (player.equalsIgnoreCase(name)) {
                players.remove(player);
                save();
                return player;
            }
        }
        return null;
    }

    public void clear() {
        players.clear();
        save();
    }

    public boolean isFriend(String string) {
        return this.players.stream().anyMatch(string2 -> string2.equalsIgnoreCase(string));
    }

    public ArrayList<String> getPlayers() {
        return this.players;
    }

    public Color getColor() {
        return this.color;
    }
}
