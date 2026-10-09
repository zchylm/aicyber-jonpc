import { useMemo, useState, type FormEvent } from "react";
import { authTokenKey } from "../api/auth";
import { submitInfrastructureRequest, type InfrastructureRequestResponse } from "../api/infrastructure";
import reviewImage from "../assets/ai-infrastructure-review.jpg";
import teamImage from "../assets/ai-infrastructure-team-v2.jpg";
import "./AiInfrastructureSection.css";

type Option = { id: string; label: string };
type Service = Option & { detail: string; category: "integration" | "software" | "support" };

type Platform = {
  id: string;
  number: string;
  name: string;
  detail: string;
  scales: string[];
  gpu: Option[];
  cpu: Option[];
  memory: Option[];
  storage: Option[];
  network: Option[];
};

const scales = [
  { id: "individual", number: "01", label: "Individual node", detail: "One focused compute system" },
  { id: "multi", number: "02", label: "Multi-node", detail: "Repeatable systems for a team" },
  { id: "rack", number: "03", label: "Rack deployment", detail: "Rack-ready compute and integration" },
  { id: "large", number: "04", label: "Large-scale infrastructure", detail: "Multi-rack projects under review" },
] as const;

const platforms: Platform[] = [
  {
    id: "tower", number: "01", name: "Tower compute", detail: "Serviceable AI compute outside a rack.", scales: ["individual", "multi"],
    gpu: [{ id: "rtx-5090", label: "1× GeForce RTX 5090 32GB" }, { id: "rtx-pro-6000", label: "1× RTX PRO 6000 Blackwell 96GB" }],
    cpu: [{ id: "ryzen-9950x", label: "AMD Ryzen 9 9950X" }, { id: "threadripper-pro", label: "AMD Ryzen Threadripper PRO 9000 WX-Series" }, { id: "xeon-w", label: "Intel Xeon W-3500 / W-2500" }],
    memory: [{ id: "128gb", label: "128GB DDR5" }, { id: "256gb-ecc", label: "256GB ECC DDR5" }, { id: "512gb-ecc", label: "512GB ECC DDR5" }],
    storage: [{ id: "4tb", label: "4TB NVMe / 2× 2TB" }, { id: "8tb", label: "8TB NVMe / 2× 4TB" }, { id: "16tb", label: "16TB NVMe / 4× 4TB" }],
    network: [{ id: "10gbe", label: "10GbE" }, { id: "25gbe", label: "25GbE" }, { id: "100gbe", label: "100GbE" }],
  },
  {
    id: "rack", number: "02", name: "Rack GPU server", detail: "ECC memory, redundant power and remote management.", scales: ["individual", "multi", "rack", "large"],
    gpu: [{ id: "l4", label: "1× NVIDIA L4 24GB" }, { id: "l40s", label: "1× NVIDIA L40S 48GB" }, { id: "dual-rtx-pro", label: "2× RTX PRO 6000 Blackwell Server Edition" }, { id: "quad-l40s", label: "4× NVIDIA L40S 48GB" }],
    cpu: [{ id: "epyc-9005", label: "1× AMD EPYC 9005" }, { id: "dual-epyc-9005", label: "2× AMD EPYC 9005" }, { id: "xeon-6", label: "1× Intel Xeon 6" }, { id: "dual-xeon-6", label: "2× Intel Xeon 6" }],
    memory: [{ id: "384gb-rdimm", label: "384GB ECC RDIMM" }, { id: "512gb-rdimm", label: "512GB ECC RDIMM" }, { id: "768gb-rdimm", label: "768GB ECC RDIMM" }, { id: "1tb-rdimm", label: "1TB ECC RDIMM" }, { id: "1-5tb-rdimm", label: "1.5TB ECC RDIMM" }, { id: "2tb-rdimm", label: "2TB ECC RDIMM" }, { id: "3tb-rdimm", label: "3TB ECC RDIMM" }, { id: "4tb-rdimm", label: "4TB ECC RDIMM" }],
    storage: [{ id: "boot-data", label: "2× boot + 2× enterprise NVMe" }, { id: "four-nvme", label: "2× boot + 4× enterprise NVMe" }, { id: "eight-nvme", label: "2× boot + 8× enterprise NVMe" }],
    network: [{ id: "25gbe", label: "Dual 25GbE" }, { id: "100gbe", label: "Dual 100GbE / RoCE" }, { id: "200gb", label: "200GbE / InfiniBand" }],
  },
  {
    id: "dense", number: "03", name: "Dense multi-GPU", detail: "High-density platforms for accelerated computing projects.", scales: ["individual", "multi", "rack", "large"],
    gpu: [{ id: "quad-rtx-pro", label: "4× RTX PRO 6000 Blackwell Server Edition" }, { id: "eight-rtx-pro", label: "8× RTX PRO 6000 Blackwell Server Edition" }, { id: "hgx-h200", label: "HGX H200 8-GPU platform" }, { id: "hgx-b200", label: "HGX B200 8-GPU platform" }, { id: "mi350", label: "AMD Instinct MI350X 8-GPU platform" }],
    cpu: [{ id: "dual-epyc-9005", label: "2× AMD EPYC 9005" }, { id: "dual-xeon-6", label: "2× Intel Xeon 6" }],
    memory: [{ id: "2tb-rdimm", label: "2TB ECC RDIMM" }, { id: "3tb-rdimm", label: "3TB ECC RDIMM" }, { id: "4tb-rdimm", label: "4TB ECC RDIMM" }],
    storage: [{ id: "four-nvme", label: "2× boot + 4× enterprise NVMe" }, { id: "eight-nvme", label: "2× boot + 8× enterprise NVMe" }, { id: "storage-review", label: "Project storage configuration" }],
    network: [{ id: "200gb", label: "200GbE / InfiniBand" }, { id: "400gb", label: "400GbE / InfiniBand" }, { id: "800gb-aggregate", label: "800Gb/s aggregate GPU fabric" }, { id: "fabric-review", label: "Project fabric configuration" }],
  },
];

