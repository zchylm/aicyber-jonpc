import { useState } from "react";
import heroImage from "../assets/jon-pc-hero.png";
import hardwareImage from "../assets/jon-pc-hardware-detail.png";
import wideHeroImage from "../assets/jon-pc-hero-wide.png";
import gamingImage from "../assets/jon-pc-gaming.png";
import creatorImage from "../assets/jon-pc-creator.png";
import enterpriseImage from "../assets/jon-pc-enterprise.png";
import gaming01 from "../assets/recommendation-slides/gaming-01.jpeg";
import gaming02 from "../assets/recommendation-slides/gaming-02.jpeg";
import gaming03 from "../assets/recommendation-slides/gaming-03.jpeg";
import ai01 from "../assets/recommendation-slides/ai-01.jpg";
import ai02 from "../assets/recommendation-slides/ai-02.jpeg";
import ai03 from "../assets/recommendation-slides/ai-03.jpeg";
import creator01 from "../assets/recommendation-slides/creator-01.jpg";
import creator02 from "../assets/recommendation-slides/creator-02.jpg";
import creator03 from "../assets/recommendation-slides/creator-03.jpg";
import workstation01 from "../assets/recommendation-slides/workstation-01.jpg";
import workstation02 from "../assets/recommendation-slides/workstation-02.jpeg";
import workstation03 from "../assets/recommendation-slides/workstation-03.jpeg";
import custom01 from "../assets/recommendation-slides/custom-01.jpeg";
import custom02 from "../assets/recommendation-slides/custom-02.jpeg";
import custom03 from "../assets/recommendation-slides/custom-03.jpg";
import { focusAreas, systemDetails, systemRecommendations } from "../data/site";

type FocusId = keyof typeof systemRecommendations;
type Build = (typeof systemRecommendations)[FocusId]["builds"][number];

const visualMap = {
  hero: heroImage,
  hardware: hardwareImage,
  wide: wideHeroImage,
  gaming: gamingImage,
  creator: creatorImage,
  enterprise: enterpriseImage,
  gaming01,
  gaming02,
  gaming03,
  ai01,
  ai02,
  ai03,
  creator01,
  creator02,
  creator03,
  workstation01,
  workstation02,
  workstation03,
  custom01,
  custom02,
  custom03,
};

function SystemsSection() {
  const [activeFocus, setActiveFocus] = useState<FocusId>("gaming");
  const [selectedBuild, setSelectedBuild] = useState<Build | null>(null);
  const recommendation = systemRecommendations[activeFocus];

  return (
    <section className="systems-section" id="systems" aria-labelledby="systems-title">
      <div className="systems-heading">
        <div>
          <span className="section-kicker">Find your direction</span>
          <h2 id="systems-title">The right system<br /><em>starts with you.</em></h2>
        </div>
        <p>Choose how you work, play or create. JON. PC will point you towards a system that makes sense for the way you use it.</p>
      </div>

      <div className="systems-selector" role="tablist" aria-label="Choose a system category">
        <span className="systems-selector-label">Choose your direction</span>
        <div className="systems-tabs">
          {focusAreas.map((area, index) => {
            const isActive = activeFocus === area.id;
            return (
              <button
                className={isActive ? "system-tab system-tab-active" : "system-tab"}
                key={area.id}
                type="button"
                role="tab"
                aria-selected={isActive}
                onClick={() => setActiveFocus(area.id as FocusId)}
              >
                <span className="focus-number">0{index + 1}</span>
                <span>{area.label}</span>
                <span className="focus-arrow" aria-hidden="true">↗</span>
              </button>
            );
          })}
        </div>
      </div>

      <div className="recommendation-heading">
        <div>
          <span className="section-kicker">{recommendation.kicker}</span>
          <h3>{recommendation.title}</h3>
        </div>
        <p>{recommendation.description}</p>
      </div>

      <div className="build-grid" role="tabpanel">
        {recommendation.builds.map((build) => (
          <article className="build-card" key={build.name}>
            <div className={`build-card-image build-card-image-${build.image}`}>
              <img src={visualMap[build.image]} alt="" />
              <span className="build-card-index">JON. PC / {recommendation.kicker.split(" /")[0]}</span>
            </div>
            <div className="build-card-content">
              <div className="build-card-title-row">
                <div>
                  <h4>{build.name}</h4>
                  <span>{build.tier}</span>
                </div>
                <strong>{build.price}</strong>
              </div>
              <ul className="build-specs">
                {build.specs.map((spec) => <li key={spec}>{spec}</li>)}
              </ul>
              <div className="build-card-actions">
                <a className="build-link build-link-primary" href="#build">Customise build <span aria-hidden="true">↗</span></a>
                <button className="build-link build-link-button" type="button" onClick={() => setSelectedBuild(build)}>
                  View details <span aria-hidden="true">↓</span>
                </button>
              </div>
            </div>
          </article>
        ))}
      </div>

      {selectedBuild && (
        <SystemDetail
          build={selectedBuild}
          detail={systemDetails[selectedBuild.name as keyof typeof systemDetails]}
          image={visualMap[selectedBuild.image]}
          onClose={() => setSelectedBuild(null)}
        />
      )}

      <p className="pricing-note">Indicative pricing in AUD. Final pricing varies with component availability and configuration.</p>
    </section>
  );
}

type SystemDetailProps = {
  build: Build;
  detail: (typeof systemDetails)[keyof typeof systemDetails];
  image: string;
  onClose: () => void;
};

function SystemDetail({ build, detail, image, onClose }: SystemDetailProps) {
  return (
    <section className="system-detail" aria-labelledby="system-detail-title">
      <div className="system-detail-header">
        <div>
          <span className="section-kicker">Configuration / {build.name}</span>
          <h3 id="system-detail-title">{detail.summary}</h3>
        </div>
        <button className="detail-close" type="button" onClick={onClose} aria-label="Close system details" title="Close details">×</button>
      </div>

      <div className="system-detail-body">
        <div className="system-detail-overview">
          <div className="system-detail-image"><img src={image} alt={`${build.name} configuration`} /></div>
          <div className="system-detail-price">
            <span>Indicative build price</span>
            <strong>{build.price}</strong>
          </div>
          <div className="system-detail-performance">
            <span>Designed for</span>
            <strong>{detail.performance}</strong>
          </div>
          <a className="button button-primary detail-cta" href="#build">Customise this build <span aria-hidden="true">↗</span></a>
        </div>

        <div className="component-list">
          <div className="component-list-heading">
            <span className="section-kicker">Full system specification</span>
            <span>Why each part is here</span>
          </div>
          {detail.components.map((component) => (
            <div className="component-row" key={component.label}>
              <div className="component-label">{component.label}</div>
              <div className="component-value"><strong>{component.value}</strong><span>{component.role}</span></div>
              <p>{component.reason}</p>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}

export default SystemsSection;
