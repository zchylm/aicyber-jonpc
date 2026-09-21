package com.aicyber.backend.configurator.service;

import com.aicyber.backend.configurator.dto.ConfiguratorCatalogOption;
import com.aicyber.backend.configurator.dto.ConfiguratorCatalogResponse;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Backend-owned catalogue used by recommendation, compatibility and quoting. */
public final class ConfiguratorCatalog {

    static final int SYSTEM_BASE_PRICE = 650;

    static final Map<String, Integer> CPU_PRICES = orderedPrices(
            "ryzen-5-7500f", 239, "ryzen-5-9600x", 348, "core-ultra-5-245k", 329,
            "ryzen-7-9700x", 455, "core-ultra-7-265k", 565, "ryzen-7-9850x3d", 744,
            "ryzen-9-9900x", 629, "ryzen-9-9950x3d", 1029
    );
    static final Map<String, Integer> GPU_PRICES = orderedPrices(
            "integrated", 0, "rtx-5060", 649, "rtx-5060-ti-16gb", 949,
            "rtx-5070", 1249, "rtx-5070-ti", 1749, "rtx-5080", 2199, "rtx-5090", 7999
    );
    static final Map<String, Integer> MEMORY_PRICES = orderedPrices("16gb", 100, "32gb", 200, "64gb", 400, "128gb", 800);
    static final Map<String, Integer> STORAGE_PRICES = orderedPrices("1tb", 100, "2tb", 200, "4tb", 400);
    static final Map<String, Integer> MOTHERBOARD_PRICES = orderedPrices(
            "b850m-wifi", 230, "b850-wifi", 300, "x870-wifi", 450, "x870e-wifi", 650,
            "b860-wifi", 260, "z890-wifi", 390
    );
    static final Map<String, Integer> PSU_PRICES = orderedPrices(
            "650-bronze", 110, "750-gold", 160, "850-gold", 210, "1000-platinum", 300
    );
    static final Map<String, Integer> CASE_PRICES = orderedPrices("compact", 140, "mid", 180, "full", 260);
    static final Map<String, Integer> COOLING_PRICES = orderedPrices("tower-air", 70, "dual-tower-air", 110, "240-liquid", 150, "360-liquid", 200);

    static final List<String> AM5_MOTHERBOARDS = List.of("b850m-wifi", "b850-wifi", "x870-wifi", "x870e-wifi");
    static final List<String> LGA1851_MOTHERBOARDS = List.of("b860-wifi", "z890-wifi");

