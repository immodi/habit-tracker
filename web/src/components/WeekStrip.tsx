import type { DayCell } from '../lib/streaks';

interface WeekStripProps {
  cells: DayCell[];
  today: string;
}

export function WeekStrip({ cells, today }: WeekStripProps) {
  return (
    <span className="week-strip" aria-hidden="true">
      {cells.map((cell) => (
        <span
          key={cell.date}
          className={`dot ${cell.status}${cell.date === today ? ' is-today' : ''}`}
        />
      ))}
    </span>
  );
}
