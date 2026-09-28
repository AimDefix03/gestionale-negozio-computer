type Props = {
  label: string;
  value: string;
  caption: string;
};

export default function StatCard({ label, value, caption }: Props) {
  return <article className="stat-card"><span>{label}</span><strong>{value}</strong><p>{caption}</p></article>;
}
