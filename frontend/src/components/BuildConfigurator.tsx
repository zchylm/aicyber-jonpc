import { useEffect, useMemo, useRef, useState, type FormEvent } from "react";
import { deleteSavedBuild, fetchConfiguratorCatalog, fetchConfiguratorCompatibility, fetchConfiguratorQuote, fetchConfiguratorRecommendation, saveBuild, submitConfiguratorBuild, type ConfiguratorBuildResponse, type ConfiguratorCatalog, type ConfiguratorCatalogOption, type ConfiguratorCompatibility, type ConfiguratorQuote, type ConfiguratorQuoteRequest, type ConfiguratorRecommendation } from "../api/configurator";
import { authTokenKey, type AuthUser } from "../api/auth";
import { configuratorDirections, configuratorQuestions, type DirectionId } from "../data/configurator";
import { getBudgetRange, type PerformanceSelection } from "../data/performance";
import compactWhite from "../assets/step07/compact-white.jpeg";
import fullBlack from "../assets/step07/full-black.jpeg";
import fullWhite from "../assets/step07/full-white.jpeg";
import midBlack from "../assets/recommendation-slides/custom-01.jpeg";
import midWhite from "../assets/recommendation-slides/custom-02.jpeg";
import coolingAir from "../assets/step07/cooling-air.jpeg";
import coolingLiquid from "../assets/step07/cooling-liquid.jpeg";
import coolingLiquidRed from "../assets/step07/cooling-liquid-red.jpeg";

function defaultsFor(direction: DirectionId) {
  return Object.fromEntries(
    configuratorQuestions[direction].map((question) => [
      question.id,
      question.options.find((option) => option.recommended)?.label ?? question.options[0].label,
    ]),
  );
}

type CaseId = "compact" | "mid" | "full";

const emptyOption: ConfiguratorCatalogOption = {
  id: "", label: "Loading…", family: "", detail: "", price: 0,
  supportedCases: [], formFactors: [],
};

const caseColours: Record<CaseId, Array<{ id: string; label: string; hex: string; image?: string }>> = {
  compact: [
    { id: "no-preference", label: "No preference", hex: "#71807d", image: compactWhite },
    { id: "white", label: "White", hex: "#eef2f0", image: compactWhite },
    { id: "black", label: "Black", hex: "#111718" },
  ],
  mid: [
    { id: "no-preference", label: "No preference", hex: "#71807d", image: midBlack },
    { id: "black", label: "Black", hex: "#111718", image: midBlack },
    { id: "white", label: "White", hex: "#eef2f0", image: midWhite },
  ],
  full: [
    { id: "no-preference", label: "No preference", hex: "#71807d", image: fullBlack },
    { id: "black", label: "Black", hex: "#111718", image: fullBlack },
    { id: "white", label: "White", hex: "#eef2f0", image: fullWhite },
  ],
};

const coolingImages: Record<string, string> = {
  "tower-air": coolingAir,
  "dual-tower-air": coolingAir,
  "240-liquid": coolingLiquid,
  "360-liquid": coolingLiquidRed,
};

type BuildConfiguratorProps = {
  user: AuthUser | null;
};