const services: Service[] = [
  { id: "rack", label: "Rack integration", detail: "Mounting, rail and rack-layout coordination", category: "integration" },
  { id: "network", label: "Network & cabling", detail: "Data, management and compute-fabric cabling", category: "integration" },
  { id: "storage", label: "Storage & data integration", detail: "Connect project storage and data paths", category: "integration" },
  { id: "power", label: "Power & cooling integration", detail: "Coordinate circuits, UPS load and thermal scope", category: "integration" },
  { id: "software", label: "OS, drivers & accelerator stack", detail: "CUDA or ROCm stack matched to the platform", category: "software" },
  { id: "cluster", label: "Cluster & management setup", detail: "Configure multi-node management and orchestration", category: "software" },
  { id: "onsite", label: "On-site installation & integration", detail: "Deploy within the agreed customer-site scope", category: "software" },
  { id: "handover", label: "Documentation & handover", detail: "Configuration record and operational handover", category: "support" },
  { id: "support", label: "Ongoing support & maintenance", detail: "Post-deployment assistance under an agreed plan", category: "support" },
];

const serviceCategories = [
  { id: "integration", number: "01", label: "Hardware & integration" },
  { id: "software", number: "02", label: "Software & deployment" },
  { id: "support", number: "03", label: "Handover & after-sales" },
] as const;

const progress = ["Scale", "Platform", "Core", "Foundation", "Power", "Management", "Services", "Review"];
const engineeringSteps = ["Review", "Build & test", "Integrate & deploy"];

const operatingSystemOptions: Option[] = [
  { id: "own-os", label: "Customer-supplied OS · validation required" },
  { id: "ubuntu", label: "Ubuntu Server LTS + GPU software stack" },
  { id: "rhel", label: "Red Hat Enterprise Linux" },
  { id: "windows", label: "Microsoft Windows deployment" },
];

function optionLabel(options: Option[], id: string) {
  return options.find((option) => option.id === id)?.label;
}

