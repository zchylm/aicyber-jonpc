import officialLogo from "../assets/jonpc-official-logo.png";
import "./BrandLockup.css";

type BrandLockupProps = {
  compact?: boolean;
};

function BrandLockup({ compact = false }: BrandLockupProps) {
  return (
    <span className={compact ? "brand-lockup brand-lockup-compact" : "brand-lockup"}>
      <img className="brand-logo" src={officialLogo} alt="JON PC AI Technology" draggable={false} />
    </span>
  );
}

export default BrandLockup;
