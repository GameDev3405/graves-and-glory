package gg.gravesandglory;

import java.lang.reflect.Field;
import java.lang.reflect.RecordComponent;
import java.util.*;

/** Unshipped. -Dgravesandglory.simOverride="troops.reaper.health=45;houseTraits.gilded_3.value=0.3" (gradle -PsimOverride=...) replaces sheet
 *  rows in memory when a simulation starts: sheet, row id and column, each change a new record in a new array, so nothing on disk changes and a
 *  one-lever ablation needs no edit of sheets/design.json. An unknown sheet, row or column, or a value of the wrong type, stops the run.
 *  Only sheets read at the moment of use take effect: troops, abilities, counters, houseTraits, classes, botCommand and botBanners. Sheets that
 *  a class copies into a static field when it loads (rules, navigation, combatDiscipline, economy) are refused. */
final class SheetOverride {
    private SheetOverride() {}
    static final Set<String> SAFE = Set.of("troops", "abilities", "counters", "houseTraits", "classes", "botCommand", "botBanners");
    /** The changes applied, "sheet.row.column old -> new", in order; empty when none. */
    static final List<String> applied = new ArrayList<>();
    private static boolean done;

    static synchronized void applyFromProperty() {
        if (done) return;
        done = true;
        String spec = System.getProperty("gravesandglory.simOverride");
        if (spec == null || spec.isBlank()) return;
        for (String item : spec.split(";")) {
            item = item.trim();
            if (item.isEmpty()) continue;
            int eq = item.indexOf('=');
            if (eq < 0) throw new IllegalArgumentException("[override] expected sheet.row.column=value, got '" + item + "'");
            String key = item.substring(0, eq).trim(), value = item.substring(eq + 1).trim();
            String[] p = key.split("\\.");
            if (p.length != 3) throw new IllegalArgumentException("[override] expected sheet.row.column, got '" + key + "'");
            if (!SAFE.contains(p[0])) throw new IllegalArgumentException("[override] sheet " + p[0] + " is not one an override can change (allowed: " + SAFE + ")");
            try { applied.add(apply(p[0], p[1], p[2], value)); }
            catch (ReflectiveOperationException e) { throw new IllegalStateException("[override] " + key + ": " + e, e); }
        }
        for (String line : applied) System.out.println("[override] " + line);
    }

    static String apply(String sheet, String id, String column, String value) throws ReflectiveOperationException {
        Field field = Spec.class.getField(sheet.toUpperCase(Locale.ROOT));
        Object[] rows = (Object[]) field.get(null);
        Class<?> type = rows.getClass().getComponentType();
        RecordComponent[] parts = type.getRecordComponents();
        int row = -1;
        for (int i = 0; i < rows.length; i++) if (id.equals(parts[0].getAccessor().invoke(rows[i]))) row = i;
        if (row < 0) throw new IllegalArgumentException("[override] " + sheet + " has no row '" + id + "'");
        Object[] args = new Object[parts.length];
        Class<?>[] types = new Class<?>[parts.length];
        int col = -1;
        for (int i = 0; i < parts.length; i++) {
            args[i] = parts[i].getAccessor().invoke(rows[row]);
            types[i] = parts[i].getType();
            if (parts[i].getName().equals(column)) col = i;
        }
        if (col < 0) throw new IllegalArgumentException("[override] " + sheet + " has no column '" + column + "'");
        Object old = args[col];
        args[col] = parse(value, types[col]);
        Object[] copy = Arrays.copyOf(rows, rows.length);
        copy[row] = type.getDeclaredConstructor(types).newInstance(args);
        field.set(null, copy);
        return sheet + "." + id + "." + column + " " + old + " -> " + args[col];
    }

    static Object parse(String value, Class<?> type) {
        if (type == int.class) return Integer.parseInt(value);
        if (type == double.class) return Double.parseDouble(value);
        if (type == boolean.class) {
            if (!value.equals("true") && !value.equals("false")) throw new IllegalArgumentException("[override] not a boolean: " + value);
            return Boolean.parseBoolean(value);
        }
        if (type == String.class) return value;
        throw new IllegalArgumentException("[override] cannot set a column of type " + type);
    }

    /** One line for a run's meta.txt. */
    static String describe() { return applied.isEmpty() ? "none" : String.join("; ", applied); }
}
