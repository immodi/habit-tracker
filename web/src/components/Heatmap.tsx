import type { DayCell } from '../lib/streaks';
import { formatFullDate } from '../lib/dates';

interface HeatmapProps {
  cells: Array<DayCell | null>;
  today: string;
  onToggle: (date: string, done: boolean) => void;
}

const STATUS_LABEL: Record<DayCell['status'], string> = {
  done: 'done',
  missed: 'missed',
  empty: 'no check-in',
  off: 'not scheduled',
  future: 'upcoming',
};

export function Heatmap({ cells, today, onToggle }: HeatmapProps) {
  const weeks = Math.ceil(cells.length / 7);

  return (
    <div className="heatmap-wrap">
      <div
        className="heatmap"
        style={{ gridTemplateColumns: `repeat(${weeks}, minmax(0, 1fr))` }}
      >
        {cells.map((cell, index) => {
          if (!cell) {
            return <span key={`blank-${index}`} className="cell is-empty" aria-hidden="true" />;
          }
          const disabled = !cell.scheduled || cell.status === 'future';
          return (
            <button
              key={cell.date}
              type="button"
              className={`cell ${cell.status}${cell.date === today ? ' is-today' : ''}`}
              disabled={disabled}
              data-date={cell.date}
              aria-label={`${formatFullDate(cell.date)}, ${STATUS_LABEL[cell.status]}`}
              title={`${formatFullDate(cell.date)} · ${STATUS_LABEL[cell.status]}`}
              onClick={() => onToggle(cell.date, !cell.done)}
            />
          );
        })}
      </div>
    </div>
  );
}

export function HeatmapLegend({ flexible = false }: { flexible?: boolean }) {
  return (
    <div className="legend" aria-hidden="true">
      <span className="legend-item">
        <span className="legend-swatch done" />
        Done
      </span>
      {flexible ? (
        <>
          <span className="legend-item">
            <span className="legend-swatch empty" />
            No check-in
          </span>
          <span className="legend-item">
            <span className="legend-swatch off" />
            Before start
          </span>
        </>
      ) : (
        <>
          <span className="legend-item">
            <span className="legend-swatch missed" />
            Missed
          </span>
          <span className="legend-item">
            <span className="legend-swatch off" />
            Not scheduled
          </span>
        </>
      )}
    </div>
  );
}
