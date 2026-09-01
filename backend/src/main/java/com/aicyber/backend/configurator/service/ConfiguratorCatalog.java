package com.aicyber.backend.configurator.service;

import com.aicyber.backend.configurator.dto.ConfiguratorCatalogOption;
import com.aicyber.backend.configurator.dto.ConfiguratorCatalogResponse;

import java.util.List;
import java.util.Map;

/** The backend's single source of truth for configurable component IDs and price deltas. */
public final class ConfiguratorCatalog {

    static final int SYSTEM_BASE_PRICE = 899;
    static final Map<String, Integer> CPU_PRICES = Map.of(
            "ryzen-5-7600", 299, "core-i5-14600k", 399, "ryzen-7-7700", 449,
            "ryzen-7-7800x3d", 599, "core-i7-14700k", 649, "ryzen-9-7900", 699
    );
    static final Map<String, Integer> GPU_PRICES = Map.of(
            "integrated", 0, "arc-b580", 399, "rx-7800-xt", 799,
            "rtx-4060", 499, "rtx-5070", 999, "rtx-5080", 1799
    );
    static final Map<String, Integer> MEMORY_PRICES = Map.of("16gb", -120, "32gb", 0, "64gb", 160, "128gb", 440);
    static final Map<String, Integer> STORAGE_PRICES = Map.of("512gb", -160, "1tb", -90, "2tb", 0, "4tb", 160);
    static final Map<String, Integer> MOTHERBOARD_PRICES = Map.of(
            "b650m-no-wifi", 0, "b650-wifi", 80, "b850-wifi", 180, "x870-wifi", 300, "x870e-wifi", 500,
            "b760-no-wifi", 0, "b760-wifi", 80, "z790-wifi", 260
    );
    static final Map<String, Integer> PSU_PRICES = Map.of(
            "550-standard", -90, "550-bronze", -60, "650-bronze", 0, "650-silver", 40,
            "750-gold", 100, "850-gold", 180, "1000-platinum", 350, "1200-titanium", 600
    );
    static final Map<String, Integer> CASE_PRICES = Map.of("compact", 0, "mid", 80, "full", 180);
    static final Map<String, Integer> COOLING_PRICES = Map.of("tower-air", 0, "dual-tower-air", 80, "240-liquid", 150, "360-liquid", 240);

