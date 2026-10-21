package com.customsmithing.structures;

import com.customsmithing.CustomSmithing;
import com.customsmithing.util.IdValidator;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.inventory.ItemStack;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Sistema de estructuras "casero": captura un cuboide del mundo (bloque por
 * bloque) y lo guarda como una lista de offsets + blockdata. Despues se puede
 * volver a pegar en cualquier lugar del mundo. No es generacion de mundo real
 * (no se integra con el chunk generator de Minecraft), pero cumple la misma
 * funcion: aparecen estructuras prefabricadas en el mapa.
 */
public class StructureManager {

    private final CustomSmithing plugin;
    private final File folder;

    public StructureManager(CustomSmithing plugin) {
        this.plugin = plugin;
        this.folder = new File(plugin.getDataFolder(), "structures");
        if (!folder.exists()) folder.mkdirs();
    }

    /** Archivo de una estructura. Solo acepta ids válidos: así el id nunca puede salirse de la carpeta. */
    private File fileFor(String id) {
        if (!IdValidator.isValid(id)) {
            throw new IllegalArgumentException("id de estructura no válido: " + id);
        }
        return new File(folder, id + ".yml");
    }

    public boolean exists(String id) {
        return IdValidator.isValid(id) && fileFor(id).exists();
    }

    public List<String> listIds() {
        List<String> ids = new ArrayList<>();
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files != null) {
            for (File f : files) {
                String id = f.getName().substring(0, f.getName().length() - ".yml".length());
                if (IdValidator.isValid(id)) ids.add(id);
                else plugin.getLogger().warning("structures/" + f.getName() + ": nombre no válido (usa a-z, 0-9 y _) — se ignora.");
            }
        }
        return ids;
    }

    public static final int ERR_DIFFERENT_WORLDS = -1;
    public static final int ERR_TOO_BIG = -2;

    /**
     * Captura el cuboide entre dos esquinas y lo guarda en disco.
     * @return bloques guardados, {@link #ERR_DIFFERENT_WORLDS} si las esquinas están en mundos distintos o
     *         {@link #ERR_TOO_BIG} si supera estructuras.max_volumen (evita congelar el servidor)
     */
    public int capture(String id, Location corner1, Location corner2) {
        World world = corner1.getWorld();
        if (world == null || corner2.getWorld() == null || !world.equals(corner2.getWorld())) return ERR_DIFFERENT_WORLDS;
        long volume = (long) (Math.abs(corner1.getBlockX() - corner2.getBlockX()) + 1)
                * (Math.abs(corner1.getBlockY() - corner2.getBlockY()) + 1)
                * (Math.abs(corner1.getBlockZ() - corner2.getBlockZ()) + 1);
        int maxSide = plugin.getConfig().getInt("estructuras.max_lado", 64);
        boolean sideTooBig = Math.abs(corner1.getBlockX() - corner2.getBlockX()) + 1 > maxSide
                || Math.abs(corner1.getBlockY() - corner2.getBlockY()) + 1 > maxSide
                || Math.abs(corner1.getBlockZ() - corner2.getBlockZ()) + 1 > maxSide;
        if (sideTooBig || volume > plugin.getConfig().getLong("estructuras.max_volumen", 100_000)) return ERR_TOO_BIG;
        int minX = Math.min(corner1.getBlockX(), corner2.getBlockX());
        int minY = Math.min(corner1.getBlockY(), corner2.getBlockY());
        int minZ = Math.min(corner1.getBlockZ(), corner2.getBlockZ());
        int maxX = Math.max(corner1.getBlockX(), corner2.getBlockX());
        int maxY = Math.max(corner1.getBlockY(), corner2.getBlockY());
        int maxZ = Math.max(corner1.getBlockZ(), corner2.getBlockZ());

        YamlConfiguration yml = new YamlConfiguration();
        List<String> blocks = new ArrayList<>();
        int count = 0;

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block block = world.getBlockAt(x, y, z);
                    BlockData data = block.getBlockData();
                    if (data.getMaterial().isAir()) continue;
                    int dx = x - minX, dy = y - minY, dz = z - minZ;
                    blocks.add(dx + "," + dy + "," + dz + "," + data.getAsString());
                    count++;
                    // Cofres, barriles, hornos...: se guarda lo que tienen dentro (solo este bloque, no el cofre doble entero).
                    if (block.getState() instanceof Container container) {
                        ItemStack[] contents = container.getSnapshotInventory().getContents();
                        boolean any = false;
                        for (ItemStack item : contents) {
                            if (item != null && !item.getType().isAir()) { any = true; break; }
                        }
                        if (any) yml.set("contenedores." + dx + "_" + dy + "_" + dz, java.util.Arrays.asList(contents));
                    }
                }
            }
        }

        yml.set("bloques", blocks);
        yml.set("ancho", (maxX - minX + 1));
        yml.set("alto", (maxY - minY + 1));
        yml.set("profundo", (maxZ - minZ + 1));
        yml.set("spawn_offset_x", (maxX - minX) / 2);
        yml.set("spawn_offset_y", 1);
        yml.set("spawn_offset_z", (maxZ - minZ) / 2);

        try {
            yml.save(fileFor(id));
        } catch (IOException e) {
            plugin.getLogger().warning("No se pudo guardar la estructura " + id + ": " + e.getMessage());
        }
        return count;
    }

    /** Devuelve a un contenedor lo que tenía al capturarse (si tenía algo). */
    private void restoreContents(YamlConfiguration yml, Block target, int dx, int dy, int dz) {
        List<?> saved = yml.getList("contenedores." + dx + "_" + dy + "_" + dz);
        if (saved == null || !(target.getState() instanceof Container container)) return;
        ItemStack[] contents = new ItemStack[container.getSnapshotInventory().getSize()];
        for (int i = 0; i < saved.size() && i < contents.length; i++) {
            if (saved.get(i) instanceof ItemStack item) contents[i] = item;
        }
        container.getSnapshotInventory().setContents(contents);
        container.update(true, false);
    }

    /**
     * Pega la estructura con la esquina mínima en {@code origin}, por lotes (estructuras.bloques_por_tick) para no
     * congelar el servidor. Devuelve ya el punto de spawn del jefe; {@code onDone} se ejecuta al terminar de pegar.
     */
    public Location paste(String id, Location origin, Runnable onDone) {
        if (!exists(id)) return null;
        File file = fileFor(id);
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        World world = origin.getWorld();
        if (world == null) return null;

        List<String> blocks = yml.getStringList("bloques");
        int perTick = Math.max(50, plugin.getConfig().getInt("estructuras.bloques_por_tick", 500));
        int[] cursor = {0};
        new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                int end = Math.min(blocks.size(), cursor[0] + perTick);
                for (int i = cursor[0]; i < end; i++) pasteLine(id, yml, world, origin, blocks.get(i));
                cursor[0] = end;
                if (end >= blocks.size()) {
                    cancel();
                    if (onDone != null) onDone.run();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);

        int sx = yml.getInt("spawn_offset_x", 0);
        int sy = yml.getInt("spawn_offset_y", 1);
        int sz = yml.getInt("spawn_offset_z", 0);
        return new Location(world, origin.getBlockX() + sx + 0.5, origin.getBlockY() + sy, origin.getBlockZ() + sz + 0.5);
    }

    private void pasteLine(String id, YamlConfiguration yml, World world, Location origin, String line) {
        String[] parts = line.split(",", 4);
        if (parts.length < 4) return;
        try {
            int dx = Integer.parseInt(parts[0].trim());
            int dy = Integer.parseInt(parts[1].trim());
            int dz = Integer.parseInt(parts[2].trim());
            BlockData data = plugin.getServer().createBlockData(parts[3]);
            Block target = world.getBlockAt(origin.getBlockX() + dx, origin.getBlockY() + dy, origin.getBlockZ() + dz);
            target.setBlockData(data, false);
            restoreContents(yml, target, dx, dy, dz);
        } catch (IllegalArgumentException e) {
            // línea dañada o bloque que ya no existe en esta versión: se salta solo esa línea
            plugin.debug("Estructura " + id + ": línea omitida '" + line + "' (" + e.getMessage() + ")");
        }
    }
}
