export type MemoryOption = {
  id: string;
  label: string;
  detail: string;
  recommendedFor: string;
};

export const memoryOptions: MemoryOption[] = [
  { id: "16gb", label: "16GB DDR5", detail: "2 x 8GB / Everyday productivity and entry gaming", recommendedFor: "Essential starting point" },
  { id: "32gb", label: "32GB DDR5", detail: "2 x 16GB / Gaming, creation and multitasking", recommendedFor: "Recommended balance" },
  { id: "64gb", label: "64GB DDR5", detail: "2 x 32GB / Heavy creation, AI and production work", recommendedFor: "More headroom" },
  { id: "128gb", label: "128GB DDR5", detail: "High-capacity kit / Large datasets and specialist workloads", recommendedFor: "Specialist capacity" },
];