    public static ConfiguratorCatalogResponse response() {
        return new ConfiguratorCatalogResponse(SYSTEM_BASE_PRICE, Map.of(
                "cpu", List.of(
                        performanceOption("ryzen-5-7600", "AMD Ryzen 5 7600", "AMD Ryzen", "Efficient 6-core AM5 starting point", 299, "AM5"),
                        performanceOption("core-i5-14600k", "Intel Core i5-14600K", "Intel Core", "Flexible gaming and productivity performance", 399, "LGA1700"),
                        performanceOption("ryzen-7-7700", "AMD Ryzen 7 7700", "AMD Ryzen", "Balanced 8-core performance for mixed workloads", 449, "AM5"),
                        performanceOption("ryzen-7-7800x3d", "AMD Ryzen 7 7800X3D", "AMD Ryzen", "Gaming-focused performance with strong frame consistency", 599, "AM5"),
                        performanceOption("core-i7-14700k", "Intel Core i7-14700K", "Intel Core", "High multi-core headroom for production workloads", 649, "LGA1700"),
                        performanceOption("ryzen-9-7900", "AMD Ryzen 9 7900", "AMD Ryzen", "12-core capacity for AI, rendering and workstation work", 699, "AM5")
                ),
                "gpu", List.of(
                        option("integrated", "Integrated graphics", "AMD / Intel", "Office, display and everyday productivity", 0),
                        option("arc-b580", "Intel Arc B580 12GB", "Intel Arc", "Accessible 1080p graphics and media work", 399),
                        option("rx-7800-xt", "AMD Radeon RX 7800 XT 16GB", "AMD Radeon", "Strong 1440p raster performance", 799),
                        option("rtx-4060", "NVIDIA GeForce RTX 4060 8GB", "NVIDIA GeForce RTX", "Efficient 1080p gaming and creator acceleration", 499),
                        option("rtx-5070", "NVIDIA GeForce RTX 5070 12GB", "NVIDIA GeForce RTX", "Balanced 1440p, AI and creator performance", 999),
                        option("rtx-5080", "NVIDIA GeForce RTX 5080 16GB", "NVIDIA GeForce RTX", "High-end 4K and local compute headroom", 1799)
                ),
                "memory", List.of(
                        option("16gb", "16GB DDR5", "Memory", "2 x 8GB / Everyday productivity and entry gaming", -120, "Essential starting point"),
                        option("32gb", "32GB DDR5", "Memory", "2 x 16GB / Smooth gaming and multitasking", 0, "Recommended balance"),
                        option("64gb", "64GB DDR5", "Memory", "2 x 32GB / Heavy creation, AI and production work", 160, "More headroom"),
                        option("128gb", "128GB DDR5", "Memory", "4 x 32GB / Large datasets and specialist workloads", 440, "Maximum capacity")
                ),
                "storage", List.of(
                        option("512gb", "512GB Gen4 NVMe", "Storage", "A focused drive for office apps, light gaming and everyday files", -160, "Essential capacity"),
                        option("1tb", "1TB Gen4 NVMe", "Storage", "Fast everyday storage for apps, games and active projects", -90, "Focused starting point"),
                        option("2tb", "2TB Gen4 NVMe", "Storage", "More space for modern games, media libraries and project files", 0, "Recommended balance"),
                        option("4tb", "4TB Gen4 NVMe", "Storage", "Large capacity for datasets, footage and specialist workflows", 160, "More working room")
                ),
                "motherboard", List.of(
                        option("b650m-no-wifi", "B650M Gaming", "AMD AM5", "Micro-ATX / DDR5 / No Wi-Fi", 0, "AM5", "B650", false, "Micro-ATX"),
                        option("b650-wifi", "B650 Gaming Wi-Fi", "AMD AM5", "ATX / DDR5 / Wi-Fi 6E", 80, "AM5", "B650", true, "ATX"),
                        option("b850-wifi", "B850 Gaming Wi-Fi", "AMD AM5", "ATX / DDR5 / PCIe 5.0 / Wi-Fi", 180, "AM5", "B850", true, "ATX"),
                        option("x870-wifi", "X870 Creator Wi-Fi", "AMD AM5", "ATX / DDR5 / USB4 / Wi-Fi", 300, "AM5", "X870", true, "ATX"),
                        option("x870e-wifi", "X870E Workstation Wi-Fi", "AMD AM5", "ATX / DDR5 / PCIe 5.0 / USB4", 500, "AM5", "X870E", true, "ATX"),
                        option("b760-no-wifi", "B760 Gaming", "Intel LGA1700", "ATX / DDR5 / No Wi-Fi", 0, "LGA1700", "B760", false, "ATX"),
                        option("b760-wifi", "B760 Gaming Wi-Fi", "Intel LGA1700", "ATX / DDR5 / Wi-Fi 6E", 80, "LGA1700", "B760", true, "ATX"),
                        option("z790-wifi", "Z790 Performance Wi-Fi", "Intel LGA1700", "ATX / DDR5 / Expanded I/O", 260, "LGA1700", "Z790", true, "ATX")
                ),
                "psu", List.of(
                        option("550-standard", "550W 80+ Standard", "Power", "Entry-level power for efficient systems", -90, "Standard", 550, "Non-modular"),
                        option("550-bronze", "550W 80+ Bronze", "Power", "Essential power for efficient systems", -60, "Bronze", 550, "Non-modular"),
                        option("650-bronze", "650W 80+ Bronze", "Power", "Budget-focused power for entry GPUs", 0, "Bronze", 650, "Semi-modular"),
                        option("650-silver", "650W 80+ Silver", "Power", "Improved efficiency for everyday systems", 40, "Silver", 650, "Semi-modular"),
                        option("750-gold", "750W 80+ Gold", "Power", "Balanced efficiency and upgrade room", 100, "Gold", 750, "Fully modular"),
                        option("850-gold", "850W 80+ Gold", "Power", "High-performance power with headroom", 180, "Gold", 850, "Fully modular"),
                        option("1000-platinum", "1000W 80+ Platinum", "Power", "Quiet, efficient power for demanding builds", 350, "Platinum", 1000, "Fully modular"),
                        option("1200-titanium", "1200W 80+ Titanium", "Power", "Maximum efficiency and expansion capacity", 600, "Titanium", 1200, "Fully modular")
                ),
                "case", List.of(
                        option("compact", "Compact", "Case", "Small footprint / Micro-ATX focused", 0, List.of("Micro-ATX")),
                        option("mid", "Mid Tower", "Case", "Balanced space / Everyday flexibility", 80, List.of("Micro-ATX", "ATX")),
                        option("full", "Full Tower", "Case", "Maximum room / Large cooling and upgrade paths", 180, List.of("ATX"))
                ),
                "cooling", List.of(
                        option("tower-air", "Tower air cooling", "Cooling", "Quiet, efficient and easy to maintain", 0, "Balanced everyday cooling", List.of("compact", "mid", "full")),
                        option("dual-tower-air", "Dual-tower air cooling", "Cooling", "More thermal headroom for long sessions", 80, "Higher sustained workloads", List.of("mid", "full")),
                        option("240-liquid", "240mm liquid cooling", "Cooling", "Low temperatures with a clean internal layout", 150, "Performance-focused builds", List.of("mid", "full")),
                        option("360-liquid", "360mm liquid cooling", "Cooling", "Maximum cooling surface for demanding systems", 240, "High-end sustained workloads", List.of("full"))
                )
        ));
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
