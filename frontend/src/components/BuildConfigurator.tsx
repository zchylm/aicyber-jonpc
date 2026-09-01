import { useMemo, useState } from "react";
import { configuratorDirections, configuratorQuestions, type DirectionId } from "../data/configurator";
import {
  cpuOptions,
  estimateCorePrice,
  getBudgetRange,
  getPerformanceOptions,
  gpuOptions,
  recommendPerformance,
  validatePerformance,
  type PerformanceSelection,
} from "../data/performance";
import { estimateMemoryPrice, getMemoryOption, memoryOptions, recommendMemory } from "../data/memory";
import { estimateStoragePrice, getStorageOption, recommendStorage, storageOptions } from "../data/storage";
import { getCompatibleMotherboards, getMotherboardOption, recommendMotherboard } from "../data/motherboard";
import { getCompatiblePsus, getPsuOption, recommendPsu } from "../data/psu";
import { getCaseOption, getCompatibleCases, recommendCase, type CaseOption } from "../data/case";
import { getCompatibleCooling, getCoolingOption, recommendCooling } from "../data/cooling";

function defaultsFor(direction: DirectionId) {
  return Object.fromEntries(
    configuratorQuestions[direction].map((question) => [
      question.id,
      question.options.find((option) => option.recommended)?.label ?? question.options[0].label,
    ]),
  );
}

