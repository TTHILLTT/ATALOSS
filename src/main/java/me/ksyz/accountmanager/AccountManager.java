package me.ksyz.accountmanager;

import com.google.gson.*;
import me.ksyz.accountmanager.auth.Account;
import me.ksyz.accountmanager.utils.Nan0EventRegister;
import me.ksyz.accountmanager.utils.SSLUtils;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;

import javax.net.ssl.SSLContext;
import java.io.*;
import java.util.ArrayList;
import java.util.Optional;

/*
 * This file is derived from https://github.com/ksyzov/AccountManager.
 * Originally licensed under the GNU LGPL.
 *
 * This modified version is licensed under the GNU GPL v3.
 */
public class AccountManager {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final File file = new File(mc.mcDataDir, "ataloss.accounts.json");
    /** 改名前的旧账号文件（openmyau.accounts.json）：新文件缺失时用于迁移读取，写入始终走新文件 */
    private static final File legacyFile = new File(mc.mcDataDir, "openmyau.accounts.json");
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static final ArrayList<Account> accounts = new ArrayList<>();

    public static void init() {
        SSLContext ignored = SSLUtils.getSSLContext();
        Nan0EventRegister.register(MinecraftForge.EVENT_BUS,new Events());

        if (!file.exists()) {
            if (legacyFile.exists()) {
                System.out.print("Found legacy openmyau.accounts.json - it will be migrated to ataloss.accounts.json on the next save.");
            } else {
                try {
                    if (file.getParentFile().exists() || file.getParentFile().mkdirs()) {
                        if (file.createNewFile()) {
                            System.out.print("Successfully created ataloss.accounts.json!");
                        }
                    }
                } catch (IOException e) {
                    System.err.print("Couldn't create ataloss.accounts.json!");
                }
            }
        }
    }

    public static void load() {
        accounts.clear();
        File source = file.exists() ? file : (legacyFile.exists() ? legacyFile : file);
        try {
            JsonElement json = new JsonParser().parse(
                    new BufferedReader(new FileReader(source))
            );
            if (json instanceof JsonArray) {
                JsonArray jsonArray = json.getAsJsonArray();
                for (JsonElement jsonElement : jsonArray) {
                    JsonObject jsonObject = jsonElement.getAsJsonObject();
                    accounts.add(new Account(
                            Optional.ofNullable(jsonObject.get("refreshToken")).map(JsonElement::getAsString).orElse(""),
                            Optional.ofNullable(jsonObject.get("accessToken")).map(JsonElement::getAsString).orElse(""),
                            Optional.ofNullable(jsonObject.get("username")).map(JsonElement::getAsString).orElse(""),
                            Optional.ofNullable(jsonObject.get("unban")).map(JsonElement::getAsLong).orElse(0L),
                            Optional.ofNullable(jsonObject.get("clientId")).map(JsonElement::getAsString).orElse(""),
                            Optional.ofNullable(jsonObject.get("scope")).map(JsonElement::getAsString).orElse("")
                    ));
                }
            }
        } catch (FileNotFoundException e) {
            System.err.print("Couldn't find ataloss.accounts.json!");
        }
    }

    public static void save() {
        try {
            if (!file.exists() && legacyFile.exists()) {
                System.out.print("Migrating openmyau.accounts.json -> ataloss.accounts.json");
            }
            JsonArray jsonArray = new JsonArray();
            for (Account account : accounts) {
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("refreshToken", account.getRefreshToken());
                jsonObject.addProperty("accessToken", account.getAccessToken());
                jsonObject.addProperty("username", account.getUsername());
                jsonObject.addProperty("unban", account.getUnban());
                jsonObject.addProperty("clientId", account.getClientId());
                jsonObject.addProperty("scope", account.getScope());
                jsonArray.add(jsonObject);
            }
            PrintWriter printWriter = new PrintWriter(new FileWriter(file));
            printWriter.println(gson.toJson(jsonArray));
            printWriter.close();
        } catch (IOException e) {
            System.err.print("Couldn't save ataloss.accounts.json!");
        }
    }
}
