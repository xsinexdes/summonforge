package com.customsmithing.recipes;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Lógica pura del árbol de recetas (sin Bukkit, probada con JUnit).
 * El árbol se describe como un mapa id -> id del prerequisito (null = raíz). Todos los ids
 * de recetas deben figurar como clave (aunque su valor sea null).
 */
public final class RecipeTree {

    private RecipeTree() {
    }

    /** Profundidad de la receta: 0 si es raíz. Se corta ante ciclos o prerequisitos inexistentes. */
    public static int tier(Map<String, String> parents, String id) {
        Set<String> seen = new HashSet<>();
        seen.add(id);
        int steps = 0;
        String current = parents.get(id);
        while (current != null && parents.containsKey(current) && seen.add(current)) {
            steps++;
            current = parents.get(current);
        }
        return steps;
    }

    /** ¿Poner {@code newParent} como prerequisito de {@code child} crearía un ciclo? */
    public static boolean wouldCreateCycle(Map<String, String> parents, String child, String newParent) {
        Set<String> seen = new HashSet<>();
        String current = newParent;
        while (current != null) {
            if (current.equals(child)) return true;
            if (!seen.add(current)) return false; // ya había un ciclo ajeno a este cambio
            current = parents.get(current);
        }
        return false;
    }

    /** Ids que forman un ciclo (el primero que se encuentre), o lista vacía si el árbol es válido. */
    public static List<String> findCycle(Map<String, String> parents) {
        for (String start : parents.keySet()) {
            List<String> path = new ArrayList<>();
            Set<String> seen = new HashSet<>();
            String current = start;
            while (current != null && parents.containsKey(current)) {
                if (!seen.add(current)) {
                    return new ArrayList<>(path.subList(path.indexOf(current), path.size()));
                }
                path.add(current);
                current = parents.get(current);
            }
        }
        return new ArrayList<>();
    }

    /** Ids cuyo prerequisito no existe como receta. */
    public static List<String> missingParents(Map<String, String> parents) {
        List<String> missing = new ArrayList<>();
        for (Map.Entry<String, String> e : parents.entrySet()) {
            if (e.getValue() != null && !parents.containsKey(e.getValue())) missing.add(e.getKey());
        }
        return missing;
    }
}