function BuildConfigurator() {
  const [direction, setDirection] = useState<DirectionId>("gaming");
  const [answers, setAnswers] = useState<Record<string, string>>(defaultsFor("gaming"));
  const [isReady, setIsReady] = useState(false);
  const [performanceSelection, setPerformanceSelection] = useState<PerformanceSelection | null>(null);
  const [isMemoryReady, setIsMemoryReady] = useState(false);
  const [memorySelection, setMemorySelection] = useState<string | null>(null);
  const [isStorageReady, setIsStorageReady] = useState(false);
  const [storageSelection, setStorageSelection] = useState<string | null>(null);
  const [isStyleReady, setIsStyleReady] = useState(false);
  const [coolingSelection, setCoolingSelection] = useState<string | null>(null);
  const [caseSelection, setCaseSelection] = useState<CaseOption["id"] | null>(null);
  const [caseColorSelection, setCaseColorSelection] = useState<string | null>(null);
  const [showMoreColours, setShowMoreColours] = useState(false);
  const [isReviewReady, setIsReviewReady] = useState(false);
  const [requestSubmitted, setRequestSubmitted] = useState(false);
  const [requestOpen, setRequestOpen] = useState(false);
  const requestReference = `JON-${direction.slice(0, 3).toUpperCase()}-2408`;
  const questions = useMemo(() => configuratorQuestions[direction], [direction]);
  const recommendedPerformance = useMemo(() => recommendPerformance(direction, answers), [direction, answers]);
  const budgetRange = useMemo(() => getBudgetRange(direction, answers), [direction, answers]);
  const directionLabel = configuratorDirections.find((item) => item.id === direction)?.label ?? direction;
  const activePerformance: PerformanceSelection = performanceSelection ?? recommendedPerformance;
  const performanceOptions = getPerformanceOptions(activePerformance);
  const recommendedCorePrice = estimateCorePrice(recommendedPerformance);
  const estimatedPrice = estimateCorePrice(activePerformance);
  const coreDelta = estimatedPrice - recommendedCorePrice;
  const recommendedMemory = useMemo(() => recommendMemory(direction, answers), [direction, answers]);
  const activeMemory = memorySelection ?? recommendedMemory;
  const memoryOption = getMemoryOption(activeMemory);
  const recommendedMemoryOption = getMemoryOption(recommendedMemory);
  const memoryDelta = memoryOption.price - recommendedMemoryOption.price;
  const isRecommendedMemory = activeMemory === recommendedMemory;
  const estimatedBuildPrice = estimatedPrice + memoryDelta;
  const recommendedStorage = useMemo(() => recommendStorage(direction, answers), [direction, answers]);
  const activeStorage = storageSelection ?? recommendedStorage;
  const storageOption = getStorageOption(activeStorage);
  const recommendedStorageOption = getStorageOption(recommendedStorage);
  const storageDelta = storageOption.price - recommendedStorageOption.price;
  const isRecommendedStorage = activeStorage === recommendedStorage;
  const estimatedFullPrice = estimatedBuildPrice + storageDelta;
  const compatibleMotherboards = getCompatibleMotherboards(performanceOptions.cpu);
  const recommendedMotherboard = recommendMotherboard(performanceOptions.cpu);
  const [isPlatformReady, setIsPlatformReady] = useState(false);
  const [motherboardSelection, setMotherboardSelection] = useState<string | null>(null);
  const [psuSelection, setPsuSelection] = useState<string | null>(null);
  const activeMotherboard = getMotherboardOption(motherboardSelection ?? recommendedMotherboard, performanceOptions.cpu);
  const compatiblePsus = getCompatiblePsus(performanceOptions.gpu);
  const recommendedPsuId = recommendPsu(performanceOptions.gpu);
  const activePsu = getPsuOption(psuSelection ?? recommendedPsuId, performanceOptions.gpu);
  const recommendedMotherboardOption = getMotherboardOption(recommendedMotherboard, performanceOptions.cpu);
  const recommendedPsuOption = getPsuOption(recommendedPsuId, performanceOptions.gpu);
  const motherboardDelta = activeMotherboard.price - recommendedMotherboardOption.price;
  const psuDelta = activePsu.price - recommendedPsuOption.price;
  const estimatedCompletePrice = estimatedFullPrice + motherboardDelta + psuDelta;
  const compatibleCases = getCompatibleCases(activeMotherboard);
  const recommendedCase = recommendCase(activeMotherboard);
  const activeCase = getCaseOption(caseSelection ?? recommendedCase, activeMotherboard);
  const visibleCaseColors = showMoreColours ? [...activeCase.colors, ...activeCase.moreColors] : activeCase.colors;
  const activeCaseColor = visibleCaseColors.find((color) => color.id === caseColorSelection) ?? activeCase.colors[0];
  const casePreviewImage = activeCaseColor.image ?? activeCase.colors[0].image;
  const casePreviewPending = !activeCaseColor.image;
  const compatibleCooling = getCompatibleCooling(performanceOptions.cpu, activeCase.id);
  const recommendedCooling = recommendCooling(performanceOptions.cpu, activeCase.id);
  const activeCooling = getCoolingOption(coolingSelection ?? recommendedCooling, performanceOptions.cpu, activeCase.id);
  const recommendedCaseOption = getCaseOption(recommendedCase, activeMotherboard);
  const recommendedCoolingOption = getCoolingOption(recommendedCooling, performanceOptions.cpu, activeCase.id);
  const caseDelta = activeCase.price - recommendedCaseOption.price;
  const coolingDelta = activeCooling.price - recommendedCoolingOption.price;
  const estimatedFinalPrice = estimatedCompletePrice + caseDelta + coolingDelta;
  const selectedAdjustments = coreDelta + memoryDelta + storageDelta + motherboardDelta + psuDelta + caseDelta + coolingDelta;
  const budgetStatus = estimatedPrice > budgetRange.max ? "Above selected budget" : estimatedPrice < budgetRange.min ? "Below selected range" : "Within selected budget";
  const performanceValidation = validatePerformance(direction, answers, activePerformance);

  function chooseDirection(nextDirection: DirectionId) {
    setDirection(nextDirection);
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
    setShowMoreColours(false);
    setIsReviewReady(false);
    setRequestSubmitted(false);
  }

  function chooseAnswer(questionId: string, value: string) {
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
    setShowMoreColours(false);
    setIsReviewReady(false);
    setRequestSubmitted(false);
  }

  function continueToRecommendation() {
    setPerformanceSelection(recommendedPerformance);
    setIsReady(true);
  }

  function choosePerformance(type: "cpuId" | "gpuId", id: string) {
    setPerformanceSelection((current) => ({ ...(current ?? recommendedPerformance), [type]: id }));
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
    setShowMoreColours(false);
    setIsReviewReady(false);
    setRequestSubmitted(false);
  }

  function continueToMemory() {
    setMemorySelection(recommendedMemory);
    setIsMemoryReady(true);
  }

  function chooseMemory(id: string) {
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
    setShowMoreColours(false);
    setIsReviewReady(false);
    setRequestSubmitted(false);
  }

  function continueToStorage() {
    setStorageSelection(recommendedStorage);
    setIsStorageReady(true);
  }

  function continueToPlatform() {
    setMotherboardSelection(recommendedMotherboard);
    setPsuSelection(recommendedPsuId);
    setIsPlatformReady(true);
    setIsStyleReady(false);
    setCoolingSelection(null);
    setCaseSelection(null);
    setCaseColorSelection(null);
    setShowMoreColours(false);
  }

  function chooseStorage(id: string) {
    setStorageSelection(id);
    setIsPlatformReady(false);
    setMotherboardSelection(null);
    setPsuSelection(null);
    setIsStyleReady(false);
    setCoolingSelection(null);
    setCaseSelection(null);
    setCaseColorSelection(null);
    setShowMoreColours(false);
    setIsReviewReady(false);
    setRequestSubmitted(false);
  }

  function chooseMotherboard(id: string) {
    setMotherboardSelection(id);
    setIsStyleReady(false);
    setCoolingSelection(null);
    setCaseSelection(null);
    setCaseColorSelection(null);
    setShowMoreColours(false);
    setIsReviewReady(false);
    setRequestSubmitted(false);
  }

  function choosePsu(id: string) {
    setPsuSelection(id);
    setIsStyleReady(false);
    setCoolingSelection(null);
    setCaseSelection(null);
    setCaseColorSelection(null);
    setShowMoreColours(false);
    setIsReviewReady(false);
    setRequestSubmitted(false);
  }

  function continueToStyle() {
    setCoolingSelection(recommendedCooling);
    setCaseSelection(recommendedCase);
    setCaseColorSelection(null);
    setShowMoreColours(false);
    setIsStyleReady(true);
    setIsReviewReady(false);
    setRequestSubmitted(false);
  }

  function chooseCase(id: CaseOption["id"]) {
    setCaseSelection(id);
    setCaseColorSelection(null);
    setShowMoreColours(false);
    setCoolingSelection(null);
    setIsReviewReady(false);
    setRequestSubmitted(false);
  }

  function chooseCaseColor(id: string) {
    setCaseColorSelection(id);
    setIsReviewReady(false);
    setRequestSubmitted(false);
  }

  function chooseCooling(id: string) {
    setCoolingSelection(id);
    setIsReviewReady(false);
    setRequestSubmitted(false);
  }

  function continueToReview() {
    setIsReviewReady(true);
    setRequestSubmitted(false);
    setRequestOpen(false);
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
        <span className="configurator-step-active"><b>02</b> What matters</span>
        <span className={isReady ? "configurator-step-active" : ""}><b>03</b> Core performance</span>
        <span className={isMemoryReady ? "configurator-step-active" : ""}><b>04</b> Memory</span>
        <span className={isStorageReady ? "configurator-step-active" : ""}><b>05</b> Storage</span>
        <span className={isPlatformReady ? "configurator-step-active" : ""}><b>06</b> Platform + Power</span>
        <span className={isStyleReady ? "configurator-step-active" : ""}><b>07</b> Cooling + Case</span>
        <span className={isReviewReady ? "configurator-step-active" : ""}><b>08</b> Review</span>
      </div>

      <div className="configurator-layout">
        <div className="configurator-main">
          <div className="configurator-block">
            <div className="configurator-block-heading">
              <span className="section-kicker">Step 01</span>
              <h3>What are you building for?</h3>
              <p>This choice shapes the recommendations that follow.</p>
            </div>
            <div className="direction-grid">
              {configuratorDirections.map((item, index) => {
                const selected = item.id === direction;
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

          <div className="configurator-block configurator-needs">
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
            <button className="button button-primary configurator-continue" type="button" onClick={continueToRecommendation}>
              Build my recommendation <span aria-hidden="true">↗</span>
            </button>
          </div>

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
                    <strong className="performance-price">${estimatedPrice.toLocaleString("en-AU")} AUD</strong>
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
                    {gpuOptions.map((option) => {
                      const selected = option.id === activePerformance.gpuId;
                      const recommended = option.id === recommendedPerformance.gpuId;
                      const recommendedOption = gpuOptions.find((item) => item.id === recommendedPerformance.gpuId) ?? option;
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
                    {cpuOptions.map((option) => {
                      const selected = option.id === activePerformance.cpuId;
                      const recommended = option.id === recommendedPerformance.cpuId;
                      const recommendedOption = cpuOptions.find((item) => item.id === recommendedPerformance.cpuId) ?? option;
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

              {performanceValidation.status === "review" && (
                <div className="performance-compatibility performance-compatibility-review">
                  <span><i /> {performanceValidation.title}</span>
                  <small>{performanceValidation.detail} Recommended range: ${budgetRange.min.toLocaleString("en-AU")}–${budgetRange.max.toLocaleString("en-AU")} AUD.</small>
                </div>
              )}
              <div className="performance-footer">
                <span><i /> {performanceValidation.status === "review" ? "PERFORMANCE REVIEW REQUIRED" : "CPU / GPU BALANCED"}</span>
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
                {memoryOptions.map((option) => {
                  const selected = option.id === activeMemory;
                  const recommended = option.id === recommendedMemory;
                  const delta = option.price - getMemoryOption(recommendedMemory).price;
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
                {storageOptions.map((option) => {
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
              <button className="button button-primary configurator-continue" type="button" onClick={continueToPlatform}>
                Continue to platform <span aria-hidden="true">↗</span>
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
                    {compatibleMotherboards.map((option) => {
                      const selected = option.id === activeMotherboard.id;
                      const recommended = option.id === recommendedMotherboard;
                      const delta = option.price - (getMotherboardOption(recommendedMotherboard, performanceOptions.cpu)?.price ?? 0);
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
                    {compatiblePsus.map((option) => {
                      const selected = option.id === activePsu.id;
                      const recommended = option.id === recommendedPsuId;
                      const delta = option.price - (getPsuOption(recommendedPsuId, performanceOptions.gpu)?.price ?? 0);
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
              <button className="button button-primary configurator-continue" type="button" onClick={continueToStyle}>
                Continue to cooling <span aria-hidden="true">↗</span>
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
                    {compatibleCooling.map((option) => {
                      const selected = option.id === activeCooling.id;
                      const recommended = option.id === recommendedCooling;
                      const delta = option.price - (getCoolingOption(recommendedCooling, performanceOptions.cpu, activeCase.id)?.price ?? 0);
                      return (
                        <button className={selected ? "cooling-choice cooling-choice-selected" : "cooling-choice"} type="button" key={option.id} onClick={() => chooseCooling(option.id)} aria-pressed={selected}>
                          <img src={option.image} alt="" />
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
                    <div><span className="case-preview-label">{activeCase.label} / {activeCaseColor.label}</span><strong>{activeCase.detail}</strong>{casePreviewPending && <small>Colour preview asset coming soon</small>}</div>
                  </div>
                  <div className="case-type-list">
                    {compatibleCases.map((option) => {
                      const selected = option.id === activeCase.id;
                      return <button className={selected ? "case-type case-type-selected" : "case-type"} type="button" key={option.id} onClick={() => chooseCase(option.id)} aria-pressed={selected}><strong>{option.label}</strong><small>{option.detail}</small></button>;
                    })}
                  </div>
                  <div className="case-colour-heading"><span>Colour</span><small>Preview updates with your selection</small></div>
                  <div className="case-colour-list">
                    {visibleCaseColors.map((color) => <button className={color.id === activeCaseColor.id ? "case-colour case-colour-selected" : "case-colour"} type="button" key={color.id} onClick={() => chooseCaseColor(color.id)} aria-label={`Choose ${color.label}`} aria-pressed={color.id === activeCaseColor.id}><i style={{ backgroundColor: color.hex }} /><span>{color.label}</span></button>)}
                    <button className="case-colour-more" type="button" onClick={() => setShowMoreColours((current) => !current)} aria-expanded={showMoreColours}>{showMoreColours ? "Show fewer" : "More colours"} <span aria-hidden="true">{showMoreColours ? "↑" : "↓"}</span></button>
                  </div>
                </div>
              </div>

              <div className="style-validation">
                <span><i /> {activeMotherboard.formFactor} case fit matched</span>
                <span><i /> {activeCooling.label} supported</span>
                <span><i /> {activeCaseColor.label} finish selected</span>
              </div>
              <button className="button button-primary configurator-continue" type="button" onClick={continueToReview}>
                Review next step <span aria-hidden="true">↗</span>
              </button>
            </div>
          )}

          {isReviewReady && (
            <div className="configurator-block configurator-review">
              <div className="configurator-block-heading">
                <span className="section-kicker">Step 08 / Review</span>
                <h3>Your JON. PC, ready to review.</h3>
                <p>Check every detail, then request the build for a final local quote.</p>
              </div>

              <div className="review-hero">
                <div>
                  <span className="section-kicker">Your configuration</span>
                  <strong>JON. Custom / {directionLabel}</strong>
                  <p>{answers.resolution || answers.workload || answers.creativeWork || answers.use || "Configured for your direction"} / {budgetRange.label}</p>
                </div>
                <div className="review-price"><span>Estimated build</span><strong>${estimatedFinalPrice.toLocaleString("en-AU")} AUD</strong></div>
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
                  <div className="review-side-section"><span>Price breakdown</span><div><small>Recommended baseline</small><strong>${recommendedCorePrice.toLocaleString("en-AU")} AUD</strong></div><div><small>Selected adjustments</small><strong>{selectedAdjustments >= 0 ? "+" : "-"}${Math.abs(selectedAdjustments).toLocaleString("en-AU")} AUD</strong></div><div className="review-total"><small>Estimated total</small><strong>${estimatedFinalPrice.toLocaleString("en-AU")} AUD</strong></div></div>
                  <div className="review-side-section"><span>JON. validation</span><p><i /> All selected components are compatible.</p><p><i /> {activePsu.wattage}W power coverage for {performanceOptions.gpu.label}.</p><p><i /> {activeMotherboard.formFactor} case fit and {activeCooling.label.toLowerCase()} matched.</p></div>
                </div>
              </div>

              <div className="review-actions">
                <button className="button button-primary" type="button" onClick={() => { setRequestOpen(true); setRequestSubmitted(false); }}>Request this build <span aria-hidden="true">↗</span></button>
                <span>{requestSubmitted ? "Request noted. A JON. PC specialist will confirm the final quote." : "Estimated pricing is a starting point. Final availability and quote will be confirmed locally."}</span>
              </div>
            </div>
          )}
        </div>

        <aside className="configurator-summary" aria-label="Current build summary">
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
              <div className="summary-price"><span>Estimated / {budgetRange.label}</span><strong>${isStyleReady ? estimatedFinalPrice.toLocaleString("en-AU") : isPlatformReady ? estimatedCompletePrice.toLocaleString("en-AU") : isStorageReady ? estimatedFullPrice.toLocaleString("en-AU") : isMemoryReady ? estimatedBuildPrice.toLocaleString("en-AU") : estimatedPrice.toLocaleString("en-AU")} AUD</strong></div>
              {isMemoryReady && <div className="summary-memory"><span>Memory</span><strong>{memoryOption.label}</strong><small>{estimateMemoryPrice(activeMemory) === 0 ? "Recommended baseline" : `${estimateMemoryPrice(activeMemory) > 0 ? "+" : "-"}$${Math.abs(estimateMemoryPrice(activeMemory)).toLocaleString("en-AU")} from recommendation`}</small></div>}
              {isStorageReady && <div className="summary-memory"><span>Storage</span><strong>{storageOption.label}</strong><small>{estimateStoragePrice(activeStorage) === 0 ? "Recommended baseline" : `${estimateStoragePrice(activeStorage) > 0 ? "+" : "-"}$${Math.abs(estimateStoragePrice(activeStorage)).toLocaleString("en-AU")} from recommendation`}</small></div>}
              {isPlatformReady && <>
                <div className="summary-memory"><span>Platform</span><strong>{activeMotherboard.label}</strong><strong>{activePsu.label}</strong></div>
              </>}
              {isStyleReady && <div className="summary-memory"><span>Cooling + Case</span><strong>{activeCooling.label}</strong><strong>{activeCase.label} / {activeCaseColor.label}</strong></div>}
              <div className="summary-next"><span>Next step</span><strong>{isStyleReady ? "Review" : isPlatformReady ? "Cooling + Case" : isStorageReady ? "Platform + Power" : isMemoryReady ? "Storage" : "Memory"}</strong><p>{isStyleReady ? "Review your complete starting configuration before requesting a build." : isPlatformReady ? "Choose cooling and a case that fit the system." : isStorageReady ? "Choose a compatible motherboard and PSU for the system." : isMemoryReady ? "Choose the capacity and speed that fit your files and projects." : "Choose the capacity that fits your files, games and projects."}</p></div>
              <div className={performanceValidation.status === "review" ? "summary-status summary-status-review" : "summary-status"}><i /> {performanceValidation.status === "review" ? "Review performance balance" : "No direct CPU / GPU conflict"}</div>
            </>
          ) : (
            <>
              <div className="summary-next"><span>Next step</span><strong>Recommended CPU + GPU</strong><p>We will balance the core performance around your answers.</p></div>
              <div className="summary-status"><i /> Ready to configure</div>
            </>
          )}
        </aside>
      </div>

      {requestOpen && (
        <div className="request-modal-backdrop" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) setRequestOpen(false); }}>
          <div className="request-modal" role="dialog" aria-modal="true" aria-labelledby="request-modal-title">
            <button className="request-modal-close" type="button" onClick={() => setRequestOpen(false)} aria-label="Close request form">×</button>
            {!requestSubmitted ? (
              <form onSubmit={(event) => { event.preventDefault(); setRequestSubmitted(true); }}>
                <span className="section-kicker">JON. PC / Build request</span>
                <h3 id="request-modal-title">Let&apos;s make this build real.</h3>
                <p className="request-modal-intro">Share your details and a JON. PC specialist will review this configuration, availability and the final local quote.</p>
                <div className="request-build-chip"><span>{directionLabel} / {performanceOptions.gpu.label}</span><strong>${estimatedFinalPrice.toLocaleString("en-AU")} AUD estimated</strong></div>
                <div className="request-form-grid">
                  <label><span>Name</span><input name="name" type="text" placeholder="Your name" required /></label>
                  <label><span>Email</span><input name="email" type="email" placeholder="you@example.com" required /></label>
                  <label><span>Phone <em>Optional</em></span><input name="phone" type="tel" placeholder="0400 000 000" /></label>
                  <label><span>Suburb / Location</span><input name="location" type="text" placeholder="Melbourne" required /></label>
                </div>
                <label className="request-form-wide"><span>Notes <em>Optional</em></span><textarea name="notes" rows={3} placeholder="Tell us anything useful about your setup or timing." /></label>
                <label className="request-check"><input name="contact" type="checkbox" defaultChecked /><span>Contact me by email about this build request.</span></label>
                <button className="button button-primary request-submit" type="submit">Send build request <span aria-hidden="true">↗</span></button>
                <small className="request-disclaimer">Estimated pricing only. Final pricing depends on availability, assembly and component validation.</small>
              </form>
            ) : (
              <div className="request-success">
                <span className="section-kicker">Request received</span>
                <h3 id="request-modal-title">Your build is with JON. PC.</h3>
                <p>Thanks. We&apos;ve recorded your configuration for a local review.</p>
                <div className="request-reference"><span>Reference</span><strong>{requestReference}</strong></div>
                <div className="request-success-checks"><span><i /> Configuration captured</span><span><i /> Compatibility checked</span><span><i /> Final quote to be confirmed</span></div>
                <button className="button button-primary request-submit" type="button" onClick={() => setRequestOpen(false)}>Back to your build <span aria-hidden="true">↗</span></button>
              </div>
            )}
          </div>
        </div>
      )}
    </section>
  );
}

export default BuildConfigurator;
