import "./BrandLockup.css";

type BrandLockupProps = {
  compact?: boolean;
};

function BrandLockup({ compact = false }: BrandLockupProps) {
  return (
    <span className={compact ? "brand-lockup brand-lockup-compact" : "brand-lockup"}>
      <span className="brand-product">JON<span className="brand-dot">.</span> PC</span>
      <span className="brand-company">by AI Cyber</span>
    </span>
  );
}

export default BrandLockup;
