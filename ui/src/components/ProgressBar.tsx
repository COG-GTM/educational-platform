interface ProgressBarProps {
  percent: number;
  label: string;
}

export default function ProgressBar({ percent, label }: ProgressBarProps) {
  const value = Math.max(0, Math.min(100, Math.round(percent)));
  return (
    <div className="progress-bar" role="progressbar" aria-label={label} aria-valuemin={0} aria-valuemax={100} aria-valuenow={value}>
      <div className="progress-bar-fill" style={{ width: `${value}%` }} />
    </div>
  );
}