function AiInfrastructureSection() {
  const [builderOpen, setBuilderOpen] = useState(false);
  const [resetConfirm, setResetConfirm] = useState(false);
  const [engineeringStep, setEngineeringStep] = useState(0);
  const [scaleId, setScaleId] = useState("");
  const [platformId, setPlatformId] = useState("");
  const [gpuId, setGpuId] = useState("");
  const [cpuId, setCpuId] = useState("");
  const [memoryId, setMemoryId] = useState("");
  const [storageId, setStorageId] = useState("");
  const [networkId, setNetworkId] = useState("");
  const [quantity, setQuantity] = useState("");
  const [systemPlatformId, setSystemPlatformId] = useState("");
  const [powerId, setPowerId] = useState("");
  const [coolingId, setCoolingId] = useState("");
  const [chassisId, setChassisId] = useState("");
  const [operatingSystemId, setOperatingSystemId] = useState("");
  const [managementId, setManagementId] = useState("");
  const [selectedServices, setSelectedServices] = useState<string[]>([]);
  const [coreConfirmed, setCoreConfirmed] = useState(false);
  const [systemConfirmed, setSystemConfirmed] = useState(false);
  const [buildConfirmed, setBuildConfirmed] = useState(false);
  const [managementConfirmed, setManagementConfirmed] = useState(false);
  const [reviewReady, setReviewReady] = useState(false);
  const [requestOpen, setRequestOpen] = useState(false);
  const [requestSubmitting, setRequestSubmitting] = useState(false);
  const [requestError, setRequestError] = useState<string | null>(null);
  const [submittedRequest, setSubmittedRequest] = useState<InfrastructureRequestResponse | null>(null);

  const scale = scales.find((item) => item.id === scaleId);
  const platform = platforms.find((item) => item.id === platformId);
  const availablePlatforms = platforms.filter((item) => item.scales.includes(scaleId));
  const coreReady = Boolean(gpuId && cpuId);
  const systemCount = Number(quantity);
  const systemReady = Boolean(coreReady && memoryId && storageId && networkId && Number.isInteger(systemCount) && systemCount >= 1);

  const systemPlatformOptions = useMemo<Option[]>(() => {
    if (!platform) return [];
    if (platform.id === "tower") {
      if (cpuId === "ryzen-9950x") return [{ id: "x870e", label: "AMD X870E workstation platform" }, { id: "x870", label: "AMD X870 professional platform" }];
      if (cpuId === "threadripper-pro") return [{ id: "wrx90", label: "AMD WRX90 workstation platform" }];
      return [{ id: "w790", label: "Intel W790 workstation platform" }];
    }
    if (platform.id === "rack") {
      const socketLabel = cpuId.startsWith("dual-") ? "dual-socket" : "single-socket";
      return gpuId === "l4" || gpuId === "l40s"
        ? [{ id: "rack-2u", label: `Validated 2U ${socketLabel} GPU server` }, { id: "rack-4u", label: `Validated 4U ${socketLabel} GPU server` }]
        : [{ id: "rack-4u", label: `Validated 4U ${socketLabel} multi-GPU server` }];
    }
    if (gpuId === "hgx-h200" || gpuId === "hgx-b200") return [{ id: "hgx-integrated", label: "HGX integrated 8-GPU server platform" }];
    if (gpuId === "mi350") return [{ id: "instinct-integrated", label: "Instinct integrated 8-GPU server platform" }];
    return [{ id: "pcie-dense", label: "Validated PCIe multi-GPU server platform" }];
  }, [platform, cpuId, gpuId]);

  const powerOptions = useMemo<Option[]>(() => {
    if (!platform) return [];
    if (platform.id === "tower") return gpuId === "rtx-5090"
      ? [{ id: "1200w", label: "1200W 80 PLUS Platinum" }, { id: "1600w", label: "1600W 80 PLUS Platinum" }]
      : [{ id: "1600w", label: "1600W 80 PLUS Platinum" }];
    if (platform.id === "rack") return [{ id: "redundant-rack", label: "OEM-validated redundant hot-swap power" }];
    return [{ id: "redundant-dense", label: "Platform-integrated redundant power" }];
  }, [platform, gpuId]);

  const coolingOptions = useMemo<Option[]>(() => {
    if (!platform) return [];
    if (platform.id === "tower") return cpuId === "ryzen-9950x"
      ? [{ id: "aio-airflow", label: "420mm AIO + validated airflow fan set" }]
      : [{ id: "workstation-thermal", label: "CPU-qualified workstation thermal package" }];
    if (platform.id === "rack") return [{ id: "rack-air", label: "GPU-qualified front-to-back server airflow" }];
    if (gpuId === "mi350") return [{ id: "mi350-air", label: "Integrated MI350X air-cooled platform" }];
    return [{ id: "dense-oem-thermal", label: "OEM-integrated platform cooling" }];
  }, [platform, cpuId, gpuId]);

  const chassisOptions = useMemo<Option[]>(() => {
    if (!platform) return [];
    if (platform.id === "tower") return [{ id: "full-airflow", label: "GPU- and radiator-qualified full tower" }];
    if (platform.id === "rack") return systemPlatformOptions
      .filter((item) => !systemPlatformId || item.id === systemPlatformId)
      .map((item) => ({ id: `${item.id}-chassis`, label: item.label.replace(" platform", " chassis") }));
    return [{ id: "integrated-chassis", label: "Validated integrated multi-GPU chassis" }];
  }, [platform, systemPlatformOptions, systemPlatformId]);

  const managementOptions = useMemo<Option[]>(() => {
    if (!platform) return [];
    if (platform.id === "tower") return [{ id: "os-remote", label: "OS-level remote management" }, { id: "external-monitoring", label: "External power & monitoring integration" }];
    return [{ id: "bmc", label: "Integrated BMC / IPMI management" }, { id: "fleet", label: "BMC + central fleet monitoring" }];
  }, [platform]);

  const buildReady = Boolean(systemPlatformId && powerId && coolingId && chassisId);
  const compatibleOperatingSystems = useMemo(() => {
    if (platform?.id !== "dense") return operatingSystemOptions;
    return operatingSystemOptions.filter((option) => option.id === "own-os" || option.id === "ubuntu" || option.id === "rhel");
  }, [platform]);

  const managementReady = Boolean(operatingSystemId && managementId);

  const compatibleMemory = useMemo(() => {
    if (!platform) return [];
    if (platform.id === "tower" && cpuId === "ryzen-9950x") return [{ id: "128gb", label: "128GB DDR5" }, { id: "256gb-ecc", label: "256GB ECC UDIMM · board-qualified" }];
    if (platform.id === "tower") return [{ id: "256gb-ecc", label: "256GB ECC RDIMM" }, { id: "512gb-ecc", label: "512GB ECC RDIMM" }, { id: "1tb-ecc", label: "1TB ECC RDIMM" }];
    if (platform.id === "rack" && cpuId === "epyc-9005") return platform.memory.filter((item) => ["384gb-rdimm", "768gb-rdimm", "1-5tb-rdimm"].includes(item.id));
    if (platform.id === "rack" && cpuId === "dual-epyc-9005") return platform.memory.filter((item) => ["768gb-rdimm", "1-5tb-rdimm", "3tb-rdimm"].includes(item.id));
    if (platform.id === "rack" && cpuId === "xeon-6") return platform.memory.filter((item) => ["512gb-rdimm", "1tb-rdimm", "2tb-rdimm"].includes(item.id));
    if (platform.id === "rack" && cpuId === "dual-xeon-6") return platform.memory.filter((item) => ["1tb-rdimm", "2tb-rdimm", "4tb-rdimm"].includes(item.id));
    if (platform.id === "dense" && gpuId === "mi350") return platform.memory.filter((item) => item.id === "3tb-rdimm" || item.id === "4tb-rdimm");
    return platform.memory;
  }, [platform, cpuId, gpuId]);

  const compatibleNetwork = useMemo(() => {
    if (!platform) return [];
    if (platform.id === "tower" && cpuId === "ryzen-9950x") return platform.network.filter((item) => item.id !== "100gbe");
    if (platform.id === "dense" && gpuId === "hgx-h200") return platform.network.filter((item) => item.id === "400gb" || item.id === "fabric-review");
    if (platform.id === "dense" && gpuId === "hgx-b200") return platform.network.filter((item) => item.id === "800gb-aggregate" || item.id === "fabric-review");
    if (platform.id === "dense" && gpuId === "mi350") return platform.network.filter((item) => item.id === "400gb" || item.id === "fabric-review");
    return platform.network;
  }, [platform, cpuId, gpuId]);

  const serviceContext = platform?.id === "tower" && systemCount <= 1 ? "Single tower node"
    : platform?.id === "tower" ? "Multi-node system"
      : scaleId === "large" || platform?.id === "dense" ? "Large-scale infrastructure"
        : "Rack deployment";

  const relevantServiceIds = useMemo(() => {
    if (serviceContext === "Single tower node") return ["software", "handover", "support"];
    if (serviceContext === "Multi-node system") return ["network", "storage", "software", "cluster", "onsite", "handover", "support"];
    if (serviceContext === "Rack deployment") return ["rack", "network", "storage", "power", "software", "onsite", "handover", "support"];
    return services.map((service) => service.id);
  }, [serviceContext]);

  const visibleServices = useMemo(() => services.filter((service) => {
    if (service.id === "rack") return platform?.id === "rack" || platform?.id === "dense";
    if (service.id === "power") return platform?.id === "rack" || platform?.id === "dense" || systemCount > 1;
    if (service.id === "network" || service.id === "storage" || service.id === "cluster") return platform?.id !== "tower" || systemCount > 1;
    return true;
  }), [platform, systemCount]);

  function resetAfterPlatform() {
    setGpuId(""); setCpuId(""); setMemoryId(""); setStorageId(""); setNetworkId(""); setQuantity("");
    setSystemPlatformId(""); setPowerId(""); setCoolingId(""); setChassisId(""); setOperatingSystemId(""); setManagementId("");
    setSelectedServices([]); setCoreConfirmed(false); setSystemConfirmed(false); setBuildConfirmed(false); setManagementConfirmed(false); setReviewReady(false);
  }

  function resetAfterCore() {
    setMemoryId(""); setStorageId(""); setNetworkId(""); setQuantity("");
    setSystemPlatformId(""); setPowerId(""); setCoolingId(""); setChassisId(""); setOperatingSystemId(""); setManagementId("");
    setSelectedServices([]); setCoreConfirmed(false); setSystemConfirmed(false); setBuildConfirmed(false); setManagementConfirmed(false); setReviewReady(false);
  }

  function resetAfterFoundation() {
    setSystemPlatformId(""); setPowerId(""); setCoolingId(""); setChassisId(""); setOperatingSystemId(""); setManagementId("");
    setSelectedServices([]); setSystemConfirmed(false); setBuildConfirmed(false); setManagementConfirmed(false); setReviewReady(false);
  }

  function resetAfterBuild() {
    setOperatingSystemId(""); setManagementId(""); setSelectedServices([]);
    setBuildConfirmed(false); setManagementConfirmed(false); setReviewReady(false);
  }

  function chooseScale(nextId: string) {
    if (nextId === scaleId) return;
    setScaleId(nextId);
    setPlatformId("");
    resetAfterPlatform();
  }

  function choosePlatform(nextId: string) {
    if (nextId === platformId) return;
    setPlatformId(nextId);
    resetAfterPlatform();
  }

  function toggleService(id: string) {
    setSelectedServices((current) => current.includes(id) ? current.filter((item) => item !== id) : [...current, id]);
    setReviewReady(false);
  }

  const currentStep = reviewReady ? 8 : managementConfirmed ? 7 : buildConfirmed ? 6 : systemConfirmed ? 5 : coreConfirmed ? 4 : platform ? 3 : scale ? 2 : 1;
  const nextStep = !platform ? ["Choose a compute platform", "The platform controls component density and available hardware."]
    : !coreConfirmed ? ["Confirm GPU and CPU", "Core choices determine the compatible system options that follow."]
      : !systemConfirmed ? ["Complete the foundation", "Memory, storage and networking keep data moving to the compute layer."]
        : !buildConfirmed ? ["Complete platform and power", "Confirm the physical system, power, cooling and chassis."]
          : !managementConfirmed ? ["Choose OS and management", "Define how each system will be operated and monitored."]
            : !reviewReady ? ["Define the service scope", "Choose where JON.PC should support integration and deployment."]
              : ["Engineering review", "Our team confirms availability, compatibility and the agreed integration scope."];

  const summary = useMemo(() => ({
    scale: scale?.label,
    platform: platform?.name,
    gpu: platform ? optionLabel(platform.gpu, gpuId) : undefined,
    cpu: platform ? optionLabel(platform.cpu, cpuId) : undefined,
    memory: optionLabel(compatibleMemory, memoryId),
    storage: platform ? optionLabel(platform.storage, storageId) : undefined,
    network: platform ? optionLabel(platform.network, networkId) : undefined,
    systemPlatform: optionLabel(systemPlatformOptions, systemPlatformId),
    power: optionLabel(powerOptions, powerId),
    cooling: optionLabel(coolingOptions, coolingId),
    chassis: optionLabel(chassisOptions, chassisId),
    operatingSystem: optionLabel(compatibleOperatingSystems, operatingSystemId),
    management: optionLabel(managementOptions, managementId),
  }), [scale, platform, gpuId, cpuId, compatibleMemory, memoryId, storageId, networkId, systemPlatformOptions, systemPlatformId, powerOptions, powerId, coolingOptions, coolingId, chassisOptions, chassisId, compatibleOperatingSystems, operatingSystemId, managementOptions, managementId]);

  const summaryRows = [
    ["Scale", summary.scale], ["Platform", summary.platform], ["GPU", summary.gpu], ["CPU", summary.cpu],
    ["Memory", summary.memory], ["Storage", summary.storage], ["Network", summary.network], ["Systems", quantity],
    ["System platform", summary.systemPlatform], ["Power", summary.power], ["Cooling", summary.cooling], ["Chassis", summary.chassis],
    ["Operating system", summary.operatingSystem], ["Management", summary.management],
  ].filter((row): row is string[] => Boolean(row[1]));

  const selectedServiceLabels = services.filter((service) => selectedServices.includes(service.id)).map((service) => service.label);
  const projectConfiguration = Object.fromEntries([
    ...summaryRows,
    ["Selected services", selectedServiceLabels.length ? selectedServiceLabels.join(", ") : "No optional services selected"],
  ]);
  const passedChecks = [
    "Project scale and physical platform rule passed",
    "CPU, socket count and memory population rule passed",
    "GPU density, power, cooling and network rule passed",
  ];
  const engineeringReviewItems = [
    "Exact manufacturer parts, firmware and current supply",
    "Driver, operating system and accelerator stack versions",
    "Site power, cooling, rack, network and deployment scope",
  ];

  function openRequest() {
    if (!window.localStorage.getItem(authTokenKey)) {
      window.dispatchEvent(new CustomEvent("jonpc:open-account"));
      return;
    }
    setRequestError(null);
    setRequestOpen(true);
  }

  function openBuilder() {
    setResetConfirm(false);
    setBuilderOpen(true);
    window.requestAnimationFrame(() => document.getElementById("ai-infrastructure-builder")?.scrollIntoView({ behavior: "smooth", block: "start" }));
  }

  function closeBuilder() {
    setResetConfirm(false);
    setBuilderOpen(false);
    window.requestAnimationFrame(() => document.getElementById("ai-infrastructure")?.scrollIntoView({ behavior: "smooth", block: "start" }));
  }

  function startProjectOver() {
    if (!resetConfirm) {
      setResetConfirm(true);
      return;
    }
    setScaleId("");
    setPlatformId("");
    resetAfterPlatform();
    setRequestOpen(false);
    setRequestError(null);
    setSubmittedRequest(null);
    setResetConfirm(false);
    window.requestAnimationFrame(() => document.getElementById("ai-infrastructure-builder")?.scrollIntoView({ behavior: "smooth", block: "start" }));
  }

  async function submitRequest(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setRequestSubmitting(true);
    setRequestError(null);
    const form = new FormData(event.currentTarget);
    try {
      const response = await submitInfrastructureRequest({
        organisation: String(form.get("organisation") ?? ""),
        phone: String(form.get("phone") ?? ""),
        location: String(form.get("location") ?? ""),
        notes: String(form.get("notes") ?? ""),
        contact: form.get("contact") === "on",
        configuration: projectConfiguration,
      });
      setSubmittedRequest(response);
      window.dispatchEvent(new CustomEvent("jonpc:order-requested"));
    } catch (error) {
      setRequestError(error instanceof Error ? error.message : "Unable to submit this infrastructure project.");
    } finally {
      setRequestSubmitting(false);
    }
  }

  const summaryContent = <>
    <div className="ai-project-summary-list">
      {summaryRows.map(([label, value]) => <div key={label}><span>{label}</span><strong>{value}</strong></div>)}
    </div>
    <div className="ai-summary-next"><span>Next</span><strong>{nextStep[0]}</strong><p>{nextStep[1]}</p></div>
    {reviewReady && <>
      <div className="ai-project-status"><i aria-hidden="true" /><p><strong>Configured option rules passed.</strong> Exact parts, firmware, power and infrastructure integration require engineering review.</p></div>
      <button className="ai-project-submit" type="button" onClick={openRequest}>Submit for engineering review <span aria-hidden="true">↗</span></button>
    </>}
  </>;

  return (
    <section className="ai-infrastructure" id="ai-infrastructure" aria-labelledby="ai-infrastructure-title">
      <header className="ai-infrastructure-hero">
        <div><span className="section-kicker">JON. AI Infrastructure</span><h2 id="ai-infrastructure-title">Your AI infrastructure.<br /><em>Built to scale.</em></h2></div>
        <div className="ai-infrastructure-intro"><p>Configure the AI systems your business needs. JON.PC reviews, builds, integrates and deploys the approved project.</p><button className="button button-primary ai-infrastructure-cta" type="button" onClick={openBuilder}>{scaleId ? "Continue your infrastructure" : "Build your infrastructure"} <span aria-hidden="true">↗</span></button></div>
      </header>

      <section className="ai-engineering-band" aria-labelledby="ai-engineering-title">
        <div className="ai-engineering-image"><img src={teamImage} alt="JON.PC engineers working with rack-mounted compute hardware" /></div>
        <div className="ai-engineering-panel">
          <div className="ai-engineering-copy"><span className="section-kicker">JON.PC Engineering</span><h3 id="ai-engineering-title">Selected by you.<br /><em>Delivered by our team.</em></h3></div>
          <div className="ai-engineering-points" aria-label="Engineering process">
            {engineeringSteps.map((step, index) => (
              <button className={engineeringStep === index ? "ai-engineering-point-active" : ""} type="button" key={step} onClick={() => setEngineeringStep(index)} onMouseEnter={() => setEngineeringStep(index)} onFocus={() => setEngineeringStep(index)} aria-pressed={engineeringStep === index}>
                <b>{String(index + 1).padStart(2, "0")}</b>
                <span>{step}</span>
              </button>
            ))}
          </div>
        </div>
      </section>

      {builderOpen && <>
        <div className="ai-configurator-toolbar"><div className="ai-configurator-session"><span>Infrastructure configurator</span><strong>{scaleId ? `Project in progress · Step ${String(currentStep).padStart(2, "0")}` : "Ready to begin"}</strong></div><div className="ai-configurator-actions"><button className="ai-control-button" type="button" onClick={closeBuilder}><i aria-hidden="true">—</i><span><strong>Pause & close</strong><small>Keep your selections</small></span></button><button className={resetConfirm ? "ai-control-button ai-control-reset ai-control-confirm" : "ai-control-button ai-control-reset"} type="button" onClick={startProjectOver}><i aria-hidden="true">↺</i><span><strong>{resetConfirm ? "Confirm reset" : "Start over"}</strong><small>{resetConfirm ? "Click again to clear" : "Clear this project"}</small></span></button></div></div>
        <div className="ai-configurator-steps" aria-label="Infrastructure configurator progress">
          {progress.map((label, index) => <span className={currentStep >= index + 1 ? "ai-progress-active" : ""} key={label}><b>0{index + 1}</b>{label}</span>)}
        </div>

        {scaleId && <div className="ai-project-mobile"><details><summary><span>Your project</span><strong>{summary.platform ?? summary.scale}</strong></summary><div className="ai-mobile-team"><img src={reviewImage} alt="JON.PC engineers reviewing an infrastructure plan" /><span>Engineering review · Project planning</span></div>{summaryContent}</details></div>}

        <div className={scaleId ? "ai-infrastructure-builder" : "ai-infrastructure-builder ai-builder-start"} id="ai-infrastructure-builder">
        <div className="ai-builder-main">
          <section className="ai-builder-step" aria-labelledby="ai-scale-title">
            <div className="ai-builder-step-heading"><span className="section-kicker">Step 01 / Project scale</span><h3 id="ai-scale-title">What are you building?</h3><p>Scale sets the project scope, not system performance.</p></div>
            <div className="ai-scale-grid">{scales.map((item) => <button key={item.id} type="button" className={scaleId === item.id ? "ai-choice-card ai-choice-card-active" : "ai-choice-card"} onClick={() => chooseScale(item.id)} aria-pressed={scaleId === item.id}><span>{item.number}</span><strong>{item.label}</strong><small>{item.detail}</small><i aria-hidden="true">↗</i></button>)}</div>
          </section>

          {scaleId && <section className="ai-builder-step" aria-labelledby="ai-platform-title">
            <div className="ai-builder-step-heading"><span className="section-kicker">Step 02 / Physical platform</span><h3 id="ai-platform-title">How should each system be built?</h3><p>Platform sets component density and available hardware.</p></div>
            <div className={`ai-platform-grid ai-platform-count-${availablePlatforms.length}`}>{availablePlatforms.map((item) => <button key={item.id} type="button" className={platformId === item.id ? "ai-choice-card ai-choice-card-active" : "ai-choice-card"} onClick={() => choosePlatform(item.id)} aria-pressed={platformId === item.id}><span>{item.number}</span><strong>{item.name}</strong><small>{item.detail}</small><i aria-hidden="true">↗</i></button>)}</div>
            <p className="ai-compatibility-note"><i aria-hidden="true" />Only platforms suited to the selected project scale are shown.</p>
          </section>}

          {platform && <section className="ai-builder-step" aria-labelledby="ai-core-title">
            <div className="ai-builder-step-heading"><span className="section-kicker">Step 03 / Core compute</span><h3 id="ai-core-title">Choose the compute engine.</h3><p>GPU drives AI compute; CPU supports data flow and I/O.</p></div>
            <div className="ai-hardware-grid">
              <label><span>GPU configuration</span><select value={gpuId} onChange={(event) => { setGpuId(event.target.value); resetAfterCore(); }}><option value="" disabled>Select GPU capacity</option>{platform.gpu.map((option) => <option value={option.id} key={option.id}>{option.label}</option>)}</select></label>
              <label><span>CPU platform</span><select value={cpuId} onChange={(event) => { setCpuId(event.target.value); resetAfterCore(); }}><option value="" disabled>Select CPU platform</option>{platform.cpu.map((option) => <option value={option.id} key={option.id}>{option.label}</option>)}</select></label>
            </div>
            {gpuId === "rtx-5090" && <p className="ai-compatibility-note"><i aria-hidden="true" />Consumer GPU deployment and software licensing require confirmation before rollout.</p>}
            <div className="ai-step-action"><span>{[gpuId, cpuId].filter(Boolean).length} / 2 selected</span><button className="button button-primary ai-step-continue" type="button" onClick={() => setCoreConfirmed(true)} disabled={!coreReady}>{coreReady ? "Continue to foundation" : "Select GPU and CPU"} <span aria-hidden="true">↓</span></button></div>
          </section>}

          {coreConfirmed && platform && <section className="ai-builder-step" aria-labelledby="ai-system-title">
            <div className="ai-builder-step-heading"><span className="section-kicker">Step 04 / System foundation</span><h3 id="ai-system-title">Keep the compute layer supplied.</h3><p>Memory, storage and network keep accelerators supplied.</p></div>
            <div className="ai-hardware-grid">
              <label><span>System memory</span><select value={memoryId} onChange={(event) => { setMemoryId(event.target.value); resetAfterFoundation(); }}><option value="" disabled>Select memory capacity</option>{compatibleMemory.map((option) => <option value={option.id} key={option.id}>{option.label}</option>)}</select></label>
              <label><span>Local storage</span><select value={storageId} onChange={(event) => { setStorageId(event.target.value); resetAfterFoundation(); }}><option value="" disabled>Select local storage</option>{platform.storage.map((option) => <option value={option.id} key={option.id}>{option.label}</option>)}</select></label>
              <label><span>Network</span><select value={networkId} onChange={(event) => { setNetworkId(event.target.value); resetAfterFoundation(); }}><option value="" disabled>Select network fabric</option>{compatibleNetwork.map((option) => <option value={option.id} key={option.id}>{option.label}</option>)}</select></label>
              <label><span>System quantity</span><input type="number" min="1" inputMode="numeric" value={quantity} placeholder="Enter 1 or more" onChange={(event) => { setQuantity(event.target.value); resetAfterFoundation(); }} /></label>
            </div>
            <p className="ai-compatibility-note"><i aria-hidden="true" />One system is supported. Quantity adjusts the project and service scope.</p>
            <p className="ai-compatibility-note"><i aria-hidden="true" />Options are filtered by platform, socket count and GPU density. Exact part numbers require engineering review.</p>
            <div className="ai-step-action"><span>{[memoryId, storageId, networkId, quantity].filter(Boolean).length} / 4 selected</span><button className="button button-primary ai-step-continue" type="button" onClick={() => setSystemConfirmed(true)} disabled={!systemReady}>{systemReady ? "Continue to platform & power" : "Complete all four fields"} <span aria-hidden="true">↓</span></button></div>
          </section>}

          {systemConfirmed && <section className="ai-builder-step" aria-labelledby="ai-power-title">
            <div className="ai-builder-step-heading"><span className="section-kicker">Step 05 / Platform & power</span><h3 id="ai-power-title">Complete the physical system.</h3><p>Match the platform, power, cooling and chassis to the selected compute.</p></div>
            <div className="ai-hardware-grid">
              <label><span>System platform</span><select value={systemPlatformId} onChange={(event) => { setSystemPlatformId(event.target.value); setChassisId(""); resetAfterBuild(); }}><option value="" disabled>Select system platform</option>{systemPlatformOptions.map((option) => <option value={option.id} key={option.id}>{option.label}</option>)}</select></label>
              <label><span>Power design</span><select value={powerId} onChange={(event) => { setPowerId(event.target.value); resetAfterBuild(); }}><option value="" disabled>Select power design</option>{powerOptions.map((option) => <option value={option.id} key={option.id}>{option.label}</option>)}</select></label>
              <label><span>Cooling design</span><select value={coolingId} onChange={(event) => { setCoolingId(event.target.value); resetAfterBuild(); }}><option value="" disabled>Select cooling design</option>{coolingOptions.map((option) => <option value={option.id} key={option.id}>{option.label}</option>)}</select></label>
              <label><span>Chassis</span><select value={chassisId} onChange={(event) => { setChassisId(event.target.value); resetAfterBuild(); }}><option value="" disabled>Select chassis</option>{chassisOptions.map((option) => <option value={option.id} key={option.id}>{option.label}</option>)}</select></label>
            </div>
            {platform?.id === "tower" && <p className="ai-compatibility-note"><i aria-hidden="true" />Tower systems are not rack-mountable. Placement, circuit load and room cooling require project review.</p>}
            <p className="ai-compatibility-note"><i aria-hidden="true" />Exact motherboard, chassis and PSU models are confirmed against availability before ordering.</p>
            <div className="ai-step-action"><span>{[systemPlatformId, powerId, coolingId, chassisId].filter(Boolean).length} / 4 selected</span><button className="button button-primary ai-step-continue" type="button" onClick={() => setBuildConfirmed(true)} disabled={!buildReady}>{buildReady ? "Continue to management" : "Complete all four fields"} <span aria-hidden="true">↓</span></button></div>
          </section>}

          {buildConfirmed && <section className="ai-builder-step" aria-labelledby="ai-management-title">
            <div className="ai-builder-step-heading"><span className="section-kicker">Step 06 / Software & management</span><h3 id="ai-management-title">Define how the systems operate.</h3><p>Choose the operating environment and remote management level.</p></div>
            <div className="ai-hardware-grid">
              <label><span>Operating system</span><select value={operatingSystemId} onChange={(event) => { setOperatingSystemId(event.target.value); setManagementConfirmed(false); setSelectedServices([]); setReviewReady(false); }}><option value="" disabled>Select operating system</option>{compatibleOperatingSystems.map((option) => <option value={option.id} key={option.id}>{option.label}</option>)}</select></label>
              <label><span>Remote management</span><select value={managementId} onChange={(event) => { setManagementId(event.target.value); setManagementConfirmed(false); setSelectedServices([]); setReviewReady(false); }}><option value="" disabled>Select management level</option>{managementOptions.map((option) => <option value={option.id} key={option.id}>{option.label}</option>)}</select></label>
            </div>
            <p className="ai-compatibility-note"><i aria-hidden="true" />Only suitable OS families are shown. Exact versions are matched to the selected platform and driver release.</p>
            <div className="ai-step-action"><span>{[operatingSystemId, managementId].filter(Boolean).length} / 2 selected</span><button className="button button-primary ai-step-continue" type="button" onClick={() => setManagementConfirmed(true)} disabled={!managementReady}>{managementReady ? "Continue to services" : "Complete both fields"} <span aria-hidden="true">↓</span></button></div>
          </section>}

          {managementConfirmed && <section className="ai-builder-step" aria-labelledby="ai-services-title">
            <div className="ai-builder-step-heading"><span className="section-kicker">Step 07 / JON.PC services</span><h3 id="ai-services-title">Where should our team step in?</h3><p>Review, build and testing are included. Add the services your project needs.</p></div>
            <div className="ai-included-services"><span>Included with every build</span><strong>Engineering review</strong><strong>Professional assembly</strong><strong>Hardware validation</strong></div>
            <div className="ai-service-context"><span>Shown for your project</span><strong>{serviceContext}</strong><p>Relevant services are brought forward. Nothing is selected automatically.</p></div>
            <div className="ai-service-sections">{serviceCategories.map((category) => {
              const categoryServices = visibleServices.filter((service) => service.category === category.id);
              if (!categoryServices.length) return null;
              return <section key={category.id} className="ai-service-section" aria-labelledby={`ai-service-${category.id}`}><h4 id={`ai-service-${category.id}`}><span>{category.number}</span>{category.label}</h4><div className="ai-service-grid">{categoryServices.map((service) => {
                const selected = selectedServices.includes(service.id);
                const relevant = relevantServiceIds.includes(service.id);
                return <label key={service.id} className={`ai-service-option${selected ? " ai-service-option-active" : ""}${relevant ? " ai-service-option-relevant" : ""}`}><input type="checkbox" checked={selected} onChange={() => toggleService(service.id)} /><span><strong>{service.label}</strong><small>{service.detail}</small></span><i aria-hidden="true">{selected ? "×" : "+"}</i></label>;
              })}</div></section>;
            })}</div>
            <button className="button button-primary ai-review-button" type="button" onClick={() => setReviewReady(true)}>Review this project <span aria-hidden="true">↗</span></button>
          </section>}

          {reviewReady && <section className="ai-builder-step ai-final-review" aria-labelledby="ai-review-title">
            <div className="ai-builder-step-heading"><span className="section-kicker">Step 08 / Project review</span><h3 id="ai-review-title">Your complete project.<br /><em>Ready to submit.</em></h3><p>Confirm the configuration, checks and engineering-review scope below.</p></div>
            <div className="ai-review-layout">
              <div className="ai-review-configuration"><div className="ai-review-section-heading"><span>Complete configuration</span><small>Return to any earlier step to make changes.</small></div>{summaryRows.map(([label, value]) => <div className="ai-review-row" key={label}><span>{label}</span><strong>{value}</strong></div>)}<div className="ai-review-row"><span>Optional services</span><strong>{selectedServiceLabels.length ? selectedServiceLabels.join(" · ") : "None selected"}</strong></div></div>
              <aside className="ai-review-validation"><section><span>Rule checks passed</span>{passedChecks.map((check) => <p key={check}><i aria-hidden="true">✓</i>{check}</p>)}</section><section className="ai-review-required"><span>Engineering review required</span>{engineeringReviewItems.map((item) => <p key={item}><i aria-hidden="true">!</i>{item}</p>)}</section></aside>
            </div>
            <div className="ai-review-submit"><div><strong>Submit this project to JON.PC</strong><p>Our team will review scope, availability and pricing before sending a final proposal to My Orders.</p></div><button className="button button-primary" type="button" onClick={openRequest}>Submit for engineering review <span aria-hidden="true">↗</span></button></div>
          </section>}
        </div>

        {scaleId && <aside className="ai-project-summary" aria-label="Current AI infrastructure project">
          <div className="ai-team-image"><img src={reviewImage} alt="JON.PC engineers reviewing an infrastructure plan" /></div>
          <div className="ai-team-strip"><span>Engineering review</span><strong>Plan · Validate · Confirm scope</strong></div>
          <span className="section-kicker">Your project / Live</span><h3>{summary.platform ?? summary.scale}</h3>{summaryContent}
        </aside>}
        </div>
      </>}

      {requestOpen && <div className="ai-request-backdrop" role="presentation" onMouseDown={(event) => event.target === event.currentTarget && setRequestOpen(false)}><section className="ai-request-modal" role="dialog" aria-modal="true" aria-labelledby="ai-request-title"><button className="ai-request-close" type="button" onClick={() => setRequestOpen(false)} aria-label="Close infrastructure request">×</button>{submittedRequest ? <div className="ai-request-success"><span className="section-kicker">Project received</span><h3 id="ai-request-title">Engineering review started.</h3><p>Your project is now visible to the JON.PC team and in My Orders.</p><div><span>Reference</span><strong>{submittedRequest.requestReference}</strong></div><button className="button button-primary" type="button" onClick={() => setRequestOpen(false)}>Done <span aria-hidden="true">↗</span></button></div> : <form onSubmit={submitRequest}><span className="section-kicker">JON.PC / Infrastructure request</span><h3 id="ai-request-title">Send your complete project.</h3><p>Tell us who and where the project is for. Your selected configuration is attached automatically.</p><div className="ai-request-project"><span>{summary.scale} / {summary.platform}</span><strong>{quantity} system{quantity === "1" ? "" : "s"} · {summary.gpu}</strong></div><div className="ai-request-fields"><label><span>Organisation</span><input name="organisation" required /></label><label><span>Project location</span><input name="location" placeholder="Melbourne, VIC" required /></label><label><span>Phone <em>Optional</em></span><input name="phone" type="tel" /></label></div><label className="ai-request-wide"><span>Project notes <em>Optional</em></span><textarea name="notes" rows={4} placeholder="Timeline, site constraints or anything our engineers should know." /></label><label className="ai-request-check"><input name="contact" type="checkbox" defaultChecked /><span>Contact me about this infrastructure project.</span></label>{requestError && <p className="ai-request-error" role="alert">{requestError}</p>}<button className="button button-primary ai-request-button" type="submit" disabled={requestSubmitting}>{requestSubmitting ? "Submitting project..." : "Confirm and submit project"} <span aria-hidden="true">↗</span></button><small>Submission starts an engineering review. It is not a confirmed order or final quote.</small></form>}</section></div>}
    </section>
  );
}

export default AiInfrastructureSection;