function BuildConfigurator({ user }: BuildConfiguratorProps) {
  const [direction, setDirection] = useState<DirectionId>("gaming");
  const [directionChosen, setDirectionChosen] = useState(false);
  const [answers, setAnswers] = useState<Record<string, string>>(defaultsFor("gaming"));
  const [isReady, setIsReady] = useState(false);
  const [performanceSelection, setPerformanceSelection] = useState<PerformanceSelection | null>(null);
  const [isMemoryReady, setIsMemoryReady] = useState(false);
  const [memorySelection, setMemorySelection] = useState<string | null>(null);
  const [isStorageReady, setIsStorageReady] = useState(false);
  const [storageSelection, setStorageSelection] = useState<string | null>(null);
  const [isStyleReady, setIsStyleReady] = useState(false);
  const [coolingSelection, setCoolingSelection] = useState<string | null>(null);
  const [caseSelection, setCaseSelection] = useState<CaseId | null>(null);
  const [caseColorSelection, setCaseColorSelection] = useState<string | null>(null);
  const [isReviewReady, setIsReviewReady] = useState(false);
  const [requestSubmitted, setRequestSubmitted] = useState(false);
  const [requestSubmitting, setRequestSubmitting] = useState(false);
  const [requestError, setRequestError] = useState<string | null>(null);
  const [submittedBuild, setSubmittedBuild] = useState<ConfiguratorBuildResponse | null>(null);
  const [requestOpen, setRequestOpen] = useState(false);
  const resumeRequestAfterLogin = useRef(false);
  const [saveState, setSaveState] = useState<"idle" | "saving" | "saved" | "error">("idle");
  const [savedBuildId, setSavedBuildId] = useState<string | null>(null);
  const [backendQuote, setBackendQuote] = useState<ConfiguratorQuote | null>(null);
  const [backendRecommendation, setBackendRecommendation] = useState<ConfiguratorRecommendation | null>(null);
  const [backendCompatibility, setBackendCompatibility] = useState<ConfiguratorCompatibility | null>(null);
  const [backendCatalog, setBackendCatalog] = useState<ConfiguratorCatalog | null>(null);
  const [configuratorError, setConfiguratorError] = useState<string | null>(null);
  const [recommendationLoading, setRecommendationLoading] = useState(false);
  const [transitionLoading, setTransitionLoading] = useState<"platform" | "style" | "review" | null>(null);
  const [transitionError, setTransitionError] = useState<{ step: "platform" | "style" | "review"; message: string } | null>(null);
  const [buildOrigin, setBuildOrigin] = useState<{ name: string; modified: boolean } | null>(null);
  const requestReference = submittedBuild?.requestReference ?? `JON-${direction.slice(0, 3).toUpperCase()}-DEMO`;
  const questions = useMemo(() => configuratorQuestions[direction], [direction]);
  useEffect(() => {
    const controller = new AbortController();
    fetchConfiguratorCatalog(controller.signal)
      .then(setBackendCatalog)
      .catch((error: unknown) => {
        if (error instanceof DOMException && error.name === "AbortError") return;
        setBackendCatalog(null);
        setConfiguratorError("The configurator is temporarily unavailable. Please try again shortly.");
      });
    return () => controller.abort();
  }, []);

  useEffect(() => {
    const sources = new Set([
      ...Object.values(caseColours).flatMap((colours) => colours.flatMap((colour) => colour.image ? [colour.image] : [])),
      ...Object.values(coolingImages),
    ]);
    sources.forEach((source) => {
      const image = new Image();
      image.src = source;
    });
  }, []);

  useEffect(() => {
    const resumeRequest = () => {
      if (!resumeRequestAfterLogin.current) return;
      resumeRequestAfterLogin.current = false;
      setRequestSubmitted(false);
      setSubmittedBuild(null);
      setRequestError(null);
      setRequestOpen(true);
    };
    window.addEventListener("jonpc:authenticated", resumeRequest);
    return () => window.removeEventListener("jonpc:authenticated", resumeRequest);
  }, []);

  function openBuildRequest() {
    if (!user) {
      resumeRequestAfterLogin.current = true;
      window.dispatchEvent(new CustomEvent("jonpc:open-account"));
      return;
    }
    setRequestOpen(true);
    setRequestSubmitted(false);
    setSubmittedBuild(null);
    setRequestError(null);
  }

  const optionsFor = (category: string) => backendCatalog?.options[category] ?? [];
  const optionFor = (category: string, id: string | null | undefined) => optionsFor(category).find((option) => option.id === id) ?? optionsFor(category)[0] ?? emptyOption;
  const catalogGpuOptions = optionsFor("gpu");
  const catalogCpuOptions = optionsFor("cpu");
  const catalogMemoryOptions = optionsFor("memory");
  const catalogStorageOptions = optionsFor("storage");
  const budgetRange = useMemo(() => getBudgetRange(direction, answers), [direction, answers]);
  const directionLabel = configuratorDirections.find((item) => item.id === direction)?.label ?? direction;
  const recommendedPerformanceSelection: PerformanceSelection = {
    cpuId: backendRecommendation?.cpuId ?? catalogCpuOptions[0]?.id ?? "",
    gpuId: backendRecommendation?.gpuId ?? catalogGpuOptions[0]?.id ?? "",
  };
  const activePerformance: PerformanceSelection = performanceSelection ?? recommendedPerformanceSelection;
  const performanceOptions = {
    cpu: optionFor("cpu", activePerformance.cpuId),
    gpu: optionFor("gpu", activePerformance.gpuId),
  };
  const recommendedCorePrice = backendCatalog
    ? backendCatalog.systemBasePrice + optionFor("cpu", recommendedPerformanceSelection.cpuId).price + optionFor("gpu", recommendedPerformanceSelection.gpuId).price
    : 0;
  const estimatedPrice = backendCatalog
    ? backendCatalog.systemBasePrice + performanceOptions.cpu.price + performanceOptions.gpu.price
    : 0;
  const recommendedMemory = backendRecommendation?.memoryId ?? catalogMemoryOptions[0]?.id ?? "";
  const activeMemory = memorySelection ?? recommendedMemory;
  const memoryOption = optionFor("memory", activeMemory);
  const recommendedMemoryOption = optionFor("memory", recommendedMemory);
  const memoryDelta = memoryOption.price - recommendedMemoryOption.price;
  const isRecommendedMemory = activeMemory === recommendedMemory;
  const estimatedBuildPrice = estimatedPrice + memoryOption.price;
  const recommendedStorage = backendRecommendation?.storageId ?? catalogStorageOptions[0]?.id ?? "";
  const activeStorage = storageSelection ?? recommendedStorage;
  const storageOption = optionFor("storage", activeStorage);
  const recommendedStorageOption = optionFor("storage", recommendedStorage);
  const storageDelta = storageOption.price - recommendedStorageOption.price;
  const isRecommendedStorage = activeStorage === recommendedStorage;
  const estimatedFullPrice = estimatedBuildPrice + storageOption.price;
  const recommendedMotherboard = backendRecommendation?.motherboardId ?? optionsFor("motherboard")[0]?.id ?? "";
  const [isPlatformReady, setIsPlatformReady] = useState(false);
  const [motherboardSelection, setMotherboardSelection] = useState<string | null>(null);
  const [psuSelection, setPsuSelection] = useState<string | null>(null);
  useEffect(() => {
    const handleLoadBuild = (event: Event) => {
      const build = (event as CustomEvent<{ id?: string; name?: string; source?: "preset"; direction: string; configuration: ConfiguratorQuoteRequest }>).detail;
      if (!build?.configuration || !configuratorDirections.some((item) => item.id === build.direction)) return;
      const configuration = build.configuration;
      const nextDirection = build.direction as DirectionId;
      setDirection(nextDirection);
      setDirectionChosen(true);
      setAnswers(configuration.answers ?? defaultsFor(nextDirection));
      setPerformanceSelection({ cpuId: configuration.cpuId, gpuId: configuration.gpuId });
      setMemorySelection(configuration.memoryId);
      setStorageSelection(configuration.storageId);
      setMotherboardSelection(configuration.motherboardId);
      setPsuSelection(configuration.psuId);
      setCaseSelection(configuration.caseId as CaseId);
      setCaseColorSelection(configuration.caseColorId ?? null);
      setCoolingSelection(configuration.coolingId);
      setBackendRecommendation({
        cpuId: configuration.recommendedCpuId,
        gpuId: configuration.recommendedGpuId,
        memoryId: configuration.recommendedMemoryId,
        storageId: configuration.recommendedStorageId,
        motherboardId: configuration.recommendedMotherboardId,
        psuId: configuration.recommendedPsuId,
        caseId: configuration.recommendedCaseId as CaseId,
        coolingId: configuration.recommendedCoolingId,
      });
      setBuildOrigin(build.source === "preset" && build.name ? { name: build.name, modified: false } : null);
      setIsReady(true);
      setIsMemoryReady(true);
      setIsStorageReady(true);
      setIsPlatformReady(true);
      setIsStyleReady(true);
      setIsReviewReady(true);
      setRequestSubmitted(false);
      setRequestOpen(false);
      setSavedBuildId(build.id ?? null);
      setSaveState(build.id ? "saved" : "idle");
      window.requestAnimationFrame(() => {
        document.getElementById("build-review")?.scrollIntoView({ behavior: "smooth", block: "start" });
      });
    };
    window.addEventListener("jonpc:load-build", handleLoadBuild);
    return () => window.removeEventListener("jonpc:load-build", handleLoadBuild);
  }, []);
  const compatibleMotherboardDefault = backendCompatibility && !backendCompatibility.motherboardIds.includes(recommendedMotherboard)
    ? backendCompatibility.motherboardIds[0] : recommendedMotherboard;
  const selectedMotherboardId = motherboardSelection && (!backendCompatibility || backendCompatibility.motherboardIds.includes(motherboardSelection))
    ? motherboardSelection : compatibleMotherboardDefault;
  const activeMotherboard = optionFor("motherboard", selectedMotherboardId);
  const recommendedPsuId = backendRecommendation?.psuId ?? optionsFor("psu")[0]?.id ?? "";
  const compatiblePsuDefault = backendCompatibility && !backendCompatibility.psuIds.includes(recommendedPsuId)
    ? backendCompatibility.psuIds[0] : recommendedPsuId;
  const selectedPsuId = psuSelection && (!backendCompatibility || backendCompatibility.psuIds.includes(psuSelection))
    ? psuSelection : compatiblePsuDefault;
  const activePsu = optionFor("psu", selectedPsuId);
  const recommendedMotherboardOption = optionFor("motherboard", recommendedMotherboard);
  const recommendedPsuOption = optionFor("psu", recommendedPsuId);
  const estimatedCompletePrice = estimatedFullPrice + activeMotherboard.price + activePsu.price;
  const recommendedCase = (backendRecommendation?.caseId ?? optionsFor("case")[0]?.id ?? "mid") as CaseId;
  const compatibleCaseDefault = backendCompatibility && !backendCompatibility.caseIds.includes(recommendedCase)
    ? backendCompatibility.caseIds[0] : recommendedCase;
  const selectedCaseId = caseSelection && (!backendCompatibility || backendCompatibility.caseIds.includes(caseSelection))
    ? caseSelection : compatibleCaseDefault;
  const activeCase = optionFor("case", selectedCaseId);
  const activeCaseId = (activeCase.id || "mid") as CaseId;
  const visibleCaseColors = caseColours[activeCaseId];
  const activeCaseColor = visibleCaseColors.find((color) => color.id === caseColorSelection) ?? visibleCaseColors[0];
  const casePreviewImage = activeCaseColor.image ?? visibleCaseColors.find((color) => color.image)?.image ?? midBlack;
  const casePreviewPending = !activeCaseColor.image;
  const recommendedCooling = backendRecommendation?.coolingId ?? optionsFor("cooling")[0]?.id ?? "";
  const compatibleCoolingDefault = backendCompatibility && !backendCompatibility.coolingIds.includes(recommendedCooling)
    ? backendCompatibility.coolingIds[0] : recommendedCooling;
  const selectedCoolingId = coolingSelection && (!backendCompatibility || backendCompatibility.coolingIds.includes(coolingSelection))
    ? coolingSelection : compatibleCoolingDefault;
  const activeCooling = optionFor("cooling", selectedCoolingId);
  const recommendedCaseOption = optionFor("case", recommendedCase);
  const recommendedCoolingOption = optionFor("cooling", recommendedCooling);
  const estimatedFinalPrice = estimatedCompletePrice + activeCase.price + activeCooling.price;
  const localRecommendedTotal = recommendedCorePrice + recommendedMemoryOption.price + recommendedStorageOption.price
    + recommendedMotherboardOption.price + recommendedPsuOption.price + recommendedCaseOption.price + recommendedCoolingOption.price;
  const selectedAdjustments = estimatedFinalPrice - localRecommendedTotal;
  const budgetStatus = estimatedFinalPrice > budgetRange.max ? "Above selected budget" : estimatedFinalPrice < budgetRange.min ? "Below selected range" : "Within selected budget";
  const serializedAnswers = JSON.stringify(answers);

  const currentQuoteRequest: ConfiguratorQuoteRequest = {
      direction,
      answers: JSON.parse(serializedAnswers) as Record<string, string>,
      recommendedCpuId: recommendedPerformanceSelection.cpuId,
      recommendedGpuId: recommendedPerformanceSelection.gpuId,
      recommendedMemoryId: recommendedMemory,
      recommendedStorageId: recommendedStorage,
      recommendedMotherboardId: recommendedMotherboard,
      recommendedPsuId,
      recommendedCaseId: recommendedCase,
      recommendedCoolingId: recommendedCooling,
      cpuId: performanceOptions.cpu.id,
      gpuId: performanceOptions.gpu.id,
      memoryId: activeMemory,
      storageId: activeStorage,
      motherboardId: activeMotherboard.id,
      psuId: activePsu.id,
      caseId: activeCase.id as CaseId,
      coolingId: activeCooling.id,
      caseColorId: activeCaseColor.id,
  };
  const serializedQuoteRequest = JSON.stringify(currentQuoteRequest);

  function startNewBuild(nextDirection: DirectionId = "gaming", chosen = false) {
    setDirection(nextDirection);
    setDirectionChosen(chosen);
    setAnswers(defaultsFor(nextDirection));
    setIsReady(false);
    setPerformanceSelection(null);
    setIsMemoryReady(false);
    setMemorySelection(null);
    setIsStorageReady(false);
    setStorageSelection(null);
    setIsPlatformReady(false);
    setMotherboardSelection(null);
    setPsuSelection(null);
    setIsStyleReady(false);
    setCoolingSelection(null);
    setCaseSelection(null);
    setCaseColorSelection(null);
    setIsReviewReady(false);
    setRequestSubmitted(false);
    setRequestOpen(false);
    setRequestError(null);
    setSubmittedBuild(null);
    setBackendRecommendation(null);
    setBackendQuote(null);
    setBackendCompatibility(null);
    setSavedBuildId(null);
    setSaveState("idle");
    setBuildOrigin(null);
    window.requestAnimationFrame(() => document.getElementById("build")?.scrollIntoView({ behavior: "smooth", block: "start" }));
  }

  useEffect(() => {
    const handleStartNewBuild = () => startNewBuild();
    const handleStartDirectedBuild = (event: Event) => {
      const nextDirection = (event as CustomEvent<{ direction?: DirectionId }>).detail?.direction;
      startNewBuild(nextDirection && configuratorDirections.some((item) => item.id === nextDirection) ? nextDirection : "gaming", true);
    };
    window.addEventListener("jonpc:start-new-build", handleStartNewBuild);
    window.addEventListener("jonpc:start-directed-build", handleStartDirectedBuild);
    return () => {
      window.removeEventListener("jonpc:start-new-build", handleStartNewBuild);
      window.removeEventListener("jonpc:start-directed-build", handleStartDirectedBuild);
    };
  }, []);

  function markPresetModified() {
    setBuildOrigin((current) => current ? { ...current, modified: true } : null);
  }

  async function saveCurrentBuild() {
    const token = window.localStorage.getItem(authTokenKey);
    if (!token) {
      setSaveState("error");
      return;
    }
    if (saveState === "saved") return;
    setSaveState("saving");
    try {
      const savedBuild = await saveBuild(token, {
        name: buildOrigin ? `${buildOrigin.modified ? "Customised from " : ""}${buildOrigin.name}` : `${directionLabel} / ${performanceOptions.gpu.label}`,
        direction,
        budgetRange: budgetRange.label,
        estimatedPrice: reviewPrice,
        recommendedBaseline: reviewBaseline,
        selectedAdjustments: reviewAdjustments,
        configuration: currentQuoteRequest,
      }, savedBuildId ?? undefined);
      setSavedBuildId(savedBuild.id);
      setSaveState("saved");
      window.dispatchEvent(new CustomEvent("jonpc:build-saved"));
    } catch {
      setSaveState("error");
    }
  }

  useEffect(() => {
    if (!isReady) return;

    const controller = new AbortController();
    fetchConfiguratorQuote(JSON.parse(serializedQuoteRequest) as ConfiguratorQuoteRequest, controller.signal)
      .then(setBackendQuote)
      .catch((error: unknown) => {
        if (error instanceof DOMException && error.name === "AbortError") return;
        setBackendQuote(null);
      });

    return () => controller.abort();
  }, [
    isReady,
    serializedQuoteRequest,
  ]);

  async function submitBuildRequest(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setRequestSubmitting(true);
    setRequestError(null);
    const form = new FormData(event.currentTarget);
    const token = window.localStorage.getItem(authTokenKey);
    try {
      const response = await submitConfiguratorBuild({
        name: String(form.get("name") ?? ""),
        email: String(form.get("email") ?? ""),
        phone: String(form.get("phone") ?? ""),
        location: String(form.get("location") ?? ""),
        notes: String(form.get("notes") ?? ""),
        contact: form.get("contact") === "on",
        configuration: currentQuoteRequest,
      }, token ?? undefined);
      setSubmittedBuild(response);
      setRequestSubmitted(true);
      if (token && savedBuildId) {
        await deleteSavedBuild(token, savedBuildId).catch(() => undefined);
        setSavedBuildId(null);
        setSaveState("idle");
        window.dispatchEvent(new CustomEvent("jonpc:build-saved"));
      }
      window.dispatchEvent(new CustomEvent("jonpc:order-requested"));
    } catch (error) {
      setRequestError(error instanceof Error ? error.message : "Unable to submit this build request.");
    } finally {
      setRequestSubmitting(false);
    }
  }

  useEffect(() => {
    if (!isReady) return;

    const controller = new AbortController();
    fetchConfiguratorCompatibility({
      cpuId: performanceOptions.cpu.id,
      gpuId: performanceOptions.gpu.id,
      motherboardId: activeMotherboard.id,
      psuId: activePsu.id,
      caseId: activeCase.id as CaseId,
      coolingId: activeCooling.id,
    }, controller.signal)
      .then(setBackendCompatibility)
      .catch((error: unknown) => {
        if (error instanceof DOMException && error.name === "AbortError") return;
        setBackendCompatibility(null);
      });

    return () => controller.abort();
  }, [
    isReady,
    performanceOptions.cpu.id,
    performanceOptions.gpu.id,
    activeMotherboard.id,
    activePsu.id,
    activeCase.id,
    activeCooling.id,
  ]);

  const reviewPrice = backendQuote?.estimatedTotal ?? estimatedFinalPrice;
  const reviewAdjustments = backendQuote?.selectedAdjustments ?? selectedAdjustments;
  const reviewBaseline = backendQuote?.recommendedBaseline ?? localRecommendedTotal;
  const backendValidation = backendQuote?.validation ?? [];
  const catalogMotherboardOptions = optionsFor("motherboard").filter((option) => !backendCompatibility || backendCompatibility.motherboardIds.includes(option.id));
  const catalogPsuOptions = optionsFor("psu").filter((option) => !backendCompatibility || backendCompatibility.psuIds.includes(option.id));
  const catalogCaseOptions = optionsFor("case").filter((option) => !backendCompatibility || backendCompatibility.caseIds.includes(option.id as CaseId));
  const catalogCoolingOptions = optionsFor("cooling").filter((option) => !backendCompatibility || backendCompatibility.coolingIds.includes(option.id));

  function chooseDirection(nextDirection: DirectionId) {
    setBuildOrigin(null);
    setDirection(nextDirection);
    setDirectionChosen(true);
    setAnswers(defaultsFor(nextDirection));
    setIsReady(false);
    setPerformanceSelection(null);
    setIsMemoryReady(false);
    setMemorySelection(null);
    setIsStorageReady(false);
    setStorageSelection(null);
    setIsPlatformReady(false);
    setMotherboardSelection(null);
    setPsuSelection(null);
    setIsStyleReady(false);
    setCoolingSelection(null);
    setCaseSelection(null);
    setCaseColorSelection(null);
    setIsReviewReady(false);
    setRequestSubmitted(false);
    setBackendRecommendation(null);
    setBackendQuote(null);
    setBackendCompatibility(null);
    setSaveState("idle");
    window.setTimeout(() => document.querySelector(".configurator-needs")?.scrollIntoView({ behavior: "smooth", block: "start" }), 80);
  }

  function chooseAnswer(questionId: string, value: string) {
    markPresetModified();
    setAnswers((current) => ({ ...current, [questionId]: value }));
    setIsReady(false);
    setPerformanceSelection(null);
    setIsMemoryReady(false);
    setMemorySelection(null);
    setIsStorageReady(false);
    setStorageSelection(null);
    setIsPlatformReady(false);
    setMotherboardSelection(null);
    setPsuSelection(null);
    setIsStyleReady(false);
    setCoolingSelection(null);
    setCaseSelection(null);
    setCaseColorSelection(null);
    setIsReviewReady(false);
    setRequestSubmitted(false);
    setBackendRecommendation(null);
    setBackendQuote(null);
    setBackendCompatibility(null);
    setSaveState("idle");
  }

  async function continueToRecommendation() {
    if (!backendCatalog) return;
    setRecommendationLoading(true);
    setConfiguratorError(null);
    try {
      const recommendation = await fetchConfiguratorRecommendation(direction, answers);
      setBackendRecommendation(recommendation);
      setPerformanceSelection({ cpuId: recommendation.cpuId, gpuId: recommendation.gpuId });
      setIsReady(true);
    } catch {
      setBackendRecommendation(null);
      setConfiguratorError("We could not build a recommendation right now. Please try again.");
    } finally {
      setRecommendationLoading(false);
      setSaveState("idle");
    }
  }

  function choosePerformance(type: "cpuId" | "gpuId", id: string) {
    markPresetModified();
    setPerformanceSelection((current) => ({ ...(current ?? recommendedPerformanceSelection), [type]: id }));
    setIsMemoryReady(false);
    setMemorySelection(null);
    setIsStorageReady(false);
    setStorageSelection(null);
    setIsPlatformReady(false);
    setMotherboardSelection(null);
    setPsuSelection(null);
    setIsStyleReady(false);
    setCoolingSelection(null);
    setCaseSelection(null);
    setCaseColorSelection(null);
    setIsReviewReady(false);
    setRequestSubmitted(false);
    setBackendCompatibility(null);
    setSaveState("idle");
  }

  function continueToMemory() {
    setMemorySelection(recommendedMemory);
    setIsMemoryReady(true);
    setSaveState("idle");
  }

  function chooseMemory(id: string) {
    markPresetModified();
    setMemorySelection(id);
    setIsStorageReady(false);
    setStorageSelection(null);
    setIsPlatformReady(false);
    setMotherboardSelection(null);
    setPsuSelection(null);
    setIsStyleReady(false);
    setCoolingSelection(null);
    setCaseSelection(null);
    setCaseColorSelection(null);
    setIsReviewReady(false);
    setRequestSubmitted(false);
    setSaveState("idle");
  }

  function continueToStorage() {
    setStorageSelection(recommendedStorage);
    setIsStorageReady(true);
    setSaveState("idle");
  }

  async function continueToPlatform() {
    setTransitionLoading("platform");
    setTransitionError(null);
    try {
      const compatibility = await fetchConfiguratorCompatibility({
        cpuId: performanceOptions.cpu.id,
        gpuId: performanceOptions.gpu.id,
        motherboardId: activeMotherboard.id,
        psuId: activePsu.id,
        caseId: activeCase.id as CaseId,
        coolingId: activeCooling.id,
      });
      const nextMotherboard = compatibility.motherboardIds.includes(recommendedMotherboard)
        ? recommendedMotherboard : compatibility.motherboardIds[0];
      const nextPsu = compatibility.psuIds.includes(recommendedPsuId)
        ? recommendedPsuId : compatibility.psuIds[0];
      if (!nextMotherboard || !nextPsu) throw new Error("No compatible platform options are available.");
      setBackendCompatibility(compatibility);
      setMotherboardSelection(nextMotherboard);
      setPsuSelection(nextPsu);
      setIsPlatformReady(true);
      setIsStyleReady(false);
      setCoolingSelection(null);
      setCaseSelection(null);
      setCaseColorSelection(null);
      setSaveState("idle");
    } catch {
      setTransitionError({ step: "platform", message: "We could not verify the platform right now. Please try again." });
    } finally {
      setTransitionLoading(null);
    }
  }

  function chooseStorage(id: string) {
    markPresetModified();
    setStorageSelection(id);
    setIsPlatformReady(false);
    setMotherboardSelection(null);
    setPsuSelection(null);
    setIsStyleReady(false);
    setCoolingSelection(null);
    setCaseSelection(null);
    setCaseColorSelection(null);
    setIsReviewReady(false);
    setRequestSubmitted(false);
    setSaveState("idle");
  }

  function chooseMotherboard(id: string) {
    markPresetModified();
    setMotherboardSelection(id);
    setIsStyleReady(false);
    setCoolingSelection(null);
    setCaseSelection(null);
    setCaseColorSelection(null);
    setIsReviewReady(false);
    setRequestSubmitted(false);
    setBackendCompatibility(null);
    setSaveState("idle");
  }

  function choosePsu(id: string) {
    markPresetModified();
    setPsuSelection(id);
    setIsStyleReady(false);
    setCoolingSelection(null);
    setCaseSelection(null);
    setCaseColorSelection(null);
    setIsReviewReady(false);
    setRequestSubmitted(false);
    setBackendCompatibility(null);
    setSaveState("idle");
  }

  async function continueToStyle() {
    setTransitionLoading("style");
    setTransitionError(null);
    try {
      let compatibility = await fetchConfiguratorCompatibility({
        cpuId: performanceOptions.cpu.id,
        gpuId: performanceOptions.gpu.id,
        motherboardId: activeMotherboard.id,
        psuId: activePsu.id,
        caseId: activeCase.id as CaseId,
        coolingId: activeCooling.id,
      });
      const nextCase = compatibility.caseIds.includes(recommendedCase)
        ? recommendedCase : compatibility.caseIds[0];
      if (!nextCase) throw new Error("No compatible case options are available.");
      if (nextCase !== activeCase.id) {
        compatibility = await fetchConfiguratorCompatibility({
          cpuId: performanceOptions.cpu.id,
          gpuId: performanceOptions.gpu.id,
          motherboardId: activeMotherboard.id,
          psuId: activePsu.id,
          caseId: nextCase,
          coolingId: activeCooling.id,
        });
      }
      const nextCooling = compatibility.coolingIds.includes(recommendedCooling)
        ? recommendedCooling : compatibility.coolingIds[0];
      if (!nextCooling) throw new Error("No compatible cooling options are available.");
      setBackendCompatibility(compatibility);
      setCoolingSelection(nextCooling);
      setCaseSelection(nextCase);
      setCaseColorSelection(null);
      setIsStyleReady(true);
      setIsReviewReady(false);
      setRequestSubmitted(false);
      setSaveState("idle");
    } catch {
      setTransitionError({ step: "style", message: "We could not verify cooling and case compatibility. Please try again." });
    } finally {
      setTransitionLoading(null);
    }
  }

  function chooseCase(id: CaseId) {
    markPresetModified();
    setCaseSelection(id);
    setCaseColorSelection(null);
    setIsReviewReady(false);
    setRequestSubmitted(false);
    setSaveState("idle");
  }

  function chooseCaseColor(id: string) {
    markPresetModified();
    setCaseColorSelection(id);
    setIsReviewReady(false);
    setRequestSubmitted(false);
    setSaveState("idle");
  }

  function chooseCooling(id: string) {
    markPresetModified();
    setCoolingSelection(id);
    setIsReviewReady(false);
    setRequestSubmitted(false);
    setSaveState("idle");
  }

  async function continueToReview() {
    setTransitionLoading("review");
    setTransitionError(null);
    try {
      const quote = await fetchConfiguratorQuote(currentQuoteRequest);
      setBackendQuote(quote);
      if (!quote.compatible) {
        setTransitionError({ step: "review", message: quote.validation[0] ?? "Please review the selected parts before continuing." });
        return;
      }
      setIsReviewReady(true);
      setRequestSubmitted(false);
      setRequestOpen(false);
    } catch {
      setTransitionError({ step: "review", message: "We could not verify this build right now. Please try again." });
    } finally {
      setTransitionLoading(null);
    }
  }

  function editFromReview(step: "performance" | "memory" | "storage" | "platform" | "style") {
    setIsReviewReady(false);
    setRequestSubmitted(false);
    if (step === "performance") {
      setIsMemoryReady(false);
      setIsStorageReady(false);
      setIsPlatformReady(false);
      setIsStyleReady(false);
    } else if (step === "memory") {
      setIsMemoryReady(true);
      setIsStorageReady(false);
      setIsPlatformReady(false);
      setIsStyleReady(false);
    } else if (step === "storage") {
      setIsStorageReady(true);
      setIsPlatformReady(false);
      setIsStyleReady(false);
    } else if (step === "platform") {
      setIsPlatformReady(true);
      setIsStyleReady(false);
    } else {
      setIsStyleReady(true);
    }
  }

  return (
    <section className="configurator-section" id="build" aria-labelledby="configurator-title">
      <div className="configurator-heading">
        <div>
          <h2 id="configurator-title">Intelligent custom<br /><em>builds.</em></h2>
        </div>
        <p>Tell us how you work, play or create. JON. PC recommends a balanced starting point, then you customise the details.</p>
      </div>

      <div className="configurator-steps" aria-label="Configurator progress">
        <span className="configurator-step-active"><b>01</b> Direction</span>
        <span className={directionChosen ? "configurator-step-active" : ""}><b>02</b> What matters</span>
        <span className={isReady ? "configurator-step-active" : ""}><b>03</b> Core performance</span>
        <span className={isMemoryReady ? "configurator-step-active" : ""}><b>04</b> Memory</span>
        <span className={isStorageReady ? "configurator-step-active" : ""}><b>05</b> Storage</span>
        <span className={isPlatformReady ? "configurator-step-active" : ""}><b>06</b> Platform + Power</span>
        <span className={isStyleReady ? "configurator-step-active" : ""}><b>07</b> Cooling + Case</span>
        <span className={isReviewReady ? "configurator-step-active" : ""}><b>08</b> Review</span>
      </div>

      <div className={directionChosen ? "configurator-layout" : "configurator-layout configurator-layout-start"}>
        <div className="configurator-main">
          <div className="configurator-block">
            <div className="configurator-block-heading">
              <span className="section-kicker">Step 01</span>
              <h3>What are you building for?</h3>
              <p>This choice shapes the recommendations that follow.</p>
            </div>
            <div className="direction-grid">
              {configuratorDirections.map((item, index) => {
                const selected = directionChosen && item.id === direction;
                return (
                  <button className={selected ? "direction-card direction-card-active" : "direction-card"} type="button" key={item.id} onClick={() => chooseDirection(item.id)} aria-pressed={selected}>
                    <span className="direction-number">0{index + 1}</span>
                    <strong>{item.label}</strong>
                    <span>{item.detail}</span>
                    <i aria-hidden="true">↗</i>
                  </button>
                );
              })}
            </div>
          </div>

          {directionChosen && <div className="configurator-block configurator-needs">
            <div className="configurator-block-heading">
              <span className="section-kicker">Step 02 / {directionLabel}</span>
              <h3>Tell us what matters.</h3>
              <p>The questions change with your direction, so your recommendation stays relevant.</p>
            </div>
            <div className="question-grid">
              {questions.map((question) => (
                <div className="question-card" key={question.id}>
                  <div className="question-heading">
                    <strong>{question.label}</strong>
                    <span>{question.description}</span>
                  </div>
                  <div className="question-options">
                    {question.options.map((option) => {
                      const selected = answers[question.id] === option.label;
                      return (
                        <button className={selected ? "question-option question-option-selected" : "question-option"} type="button" key={option.label} onClick={() => chooseAnswer(question.id, option.label)} aria-pressed={selected}>
                          <span>{option.label}</span>
                          {option.detail && <small>{option.detail}</small>}
                        </button>
                      );
                    })}
                  </div>
                </div>
              ))}
            </div>
            {configuratorError && <p className="request-error" role="alert">{configuratorError}</p>}
            <button className="button button-primary configurator-continue" type="button" onClick={continueToRecommendation} disabled={!backendCatalog || recommendationLoading}>
              {recommendationLoading ? "Building recommendation..." : backendCatalog ? "Build my recommendation" : "Loading configurator..."} <span aria-hidden="true">↗</span>
            </button>
          </div>}

          {isReady && (
            <div className="configurator-block configurator-performance">
              <div className="configurator-block-heading">
                <span className="section-kicker">Step 03 / Core performance</span>
                <h3>Start with the right engine.</h3>
                <p>JON. PC has matched a starting point to your needs and budget. You stay in control of every change.</p>
              </div>

              <div className="performance-recommendation">
                <div className="performance-recommendation-heading">
                  <div>
                    <span className="section-kicker">JON. PC recommends</span>
                    <h4>{answers.resolution || answers.workload || answers.creativeWork || answers.use || "Your workload"}</h4>
                  </div>
                  <div className="performance-price-block">
                    <span className="performance-price-label">Your budget / {budgetRange.label}</span>
                    <strong className="performance-price">${(backendQuote?.estimatedTotal ?? estimatedFinalPrice).toLocaleString("en-AU")} AUD</strong>
                    <span className="performance-price-status">{budgetStatus}</span>
                  </div>
                </div>
                <div className="performance-pair">
                  <div className="performance-card performance-card-recommended">
                    <span>GPU</span>
                    <strong>{performanceOptions.gpu.label}</strong>
                    <small>{performanceOptions.gpu.detail}</small>
                    <em>Recommended starting point</em>
                  </div>
                  <div className="performance-plus" aria-hidden="true">+</div>
                  <div className="performance-card performance-card-recommended">
                    <span>CPU</span>
                    <strong>{performanceOptions.cpu.label}</strong>
                    <small>{performanceOptions.cpu.detail}</small>
                    <em>Recommended starting point</em>
                  </div>
                </div>
              </div>

              <div className="performance-choice-grid">
                <div className="performance-choice-panel">
                  <div className="performance-choice-heading"><span>GPU / Change GPU</span><small>Choose the graphics level that fits your work.</small></div>
                  <div className="performance-choice-list">
                    {catalogGpuOptions.filter((option) => option.id !== "integrated" || activePerformance.cpuId !== "ryzen-5-7500f").map((option) => {
                      const selected = option.id === activePerformance.gpuId;
                      const recommended = option.id === recommendedPerformanceSelection.gpuId;
                      const recommendedOption = optionFor("gpu", recommendedPerformanceSelection.gpuId);
                      const delta = option.price - recommendedOption.price;
                      return (
                        <button className={selected ? "performance-choice performance-choice-selected" : "performance-choice"} type="button" key={option.id} onClick={() => choosePerformance("gpuId", option.id)} aria-pressed={selected}>
                          <span><strong>{option.label}</strong><small>{option.family} / {option.detail}</small></span>
                          <em>{recommended ? "Recommended" : delta === 0 ? "Same tier" : `${delta > 0 ? "+" : "-"}$${Math.abs(delta).toLocaleString("en-AU")}`}</em>
                        </button>
                      );
                    })}
                  </div>
                </div>

                <div className="performance-choice-panel">
                  <div className="performance-choice-heading"><span>CPU / Change CPU</span><small>Keep processing balanced with your GPU.</small></div>
                  <div className="performance-choice-list">
                    {catalogCpuOptions.filter((option) => activePerformance.gpuId !== "integrated" || option.id !== "ryzen-5-7500f").map((option) => {
                      const selected = option.id === activePerformance.cpuId;
                      const recommended = option.id === recommendedPerformanceSelection.cpuId;
                      const recommendedOption = optionFor("cpu", recommendedPerformanceSelection.cpuId);
                      const delta = option.price - recommendedOption.price;
                      return (
                        <button className={selected ? "performance-choice performance-choice-selected" : "performance-choice"} type="button" key={option.id} onClick={() => choosePerformance("cpuId", option.id)} aria-pressed={selected}>
                          <span><strong>{option.label}</strong><small>{option.family} / {option.detail}</small></span>
                          <em>{recommended ? "Recommended" : delta === 0 ? "Same tier" : `${delta > 0 ? "+" : "-"}$${Math.abs(delta).toLocaleString("en-AU")}`}</em>
                        </button>
                      );
                    })}
                  </div>
                </div>
              </div>

              <div className="performance-footer">
                <span><i /> CURRENT-GENERATION CPU / GPU STARTING POINT</span>
              </div>
              <button className="button button-primary configurator-continue" type="button" onClick={continueToMemory}>
                Continue to memory <span aria-hidden="true">↗</span>
              </button>
            </div>
          )}

          {isMemoryReady && (
            <div className="configurator-block configurator-memory">
              <div className="configurator-block-heading">
                <span className="section-kicker">Step 04 / Memory</span>
                <h3>Give your system room to think.</h3>
                <p>More memory keeps larger games, projects and datasets moving without closing the door on future upgrades.</p>
              </div>

              <div className="memory-recommendation">
                <div>
                  <span className="section-kicker">{isRecommendedMemory ? "JON. PC recommends" : "Your selection"}</span>
                  <strong>{memoryOption.label}</strong>
                  <small>{isRecommendedMemory ? `${memoryOption.recommendedFor} for your selected direction.` : `${memoryOption.recommendedFor} / Adjusted from the recommended baseline.`}</small>
                </div>
                <span>{isRecommendedMemory ? "Recommended baseline" : `${memoryDelta > 0 ? "+" : "-"}$${Math.abs(memoryDelta).toLocaleString("en-AU")} from recommendation`}</span>
              </div>

              <div className="memory-choice-grid">
                {catalogMemoryOptions.map((option) => {
                  const selected = option.id === activeMemory;
                  const recommended = option.id === recommendedMemory;
                  const delta = option.price - recommendedMemoryOption.price;
                  return (
                    <button className={selected ? "memory-choice memory-choice-selected" : "memory-choice"} type="button" key={option.id} onClick={() => chooseMemory(option.id)} aria-pressed={selected}>
                      <span className="memory-choice-capacity">{option.label}</span>
                      <small>{option.detail}</small>
                      <em>{recommended ? "Recommended" : `${delta > 0 ? "+" : "-"}$${Math.abs(delta).toLocaleString("en-AU")}`}</em>
                    </button>
                  );
                })}
              </div>

              <div className="memory-footer">
                <span><i /> DDR5 platform matched</span>
                <span><i /> {memoryOption.label} selected</span>
              </div>
              <button className="button button-primary configurator-continue" type="button" onClick={continueToStorage}>
                Continue to storage <span aria-hidden="true">↗</span>
              </button>
            </div>
          )}

          {isStorageReady && (
            <div className="configurator-block configurator-storage">
              <div className="configurator-block-heading">
                <span className="section-kicker">Step 05 / Storage</span>
                <h3>Keep the important things close.</h3>
                <p>Choose the capacity that matches your library, files and working projects, with fast Gen4 NVMe storage as the starting point.</p>
              </div>

              <div className="storage-recommendation">
                <div>
                  <span className="section-kicker">{isRecommendedStorage ? "JON. PC recommends" : "Your selection"}</span>
                  <strong>{storageOption.label}</strong>
                  <small>{isRecommendedStorage ? `${storageOption.recommendedFor} for your selected direction.` : `${storageOption.recommendedFor} / Adjusted from the recommended baseline.`}</small>
                </div>
                <span>{isRecommendedStorage ? "Recommended baseline" : `${storageDelta > 0 ? "+" : "-"}$${Math.abs(storageDelta).toLocaleString("en-AU")} from recommendation`}</span>
              </div>

              <div className="storage-choice-grid">
                {catalogStorageOptions.map((option) => {
                  const selected = option.id === activeStorage;
                  const recommended = option.id === recommendedStorage;
                  const delta = option.price - recommendedStorageOption.price;
                  return (
                    <button className={selected ? "storage-choice storage-choice-selected" : "storage-choice"} type="button" key={option.id} onClick={() => chooseStorage(option.id)} aria-pressed={selected}>
                      <span className="storage-choice-capacity">{option.label}</span>
                      <small>{option.detail}</small>
                      <em>{recommended ? "Recommended" : `${delta > 0 ? "+" : "-"}$${Math.abs(delta).toLocaleString("en-AU")}`}</em>
                    </button>
                  );
                })}
              </div>

              <div className="storage-footer">
                <span><i /> Gen4 NVMe platform matched</span>
                <span><i /> {storageOption.label} selected</span>
              </div>
              {transitionError?.step === "platform" && <p className="request-error" role="alert">{transitionError.message}</p>}
              <button className="button button-primary configurator-continue" type="button" onClick={continueToPlatform} disabled={transitionLoading === "platform"}>
                {transitionLoading === "platform" ? "Checking compatibility..." : "Continue to platform"} <span aria-hidden="true">↗</span>
              </button>
            </div>
          )}

          {isPlatformReady && (
            <div className="configurator-block configurator-platform">
              <div className="configurator-block-heading">
                <span className="section-kicker">Step 06 / Platform + Power</span>
                <h3>Connect the system with confidence.</h3>
                <p>We filter the platform and power choices around your CPU, GPU and memory so every visible option is a valid starting point.</p>
              </div>

              <div className="platform-choice-grid">
                <div className="platform-choice-panel">
                  <div className="platform-choice-heading"><span>Motherboard / Choose your platform</span><small>{performanceOptions.cpu.platform} / DDR5 compatible options only</small></div>
                  <div className="platform-choice-list">
                    {catalogMotherboardOptions.map((option) => {
                      const selected = option.id === activeMotherboard.id;
                      const recommended = option.id === compatibleMotherboardDefault;
                      const delta = option.price - optionFor("motherboard", compatibleMotherboardDefault).price;
                      return (
                        <button className={selected ? "platform-choice platform-choice-selected" : "platform-choice"} type="button" key={option.id} onClick={() => chooseMotherboard(option.id)} aria-pressed={selected}>
                          <span><strong>{option.label}</strong><small>{option.chipset} / {option.detail}</small></span>
                          <em>{recommended ? "Recommended" : delta === 0 ? "Same tier" : `${delta > 0 ? "+" : "-"}$${Math.abs(delta).toLocaleString("en-AU")}`}</em>
                        </button>
                      );
                    })}
                  </div>
                </div>

                <div className="platform-choice-panel">
                  <div className="platform-choice-heading"><span>PSU / Choose your power</span><small>Filtered for {performanceOptions.gpu.label}</small></div>
                  <div className="platform-choice-list">
                    {catalogPsuOptions.map((option) => {
                      const selected = option.id === activePsu.id;
                      const recommended = option.id === compatiblePsuDefault;
                      const delta = option.price - optionFor("psu", compatiblePsuDefault).price;
                      return (
                        <button className={selected ? "platform-choice platform-choice-selected" : "platform-choice"} type="button" key={option.id} onClick={() => choosePsu(option.id)} aria-pressed={selected}>
                          <span><strong>{option.label}</strong><small>{option.detail} / {option.modular}</small></span>
                          <em>{recommended ? "Recommended" : delta === 0 ? "Same tier" : `${delta > 0 ? "+" : "-"}$${Math.abs(delta).toLocaleString("en-AU")}`}</em>
                        </button>
                      );
                    })}
                  </div>
                </div>
              </div>

              <div className="platform-validation">
                <span><i /> {activeMotherboard.platform} socket matched</span>
                <span><i /> DDR5 memory matched</span>
                <span><i /> {activePsu.wattage}W power coverage</span>
              </div>
              {transitionError?.step === "style" && <p className="request-error" role="alert">{transitionError.message}</p>}
              <button className="button button-primary configurator-continue" type="button" onClick={continueToStyle} disabled={transitionLoading === "style"}>
                {transitionLoading === "style" ? "Checking compatibility..." : "Continue to cooling"} <span aria-hidden="true">↗</span>
              </button>
            </div>
          )}

          {isStyleReady && (
            <div className="configurator-block configurator-style">
              <div className="configurator-block-heading">
                <span className="section-kicker">Step 07 / Cooling + Case</span>
                <h3>Make the system feel like yours.</h3>
                <p>Choose thermal performance, then shape your own style. Every visible option stays matched to your platform.</p>
              </div>

              <div className="style-choice-grid">
                <div className="style-choice-panel cooling-panel">
                  <div className="style-choice-heading"><span>Cooling / Choose thermal performance</span><small>Filtered for {performanceOptions.cpu.label} and {activeCase.label}</small></div>
                  <div className="cooling-choice-list">
                    {catalogCoolingOptions.map((option) => {
                      const selected = option.id === activeCooling.id;
                      const recommended = option.id === compatibleCoolingDefault;
                      const delta = option.price - optionFor("cooling", compatibleCoolingDefault).price;
                      return (
                        <button className={selected ? "cooling-choice cooling-choice-selected" : "cooling-choice"} type="button" key={option.id} onClick={() => chooseCooling(option.id)} aria-pressed={selected}>
                          <img src={coolingImages[option.id] ?? coolingAir} alt="" />
                          <span><strong>{option.label}</strong><small>{option.detail}</small></span>
                          <em>{recommended ? "Recommended" : delta === 0 ? "Same tier" : `${delta > 0 ? "+" : "-"}$${Math.abs(delta).toLocaleString("en-AU")}`}</em>
                        </button>
                      );
                    })}
                  </div>
                </div>

                <div className="style-choice-panel case-panel">
                  <div className="style-choice-heading"><span>Case + Style / Choose your presence</span><small>Filtered for {activeMotherboard.formFactor} motherboard support</small></div>
                  <div className="case-preview">
                    <img src={casePreviewImage} alt={`${activeCase.label} ${activeCaseColor.label} case`} />
                    <div><span className="case-preview-label">{activeCase.label} / {activeCaseColor.label}</span><strong>{activeCase.detail}</strong>{casePreviewPending && <small>Final finish confirmed during review</small>}</div>
                  </div>
                  <div className="case-type-list">
                    {catalogCaseOptions.map((option) => {
                      const selected = option.id === activeCase.id;
                      return <button className={selected ? "case-type case-type-selected" : "case-type"} type="button" key={option.id} onClick={() => chooseCase(option.id as CaseId)} aria-pressed={selected}><strong>{option.label}</strong><small>{option.detail}</small></button>;
                    })}
                  </div>
                  <div className="case-colour-heading"><span>Colour</span><small>Preview updates with your selection</small></div>
                  <div className="case-colour-list">
                    {visibleCaseColors.map((color) => <button className={color.id === activeCaseColor.id ? "case-colour case-colour-selected" : "case-colour"} type="button" key={color.id} onClick={() => chooseCaseColor(color.id)} aria-label={`Choose ${color.label}`} aria-pressed={color.id === activeCaseColor.id}><i style={{ backgroundColor: color.hex }} /><span>{color.label}</span></button>)}
                  </div>
                </div>
              </div>

              <div className="style-validation">
                <span><i /> {activeMotherboard.formFactor} case fit matched</span>
                <span><i /> {activeCooling.label} supported</span>
                <span><i /> {activeCaseColor.label} finish selected</span>
              </div>
              {transitionError?.step === "review" && <p className="request-error" role="alert">{transitionError.message}</p>}
              <button className="button button-primary configurator-continue" type="button" onClick={continueToReview} disabled={transitionLoading === "review"}>
                {transitionLoading === "review" ? "Checking build..." : "Review next step"} <span aria-hidden="true">↗</span>
              </button>
            </div>
          )}

          {isReviewReady && (
            <div className="configurator-block configurator-review" id="build-review">
              <div className="configurator-block-heading">
                <span className="section-kicker">Step 08 / Review</span>
                <h3>Your JON. PC, ready to review.</h3>
                <p>Check every detail, then request the build for a final quote.</p>
              </div>

              <div className="review-hero">
                <div>
                  <span className="section-kicker">{buildOrigin ? buildOrigin.modified ? "Customised preset" : "JON. PC preset" : "Your configuration"}</span>
                  <strong>{buildOrigin ? buildOrigin.modified ? `Customised from ${buildOrigin.name}` : buildOrigin.name : `JON. Custom / ${directionLabel}`}</strong>
                  <p>{answers.resolution || answers.workload || answers.creativeWork || answers.use || "Configured for your direction"} / {budgetRange.label}</p>
                </div>
                  <div className="review-price"><span>Estimated build</span><strong>${reviewPrice.toLocaleString("en-AU")} AUD</strong></div>
              </div>

              <div className="review-grid">
                <div className="review-config-list">
                  <div className="review-list-heading"><span>Configuration</span><small>Change any stage before requesting your build.</small></div>
                  <div className="review-config-row"><span>Core performance</span><strong>{performanceOptions.cpu.label}</strong><strong>{performanceOptions.gpu.label}</strong><button type="button" onClick={() => editFromReview("performance")}>Edit ↗</button></div>
                  <div className="review-config-row"><span>Memory</span><strong>{memoryOption.label}</strong><small>{memoryOption.detail}</small><button type="button" onClick={() => editFromReview("memory")}>Edit ↗</button></div>
                  <div className="review-config-row"><span>Storage</span><strong>{storageOption.label}</strong><small>{storageOption.detail}</small><button type="button" onClick={() => editFromReview("storage")}>Edit ↗</button></div>
                  <div className="review-config-row"><span>Motherboard + PSU</span><strong>{activeMotherboard.label}</strong><strong>{activePsu.label}</strong><button type="button" onClick={() => editFromReview("platform")}>Edit ↗</button></div>
                  <div className="review-config-row"><span>Cooling + Case</span><strong>{activeCooling.label}</strong><strong>{activeCase.label} / {activeCaseColor.label}</strong><button type="button" onClick={() => editFromReview("style")}>Edit ↗</button></div>
                </div>

                <div className="review-side-panel">
                  <div className="review-side-section"><span>Price breakdown</span><div><small>Recommended baseline</small><strong>${reviewBaseline.toLocaleString("en-AU")} AUD</strong></div><div><small>Selected adjustments</small><strong>{reviewAdjustments >= 0 ? "+" : "-"}${Math.abs(reviewAdjustments).toLocaleString("en-AU")} AUD</strong></div><div className="review-total"><small>Estimated total</small><strong>${reviewPrice.toLocaleString("en-AU")} AUD</strong></div></div>
                  <div className="review-side-section"><span>JON. validation</span>{backendValidation.length > 0 ? backendValidation.map((message) => <p key={message}><i /> {message}</p>) : <p><i /> All selected components are compatible.</p>}<p><i /> {activePsu.wattage}W power coverage for {performanceOptions.gpu.label}.</p><p><i /> {activeMotherboard.formFactor} case fit and {activeCooling.label.toLowerCase()} matched.</p></div>
                </div>
              </div>

              <div className="review-actions">
                <div className="review-cta-row">
                  <div className="save-build-action">
                    <button className="button button-primary" type="button" onClick={saveCurrentBuild} disabled={!backendQuote || !backendQuote.compatible}>{saveState === "saving" ? "Saving..." : saveState === "saved" ? "Saved" : "Save this build"} <span aria-hidden="true">↓</span></button>
                    {saveState === "error" && <small>{window.localStorage.getItem(authTokenKey) ? "Unable to save this build." : "Log in to save your build."}</small>}
                  </div>
                  <button className="button button-primary" type="button" onClick={openBuildRequest} disabled={!backendQuote || !backendQuote.compatible}>Request this build <span aria-hidden="true">↗</span></button>
                </div>
                <span>{requestSubmitted ? "Request noted. A JON. PC specialist will confirm the final quote." : "Estimated pricing is a starting point. Final availability and quote will be confirmed by JON. PC."}</span>
              </div>
            </div>
          )}
        </div>

        {directionChosen && <aside className="configurator-summary" aria-label="Current build summary">
          <div className="summary-label">Your build / live summary</div>
          <h3>{directionLabel}</h3>
          <div className="summary-selection-list">
            {questions.map((question) => (
              <div key={question.id}><span>{question.label}</span><strong>{answers[question.id]}</strong></div>
            ))}
          </div>
          {isReady ? (
            <>
              <div className="summary-performance"><span>Core performance</span><strong>{performanceOptions.cpu.label}</strong><strong>{performanceOptions.gpu.label}</strong></div>
              <div className="summary-price"><span>Estimated / {budgetRange.label}</span><strong>${(backendQuote?.estimatedTotal ?? estimatedFinalPrice).toLocaleString("en-AU")} AUD</strong></div>
              {isMemoryReady && <div className="summary-memory"><span>Memory</span><strong>{memoryOption.label}</strong><small>{memoryDelta === 0 ? "Recommended baseline" : `${memoryDelta > 0 ? "+" : "-"}$${Math.abs(memoryDelta).toLocaleString("en-AU")} from recommendation`}</small></div>}
              {isStorageReady && <div className="summary-memory"><span>Storage</span><strong>{storageOption.label}</strong><small>{storageDelta === 0 ? "Recommended baseline" : `${storageDelta > 0 ? "+" : "-"}$${Math.abs(storageDelta).toLocaleString("en-AU")} from recommendation`}</small></div>}
              {isPlatformReady && <>
                <div className="summary-memory"><span>Platform</span><strong>{activeMotherboard.label}</strong><strong>{activePsu.label}</strong></div>
              </>}
              {isStyleReady && <div className="summary-memory"><span>Cooling + Case</span><strong>{activeCooling.label}</strong><strong>{activeCase.label} / {activeCaseColor.label}</strong></div>}
              <div className="summary-next"><span>Next step</span><strong>{isStyleReady ? "Review" : isPlatformReady ? "Cooling + Case" : isStorageReady ? "Platform + Power" : isMemoryReady ? "Storage" : "Memory"}</strong><p>{isStyleReady ? "Review your complete starting configuration before requesting a build." : isPlatformReady ? "Choose cooling and a case that fit the system." : isStorageReady ? "Choose a compatible motherboard and PSU for the system." : isMemoryReady ? "Choose the capacity and speed that fit your files and projects." : "Choose the capacity that fits your files, games and projects."}</p></div>
              <div className={backendQuote && !backendQuote.compatible ? "summary-status summary-status-review" : "summary-status"}><i /> {backendQuote && !backendQuote.compatible ? "Selection needs review" : "Configuration synced"}</div>
            </>
          ) : (
            <>
              <div className="summary-next"><span>Next step</span><strong>Recommended CPU + GPU</strong><p>We will balance the core performance around your answers.</p></div>
              <div className="summary-status"><i /> Ready to configure</div>
            </>
          )}
        </aside>}
      </div>

      {requestOpen && (
        <div className="request-modal-backdrop" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) setRequestOpen(false); }}>
          <div className="request-modal" role="dialog" aria-modal="true" aria-labelledby="request-modal-title">
            <button className="request-modal-close" type="button" onClick={() => setRequestOpen(false)} aria-label="Close request form">×</button>
            {!requestSubmitted ? (
                <form onSubmit={submitBuildRequest}>
                <span className="section-kicker">JON. PC / Build request</span>
                <h3 id="request-modal-title">Send your configuration.</h3>
                <p className="request-modal-intro">We&apos;ll review it and send you a final quote.</p>
                <div className="request-build-chip"><span>{directionLabel} / {performanceOptions.gpu.label}</span><strong>${reviewPrice.toLocaleString("en-AU")} AUD estimated</strong></div>
                <div className="request-form-grid">
                  <label><span>Name</span><input name="name" type="text" value={user?.displayName ?? ""} readOnly /></label>
                  <label><span>Email</span><input name="email" type="email" value={user?.email ?? ""} readOnly /></label>
                  <label><span>Phone <em>Optional</em></span><input name="phone" type="tel" placeholder="0400 000 000" /></label>
                </div>
                <label className="request-form-wide"><span>Notes <em>Optional</em></span><textarea name="notes" rows={3} placeholder="Tell us anything useful about your setup or timing." /></label>
                <label className="request-check"><input name="contact" type="checkbox" defaultChecked /><span>Contact me by email about this build request.</span></label>
                {requestError && <p className="request-error" role="alert">{requestError}</p>}
                <button className="button button-primary request-submit" type="submit" disabled={requestSubmitting}>{requestSubmitting ? "Validating build..." : "Send build request"} <span aria-hidden="true">↗</span></button>
                <small className="request-disclaimer">Estimated pricing only. Final pricing depends on availability, assembly and component validation.</small>
              </form>
            ) : (
              <div className="request-success">
                <span className="section-kicker">Request received</span>
                <h3 id="request-modal-title">Ready for review.</h3>
                <p>Your final quote will appear in My orders.</p>
                <div className="request-reference"><span>Reference</span><strong>{requestReference}</strong></div>
                <button className="button button-primary request-submit" type="button" onClick={() => setRequestOpen(false)}>Done <span aria-hidden="true">↗</span></button>
              </div>
            )}
          </div>
        </div>
      )}
    </section>
  );
}

export default BuildConfigurator;
