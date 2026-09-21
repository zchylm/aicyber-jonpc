import compactWhite from "../assets/step07/compact-white.jpeg";
import fullBlack from "../assets/step07/full-black.jpeg";
import fullWhite from "../assets/step07/full-white.jpeg";
import midBlack from "../assets/step07/mid-black.jpeg";
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
    price: 140,
    colors: [{ id: "no-preference", label: "No preference", hex: "#71807d", image: compactWhite }, { id: "white", label: "White", hex: "#eef2f0", image: compactWhite }, { id: "black", label: "Black", hex: "#111718" }],
    moreColors: [],
  },
  {
    id: "mid",
    label: "Mid Tower",
    detail: "Balanced space / Everyday flexibility",
    formFactors: ["Micro-ATX", "ATX"],
    price: 180,
    colors: [
      { id: "no-preference", label: "No preference", hex: "#71807d", image: midBlack },
      { id: "black", label: "Black", hex: "#111718", image: midBlack },
      { id: "white", label: "White", hex: "#eef2f0", image: midWhite },
    ],
    moreColors: [],
  },
  {
    id: "full",
    label: "Full Tower",
    detail: "Maximum room / Large cooling and upgrade paths",
    formFactors: ["ATX"],
    price: 260,
    colors: [
      { id: "no-preference", label: "No preference", hex: "#71807d", image: fullBlack },
      { id: "black", label: "Black", hex: "#111718", image: fullBlack },
      { id: "white", label: "White", hex: "#eef2f0", image: fullWhite },
    ],
    moreColors: [],
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
