package com.epic60869.tastyfish;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Reads Skysoft's live Farming Profit Tracker session.
 *
 * Skysoft changed the location of its session map in recent releases: the
 * map is now owned by ProfitTrackerStatistics and ProfitTracker itself keeps
 * the statistics object private. We intentionally use reflection here so
 * TastyFish does not have to bundle Skysoft or hard-link against its internals.
 */
public final class SkysoftSessionReader {
    private static final String FARMING = "FARMING";
    private static final String PROFIT_TRACKER_CLASS = "com.skysoft.features.profit.ProfitTracker";
    private static final String TARGET_CLASS = "com.skysoft.features.profit.ProfitTrackerTarget";
    private static final String PRESET_CLASS = "com.skysoft.features.profit.ProfitTrackerPreset";
    private static final String ITEM_CUSTOMIZATIONS_CLASS = "com.skysoft.features.profit.ProfitTrackerItemCustomizations";

    private SkysoftSessionReader() {}

    public static Snapshot read() {
        try {
            Class<?> trackerClass = Class.forName(PROFIT_TRACKER_CLASS);
            Object tracker = getKotlinObjectInstance(trackerClass);
            Object target = createFarmingTarget();
            Object stats = readLiveSessionStats(trackerClass, tracker, target);

            if (stats == null) return Snapshot.empty();

            Map<String, Long> items = longMap(stats, "itemCounts");
            Map<String, Long> pests = longMap(stats, "pestKills");
            Map<String, Long> costs = longMap(stats, "costs");
            long activeMillis = longField(stats, "activeMillis");
            long actions = longField(stats, "actions");
            double coins = doubleField(stats, "coins");

            Set<String> trackedItemIds = invokeTrackedItemIds(trackerClass, tracker, target);
            double itemValue = 0.0;
            long valuedItems = 0L;
            long pricedItemTypes = 0L;
            boolean hadTrackedItems = false;

            for (Map.Entry<String, Long> entry : items.entrySet()) {
                String itemId = entry.getKey();
                long count = entry.getValue();
                if (count <= 0L || !trackedItemIds.contains(itemId)) continue;
                if (isExcluded(target, itemId)) continue;

                hadTrackedItems = true;
                Double unitValue = null;
                try {
                    unitValue = invokeUnitValue(trackerClass, tracker, target, itemId);
                } catch (Throwable ignored) {
                    // Skysoft can temporarily have no price for an item.
                }

                if (unitValue != null && Double.isFinite(unitValue) && unitValue > 0.0) {
                    itemValue += unitValue * count;
                    valuedItems += count;
                    pricedItemTypes++;
                }
            }

            // Do not turn a temporary price/API failure into a false zero-profit
            // upload when Skysoft is clearly tracking items.
            if (hadTrackedItems && pricedItemTypes == 0L) return Snapshot.invalid();

            double coinCosts = costs.getOrDefault("Coins", 0L).doubleValue();
            double profit = itemValue + coins - coinCosts;
            if (!Double.isFinite(profit)) return Snapshot.invalid();

            return new Snapshot(items, pests, activeMillis, actions, coins, profit, valuedItems, true);
        } catch (Throwable error) {
            System.err.println("[TastyFish] Failed to read Skysoft farming session: " + rootMessage(error));
            return Snapshot.invalid();
        }
    }

    /**
     * Skysoft 0.1.8 and older exposed sessionStats directly on ProfitTracker.
     * Newer Skysoft keeps it in ProfitTrackerStatistics, referenced by the
     * private ProfitTracker.statistics field. Prefer the new layout and retain
     * the old path as a compatibility fallback.
     */
    private static Object readLiveSessionStats(Class<?> trackerClass, Object tracker, Object target)
        throws ReflectiveOperationException {
        try {
            Field statisticsField = findField(trackerClass, "statistics");
            statisticsField.setAccessible(true);
            Object statistics = statisticsField.get(tracker);
            Field sessionStatsField = findField(statistics.getClass(), "sessionStats");
            sessionStatsField.setAccessible(true);
            Object raw = sessionStatsField.get(statistics);
            if (raw instanceof Map<?, ?> map) {
                return map.get(FARMING);
            }
        } catch (Throwable ignored) {
            // Fall through to the older layout.
        }

        try {
            Field sessionStatsField = findField(trackerClass, "sessionStats");
            sessionStatsField.setAccessible(true);
            Object raw = sessionStatsField.get(tracker);
            if (raw instanceof Map<?, ?> map) return map.get(FARMING);
        } catch (Throwable ignored) {
            // The current API is handled above; report the failure below.
        }

        // Last compatibility path: ask Skysoft for stats only when it is
        // configured to display the Farming session. This avoids fabricating
        // data if the user has selected Today/Total in Skysoft.
        Method statsMethod = findStatsMethod(trackerClass, target.getClass());
        Object value = statsMethod.invoke(tracker, target);
        return value;
    }

