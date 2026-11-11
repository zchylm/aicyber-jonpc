import compactBlack from "../assets/step07/original/case-compact-black.jpg";
import compactWhite from "../assets/step07/original/case-compact-white.jpg";
import fullBlack from "../assets/step07/original/case-full-black.jpg";
import fullWhite from "../assets/step07/original/case-full-white.jpg";
import midBlack from "../assets/step07/original/case-mid-black.jpg";
import midWhite from "../assets/step07/original/case-mid-white.jpg";
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
  colors: CaseColor[];
  moreColors: CaseColor[];
};

export const caseOptions: CaseOption[] = [
  {
    id: "compact",
    label: "Compact",
    detail: "Small footprint / Micro-ATX focused",
    formFactors: ["Micro-ATX"],
    colors: [{ id: "no-preference", label: "No preference", hex: "#71807d", image: compactWhite }, { id: "white", label: "White", hex: "#eef2f0", image: compactWhite }, { id: "black", label: "Black", hex: "#111718", image: compactBlack }],
    moreColors: [],
  },
  {
    id: "mid",
    label: "Mid Tower",
    detail: "Balanced space / Everyday flexibility",
    formFactors: ["Micro-ATX", "ATX"],
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
    colors: [
      { id: "no-preference", label: "No preference", hex: "#71807d", image: fullBlack },
      { id: "black", label: "Black", hex: "#111718", image: fullBlack },
      { id: "white", label: "White", hex: "#eef2f0", image: fullWhite },
    ],
    moreColors: [],
  },
];
