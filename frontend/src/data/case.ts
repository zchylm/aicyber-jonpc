import compactWhite from "../assets/step07/compact-white.jpeg";
import fullBlack from "../assets/step07/full-black.jpeg";
import fullSilver from "../assets/step07/full-silver.jpeg";
import fullWhite from "../assets/step07/full-white.jpeg";
import midBlack from "../assets/step07/mid-black.jpeg";
import midRed from "../assets/step07/mid-red.jpeg";
import midWhite from "../assets/step07/mid-white.jpeg";
import type { MotherboardOption } from "./motherboard";

export type CaseColor = {
  id: string;
  label: string;
  hex: string;
  image?: string;
};

export type CaseOption = {
  id: "compact" | "mid" | "full";
  label: string;
  detail: string;
  formFactors: MotherboardOption["formFactor"][];
  price: number;
  colors: CaseColor[];
  moreColors: CaseColor[];
};

export const caseOptions: CaseOption[] = [
  {
    id: "compact",
    label: "Compact",
    detail: "Small footprint / Micro-ATX focused",
    formFactors: ["Micro-ATX"],
    price: 0,
    colors: [{ id: "white", label: "White", hex: "#eef2f0", image: compactWhite }],
    moreColors: [
      { id: "graphite", label: "Graphite", hex: "#4f5a59" },
      { id: "blue", label: "Blue", hex: "#2b70a4" },
      { id: "green", label: "Green", hex: "#4d8a68" },
      { id: "orange", label: "Orange", hex: "#d47732" },
    ],
  },
  {
    id: "mid",
    label: "Mid Tower",
    detail: "Balanced space / Everyday flexibility",
    formFactors: ["Micro-ATX", "ATX"],
    price: 80,
    colors: [
      { id: "black", label: "Black", hex: "#111718", image: midBlack },
      { id: "white", label: "White", hex: "#eef2f0", image: midWhite },
      { id: "red", label: "Red", hex: "#bb3d3d", image: midRed },
    ],
    moreColors: [
      { id: "graphite", label: "Graphite", hex: "#4f5a59" },
      { id: "blue", label: "Blue", hex: "#2b70a4" },
      { id: "green", label: "Green", hex: "#4d8a68" },
      { id: "orange", label: "Orange", hex: "#d47732" },
      { id: "purple", label: "Purple", hex: "#76528e" },
    ],
  },
  {
    id: "full",
    label: "Full Tower",
    detail: "Maximum room / Large cooling and upgrade paths",
    formFactors: ["ATX"],
    price: 180,
    colors: [
      { id: "black", label: "Black", hex: "#111718", image: fullBlack },
      { id: "silver", label: "Silver", hex: "#b9c0bd", image: fullSilver },
      { id: "white", label: "White", hex: "#eef2f0", image: fullWhite },
    ],
    moreColors: [
      { id: "graphite", label: "Graphite", hex: "#4f5a59" },
      { id: "blue", label: "Blue", hex: "#2b70a4" },
      { id: "green", label: "Green", hex: "#4d8a68" },
      { id: "orange", label: "Orange", hex: "#d47732" },
      { id: "purple", label: "Purple", hex: "#76528e" },
    ],
  },
];

export function getCompatibleCases(motherboard: MotherboardOption) {
  return caseOptions.filter((option) => option.formFactors.includes(motherboard.formFactor));
}

export function getCaseOption(id: CaseOption["id"], motherboard: MotherboardOption) {
  return getCompatibleCases(motherboard).find((option) => option.id === id) ?? getCompatibleCases(motherboard)[0];
}

export function recommendCase(motherboard: MotherboardOption) {
  return motherboard.formFactor === "Micro-ATX" ? "compact" : "mid";
}