    private static Method findStatsMethod(Class<?> trackerClass, Class<?> targetClass)
        throws NoSuchMethodException {
        for (Method method : allMethods(trackerClass)) {
            if (!method.getName().equals("stats") || method.getParameterCount() != 1) continue;
            Class<?> parameter = method.getParameterTypes()[0];
            if (parameter.isAssignableFrom(targetClass) || targetClass.isAssignableFrom(parameter)) {
                method.setAccessible(true);
                return method;
            }
        }
        throw new NoSuchMethodException("No compatible Skysoft ProfitTracker.stats method found");
    }

    private static Object getKotlinObjectInstance(Class<?> type) throws ReflectiveOperationException {
        try {
            Field instance = type.getField("INSTANCE");
            instance.setAccessible(true);
            return instance.get(null);
        } catch (NoSuchFieldException ignored) {
            Field instance = findField(type, "INSTANCE");
            instance.setAccessible(true);
            return instance.get(null);
        }
    }

    private static Object createFarmingTarget() throws ReflectiveOperationException {
        Class<?> targetClass = Class.forName(TARGET_CLASS);
        Class<?> presetClass = Class.forName(PRESET_CLASS);
        @SuppressWarnings("unchecked")
        Object farmingPreset = Enum.valueOf(
            (Class<? extends Enum>) presetClass.asSubclass(Enum.class), FARMING
        );

        try {
            Field companionField = findField(targetClass, "Companion");
            companionField.setAccessible(true);
            Object companion = companionField.get(null);
            for (Method method : allMethods(companion.getClass())) {
                if (!method.getName().equals("preset") || method.getParameterCount() != 1) continue;
                Class<?> parameter = method.getParameterTypes()[0];
                if (!parameter.isAssignableFrom(presetClass) && !presetClass.isAssignableFrom(parameter)) continue;
                method.setAccessible(true);
                return method.invoke(companion, farmingPreset);
            }
        } catch (Throwable ignored) {
            // Fall through to constructor compatibility.
        }

        for (Constructor<?> constructor : targetClass.getDeclaredConstructors()) {
            Class<?>[] parameters = constructor.getParameterTypes();
            if (parameters.length == 2 && parameters[0].isAssignableFrom(presetClass)
                && parameters[1] == String.class) {
                constructor.setAccessible(true);
                return constructor.newInstance(farmingPreset, null);
            }
            if (parameters.length == 1 && parameters[0].isAssignableFrom(presetClass)) {
                constructor.setAccessible(true);
                return constructor.newInstance(farmingPreset);
            }
        }
        throw new NoSuchMethodException("Could not construct Skysoft ProfitTrackerTarget.FARMING");
    }

    private static Set<String> invokeTrackedItemIds(Class<?> trackerClass, Object tracker, Object target)
        throws ReflectiveOperationException {
        Throwable lastFailure = null;
        for (Method method : allMethods(trackerClass)) {
            if (!method.getName().startsWith("trackedItemIds") || method.getParameterCount() != 1) continue;
            Class<?> parameter = method.getParameterTypes()[0];
            if (!parameter.isAssignableFrom(target.getClass()) && !target.getClass().isAssignableFrom(parameter)) continue;
            try {
                method.setAccessible(true);
                Object receiver = Modifier.isStatic(method.getModifiers()) ? null : tracker;
                Object result = method.invoke(receiver, target);
                if (result instanceof Set<?> raw) {
                    java.util.LinkedHashSet<String> ids = new java.util.LinkedHashSet<>();
                    for (Object value : raw) if (value instanceof String id) ids.add(id);
                    return ids;
                }
            } catch (InvocationTargetException invocation) {
                lastFailure = invocation.getCause() == null ? invocation : invocation.getCause();
            } catch (Throwable failure) {
                lastFailure = failure;
            }
        }
        if (lastFailure != null) throw new ReflectiveOperationException("Skysoft trackedItemIds failed", lastFailure);
        throw new NoSuchMethodException("No compatible Skysoft trackedItemIds method found");
    }

    private static boolean isExcluded(Object target, String itemId) throws ReflectiveOperationException {
        Class<?> customizationClass = Class.forName(ITEM_CUSTOMIZATIONS_CLASS);
        Object customization = getKotlinObjectInstance(customizationClass);
        for (Method method : allMethods(customizationClass)) {
            if (!method.getName().startsWith("isExcluded") || method.getParameterCount() != 2) continue;
            Class<?>[] parameters = method.getParameterTypes();
            boolean targetCompatible = parameters[0].isAssignableFrom(target.getClass())
                || target.getClass().isAssignableFrom(parameters[0]);
            boolean stringCompatible = parameters[1].isAssignableFrom(String.class) || parameters[1] == Object.class;
            if (!targetCompatible || !stringCompatible) continue;
            method.setAccessible(true);
            Object receiver = Modifier.isStatic(method.getModifiers()) ? null : customization;
            Object result = method.invoke(receiver, target, itemId);
            return result instanceof Boolean && (Boolean) result;
        }
        throw new NoSuchMethodException("No compatible Skysoft isExcluded method found");
    }