    public static ConfiguratorCatalogResponse response() {
        return new ConfiguratorCatalogResponse(SYSTEM_BASE_PRICE, Map.of(
                "cpu", List.of(
                        performanceOption("ryzen-5-7500f", "AMD Ryzen 5 7500F", "AMD Ryzen", "Efficient 6-core AM5 value choice", 239, "AM5"),
                        performanceOption("ryzen-5-9600x", "AMD Ryzen 5 9600X", "AMD Ryzen", "Current-generation gaming and everyday performance", 348, "AM5"),
                        performanceOption("core-ultra-5-245k", "Intel Core Ultra 5 245K", "Intel Core Ultra", "Balanced current-generation productivity platform", 329, "LGA1851"),
                        performanceOption("ryzen-7-9700x", "AMD Ryzen 7 9700X", "AMD Ryzen", "Efficient 8-core performance for mixed workloads", 455, "AM5"),
                        performanceOption("core-ultra-7-265k", "Intel Core Ultra 7 265K", "Intel Core Ultra", "Strong multi-core performance for creation and development", 565, "LGA1851"),
                        performanceOption("ryzen-7-9850x3d", "AMD Ryzen 7 9850X3D", "AMD Ryzen", "Gaming-focused performance with high frame consistency", 744, "AM5"),
                        performanceOption("ryzen-9-9900x", "AMD Ryzen 9 9900X", "AMD Ryzen", "12-core capacity for creation, AI and workstation work", 629, "AM5"),
                        performanceOption("ryzen-9-9950x3d", "AMD Ryzen 9 9950X3D", "AMD Ryzen", "Flagship gaming and heavy multi-core performance", 1029, "AM5")
                ),
                "gpu", List.of(
                        option("integrated", "Integrated graphics", "AMD / Intel", "Office, display and everyday productivity", 0),
                        option("rtx-5060", "NVIDIA GeForce RTX 5060 8GB", "NVIDIA GeForce RTX 50", "Current-generation 1080p gaming and creator acceleration", 649),
                        option("rtx-5060-ti-16gb", "NVIDIA GeForce RTX 5060 Ti 16GB", "NVIDIA GeForce RTX 50", "More VRAM for 1440p, creation and local AI", 949),
                        option("rtx-5070", "NVIDIA GeForce RTX 5070 12GB", "NVIDIA GeForce RTX 50", "Balanced 1440p, AI and creator performance", 1249),
                        option("rtx-5070-ti", "NVIDIA GeForce RTX 5070 Ti 16GB", "NVIDIA GeForce RTX 50", "High-refresh 1440p and stronger compute headroom", 1749),
                        option("rtx-5080", "NVIDIA GeForce RTX 5080 16GB", "NVIDIA GeForce RTX 50", "High-end 4K and local compute performance", 2199),
                        option("rtx-5090", "NVIDIA GeForce RTX 5090 32GB", "NVIDIA GeForce RTX 50", "Specialist flagship graphics and AI capacity", 7999)
                ),
                "memory", List.of(
                        option("16gb", "16GB DDR5", "Memory", "2 x 8GB / Everyday productivity and entry gaming", 100, "Essential starting point"),
                        option("32gb", "32GB DDR5", "Memory", "2 x 16GB / Gaming, creation and multitasking", 200, "Recommended balance"),
                        option("64gb", "64GB DDR5", "Memory", "2 x 32GB / Heavy creation, AI and production work", 400, "More headroom"),
                        option("128gb", "128GB DDR5", "Memory", "High-capacity kit / Large datasets and specialist workloads", 800, "Specialist capacity")
                ),
                "storage", List.of(
                        option("1tb", "1TB Gen4 NVMe", "Storage", "Fast everyday storage for apps, games and active projects", 100, "Focused starting point"),
                        option("2tb", "2TB Gen4 NVMe", "Storage", "More space for modern games, media libraries and project files", 200, "Recommended balance"),
                        option("4tb", "4TB Gen4 NVMe", "Storage", "Large capacity for datasets, footage and specialist workflows", 400, "More working room")
                ),
                "motherboard", List.of(
                        option("b850m-wifi", "B850M Gaming Wi-Fi", "AMD AM5", "Micro-ATX / DDR5 / Wi-Fi", 230, "AM5", "B850", true, "Micro-ATX"),
                        option("b850-wifi", "B850 Gaming Wi-Fi", "AMD AM5", "ATX / DDR5 / PCIe 5.0 / Wi-Fi", 300, "AM5", "B850", true, "ATX"),
                        option("x870-wifi", "X870 Creator Wi-Fi", "AMD AM5", "ATX / DDR5 / USB4 / Wi-Fi", 450, "AM5", "X870", true, "ATX"),
                        option("x870e-wifi", "X870E Workstation Wi-Fi", "AMD AM5", "ATX / DDR5 / expanded PCIe and USB4", 650, "AM5", "X870E", true, "ATX"),
                        option("b860-wifi", "B860 Gaming Wi-Fi", "Intel LGA1851", "ATX / DDR5 / Wi-Fi", 260, "LGA1851", "B860", true, "ATX"),
                        option("z890-wifi", "Z890 Performance Wi-Fi", "Intel LGA1851", "ATX / DDR5 / expanded I/O", 390, "LGA1851", "Z890", true, "ATX")
                ),
                "psu", List.of(
                        option("650-bronze", "650W 80+ Bronze", "Power", "Reliable power for efficient graphics configurations", 110, "Bronze", 650, "Semi-modular"),
                        option("750-gold", "750W 80+ Gold ATX 3.1", "Power", "Efficient current-generation power with upgrade room", 160, "Gold", 750, "Fully modular"),
                        option("850-gold", "850W 80+ Gold ATX 3.1", "Power", "High-performance power with additional headroom", 210, "Gold", 850, "Fully modular"),
                        option("1000-platinum", "1000W 80+ Platinum ATX 3.1", "Power", "Premium power for demanding flagship builds", 300, "Platinum", 1000, "Fully modular")
                ),
                "case", List.of(
                        option("compact", "Compact", "Case", "Small footprint / Micro-ATX focused", 140, List.of("Micro-ATX")),
                        option("mid", "Mid Tower", "Case", "Balanced space / broad component availability", 180, List.of("Micro-ATX", "ATX")),
                        option("full", "Full Tower", "Case", "Maximum room / flagship cooling and upgrade paths", 260, List.of("ATX"))
                ),
                "cooling", List.of(
                        option("tower-air", "Tower air cooling", "Cooling", "Quiet, efficient and easy to maintain", 70, "Balanced everyday cooling", List.of("compact", "mid", "full")),
                        option("dual-tower-air", "Dual-tower air cooling", "Cooling", "More thermal headroom for long sessions", 110, "Higher sustained workloads", List.of("mid", "full")),
                        option("240-liquid", "240mm liquid cooling", "Cooling", "Performance cooling with a clean internal layout", 150, "Performance-focused builds", List.of("compact", "mid", "full")),
                        option("360-liquid", "360mm liquid cooling", "Cooling", "Maximum cooling surface for demanding systems", 200, "High-end sustained workloads", List.of("mid", "full"))
                )
        ));
    }

