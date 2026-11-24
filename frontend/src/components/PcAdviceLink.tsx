import "./PcAdviceLink.css";

type Props = {
  category?: "gaming" | "mini";
  product?: { name: string; ram?: number };
};

const supportEmail = "support@jonpc.com.au";

export default function PcAdviceLink({ category = "gaming", product }: Props) {
  const isMini = category === "mini";
  const system = isMini ? "Mini PC" : "PC";
  const title = product ? `Ask us about this ${system}` : isMini ? "Choosing a Mini PC?" : "Need help choosing a PC?";
  const ram = product?.ram ? ` (${product.ram}GB RAM)` : "";
  const subject = product ? `A question about the ${product.name}${ram}` : `Help choosing a ${isMini ? "Mini PC" : "gaming PC"}`;
  const body = "Hi JON.PC team,\r\n\r\n";
  const href = `mailto:${supportEmail}?subject=${encodeURIComponent(subject)}&body=${encodeURIComponent(body)}`;

  return <a className={`pc-advice-card${isMini ? " pc-advice-card-mini" : ""}${product ? " pc-advice-card-compact" : ""}`} href={href}>
    <span className="pc-advice-icon" aria-hidden="true"><span /></span>
    <span className="pc-advice-copy">
      <span className="pc-advice-title">{title}</span>
      <span className="pc-advice-detail">{product ? "Email our team" : isMini ? "Email us about your setup." : "Email our team for a second opinion."}</span>
    </span>
    <span className="pc-advice-arrow" aria-hidden="true">↗</span>
  </a>;
}