    private static Double invokeUnitValue(
        Class<?> trackerClass, Object tracker, Object target, String itemId
    ) throws ReflectiveOperationException {
        Throwable lastFailure = null;
        for (Method method : allMethods(trackerClass)) {
            String name = method.getName();
            if (!(name.equals("unitValue") || name.startsWith("unitValue")) || method.getParameterCount() != 2) continue;
            Class<?>[] parameters = method.getParameterTypes();
            boolean targetCompatible = parameters[0].isAssignableFrom(target.getClass())
                || target.getClass().isAssignableFrom(parameters[0]);
            boolean stringCompatible = parameters[1].isAssignableFrom(String.class) || parameters[1] == Object.class;
            if (!targetCompatible || !stringCompatible) continue;
            try {
                method.setAccessible(true);
                Object receiver = Modifier.isStatic(method.getModifiers()) ? null : tracker;
                Object result = method.invoke(receiver, target, itemId);
                if (result == null) return null;
                if (result instanceof Number number) return number.doubleValue();
            } catch (InvocationTargetException invocation) {
                lastFailure = invocation.getCause() == null ? invocation : invocation.getCause();
            } catch (Throwable failure) {
                lastFailure = failure;
            }
        }
        if (lastFailure != null) throw new ReflectiveOperationException("Skysoft unitValue failed", lastFailure);
        throw new NoSuchMethodException("No compatible Skysoft unitValue method found");
    }

    private static List<Method> allMethods(Class<?> type) {
        List<Method> result = new ArrayList<>();
        Class<?> current = type;
        while (current != null) {
            for (Method method : current.getDeclaredMethods()) {
                if (!containsSameMethod(result, method)) result.add(method);
            }
            for (Class<?> iface : current.getInterfaces()) {
                for (Method method : iface.getMethods()) {
                    if (!containsSameMethod(result, method)) result.add(method);
                }
            }
            current = current.getSuperclass();
        }
        return result;
    }

    private static boolean containsSameMethod(List<Method> methods, Method candidate) {
        for (Method method : methods) {
            if (method.getName().equals(candidate.getName())
                && java.util.Arrays.equals(method.getParameterTypes(), candidate.getParameterTypes())) return true;
        }
        return false;
    }

    private static Map<String, Long> longMap(Object object, String fieldName) throws ReflectiveOperationException {
        Field field = findField(object.getClass(), fieldName);
        field.setAccessible(true);
        Object raw = field.get(object);
        if (!(raw instanceof Map<?, ?> source)) throw new IllegalStateException("Skysoft field " + fieldName + " is not a Map");
        Map<String, Long> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            if (entry.getKey() instanceof String key && entry.getValue() instanceof Number value) result.put(key, value.longValue());
        }
        return result;
    }

    private static long longField(Object object, String fieldName) throws ReflectiveOperationException {
        Field field = findField(object.getClass(), fieldName);
        field.setAccessible(true);
        Object value = field.get(object);
        if (!(value instanceof Number number)) throw new IllegalStateException("Skysoft field " + fieldName + " is not numeric");
        return number.longValue();
    }

    private static double doubleField(Object object, String fieldName) throws ReflectiveOperationException {
        Field field = findField(object.getClass(), fieldName);
        field.setAccessible(true);
        Object value = field.get(object);
        if (!(value instanceof Number number)) throw new IllegalStateException("Skysoft field " + fieldName + " is not numeric");
        return number.doubleValue();
    }

    private static Field findField(Class<?> type, String name) throws NoSuchFieldException {
        Class<?> current = type;
        while (current != null) {
            try { return current.getDeclaredField(name); }
            catch (NoSuchFieldException ignored) { current = current.getSuperclass(); }
        }
        throw new NoSuchFieldException(type.getName() + "." + name);
    }

    private static String rootMessage(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null) current = current.getCause();
        String message = current.getMessage();
        return message == null ? current.toString() : message;
    }

    public record Snapshot(
        Map<String, Long> items,
        Map<String, Long> pests,
        long activeMillis,
        long actions,
        double coins,
        double profit,
        long valuedItems,
        boolean valid
    ) {
        public static Snapshot empty() {
            return new Snapshot(Map.of(), Map.of(), 0L, 0L, 0.0, 0.0, 0L, true);
        }

        public static Snapshot invalid() {
            return new Snapshot(Map.of(), Map.of(), 0L, 0L, 0.0, 0.0, 0L, false);
        }
    }
}
