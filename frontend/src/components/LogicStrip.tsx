import { useState } from "react";

const logicSteps = [
  { number: "01", title: "Choose your goal" },
  { number: "02", title: "Get a recommendation" },
  { number: "03", title: "Customise" },
  { number: "04", title: "Validate & build" },
];

function LogicStrip() {
  const [activeStep, setActiveStep] = useState(0);

  return (
    <section className="logic-strip" aria-labelledby="logic-title">
      <div className="logic-strip-heading">
        <span id="logic-title">JON.PC LOGIC</span>
        <i aria-hidden="true" />
      </div>
      <ol className="logic-strip-steps">
        {logicSteps.map((step, index) => (
          <li className={activeStep === index ? "logic-step-active" : ""} key={step.number}>
            <button type="button" onClick={() => setActiveStep(index)} onMouseEnter={() => setActiveStep(index)} onFocus={() => setActiveStep(index)} aria-pressed={activeStep === index}>
              <span className="logic-step-number">{step.number}</span>
              <strong>{step.title}</strong>
              <span className="logic-step-node" aria-hidden="true" />
            </button>
          </li>
        ))}
      </ol>
    </section>
  );
}

export default LogicStrip;