    static String platformForCpu(String cpuId) {
        return cpuId != null && cpuId.startsWith("core-ultra-") ? "LGA1851" : "AM5";
    }

    static int minimumPsuWattage(String gpuId) {
        return switch (gpuId) {
            case "rtx-5090" -> 1000;
            case "rtx-5080", "rtx-5070-ti" -> 850;
            case "rtx-5070" -> 750;
            default -> 650;
        };
    }

    static boolean highHeatCpu(String cpuId) {
        return List.of("core-ultra-7-265k", "ryzen-7-9850x3d", "ryzen-9-9900x", "ryzen-9-9950x3d").contains(cpuId);
    }

    static boolean hasIntegratedGraphics(String cpuId) {
        return !"ryzen-5-7500f".equals(cpuId);
    }

    private static Map<String, Integer> orderedPrices(Object... values) {
        Map<String, Integer> prices = new LinkedHashMap<>();
        for (int index = 0; index < values.length; index += 2) prices.put((String) values[index], (Integer) values[index + 1]);
        return Map.copyOf(prices);
    }

    private static ConfiguratorCatalogOption performanceOption(String id, String label, String family, String detail, int price, String platform) {
        return new ConfiguratorCatalogOption(id, label, family, detail, price, platform, null, null, null, null, null, null, null, List.of(), List.of());
    }
    private static ConfiguratorCatalogOption option(String id, String label, String family, String detail, int price) {
        return new ConfiguratorCatalogOption(id, label, family, detail, price, null, null, null, null, null, null, null, null, List.of(), List.of());
    }
    private static ConfiguratorCatalogOption option(String id, String label, String family, String detail, int price, String recommendedFor) {
        return new ConfiguratorCatalogOption(id, label, family, detail, price, null, null, null, null, null, null, null, recommendedFor, List.of(), List.of());
    }
    private static ConfiguratorCatalogOption option(String id, String label, String family, String detail, int price, String platform, String chipset, Boolean wifi, String formFactor) {
        return new ConfiguratorCatalogOption(id, label, family, detail, price, platform, chipset, wifi, formFactor, null, null, null, null, List.of(), List.of());
    }
    private static ConfiguratorCatalogOption option(String id, String label, String family, String detail, int price, String efficiency, int wattage, String modular) {
        return new ConfiguratorCatalogOption(id, label, family, detail, price, null, null, null, null, efficiency, wattage, modular, null, List.of(), List.of());
    }
    private static ConfiguratorCatalogOption option(String id, String label, String family, String detail, int price, List<String> formFactors) {
        return new ConfiguratorCatalogOption(id, label, family, detail, price, null, null, null, null, null, null, null, null, List.of(), formFactors);
    }
    private static ConfiguratorCatalogOption option(String id, String label, String family, String detail, int price, String recommendedFor, List<String> supportedCases) {
        return new ConfiguratorCatalogOption(id, label, family, detail, price, null, null, null, null, null, null, null, recommendedFor, supportedCases, List.of());
    }

    private ConfiguratorCatalog() { }
}
