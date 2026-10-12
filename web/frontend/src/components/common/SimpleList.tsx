type Props = {
  title: string;
  items: string[];
};

export default function SimpleList({ title, items }: Props) {
  return (
    <section className="panel">
      <div className="section-heading compact"><span>Preview</span><h2>{title}</h2></div>
      {items.length ? <ul className="clean-list">{items.map((item) => <li key={item}>{item}</li>)}</ul> : <div className="empty-state">Nessun dato disponibile.</div>}
    </section>
  );
}
