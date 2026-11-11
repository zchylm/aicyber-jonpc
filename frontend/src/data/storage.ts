export type StorageOption = {
  id: string;
  label: string;
  detail: string;
  recommendedFor: string;
};

export const storageOptions: StorageOption[] = [
  { id: "1tb", label: "1TB Gen4 NVMe", detail: "Fast everyday storage for apps, games and active projects", recommendedFor: "Focused starting point" },
  { id: "2tb", label: "2TB Gen4 NVMe", detail: "More space for modern games, media libraries and project files", recommendedFor: "Recommended balance" },
  { id: "4tb", label: "4TB Gen4 NVMe", detail: "Large capacity for datasets, footage and specialist workflows", recommendedFor: "More working room" },
];
