import { useEffect, useState } from "react";
import { fetchSystemPreviews, type PreviewSystem } from "../api/systems";
import FirstBatchCatalog from "./FirstBatchCatalog";

function SystemsSection() {
  const [realSystems, setRealSystems] = useState<PreviewSystem[] | null | undefined>(undefined);

  useEffect(() => {
    let active = true;
    const reload = () => void fetchSystemPreviews().then((systems) => { if (active) setRealSystems(systems); }).catch(() => { if (active) setRealSystems(null); });
    reload();
    window.addEventListener("jonpc:payment-completed", reload);
    window.addEventListener("jonpc:system-stock-changed", reload);
    return () => { active = false; window.removeEventListener("jonpc:payment-completed", reload); window.removeEventListener("jonpc:system-stock-changed", reload); };
  }, []);

  if (realSystems === undefined) return <section className="systems-section" id="systems" aria-busy="true"><span className="section-kicker">Find your direction</span><h2 className="systems-loading-title">Your next system starts here.</h2></section>;
  if (realSystems?.length) return <FirstBatchCatalog systems={realSystems} />;
  return <section className="systems-section" id="systems"><span className="section-kicker">JON. PC / Systems</span><h2 className="systems-loading-title">Systems are temporarily unavailable.</h2><p>Please try again shortly.</p></section>;
}

export default SystemsSection;
